import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.codeberg.anari.api.Array1D;
import org.codeberg.anari.api.Array2D;
import org.codeberg.anari.api.DataType;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Group;
import org.codeberg.anari.api.Instance;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Sampler;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.AnariPane;
import org.codeberg.anari.javafx.TimerState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JanariApp extends Application {
    private static String modelName = "BoxEm";

    @Override
    public void start(Stage primaryStage) {
        try {
            AnariPane anariPane = new AnariPane();
            String raw = getParameters().getRaw().isEmpty() ? modelName : getParameters().getRaw().get(0);
            String which = raw.replaceAll("\\.(java|x3d|json)$", "").replaceAll("/", ".");

            net.coderextreme.X3DRoots roots = null;
            List<String> candidateClasses = List.of(
                which,
                "net.coderextreme.data." + which,
                "net.coderextreme." + which
            );

            for (String cName : candidateClasses) {
                try {
                    Class<?> clazz = Class.forName(cName);
                    roots = (net.coderextreme.X3DRoots) clazz.getDeclaredConstructor().newInstance();
                    modelName = cName;
                    break;
                } catch (ClassNotFoundException ignored) {
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            if (roots == null) {
                System.err.println("Could not load class for: " + which + ". Falling back to BoxEm.");
                roots = new net.coderextreme.data.BoxEm();
                modelName = "net.coderextreme.data.BoxEm";
            }

            System.out.println("Loading model: " + modelName);
            org.web3d.x3d.jsail.Core.X3D x3dModel = roots.getRootNodeList().get(0);
            X3DAnariHandler handler = new X3DAnariHandler(x3dModel);
            anariPane.setHandler(handler);

            for (String mName : List.of("setAnimated", "startAnimation", "setContinuous", "start")) {
                try {
                    Method enableAnimMethod = anariPane.getClass().getMethod(mName, boolean.class);
                    enableAnimMethod.invoke(anariPane, true);
                    System.out.println("Configured AnariPane." + mName + "(true)");
                    break;
                } catch (Exception ignored) {}
            }

            AnimationTimer animTimer = new AnimationTimer() {
                private Method repaintMethod = null;
                private boolean methodSearched = false;

                @Override
                public void handle(long now) {
                    if (!methodSearched) {
                        methodSearched = true;
                        for (String mName : List.of("requestRender", "requestRepaint", "renderLater", "repaint", "render")) {
                            try {
                                repaintMethod = anariPane.getClass().getMethod(mName);
                                System.out.println("Animation loop bound to: AnariPane." + mName + "()");
                                break;
                            } catch (Exception ignored) {}
                        }
                        if (repaintMethod == null) {
                            for (Method m : anariPane.getClass().getDeclaredMethods()) {
                                if (m.getParameterCount() == 0) {
                                    String n = m.getName().toLowerCase();
                                    if (n.contains("render") || n.contains("repaint")) {
                                        m.setAccessible(true);
                                        repaintMethod = m;
                                        System.out.println("Animation loop bound to internal method: " + m.getName() + "()");
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (repaintMethod != null) {
                        try {
                            repaintMethod.invoke(anariPane);
                        } catch (Exception ignored) {}
                    } else {
                        anariPane.requestLayout();
                    }
                }
            };
            animTimer.start();
            System.out.println("Continuous animation timer started.");

            StackPane root = new StackPane(anariPane);
            Scene scene = new Scene(root, 1024, 768);
            primaryStage.setTitle("Janari: " + modelName);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            modelName = args[0];
        }
        launch(args);
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
class X3DAnariHandler extends AbstractHandler {

    private final Object x3dModel;

    private final Arena sceneArena = Arena.ofShared();
    private final List<Object> keepAlive = new ArrayList<>();
    private final List<Instance> anariInstances = new ArrayList<>();
    private final Map<String, Sampler> textureCache = new HashMap<>();
    private Material<?> defaultMaterial;
    private Device device;
    private final float[] bmin = { Float.MAX_VALUE,  Float.MAX_VALUE,  Float.MAX_VALUE };
    private final float[] bmax = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
    private boolean viewpointSet = false;

    private static final float HEADLIGHT_IRRADIANCE = 1.0f;
    private static final float AMBIENT_RADIANCE = 0.25f;
    private Light.Directional headlight;

    // Compensates for JavaFX image display in AnariPane
    private static final boolean FLIP_Y = true;
    private static final boolean SWAP_RED_BLUE = true;
    private static final boolean SRGB_ENCODE_COLORS = true;
    private float angleScale = 1f;
    private float lastAz = Float.NaN, lastEl = Float.NaN;
    private boolean built = false;

    private long animStartTime = 0;
    private long frameCount = 0;
    private final List<X3DRoute> routes = new ArrayList<>();
    private final List<TimeSensorAnim> timeSensors = new ArrayList<>();
    private final Map<String, ScalarInterpolatorAnim> interpolators = new HashMap<>();
    private final Map<String, DisplacerAnim> displacers = new HashMap<>();
    private final List<DisplacerMeshBinding> activeBindings = new ArrayList<>();

    private final Map<String, Object> defMap = new HashMap<>();
    private final Map<String, Object> protoMap = new HashMap<>();
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    public X3DAnariHandler(Object x3dModel) {
        this.x3dModel = x3dModel;
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        this.device = device;
        System.out.println("initialize(Device): world=" + world + " renderer=" + renderer
                           + " camera=" + camera + " frame=" + frame);
    }

    @Override
    protected void updateScene(TimerState state) throws Throwable {
        super.updateScene(state);
        if (!built && device != null && world != null) {
            built = true;
            System.out.println("--- Building Janari scene ---");
            try {
                build(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        updateAnimation();
        updateHeadlight();
    }

    private void updateAnimation() {
        if (!built || activeBindings.isEmpty() || timeSensors.isEmpty()) return;
        if (animStartTime == 0) animStartTime = System.currentTimeMillis();

        double nowSec = (System.currentTimeMillis() - animStartTime) / 1000.0;
        frameCount++;
        boolean meshUpdated = false;

        float lastFrac = 0f;
        float lastWeight = 0f;

        for (TimeSensorAnim ts : timeSensors) {
            if (!ts.enabled || !ts.isRunning) continue;
            if (!ts.loop && nowSec > ts.cycleInterval) {
                ts.isRunning = false;
                continue;
            }
            float frac = (float) ((nowSec % ts.cycleInterval) / ts.cycleInterval);
            lastFrac = frac;

            for (X3DRoute r : routes) {
                if (r.fromNode.equals(ts.def) && r.fromField.equals("fraction_changed")) {
                    ScalarInterpolatorAnim si = interpolators.get(r.toNode);
                    if (si != null) {
                        float weight = si.evaluate(frac);
                        lastWeight = weight;
                        for (X3DRoute r2 : routes) {
                            if (r2.fromNode.equals(si.def) && r2.fromField.equals("value_changed")) {
                                DisplacerAnim da = displacers.get(r2.toNode);
                                if (da != null) {
                                    da.currentWeight = weight;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (DisplacerMeshBinding binding : activeBindings) {
            float weight = binding.displacer.currentWeight;
            if (binding.lastWeight != weight) {
                binding.lastWeight = weight;
                binding.apply(device, sceneArena, weight);
                meshUpdated = true;
            }
        }

        if (meshUpdated && world != null) {
            try {
                world.commit();
            } catch (Throwable ignored) {}
        }

        if (frameCount % 30 == 0) {
            System.out.printf("[Anim Telemetry] t=%.2fs | frac=%.3f | weight=%.3f | bindings=%d%n",
                              nowSec, lastFrac, lastWeight, activeBindings.size());
        }
    }

    private void updateHeadlight() {
        if (headlight == null || world == null) return;
        if (cameraAzimuth == lastAz && cameraElevation == lastEl) return;
        lastAz = cameraAzimuth;
        lastEl = cameraElevation;
        try {
            double az = cameraAzimuth * angleScale, el = cameraElevation * angleScale;
            float ox = (float) (Math.cos(el) * Math.sin(az));
            float oy = (float) Math.sin(el);
            float oz = (float) (Math.cos(el) * Math.cos(az));
            headlight.setDirection(-ox, -oy, -oz);
            headlight.commit();
            world.commit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void release(Device device) {
        super.release(device);
        sceneArena.close();
    }

    // ------------------------------------------------------------------

    private void build(Device device) throws Throwable {
        long t0 = System.currentTimeMillis();
        float[] gray = displayColor(0.8f, 0.8f, 0.8f);
        defaultMaterial = device.newMaterial(Material.SubType.MATTE).setColor(gray[0], gray[1], gray[2]);
        defaultMaterial.commit();
        keepAlive.add(defaultMaterial);

        System.out.println("Scanning scene tree for DEFs, Protos, and Animation nodes...");
        buildCache(x3dModel);
        long tCache = System.currentTimeMillis() - t0;
        System.out.println("Cache built in " + tCache + " ms: found " + defMap.size()
                           + " DEFs, " + protoMap.size() + " Protos, " + displacers.size() + " Displacers, " + routes.size() + " ROUTEs.");

        float[] identity = { 1,0,0,0, 0,(FLIP_Y ? -1 : 1),0,0, 0,0,1,0, 0,0,0,1 };
        Object scene = null;
        try { scene = x3dModel.getClass().getMethod("getScene").invoke(x3dModel); } catch (Exception ignored) {}
        if (scene != null) {
            traverseList(device, scene.getClass().getMethod("getChildren").invoke(scene),
                         identity, new HashMap<>());
        }

        if (bmin[0] <= bmax[0]) {
            float cx = (bmin[0] + bmax[0]) / 2, cy = (bmin[1] + bmax[1]) / 2, cz = (bmin[2] + bmax[2]) / 2;
            float dx = bmax[0] - bmin[0], dy = bmax[1] - bmin[1], dz = bmax[2] - bmin[2];
            float radius = 0.5f * (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            System.out.printf("Scene bounds: min=[%.2f, %.2f, %.2f] max=[%.2f, %.2f, %.2f]%n",
                              bmin[0], bmin[1], bmin[2], bmax[0], bmax[1], bmax[2]);
            if (!viewpointSet) {
                cameraTarget = new float[] { cx, cy, cz };
                cameraDistance = Math.max(radius * 2.5f, 1f);
                System.out.printf("Auto-framing camera: target=[%.2f, %.2f, %.2f] distance=%.2f%n",
                                  cx, cy, cz, cameraDistance);
            }
        }

        angleScale = Math.abs(cameraElevation) > 1.6f ? (float) (Math.PI / 180.0) : 1f;
        cameraAzimuth = 0f;
        cameraElevation = 0f;

        if (renderer != null) {
            try {
                renderer.setFloat32("ambientRadiance", AMBIENT_RADIANCE);
                renderer.set("background", DataType.FLOAT32_VEC4,
                             sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, 0.05f, 0.05f, 0.07f, 1f));
                renderer.commit();
            } catch (Throwable t) { t.printStackTrace(); }
        }

        headlight = device.newLight(Light.SubType.DIRECTIONAL).setDirection(0f, 0f, -1f);
        headlight.setIrradiance(HEADLIGHT_IRRADIANCE);
        headlight.commit();
        keepAlive.add(headlight);

        if (!anariInstances.isEmpty()) {
            Array1D instArray = device.newArray1D(anariInstances, DataType.INSTANCE);
            instArray.commit();
            world.setInstance(instArray);
            keepAlive.add(instArray);
        }
        Array1D lightArray = device.newArray1D(List.of(headlight), DataType.LIGHT);
        lightArray.commit();
        world.setLight(lightArray);
        keepAlive.add(lightArray);

        world.commit();

        System.out.printf("Animation initialized: %d TimeSensors, %d Interpolators, %d Displacers, %d ROUTEs, %d Bindings%n",
                          timeSensors.size(), interpolators.size(), displacers.size(), routes.size(), activeBindings.size());
        System.out.println("--- Janari Load Complete: " + anariInstances.size() + " shape instances in world ---");
    }

    // ------------------------------------------------------------------
    // Fast, targeted X3D hierarchy cache

    private void buildCache(Object node) {
        if (node == null || !visited.add(node)) return;

        String def = extractString(node, "getDEF");
        if (def != null && !def.trim().isEmpty()) {
            def = def.trim();
            defMap.put(def, node);
        }

        String cName = node.getClass().getSimpleName();
        if (cName.contains("ProtoDeclare")) {
            String name = extractString(node, "getName");
            if (name != null && !name.trim().isEmpty()) protoMap.put(name.trim(), node);
        }

        if (cName.contains("TimeSensor") && def != null) {
            boolean enabled = extractBoolean(node, "getEnabled", true);
            double interval = extractDouble(node, "getCycleInterval", 1.0);
            boolean loop = extractBoolean(node, "getLoop", false);
            TimeSensorAnim ts = new TimeSensorAnim(def, interval, loop, enabled);
            timeSensors.add(ts);
            System.out.printf("TimeSensor '%s' parsed: enabled=%b, loop=%b, cycleInterval=%.2fs%n",
                              def, enabled, loop, interval);
        }

        if (cName.contains("ScalarInterpolator") && def != null) {
            float[] key = extractFloatArray(node, "getKey");
            float[] val = extractFloatArray(node, "getKeyValue");
            if (key != null && val != null && key.length > 0 && val.length > 0) {
                interpolators.put(def, new ScalarInterpolatorAnim(def, key, val));
            }
        }

        if (cName.contains("HAnimDisplacer") && def != null) {
            int[] ci = extractIntArray(node, "getCoordIndex");
            float[] d = extractFloatArray(node, "getDisplacements");
            if (ci != null && d != null && ci.length > 0 && d.length > 0) {
                displacers.put(def, new DisplacerAnim(def, ci, d));
            }
        }

        if (cName.contains("ROUTE")) {
            String fNode = extractString(node, "getFromNode");
            String fField = extractString(node, "getFromField");
            String tNode = extractString(node, "getToNode");
            String tField = extractString(node, "getToField");
            if (fNode != null && tNode != null) {
                routes.add(new X3DRoute(fNode, fField, tNode, tField));
            }
        }

        if (cName.equals("X3D")) {
            tryInvokeAndCache(node, "getScene");
            return;
        }

        tryInvokeAndCache(node, "getChildren");
        tryInvokeAndCache(node, "getSkeleton");
        tryInvokeAndCache(node, "getSkeletonList");
        tryInvokeAndCache(node, "getDisplacers");
        tryInvokeAndCache(node, "getDisplacerList");
        tryInvokeAndCache(node, "getAppearance");
        tryInvokeAndCache(node, "getGeometry");
        tryInvokeAndCache(node, "getMaterial");
        tryInvokeAndCache(node, "getTexture");
        tryInvokeAndCache(node, "getCoord");
        tryInvokeAndCache(node, "getTexCoord");
        tryInvokeAndCache(node, "getProtoBody");
        tryInvokeAndCache(node, "getProtoInterface");
        tryInvokeAndCache(node, "getProtoDeclareList");
        tryInvokeAndCache(node, "getFieldList");
        tryInvokeAndCache(node, "getFieldValueList");
    }

    private void tryInvokeAndCache(Object node, String methodName) {
        try {
            Method m = node.getClass().getMethod(methodName);
            if (m.getParameterCount() == 0) {
                Object res = m.invoke(node);
                if (res instanceof List<?>) {
                    for (Object child : (List<?>) res) {
                        if (child != null && isX3DNode(child)) buildCache(child);
                    }
                } else if (res instanceof Object[]) {
                    for (Object child : (Object[]) res) {
                        if (child != null && isX3DNode(child)) buildCache(child);
                    }
                } else if (res != null && isX3DNode(res)) {
                    buildCache(res);
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean isX3DNode(Object o) {
        if (o == null) return false;
        String pkg = o.getClass().getName();
        return pkg.contains("org.web3d.x3d.jsail.") && !pkg.contains(".fields.");
    }

    private static String extractString(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return res != null ? res.toString().trim() : null;
        } catch (Exception ignored) { return null; }
    }

    private static double extractDouble(Object node, String method, double fallback) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            if (res instanceof Number) return ((Number) res).doubleValue();
            if (res != null) {
                Method mVal = res.getClass().getMethod("getValue");
                Object v = mVal.invoke(res);
                if (v instanceof Number) return ((Number) v).doubleValue();
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private static boolean extractBoolean(Object node, String method, boolean fallback) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            if (res instanceof Boolean) return (Boolean) res;
            if (res != null) {
                Method mVal = res.getClass().getMethod("getValue");
                Object v = mVal.invoke(res);
                if (v instanceof Boolean) return (Boolean) v;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private static float[] extractFloatArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toFloatArray(res);
        } catch (Exception ignored) { return null; }
    }

    private static int[] extractIntArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toIntArray(res);
        } catch (Exception ignored) { return null; }
    }

    private static String[] extractStringArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toStringArray(res);
        } catch (Exception ignored) { return null; }
    }

    private static float[] toFloatArray(Object o) {
        if (o == null) return null;
        if (o instanceof float[]) return (float[]) o;
        for (String mName : List.of("getValue", "getArray")) {
            try {
                Object res = o.getClass().getMethod(mName).invoke(o);
                if (res instanceof float[]) return (float[]) res;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static int[] toIntArray(Object o) {
        if (o == null) return null;
        if (o instanceof int[]) return (int[]) o;
        for (String mName : List.of("getValue", "getArray")) {
            try {
                Object res = o.getClass().getMethod(mName).invoke(o);
                if (res instanceof int[]) return (int[]) res;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String[] toStringArray(Object o) {
        if (o == null) return null;
        if (o instanceof String[]) return (String[]) o;
        if (o instanceof List<?>) {
            List<?> l = (List<?>) o;
            String[] arr = new String[l.size()];
            for (int i = 0; i < l.size(); i++) arr[i] = l.get(i) != null ? l.get(i).toString() : "";
            return arr;
        }
        for (String mName : List.of("getArray", "getValue", "getStrings")) {
            try {
                Object res = o.getClass().getMethod(mName).invoke(o);
                if (res instanceof String[]) return (String[]) res;
                if (res instanceof Object[]) {
                    Object[] oa = (Object[]) res;
                    String[] sa = new String[oa.length];
                    for (int i = 0; i < oa.length; i++) sa[i] = String.valueOf(oa[i]);
                    return sa;
                }
            } catch (Exception ignored) {}
        }
        return new String[]{ o.toString() };
    }

    private void traverseList(Device device, Object children, float[] matrix, Map<String, Object> protoArgs) {
        if (children == null) return;
        if (children instanceof List<?>) {
            for (Object c : (List<?>) children) processNode(device, c, matrix, protoArgs);
        } else if (children instanceof Object[]) {
            for (Object c : (Object[]) children) processNode(device, c, matrix, protoArgs);
        } else {
            processNode(device, children, matrix, protoArgs);
        }
    }

    private void processNode(Device device, Object node, float[] parentTransform, Map<String, Object> protoArgs) {
        if (node == null) return;

        try {
            String def = (String) node.getClass().getMethod("getDEF").invoke(node);
            if (def != null && !def.trim().isEmpty()) {
                defMap.putIfAbsent(def.trim(), node);
            }
        } catch (Exception ignored) {}

        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty() && defMap.containsKey(use)) {
                processNode(device, defMap.get(use), parentTransform, protoArgs);
                return;
            }
        } catch (Exception ignored) {}

        String cName = node.getClass().getSimpleName();
        if (cName.contains("ProtoDeclare") || cName.contains("ROUTE") || cName.contains("TimeSensor")
            || cName.contains("ScalarInterpolator")) return;

        if (cName.contains("Background")) {
            try {
                float[] sky = (float[]) node.getClass().getMethod("getSkyColor").invoke(node);
                if (sky != null && sky.length >= 3 && renderer != null) {
                    float[] sc = displayColor(sky[0], sky[1], sky[2]);
                    renderer.set("background", DataType.FLOAT32_VEC4,
                                 sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, sc[0], sc[1], sc[2], 1f));
                    renderer.commit();
                }
            } catch (Throwable ignored) {}
            return;
        }

        if (cName.contains("Viewpoint")) {
            if (!viewpointSet) {
                try {
                    float[] pos = (float[]) node.getClass().getMethod("getPosition").invoke(node);
                    if (pos != null && pos.length >= 3) {
                        float vy = FLIP_Y ? -pos[1] : pos[1];
                        cameraTarget = new float[] { pos[0], vy, 0f };
                        cameraDistance = Math.abs(pos[2]) > 0.001f ? Math.abs(pos[2])
                                         : (float) Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
                        viewpointSet = true;
                        System.out.printf("Viewpoint configured: target=[%.2f, %.2f, 0] distance=%.2f%n",
                                          cameraTarget[0], cameraTarget[1], cameraDistance);
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        // --- ProtoInstance Execution ---
        if (cName.contains("ProtoInstance")) {
            try {
                String name = extractString(node, "getName");
                Object protoDecl = protoMap.get(name);
                if (protoDecl != null) {
                    Map<String, Object> newArgs = new HashMap<>();

                    // 1. Defaults from ProtoInterface
                    Object protoInterface = null;
                    try { protoInterface = protoDecl.getClass().getMethod("getProtoInterface").invoke(protoDecl); } catch (Exception ignored) {}
                    if (protoInterface != null) {
                        try {
                            List<?> fList = (List<?>) protoInterface.getClass().getMethod("getFieldList").invoke(protoInterface);
                            for (Object f : fList) {
                                String fName = extractString(f, "getName");
                                Object fVal = extractString(f, "getValue");
                                if (fVal == null || fVal.toString().trim().isEmpty()) {
                                    try { fVal = f.getClass().getMethod("getChildren").invoke(f); } catch (Exception ignored) {}
                                }
                                if (fVal != null) newArgs.put(fName, fVal);
                            }
                        } catch (Exception ignored) {}
                    }

                    // 2. IS connects from parent proto scope
                    Object isNode = null;
                    try { isNode = node.getClass().getMethod("getIS").invoke(node); } catch (Exception ignored) {}
                    if (isNode != null) {
                        try {
                            List<?> connects = (List<?>) isNode.getClass().getMethod("getConnectList").invoke(isNode);
                            for (Object c : connects) {
                                String nField = extractString(c, "getNodeField");
                                String pField = extractString(c, "getProtoField");
                                if (protoArgs.containsKey(pField)) {
                                    newArgs.put(nField, protoArgs.get(pField));
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // 3. Overrides from fieldValueList on this ProtoInstance
                    try {
                        List<?> fvList = (List<?>) node.getClass().getMethod("getFieldValueList").invoke(node);
                        for (Object fv : fvList) {
                            String fName = extractString(fv, "getName");
                            Object fVal = extractString(fv, "getValue");
                            if (fVal == null || fVal.toString().trim().isEmpty()) {
                                try { fVal = fv.getClass().getMethod("getChildren").invoke(fv); } catch (Exception ignored) {}
                            }
                            if (fVal != null) newArgs.put(fName, fVal);
                        }
                    } catch (Exception ignored) {}

                    // 4. Instantiate ProtoBody
                    Object pBody = protoDecl.getClass().getMethod("getProtoBody").invoke(protoDecl);
                    Object bodyChildren = pBody.getClass().getMethod("getChildren").invoke(pBody);
                    traverseList(device, bodyChildren, parentTransform, newArgs);
                } else {
                    System.err.println("Unrecognized ProtoDeclare: " + name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (cName.contains("HAnimHumanoid")) {
            try {
                Object skel = null;
                try { skel = node.getClass().getMethod("getSkeleton").invoke(node); } catch (Exception ignored) {}
                if (skel == null) {
                    try { skel = node.getClass().getMethod("getSkeletonList").invoke(node); } catch (Exception ignored) {}
                }
                if (skel != null) traverseList(device, skel, parentTransform, protoArgs);
            } catch (Exception ignored) {}
        }

        if (cName.contains("HAnimSegment")) {
            List<DisplacerAnim> segDisplacers = new ArrayList<>();
            for (String mName : List.of("getDisplacers", "getDisplacerList")) {
                try {
                    Object res = node.getClass().getMethod(mName).invoke(node);
                    List<?> list = (res instanceof List<?>) ? (List<?>) res
                                   : (res instanceof Object[]) ? List.of((Object[]) res) : null;
                    if (list != null) {
                        for (Object dObj : list) {
                            String dDef = extractString(dObj, "getDEF");
                            if (dDef != null && displacers.containsKey(dDef)) {
                                segDisplacers.add(displacers.get(dDef));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!segDisplacers.isEmpty()) {
                protoArgs = new HashMap<>(protoArgs);
                protoArgs.put("_activeDisplacers", segDisplacers);
            }
        }

        // --- Transform Handling ---
        if (cName.contains("Transform") || cName.contains("HAnimJoint")) {
            float[] tr = null;
            float[] sc = null;
            float[] rot = null;
            Object childrenToTraverse = null;

            try { tr = extractFloatArray(node, "getTranslation"); } catch (Exception ignored) {}
            try { sc = extractFloatArray(node, "getScale"); } catch (Exception ignored) {}
            try { rot = extractFloatArray(node, "getRotation"); } catch (Exception ignored) {}

            try {
                Object isNode = node.getClass().getMethod("getIS").invoke(node);
                if (isNode != null) {
                    List<?> connects = (List<?>) isNode.getClass().getMethod("getConnectList").invoke(isNode);
                    for (Object c : connects) {
                        String nField = extractString(c, "getNodeField");
                        String pField = extractString(c, "getProtoField");
                        if (protoArgs.containsKey(pField)) {
                            Object val = protoArgs.get(pField);
                            if ("translation".equals(nField)) tr = parseVec3(val);
                            else if ("scale".equals(nField)) sc = parseVec3(val);
                            else if ("rotation".equals(nField)) rot = parseVec4(val);
                            else if ("children".equals(nField)) childrenToTraverse = val;
                        }
                    }
                }
            } catch (Exception ignored) {}

            float[] localMat = buildTransformMatrix(tr, sc, rot);
            float[] currentMat = multiplyMatrix(parentTransform, localMat);

            try {
                if (childrenToTraverse == null) {
                    childrenToTraverse = node.getClass().getMethod("getChildren").invoke(node);
                }
                traverseList(device, childrenToTraverse, currentMat, protoArgs);
            } catch (Exception ignored) {}
            return;
        }

        if (cName.contains("Shape")) {
            try {
                Instance inst = buildShapeInstance(device, node, parentTransform, protoArgs);
                if (inst != null) anariInstances.add(inst);
            } catch (Throwable t) { t.printStackTrace(); }
            return;
        }

        try {
            traverseList(device, node.getClass().getMethod("getChildren").invoke(node), parentTransform, protoArgs);
        } catch (Exception ignored) {}
    }

    private static float[] parseVec3(Object val) {
        if (val == null) return null;
        if (val instanceof float[]) return (float[]) val;
        if (val instanceof String) {
            String[] p = ((String) val).trim().split("[,\\s]+");
            if (p.length >= 3) {
                try { return new float[]{ Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]) }; }
                catch (Exception ignored) {}
            }
        }
        try {
            Object v = val.getClass().getMethod("getValue").invoke(val);
            if (v instanceof float[]) return (float[]) v;
        } catch (Exception ignored) {}
        return null;
    }

    private static float[] parseVec4(Object val) {
        if (val == null) return null;
        if (val instanceof float[]) return (float[]) val;
        if (val instanceof String) {
            String[] p = ((String) val).trim().split("[,\\s]+");
            if (p.length >= 4) {
                try { return new float[]{ Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]) }; }
                catch (Exception ignored) {}
            }
        }
        try {
            Object v = val.getClass().getMethod("getValue").invoke(val);
            if (v instanceof float[]) return (float[]) v;
        } catch (Exception ignored) {}
        return null;
    }

    private static float[] buildTransformMatrix(float[] tr, float[] sc, float[] rot) {
        float sx = (sc != null && sc.length >= 3) ? sc[0] : 1f;
        float sy = (sc != null && sc.length >= 3) ? sc[1] : 1f;
        float sz = (sc != null && sc.length >= 3) ? sc[2] : 1f;

        float[] m = {
            sx, 0,  0,  0,
            0,  sy, 0,  0,
            0,  0,  sz, 0,
            0,  0,  0,  1
        };

        if (rot != null && rot.length >= 4 && Math.abs(rot[3]) > 1e-6f) {
            float ax = rot[0], ay = rot[1], az = rot[2], angle = rot[3];
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (len > 1e-6f) {
                ax /= len; ay /= len; az /= len;
                float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
                float t = 1f - c;
                float[] r = {
                    t*ax*ax + c,     t*ax*ay + s*az, t*ax*az - s*ay, 0,
                    t*ax*ay - s*az, t*ay*ay + c,     t*ay*az + s*ax, 0,
                    t*ax*az + s*ay, t*ay*az - s*ax, t*az*az + c,     0,
                    0,               0,               0,              1
                };
                m = multiplyMatrixStatic(r, m);
            }
        }

        if (tr != null && tr.length >= 3) {
            m[12] += tr[0];
            m[13] += tr[1];
            m[14] += tr[2];
        }
        return m;
    }

    private float[] multiplyMatrix(float[] parent, float[] local) {
        return multiplyMatrixStatic(parent, local);
    }

    private static float[] multiplyMatrixStatic(float[] parent, float[] local) {
        float[] r = new float[16];
        for (int c = 0; c < 4; c++)
            for (int row = 0; row < 4; row++)
                r[row + c*4] = parent[row]    * local[c*4]
                             + parent[row+4]  * local[1+c*4]
                             + parent[row+8]  * local[2+c*4]
                             + parent[row+12] * local[3+c*4];
        return r;
    }

    // ------------------------------------------------------------------
    // ANARI Object Parameter Helper

    private MemorySegment getAnariHandle(Object anariObj) {
        if (anariObj == null) return null;
        for (String name : List.of("handle", "segment", "getHandle", "getSegment", "address")) {
            try {
                Method m = anariObj.getClass().getMethod(name);
                Object res = m.invoke(anariObj);
                if (res instanceof MemorySegment) return (MemorySegment) res;
                if (res instanceof Long) return MemorySegment.ofAddress((Long) res);
            } catch (Exception ignored) {}
        }
        for (Field f : anariObj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object res = f.get(anariObj);
                if (res instanceof MemorySegment) return (MemorySegment) res;
                if (res instanceof Long) return MemorySegment.ofAddress((Long) res);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void setAnariObjectParameter(org.codeberg.anari.api.Object<?> target,
                                         String paramName,
                                         DataType type,
                                         Object anariObj) {
        if (target == null || anariObj == null) return;

        // 1. Try public typed method
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() == 1) {
                String mName = m.getName().toLowerCase();
                String cleanParam = paramName.replace(".", "").replace("_", "").toLowerCase();
                if (mName.endsWith(cleanParam) || mName.equals("set" + cleanParam)) {
                    Class<?> pt = m.getParameterTypes()[0];
                    if (pt.isInstance(anariObj)) {
                        try {
                            m.invoke(target, anariObj);
                            return;
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }

        // 2. Try generic set(String, Object)
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals("set") && m.getParameterCount() == 2) {
                if (m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1].isInstance(anariObj)) {
                    try {
                        m.invoke(target, paramName, anariObj);
                        return;
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 3. Try low-level set(String, DataType, MemorySegment)
        MemorySegment h = getAnariHandle(anariObj);
        if (h != null) {
            MemorySegment ptr = sceneArena.allocateFrom(ValueLayout.ADDRESS, h);
            try {
                target.set(paramName, type, ptr);
                return;
            } catch (Throwable ignored) {}
            try {
                target.set(paramName, type, h);
            } catch (Throwable ignored) {}
        }
    }

    private Instance buildShapeInstance(Device device, Object shape, float[] transformMatrix, Map<String, Object> protoArgs) throws Throwable {
        Object x3dGeom = null;
        try { x3dGeom = shape.getClass().getMethod("getGeometry").invoke(shape); } catch (Exception ignored) {}

        try {
            Object isNode = shape.getClass().getMethod("getIS").invoke(shape);
            if (isNode != null) {
                List<?> connects = (List<?>) isNode.getClass().getMethod("getConnectList").invoke(isNode);
                for (Object c : connects) {
                    String nField = extractString(c, "getNodeField");
                    String pField = extractString(c, "getProtoField");
                    if ("geometry".equals(nField) && protoArgs.containsKey(pField)) {
                        x3dGeom = protoArgs.get(pField);
                    }
                }
            }
        } catch (Exception ignored) {}

        x3dGeom = resolveUse(x3dGeom);
        Geometry.Triangle geometry = createGeometry(device, x3dGeom, transformMatrix, protoArgs);
        if (geometry == null) return null;

        Material<?> material = defaultMaterial;
        try {
            Object app = null;
            try { app = shape.getClass().getMethod("getAppearance").invoke(shape); } catch (Exception ignored) {}
            app = resolveUse(app);

            Object mat = (app == null) ? null : app.getClass().getMethod("getMaterial").invoke(app);
            mat = resolveUse(mat);
            Object tex = (app == null) ? null : app.getClass().getMethod("getTexture").invoke(app);
            tex = resolveUse(tex);

            Sampler sampler = null;
            if (tex != null && tex.getClass().getSimpleName().contains("ImageTexture")) {
                sampler = getOrCreateTextureSampler(device, tex);
            }

            if (mat != null || sampler != null) {
                float[] d = null;
                float transparency = 0f;

                if (mat != null) {
                    try { d = (float[]) mat.getClass().getMethod("getDiffuseColor").invoke(mat); } catch (Exception ignored) {}
                    try { transparency = (float) extractDouble(mat, "getTransparency", 0.0); } catch (Exception ignored) {}

                    if (d == null && mat.getClass().getSimpleName().contains("ProtoInstance")) {
                        try {
                            String pName = (String) mat.getClass().getMethod("getName").invoke(mat);
                            if (pName != null) {
                                int h = Math.abs(pName.hashCode());
                                float r = (h & 0xFF) / 255.0f;
                                float g = ((h >> 8) & 0xFF) / 255.0f;
                                float b = ((h >> 16) & 0xFF) / 255.0f;
                                d = new float[]{ Math.max(0.25f, r), Math.max(0.25f, g), Math.max(0.25f, b) };
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (d == null) d = new float[]{ 1f, 1f, 1f };
                float[] c = displayColor(d[0], d[1], d[2]);
                float opacity = Math.max(0f, Math.min(1f, 1f - transparency));

                Material.Matte m = device.newMaterial(Material.SubType.MATTE);
                if (sampler != null) {
                    setAnariObjectParameter(m, "color", DataType.SAMPLER, sampler);
                } else {
                    m.setColor(c[0], c[1], c[2]);
                }

                if (opacity < 0.999f) {
                    try {
                        m.setFloat32("opacity", opacity);
                    } catch (Throwable ignored) {}
                }

                m.commit();
                keepAlive.add(m);
                material = m;
            }
        } catch (Throwable ignored) {}

        Surface surface = device.newSurface().setGeometry(geometry).setMaterial(material);
        surface.commit();
        keepAlive.add(surface);

        Group group = device.newGroup();
        Array1D surfArray = device.newArray1D(List.of(surface), DataType.SURFACE);
        surfArray.commit();
        group.setSurface(surfArray);
        group.commit();
        keepAlive.add(surfArray);
        keepAlive.add(group);

        Instance instance = device.newInstance(Instance.SubType.TRANSFORM);
        instance.setGroup(group);
        instance.setTransform(transformMatrix);
        instance.commit();
        return instance;
    }

    private Sampler getOrCreateTextureSampler(Device device, Object imageTexture) {
        try {
            String[] urls = extractStringArray(imageTexture, "getUrl");
            if (urls == null || urls.length == 0) return null;
            String firstUrl = urls[0];
            if (textureCache.containsKey(firstUrl)) return textureCache.get(firstUrl);

            BufferedImage img = null;
            for (String u : urls) {
                if (u == null || u.trim().isEmpty()) continue;
                u = u.trim().replace("\"", "");

                List<File> candidates = List.of(
                    new File(u),
                    new File("data", u),
                    new File("../data", u),
                    new File("../../data", u),
                    new File("src/main/resources", u),
                    new File(new File(u).getName()),
                    new File("data", new File(u).getName()),
                    new File("../data", new File(u).getName())
                );
                for (File f : candidates) {
                    if (f.exists() && f.isFile()) {
                        try {
                            img = ImageIO.read(f);
                            if (img != null) {
                                System.out.println("Loaded texture file: " + f.getAbsolutePath());
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (img != null) break;

                if (u.startsWith("http://") || u.startsWith("https://")) {
                    try {
                        java.net.URLConnection conn = URI.create(u).toURL().openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);
                        try (InputStream is = conn.getInputStream()) {
                            img = ImageIO.read(is);
                            if (img != null) {
                                System.out.println("Downloaded texture: " + u);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Could not load remote texture from " + u + ": " + e.getMessage());
                    }
                }
            }

            if (img == null) {
                System.out.println("ImageTexture: could not resolve image file for: " + firstUrl);
                return null;
            }

            int width = img.getWidth(), height = img.getHeight();
            byte[] rgba = new byte[width * height * 4];
            int k = 0;
            // Rows read bottom-to-top so (u, v)=(0, 0) starts at the bottom-left matching X3D
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int argb = img.getRGB(x, y);
                    byte r = (byte) ((argb >> 16) & 0xFF);
                    byte g = (byte) ((argb >> 8)  & 0xFF);
                    byte b = (byte) (argb         & 0xFF);
                    byte a = (byte) ((argb >> 24) & 0xFF);

                    // Pre-swap Red and Blue so AnariPane's frame display outputs correct colors
                    if (SWAP_RED_BLUE) {
                        rgba[k++] = b;
                        rgba[k++] = g;
                        rgba[k++] = r;
                    } else {
                        rgba[k++] = r;
                        rgba[k++] = g;
                        rgba[k++] = b;
                    }
                    rgba[k++] = a;
                }
            }

            MemorySegment imgSeg = sceneArena.allocateFrom(ValueLayout.JAVA_BYTE, rgba);
            Array2D imgArray = device.newArray2D(imgSeg, MemorySegment.NULL, MemorySegment.NULL,
                                                DataType.UFIXED8_VEC4, width, height);
            imgArray.commit();
            keepAlive.add(imgArray);

            Sampler sampler = null;
            try {
                sampler = device.newSampler(Sampler.SubType.IMAGE2D);
            } catch (Throwable t) {
                try {
                    Method mNew = device.getClass().getMethod("newSampler", String.class);
                    sampler = (Sampler) mNew.invoke(device, "image2D");
                } catch (Throwable ignored) {}
            }

            if (sampler != null) {
                setAnariObjectParameter(sampler, "image", DataType.ARRAY2D, imgArray);

                boolean inAttrSet = false;
                try {
                    Method mAttr = sampler.getClass().getMethod("setInAttribute", String.class);
                    mAttr.invoke(sampler, "attribute0");
                    inAttrSet = true;
                } catch (Throwable ignored) {}
                if (!inAttrSet) {
                    try {
                        sampler.set("inAttribute", DataType.STRING, sceneArena.allocateFrom("attribute0\0", StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {}
                }

                try {
                    Method mWrap1 = sampler.getClass().getMethod("setWrap1", String.class);
                    mWrap1.invoke(sampler, "repeat");
                } catch (Throwable ignored) {}
                try {
                    Method mWrap2 = sampler.getClass().getMethod("setWrap2", String.class);
                    mWrap2.invoke(sampler, "repeat");
                } catch (Throwable ignored) {}

                sampler.commit();
                keepAlive.add(sampler);
                textureCache.put(firstUrl, sampler);
                System.out.println("ImageTexture successfully initialized (" + width + "x" + height + "): " + firstUrl);
                return sampler;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static float srgbToLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static float[] displayColor(float r, float g, float b) {
        if (SRGB_ENCODE_COLORS) { r = srgbToLinear(r); g = srgbToLinear(g); b = srgbToLinear(b); }
        return SWAP_RED_BLUE ? new float[]{ b, g, r } : new float[]{ r, g, b };
    }

    private Object resolveUse(Object node) {
        if (node == null) return null;
        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty() && defMap.containsKey(use)) return defMap.get(use);
        } catch (Exception ignored) {}
        return node;
    }

    private Geometry.Triangle createGeometry(Device device, Object x3dGeom, float[] m, Map<String, Object> protoArgs) throws Throwable {
        if (x3dGeom == null) return null;
        String g = x3dGeom.getClass().getSimpleName();
        switch (g) {
            case "Box":            return createBox(device, x3dGeom, m);
            case "Sphere":         return createSphere(device, x3dGeom, m);
            case "Cylinder":       return createCylinder(device, x3dGeom, m);
            case "Extrusion":      return createExtrusion(device, x3dGeom, m);
            case "IndexedFaceSet": return createIndexedFaceSet(device, x3dGeom, m, protoArgs);
            default:
                System.out.println("Unsupported geometry type: " + g);
                return null;
        }
    }

    private void addBounds(float[] pts, float[] m) {
        for (int i = 0; i + 2 < pts.length; i += 3) {
            float x = pts[i], y = pts[i+1], z = pts[i+2];
            float wx = m[0]*x + m[4]*y + m[8]*z  + m[12];
            float wy = m[1]*x + m[5]*y + m[9]*z  + m[13];
            float wz = m[2]*x + m[6]*y + m[10]*z + m[14];
            bmin[0] = Math.min(bmin[0], wx); bmax[0] = Math.max(bmax[0], wx);
            bmin[1] = Math.min(bmin[1], wy); bmax[1] = Math.max(bmax[1], wy);
            bmin[2] = Math.min(bmin[2], wz); bmax[2] = Math.max(bmax[2], wz);
        }
    }

    private Geometry.Triangle createSphere(Device device, Object sphere, float[] m) throws Throwable {
        float radius = 1.0f;
        try { radius = (float) sphere.getClass().getMethod("getRadius").invoke(sphere); } catch (Exception ignored) {}

        int rings = 20;
        int sectors = 32;
        int nverts = (rings + 1) * (sectors + 1);
        float[] vertices = new float[nverts * 3];
        int vIdx = 0;

        for (int r = 0; r <= rings; r++) {
            double phi = (double) r / rings * Math.PI;
            float y = (float) (radius * Math.cos(phi));
            float sinPhi = (float) Math.sin(phi);

            for (int s = 0; s <= sectors; s++) {
                double theta = (double) s / sectors * (2.0 * Math.PI);
                float x = (float) (radius * sinPhi * Math.cos(theta));
                float z = (float) (radius * sinPhi * Math.sin(theta));

                vertices[vIdx++] = x;
                vertices[vIdx++] = y;
                vertices[vIdx++] = z;
            }
        }

        int ntris = rings * sectors * 2;
        int[] indices = new int[ntris * 3];
        int iIdx = 0;

        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                int first = r * (sectors + 1) + s;
                int second = first + sectors + 1;

                indices[iIdx++] = first;
                indices[iIdx++] = second;
                indices[iIdx++] = first + 1;

                indices[iIdx++] = second;
                indices[iIdx++] = second + 1;
                indices[iIdx++] = first + 1;
            }
        }

        addBounds(vertices, m);
        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, nverts);
        vArray.commit();
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, ntris);
        iArray.commit();

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        return geom;
    }

    private Geometry.Triangle createCylinder(Device device, Object cyl, float[] m) throws Throwable {
        float radius = (float) extractDouble(cyl, "getRadius", 1.0);
        float height = (float) extractDouble(cyl, "getHeight", 2.0);
        boolean top = extractBoolean(cyl, "getTop", true);
        boolean bottom = extractBoolean(cyl, "getBottom", true);
        boolean side = extractBoolean(cyl, "getSide", true);

        int slices = 32;
        float halfH = height / 2.0f;
        List<Float> vList = new ArrayList<>();
        List<Integer> iList = new ArrayList<>();

        if (side) {
            int baseIdx = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                float x = (float) (radius * Math.cos(theta));
                float z = (float) (radius * Math.sin(theta));
                vList.add(x); vList.add(-halfH); vList.add(z);
                vList.add(x); vList.add(halfH);  vList.add(z);
            }
            for (int i = 0; i < slices; i++) {
                int i0 = baseIdx + i * 2;
                int i1 = baseIdx + i * 2 + 1;
                int i2 = baseIdx + (i + 1) * 2 + 1;
                int i3 = baseIdx + (i + 1) * 2;
                iList.add(i0); iList.add(i2); iList.add(i1);
                iList.add(i0); iList.add(i3); iList.add(i2);
            }
        }

        if (top) {
            int centerIdx = vList.size() / 3;
            vList.add(0f); vList.add(halfH); vList.add(0f);
            int rimStart = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                vList.add((float) (radius * Math.cos(theta)));
                vList.add(halfH);
                vList.add((float) (radius * Math.sin(theta)));
            }
            for (int i = 0; i < slices; i++) {
                iList.add(centerIdx);
                iList.add(rimStart + i + 1);
                iList.add(rimStart + i);
            }
        }

        if (bottom) {
            int centerIdx = vList.size() / 3;
            vList.add(0f); vList.add(-halfH); vList.add(0f);
            int rimStart = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                vList.add((float) (radius * Math.cos(theta)));
                vList.add(-halfH);
                vList.add((float) (radius * Math.sin(theta)));
            }
            for (int i = 0; i < slices; i++) {
                iList.add(centerIdx);
                iList.add(rimStart + i);
                iList.add(rimStart + i + 1);
            }
        }

        if (iList.isEmpty()) return null;

        float[] vertices = new float[vList.size()];
        for (int i = 0; i < vList.size(); i++) vertices[i] = vList.get(i);
        int[] indices = new int[iList.size()];
        for (int i = 0; i < iList.size(); i++) indices[i] = iList.get(i);

        addBounds(vertices, m);

        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, vertices.length / 3);
        vArray.commit();
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, indices.length / 3);
        iArray.commit();

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        return geom;
    }

    private Geometry.Triangle createExtrusion(Device device, Object extrusion, float[] m) throws Throwable {
        float[] rawCS = extractFloatArray(extrusion, "getCrossSection");
        if (rawCS == null || rawCS.length < 4) {
            rawCS = new float[]{ 1f, 1f,  1f, -1f,  -1f, -1f,  -1f, 1f,  1f, 1f };
        }
        int numCsPts = rawCS.length / 2;

        float[] rawSpine = extractFloatArray(extrusion, "getSpine");
        if (rawSpine == null || rawSpine.length < 6) {
            rawSpine = new float[]{ 0f, 0f, 0f,  0f, 1f, 0f };
        }
        int numSpinePts = rawSpine.length / 3;

        float[] rawScale = extractFloatArray(extrusion, "getScale");
        boolean beginCap = extractBoolean(extrusion, "getBeginCap", true);
        boolean endCap = extractBoolean(extrusion, "getEndCap", true);

        boolean spineClosed = numSpinePts > 2
                && Math.abs(rawSpine[0] - rawSpine[(numSpinePts - 1) * 3]) < 1e-5f
                && Math.abs(rawSpine[1] - rawSpine[(numSpinePts - 1) * 3 + 1]) < 1e-5f
                && Math.abs(rawSpine[2] - rawSpine[(numSpinePts - 1) * 3 + 2]) < 1e-5f;

        float[][] scpX = new float[numSpinePts][3];
        float[][] scpY = new float[numSpinePts][3];
        float[][] scpZ = new float[numSpinePts][3];

        for (int i = 0; i < numSpinePts; i++) {
            float[] d = new float[3];
            if (spineClosed) {
                int prev = (i - 1 + numSpinePts - 1) % (numSpinePts - 1);
                int next = (i + 1) % (numSpinePts - 1);
                d[0] = rawSpine[next * 3]     - rawSpine[prev * 3];
                d[1] = rawSpine[next * 3 + 1] - rawSpine[prev * 3 + 1];
                d[2] = rawSpine[next * 3 + 2] - rawSpine[prev * 3 + 2];
            } else {
                if (i == 0) {
                    d[0] = rawSpine[3]     - rawSpine[0];
                    d[1] = rawSpine[4]     - rawSpine[1];
                    d[2] = rawSpine[5]     - rawSpine[2];
                } else if (i == numSpinePts - 1) {
                    d[0] = rawSpine[i * 3]     - rawSpine[(i - 1) * 3];
                    d[1] = rawSpine[i * 3 + 1] - rawSpine[(i - 1) * 3 + 1];
                    d[2] = rawSpine[i * 3 + 2] - rawSpine[(i - 1) * 3 + 2];
                } else {
                    d[0] = rawSpine[(i + 1) * 3]     - rawSpine[(i - 1) * 3];
                    d[1] = rawSpine[(i + 1) * 3 + 1] - rawSpine[(i - 1) * 3 + 1];
                    d[2] = rawSpine[(i + 1) * 3 + 2] - rawSpine[(i - 1) * 3 + 2];
                }
            }
            normalize(d);
            scpY[i] = d;
        }

        for (int i = 0; i < numSpinePts; i++) {
            float[] y = scpY[i];
            float[] z = new float[3];

            if (Math.abs(y[0]) < 1e-4f && Math.abs(y[2]) < 1e-4f) {
                z[0] = 0f;
                z[1] = 0f;
                z[2] = (y[1] > 0) ? -1f : 1f;
            } else {
                z[0] = -y[2];
                z[1] = 0f;
                z[2] = y[0];
                normalize(z);
            }
            scpZ[i] = z;
            cross(y, z, scpX[i]);
            normalize(scpX[i]);
        }

        int totalRingVerts = numSpinePts * numCsPts;
        float[] vertices = new float[totalRingVerts * 3];
        int vIdx = 0;

        for (int s = 0; s < numSpinePts; s++) {
            float sx = 1f, sz = 1f;
            if (rawScale != null) {
                if (s * 2 + 1 < rawScale.length) {
                    sx = rawScale[s * 2];
                    sz = rawScale[s * 2 + 1];
                } else if (rawScale.length >= 2) {
                    sx = rawScale[rawScale.length - 2];
                    sz = rawScale[rawScale.length - 1];
                }
            }

            float spX = rawSpine[s * 3];
            float spY = rawSpine[s * 3 + 1];
            float spZ = rawSpine[s * 3 + 2];

            for (int c = 0; c < numCsPts; c++) {
                float cx = rawCS[c * 2] * sx;
                float cz = rawCS[c * 2 + 1] * sz;

                vertices[vIdx++] = spX + cx * scpX[s][0] + cz * scpZ[s][0];
                vertices[vIdx++] = spY + cx * scpX[s][1] + cz * scpZ[s][1];
                vertices[vIdx++] = spZ + cx * scpX[s][2] + cz * scpZ[s][2];
            }
        }

        List<Integer> indexList = new ArrayList<>();
        int spineSegments = numSpinePts - 1;

        for (int s = 0; s < spineSegments; s++) {
            int nextS = (s + 1) % numSpinePts;
            for (int c = 0; c < numCsPts - 1; c++) {
                int i0 = s * numCsPts + c;
                int i1 = nextS * numCsPts + c;
                int i2 = nextS * numCsPts + (c + 1);
                int i3 = s * numCsPts + (c + 1);

                indexList.add(i0);
                indexList.add(i1);
                indexList.add(i2);

                indexList.add(i0);
                indexList.add(i2);
                indexList.add(i3);
            }
        }

        boolean csClosed = numCsPts > 2
                && Math.abs(rawCS[0] - rawCS[(numCsPts - 1) * 2]) < 1e-4f
                && Math.abs(rawCS[1] - rawCS[(numCsPts - 1) * 2 + 1]) < 1e-4f;

        if (!spineClosed && csClosed) {
            int capPts = numCsPts - 1;
            if (beginCap) {
                for (int i = 1; i < capPts - 1; i++) {
                    indexList.add(0);
                    indexList.add(i + 1);
                    indexList.add(i);
                }
            }
            if (endCap) {
                int base = (numSpinePts - 1) * numCsPts;
                for (int i = 1; i < capPts - 1; i++) {
                    indexList.add(base);
                    indexList.add(base + i);
                    indexList.add(base + i + 1);
                }
            }
        }

        int[] indices = new int[indexList.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = indexList.get(i);

        addBounds(vertices, m);

        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, totalRingVerts);
        vArray.commit();
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, indices.length / 3);
        iArray.commit();

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        return geom;
    }

    private static void cross(float[] a, float[] b, float[] out) {
        out[0] = a[1] * b[2] - a[2] * b[1];
        out[1] = a[2] * b[0] - a[0] * b[2];
        out[2] = a[0] * b[1] - a[1] * b[0];
    }

    private static float norm(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void normalize(float[] v) {
        float len = norm(v);
        if (len > 1e-6f) {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        }
    }

    private Geometry.Triangle createIndexedFaceSet(Device device, Object ifs, float[] m, Map<String, Object> protoArgs) throws Throwable {
        int[] ci = extractIntArray(ifs, "getCoordIndex");
        Object coord = resolveUse(ifs.getClass().getMethod("getCoord").invoke(ifs));
        float[] pts = (coord == null) ? null : extractFloatArray(coord, "getPoint");
        if (ci == null || ci.length == 0 || pts == null || pts.length < 9) {
            return null;
        }
        int nverts = pts.length / 3;

        int[] tci = extractIntArray(ifs, "getTexCoordIndex");
        Object tcNode = null;
        try { tcNode = resolveUse(ifs.getClass().getMethod("getTexCoord").invoke(ifs)); } catch (Exception ignored) {}
        float[] uvs = (tcNode != null) ? extractFloatArray(tcNode, "getPoint") : null;
        boolean hasUV = (uvs != null && uvs.length >= 2);

        List<int[]> tris = new ArrayList<>();
        List<int[]> uvTris = hasUV ? new ArrayList<>() : null;
        List<Integer> faceCoord = new ArrayList<>();
        List<Integer> faceUV = hasUV ? new ArrayList<>() : null;

        for (int i = 0; i < ci.length; i++) {
            int idx = ci[i];
            if (idx < 0) {
                triangulatePolygon(pts, faceCoord, faceUV, tris, uvTris);
                faceCoord.clear();
                if (faceUV != null) faceUV.clear();
            } else if (idx < nverts) {
                faceCoord.add(idx);
                if (hasUV) {
                    int uvIdx = (tci != null && i < tci.length && tci[i] >= 0) ? tci[i] : idx;
                    faceUV.add(uvIdx);
                }
            }
        }
        if (!faceCoord.isEmpty()) triangulatePolygon(pts, faceCoord, faceUV, tris, uvTris);
        if (tris.isEmpty()) return null;

        int totalTris = tris.size();
        int totalVerts = totalTris * 3;
        float[] unrolledPts = new float[totalVerts * 3];
        float[] unrolledUV = hasUV ? new float[totalVerts * 2] : null;
        int[] indices = new int[totalVerts];
        int[] unrolledToOrigCoord = new int[totalVerts];

        int vK = 0, uvK = 0;
        for (int t = 0; t < totalTris; t++) {
            int[] cTri = tris.get(t);
            int[] uTri = (hasUV && uvTris != null && t < uvTris.size()) ? uvTris.get(t) : null;
            for (int corner = 0; corner < 3; corner++) {
                int cIdx = cTri[corner];
                int vertIdx = t * 3 + corner;
                unrolledToOrigCoord[vertIdx] = cIdx;

                unrolledPts[vK++] = pts[3 * cIdx];
                unrolledPts[vK++] = pts[3 * cIdx + 1];
                unrolledPts[vK++] = pts[3 * cIdx + 2];

                if (hasUV) {
                    int uvIdx = (uTri != null) ? uTri[corner] : cIdx;
                    if (uvIdx * 2 + 1 < uvs.length) {
                        unrolledUV[uvK++] = uvs[2 * uvIdx];
                        unrolledUV[uvK++] = uvs[2 * uvIdx + 1];
                    } else {
                        unrolledUV[uvK++] = 0f;
                        unrolledUV[uvK++] = 0f;
                    }
                }
                indices[vertIdx] = vertIdx;
            }
        }

        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledPts);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, totalVerts);
        vArray.commit();
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, totalTris);
        iArray.commit();

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        if (hasUV && unrolledUV != null) {
            MemorySegment uvSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledUV);
            Array1D uvArray = device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL,
                                                DataType.FLOAT32_VEC2, totalVerts);
            uvArray.commit();
            setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
            keepAlive.add(uvArray);
        }

        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        addBounds(unrolledPts, m);

        Object activeDispObj = protoArgs.get("_activeDisplacers");
        if (activeDispObj instanceof List<?>) {
            List<?> dList = (List<?>) activeDispObj;
            for (Object o : dList) {
                if (o instanceof DisplacerAnim) {
                    DisplacerAnim da = (DisplacerAnim) o;
                    if (da.displacements != null && da.displacements.length > 0) {
                        DisplacerMeshBinding binding = new DisplacerMeshBinding(da, geom, vArray, vSeg, pts, unrolledToOrigCoord);
                        activeBindings.add(binding);
                        System.out.printf("Bound animated displacer '%s' (%d deltas) to mesh (%d vertices)%n",
                                          da.def, da.coordIndex.length, totalVerts);
                    }
                }
            }
        }

        return geom;
    }

    private static void triangulatePolygon(float[] pts, List<Integer> faceCoord, List<Integer> faceUV,
                                           List<int[]> outCoord, List<int[]> outUV) {
        int n0 = faceCoord.size();
        if (n0 < 3) return;

        if (n0 == 3) {
            outCoord.add(new int[]{ faceCoord.get(0), faceCoord.get(1), faceCoord.get(2) });
            if (outUV != null && faceUV != null && faceUV.size() >= 3) {
                outUV.add(new int[]{ faceUV.get(0), faceUV.get(1), faceUV.get(2) });
            }
            return;
        }

        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n0; i++) {
            int a = faceCoord.get(i) * 3, b = faceCoord.get((i + 1) % n0) * 3;
            nx += (pts[a+1] - pts[b+1]) * (pts[a+2] + pts[b+2]);
            ny += (pts[a+2] - pts[b+2]) * (pts[a]   + pts[b]);
            nz += (pts[a]   - pts[b])   * (pts[a+1] + pts[b+1]);
        }
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        int drop = (ax >= ay && ax >= az) ? 0 : (ay >= az ? 1 : 2);
        double orient = (drop == 0 ? nx : drop == 1 ? ny : nz) >= 0 ? 1.0 : -1.0;

        List<Integer> v = new ArrayList<>(faceCoord);
        List<Integer> u = (faceUV != null) ? new ArrayList<>(faceUV) : null;
        int guard = 0;
        while (v.size() > 3 && guard++ < 100000) {
            int n = v.size();
            boolean clipped = false;
            for (int i = 0; i < n; i++) {
                int a = v.get((i + n - 1) % n), b = v.get(i), c = v.get((i + 1) % n);
                double cr = cross2(pts, drop, a, b, c) * orient;
                if (cr < -1e-12) continue;
                boolean inside = false;
                for (int k = 0; k < n && !inside; k++) {
                    int p = v.get(k);
                    if (p == a || p == b || p == c) continue;
                    inside = strictlyInside(pts, drop, p, a, b, c, orient);
                }
                if (inside) continue;
                if (cr > 1e-12) {
                    outCoord.add(new int[]{ a, b, c });
                    if (outUV != null && u != null) {
                        outUV.add(new int[]{ u.get((i + n - 1) % n), u.get(i), u.get((i + 1) % n) });
                    }
                }
                v.remove(i);
                if (u != null) u.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {
                int n2 = v.size();
                outCoord.add(new int[]{ v.get(n2 - 1), v.get(0), v.get(1) });
                if (outUV != null && u != null) {
                    outUV.add(new int[]{ u.get(n2 - 1), u.get(0), u.get(1) });
                }
                v.remove(0);
                if (u != null) u.remove(0);
            }
        }
        if (v.size() == 3) {
            outCoord.add(new int[]{ v.get(0), v.get(1), v.get(2) });
            if (outUV != null && u != null && u.size() >= 3) {
                outUV.add(new int[]{ u.get(0), u.get(1), u.get(2) });
            }
        }
    }

    private static double u(float[] p, int drop, int i) { return drop == 0 ? p[i*3+1] : drop == 1 ? p[i*3+2] : p[i*3]; }
    private static double w(float[] p, int drop, int i) { return drop == 0 ? p[i*3+2] : drop == 1 ? p[i*3]   : p[i*3+1]; }

    private static double cross2(float[] p, int d, int a, int b, int c) {
        return (u(p,d,b) - u(p,d,a)) * (w(p,d,c) - w(p,d,b)) - (w(p,d,b) - w(p,d,a)) * (u(p,d,c) - u(p,d,b));
    }

    private static boolean strictlyInside(float[] p, int d, int q, int a, int b, int c, double orient) {
        double e = 1e-12;
        double d1 = cross2(p, d, a, b, q) * orient;
        double d2 = cross2(p, d, b, c, q) * orient;
        double d3 = cross2(p, d, c, a, q) * orient;
        return d1 > e && d2 > e && d3 > e;
    }

    private Geometry.Triangle createBox(Device device, Object box, float[] m) throws Throwable {
        float x = 1f, y = 1f, z = 1f;
        try {
            float[] s = (float[]) box.getClass().getMethod("getSize").invoke(box);
            if (s != null) { x = s[0] / 2f; y = s[1] / 2f; z = s[2] / 2f; }
        } catch (Exception ignored) {}

        float[] vertices = { -x,-y,-z,  x,-y,-z,  x,y,-z,  -x,y,-z,
                             -x,-y, z,  x,-y, z,  x,y, z,  -x,y, z };
        int[] indices = { 4,5,6, 6,7,4,  1,0,3, 3,2,1,  5,1,2, 2,6,5,
                          0,4,7, 7,3,0,  7,6,2, 2,3,7,  0,1,5, 5,4,0 };

        addBounds(vertices, m);
        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, vertices.length / 3);
        vArray.commit();
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, indices.length / 3);
        iArray.commit();

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        return geom;
    }

    // ------------------------------------------------------------------
    // Animation model classes

    private static class X3DRoute {
        final String fromNode, fromField, toNode, toField;
        X3DRoute(String fn, String ff, String tn, String tf) {
            this.fromNode = fn; this.fromField = ff; this.toNode = tn; this.toField = tf;
        }
    }

    private static class TimeSensorAnim {
        final String def;
        final double cycleInterval;
        final boolean loop;
        final boolean enabled;
        boolean isRunning;

        TimeSensorAnim(String def, double cycleInterval, boolean loop, boolean enabled) {
            this.def = def;
            this.cycleInterval = cycleInterval > 0 ? cycleInterval : 1.0;
            this.loop = loop;
            this.enabled = enabled;
            this.isRunning = enabled;
        }
    }

    private static class ScalarInterpolatorAnim {
        final String def;
        final float[] key;
        final float[] keyValue;

        ScalarInterpolatorAnim(String def, float[] key, float[] keyValue) {
            this.def = def; this.key = key; this.keyValue = keyValue;
        }

        float evaluate(float fraction) {
            if (key.length == 0) return 0f;
            if (fraction <= key[0]) return keyValue[0];
            if (fraction >= key[key.length - 1]) return keyValue[keyValue.length - 1];
            for (int i = 0; i < key.length - 1; i++) {
                if (fraction >= key[i] && fraction <= key[i + 1]) {
                    float span = key[i + 1] - key[i];
                    float alpha = span > 1e-6f ? (fraction - key[i]) / span : 0f;
                    return keyValue[i] + alpha * (keyValue[i + 1] - keyValue[i]);
                }
            }
            return keyValue[0];
        }
    }

    private static class DisplacerAnim {
        final String def;
        final int[] coordIndex;
        final float[] displacements;
        float currentWeight = 0f;

        DisplacerAnim(String def, int[] coordIndex, float[] displacements) {
            this.def = def; this.coordIndex = coordIndex; this.displacements = displacements;
        }
    }

    private static class DisplacerMeshBinding {
        final DisplacerAnim displacer;
        final Geometry.Triangle geometry;
        Array1D vArray;
        final MemorySegment vSeg;
        final float[] baseCoords;
        final int[] unrolledToOrig;
        float lastWeight = Float.NaN;

        DisplacerMeshBinding(DisplacerAnim displacer, Geometry.Triangle geometry, Array1D vArray,
                             MemorySegment vSeg, float[] baseCoords, int[] unrolledToOrig) {
            this.displacer = displacer;
            this.geometry = geometry;
            this.vArray = vArray;
            this.vSeg = vSeg;
            this.baseCoords = baseCoords.clone();
            this.unrolledToOrig = unrolledToOrig;
        }

        void apply(Device device, Arena arena, float weight) {
            float[] deformed = baseCoords.clone();
            int nDisps = Math.min(displacer.coordIndex.length, displacer.displacements.length / 3);
            for (int i = 0; i < nDisps; i++) {
                int cIdx = displacer.coordIndex[i];
                if (cIdx * 3 + 2 < deformed.length) {
                    deformed[cIdx * 3]     += weight * displacer.displacements[i * 3];
                    deformed[cIdx * 3 + 1] += weight * displacer.displacements[i * 3 + 1];
                    deformed[cIdx * 3 + 2] += weight * displacer.displacements[i * 3 + 2];
                }
            }

            for (int v = 0; v < unrolledToOrig.length; v++) {
                int cIdx = unrolledToOrig[v];
                long offset = (long) v * 3 * Float.BYTES;
                vSeg.set(ValueLayout.JAVA_FLOAT, offset,                  deformed[cIdx * 3]);
                vSeg.set(ValueLayout.JAVA_FLOAT, offset + Float.BYTES,     deformed[cIdx * 3 + 1]);
                vSeg.set(ValueLayout.JAVA_FLOAT, offset + 2 * Float.BYTES, deformed[cIdx * 3 + 2]);
            }

            try {
                Array1D freshArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                                      DataType.FLOAT32_VEC3, unrolledToOrig.length);
                geometry.setVertexPosition(freshArray);
                freshArray.commit();
                geometry.commit();
                vArray = freshArray;
            } catch (Throwable ignored) {}
        }
    }
}

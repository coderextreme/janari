import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.codeberg.anari.api.Array1D;
import org.codeberg.anari.api.DataType;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Group;
import org.codeberg.anari.api.Instance;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.AnariPane;
import org.codeberg.anari.javafx.TimerState;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JanariApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            AnariPane anariPane = new AnariPane();
            org.web3d.x3d.jsail.Core.X3D x3dModel = new ArchHalf().getRootNodeList().get(0);
            anariPane.setHandler(new X3DAnariHandler(x3dModel));

            StackPane root = new StackPane(anariPane);
            Scene scene = new Scene(root, 1024, 768);
            primaryStage.setTitle("Janari: X3D to ANARI Renderer");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

@SuppressWarnings({"rawtypes", "unchecked"})
class X3DAnariHandler extends AbstractHandler {

    private final Object x3dModel;

    // Everything ANARI must see stays reachable here until release().
    private final Arena sceneArena = Arena.ofShared();
    private final List<Object> keepAlive = new ArrayList<>();
    private final List<Instance> anariInstances = new ArrayList<>();
    private Material<?> defaultMaterial;
    private Device device;
    private boolean built = false;

    // Mini proto engine
    private final Map<String, Object> defMap = new HashMap<>();
    private final Map<String, Object> protoMap = new HashMap<>();
    private final Set<Object> visited = new HashSet<>();

    public X3DAnariHandler(Object x3dModel) {
        this.x3dModel = x3dModel;
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        this.device = device;
        System.out.println("initialize(Device): world=" + world + " renderer=" + renderer
                           + " camera=" + camera + " frame=" + frame);
        // world is not created yet at this point, so the scene is built lazily in updateScene()
    }

    @Override
    protected void updateScene(TimerState state) throws Throwable {
        super.updateScene(state);
        if (!built && device != null && world != null) {
            built = true;
            System.out.println("--- Building Janari scene (world is ready) ---");
            try {
                build(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void release(Device device) {
        super.release(device);
        sceneArena.close();
    }

    // ------------------------------------------------------------------

    private void build(Device device) throws Throwable {
        defaultMaterial = device.newMaterial(Material.SubType.MATTE).setColor(0.8f, 0.8f, 0.8f);
        defaultMaterial.commit();
        keepAlive.add(defaultMaterial);

        buildCache(x3dModel);
        System.out.println("Found " + defMap.size() + " DEFs and " + protoMap.size() + " Protos.");

        float[] identity = { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 };
        Object scene = x3dModel.getClass().getMethod("getScene").invoke(x3dModel);
        if (scene != null) {
            traverseList(device, scene.getClass().getMethod("getChildren").invoke(scene),
                         identity, new HashMap<>());
        }

        Light.Directional light = device.newLight(Light.SubType.DIRECTIONAL).setDirection(-1f, -1f, -1f);
        light.commit();
        keepAlive.add(light);

        if (!anariInstances.isEmpty()) {
            world.setInstance(device.newArray1D(anariInstances, DataType.INSTANCE));
        }
        world.setLight(device.newArray1D(List.of(light), DataType.LIGHT));
        world.commit();

        System.out.println("--- Janari Load Complete: " + anariInstances.size() + " boxes ---");
    }

    // ------------------------------------------------------------------
    // X3D traversal (JSAIL objects accessed reflectively)

    private void buildCache(Object node) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);

        try {
            String def = (String) node.getClass().getMethod("getDEF").invoke(node);
            if (def != null && !def.isEmpty()) defMap.put(def, node);
        } catch (Exception e) {}

        if (node.getClass().getSimpleName().contains("ProtoDeclare")) {
            try {
                String name = (String) node.getClass().getMethod("getName").invoke(node);
                if (name != null && !name.isEmpty()) protoMap.put(name, node);
            } catch (Exception e) {}
        }

        for (Method m : node.getClass().getMethods()) {
            if (m.getName().startsWith("get") && m.getParameterCount() == 0 && !m.getName().equals("getClass")) {
                try {
                    Object res = m.invoke(node);
                    if (res instanceof List) {
                        for (Object o : (List<?>) res) buildCache(o);
                    } else if (res != null && res.getClass().getName().contains("x3d")
                               && !res.getClass().isArray()) {
                        buildCache(res);
                    }
                } catch (Exception e) {}
            }
        }
    }

    private void traverseList(Device device, Object children, float[] matrix, Map<String, Object> protoArgs) {
        if (children instanceof List) {
            for (Object c : (List<?>) children) processNode(device, c, matrix, protoArgs);
        } else if (children instanceof Object[]) {
            for (Object c : (Object[]) children) processNode(device, c, matrix, protoArgs);
        }
    }

    private void processNode(Device device, Object node, float[] parentTransform, Map<String, Object> protoArgs) {
        if (node == null) return;

        // USE
        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty() && defMap.containsKey(use)) {
                processNode(device, defMap.get(use), parentTransform, protoArgs);
                return;
            }
        } catch (Exception e) {}

        String cName = node.getClass().getSimpleName();
        if (cName.contains("ProtoDeclare")) return;

        if (cName.contains("Viewpoint")) {
            try {
                float[] pos = (float[]) node.getClass().getMethod("getPosition").invoke(node);
                if (pos != null) {
                    cameraDistance = (float) Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
                    System.out.println("Viewpoint distance -> " + cameraDistance);
                }
            } catch (Exception e) {}
            return;
        }

        if (cName.contains("ProtoInstance")) {
            try {
                String name = (String) node.getClass().getMethod("getName").invoke(node);
                Object protoDecl = protoMap.get(name);
                if (protoDecl != null) {
                    Map<String, Object> newArgs = new HashMap<>(protoArgs);
                    List<?> fvList = (List<?>) node.getClass().getMethod("getFieldValueList").invoke(node);
                    for (Object fv : fvList) {
                        String fName = (String) fv.getClass().getMethod("getName").invoke(fv);
                        Object fVal = fv.getClass().getMethod("getValue").invoke(fv);
                        if (fVal == null || fVal.toString().trim().isEmpty()) {
                            fVal = fv.getClass().getMethod("getChildren").invoke(fv);
                        }
                        newArgs.put(fName, fVal);
                    }
                    Object pBody = protoDecl.getClass().getMethod("getProtoBody").invoke(protoDecl);
                    traverseList(device, pBody.getClass().getMethod("getChildren").invoke(pBody),
                                 parentTransform, newArgs);
                }
            } catch (Exception e) { e.printStackTrace(); }
            return;
        }

        if (cName.contains("Transform")) {
            float[] localMat = { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 };
            Object childrenToTraverse = null;
            try {
                float[] tr = (float[]) node.getClass().getMethod("getTranslation").invoke(node);
                Object isNode = node.getClass().getMethod("getIS").invoke(node);
                if (isNode != null) {
                    List<?> connects = (List<?>) isNode.getClass().getMethod("getConnectList").invoke(isNode);
                    for (Object c : connects) {
                        String nField = (String) c.getClass().getMethod("getNodeField").invoke(c);
                        String pField = (String) c.getClass().getMethod("getProtoField").invoke(c);
                        if (nField.equals("translation") && protoArgs.containsKey(pField)) {
                            Object val = protoArgs.get(pField);
                            if (val instanceof String) {
                                String[] p = ((String) val).trim().split("[,\\s]+");
                                if (p.length >= 3) tr = new float[]{ Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]) };
                            }
                        } else if (nField.equals("children") && protoArgs.containsKey(pField)) {
                            childrenToTraverse = protoArgs.get(pField);
                        }
                    }
                }
                if (tr != null) { localMat[12] = tr[0]; localMat[13] = tr[1]; localMat[14] = tr[2]; }
            } catch (Exception e) {}

            float[] currentMat = multiplyMatrix(parentTransform, localMat);
            try {
                if (childrenToTraverse == null) childrenToTraverse = node.getClass().getMethod("getChildren").invoke(node);
                traverseList(device, childrenToTraverse, currentMat, protoArgs);
            } catch (Exception e) {}
            return;
        }

        if (cName.contains("Shape")) {
            try {
                Instance inst = buildShapeInstance(device, node, parentTransform);
                if (inst != null) anariInstances.add(inst);
            } catch (Throwable t) { t.printStackTrace(); }
            return;
        }

        try {
            traverseList(device, node.getClass().getMethod("getChildren").invoke(node), parentTransform, protoArgs);
        } catch (Exception e) {}
    }

    /** Column-major 4x4 multiply: parent * local. */
    private float[] multiplyMatrix(float[] parent, float[] local) {
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
    // ANARI object construction (typed API, no reflection)

    private Instance buildShapeInstance(Device device, Object shape, float[] transformMatrix) throws Throwable {
        Object x3dGeom = shape.getClass().getMethod("getGeometry").invoke(shape);
        if (x3dGeom == null || !x3dGeom.getClass().getSimpleName().contains("Box")) return null;

        Geometry.Triangle geometry = createBox(device, x3dGeom);

        Material<?> material = defaultMaterial;
        try {
            Object app = shape.getClass().getMethod("getAppearance").invoke(shape);
            Object mat = (app == null) ? null : app.getClass().getMethod("getMaterial").invoke(app);
            if (mat != null) {
                float[] d = (float[]) mat.getClass().getMethod("getDiffuseColor").invoke(mat);
                if (d != null && d.length == 3) {
                    Material.Matte m = device.newMaterial(Material.SubType.MATTE).setColor(d[0], d[1], d[2]);
                    m.commit();
                    keepAlive.add(m);
                    material = m;
                }
            }
        } catch (Exception e) {}

        Surface surface = device.newSurface().setGeometry(geometry).setMaterial(material);
        surface.commit();
        keepAlive.add(surface);

        Group group = device.newGroup();
        group.setSurface(device.newArray1D(List.of(surface), DataType.SURFACE));
        group.commit();
        keepAlive.add(group);

        Instance instance = device.newInstance(Instance.SubType.TRANSFORM);
        instance.setGroup(group);
        instance.setTransform(transformMatrix);
        instance.commit();
        return instance;
    }

    private Geometry.Triangle createBox(Device device, Object box) throws Throwable {
        float x = 1f, y = 1f, z = 1f;
        try {
            float[] s = (float[]) box.getClass().getMethod("getSize").invoke(box);
            if (s != null) { x = s[0] / 2f; y = s[1] / 2f; z = s[2] / 2f; }
        } catch (Exception e) {}

        float[] vertices = { -x,-y,-z,  x,-y,-z,  x,y,-z,  -x,y,-z,
                             -x,-y, z,  x,-y, z,  x,y, z,  -x,y, z };
        int[] indices = { 4,5,6, 6,7,4,  1,0,3, 3,2,1,  5,1,2, 2,6,5,
                          0,4,7, 7,3,0,  7,6,2, 2,3,7,  0,1,5, 5,4,0 };

        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, vertices.length / 3);
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, indices.length / 3);

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        return geom;
    }
}

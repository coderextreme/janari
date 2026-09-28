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
            String which = getParameters().getRaw().isEmpty() ? "BoxEm" : getParameters().getRaw().get(0);
            X3DRoots roots;
            switch (which) {
                case "ArchHalf": roots = new ArchHalf(); break;
                default:         roots = new BoxEm();    which = "BoxEm";
            }
            System.out.println("Loading model: " + which);
            org.web3d.x3d.jsail.Core.X3D x3dModel = roots.getRootNodeList().get(0);
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
    private final float[] bmin = { Float.MAX_VALUE,  Float.MAX_VALUE,  Float.MAX_VALUE };
    private final float[] bmax = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
    private boolean viewpointSet = false;
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

        if (!viewpointSet && bmin[0] <= bmax[0]) {
            float cx = (bmin[0] + bmax[0]) / 2, cy = (bmin[1] + bmax[1]) / 2, cz = (bmin[2] + bmax[2]) / 2;
            float dx = bmax[0] - bmin[0], dy = bmax[1] - bmin[1], dz = bmax[2] - bmin[2];
            float radius = 0.5f * (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            cameraTarget = new float[] { cx, cy, cz };
            cameraDistance = Math.max(radius * 2.6f, 1f);
            System.out.println("Auto-framing camera: target=(" + cx + "," + cy + "," + cz
                               + ") distance=" + cameraDistance);
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
                    viewpointSet = true;
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
        x3dGeom = resolveUse(x3dGeom);
        Geometry.Triangle geometry = createGeometry(device, x3dGeom, transformMatrix);
        if (geometry == null) return null;

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

    private Object resolveUse(Object node) {
        if (node == null) return null;
        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty() && defMap.containsKey(use)) return defMap.get(use);
        } catch (Exception e) {}
        return node;
    }

    /** Geometry dispatch: add a new X3D geometry node type here. */
    private Geometry.Triangle createGeometry(Device device, Object x3dGeom, float[] m) throws Throwable {
        if (x3dGeom == null) return null;
        String g = x3dGeom.getClass().getSimpleName();
        switch (g) {
            case "Box":            return createBox(device, x3dGeom, m);
            case "IndexedFaceSet": return createIndexedFaceSet(device, x3dGeom, m);
            default:
                System.out.println("Unsupported geometry: " + g);
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

    /** IndexedFaceSet: coord.point + coordIndex (faces separated by -1), triangulated by ear clipping. */
    private Geometry.Triangle createIndexedFaceSet(Device device, Object ifs, float[] m) throws Throwable {
        int[] ci = (int[]) ifs.getClass().getMethod("getCoordIndex").invoke(ifs);
        Object coord = resolveUse(ifs.getClass().getMethod("getCoord").invoke(ifs));
        float[] pts = (coord == null) ? null : (float[]) coord.getClass().getMethod("getPoint").invoke(coord);
        if (ci == null || ci.length == 0 || pts == null || pts.length < 9) {
            System.out.println("IndexedFaceSet: missing coordIndex or coord.point, skipped");
            return null;
        }
        int nverts = pts.length / 3;

        List<int[]> tris = new ArrayList<>();
        List<Integer> face = new ArrayList<>();
        int bad = 0;
        for (int idx : ci) {
            if (idx < 0) {
                triangulate(pts, face, tris);
                face.clear();
            } else if (idx < nverts) {
                face.add(idx);
            } else {
                bad++;
            }
        }
        if (!face.isEmpty()) triangulate(pts, face, tris);   // last face without trailing -1
        if (bad > 0) System.out.println("IndexedFaceSet: " + bad + " out-of-range indices ignored");
        if (tris.isEmpty()) return null;

        int[] indices = new int[tris.size() * 3];
        for (int t = 0; t < tris.size(); t++) {
            indices[3*t] = tris.get(t)[0]; indices[3*t+1] = tris.get(t)[1]; indices[3*t+2] = tris.get(t)[2];
        }
        System.out.println("IndexedFaceSet: " + nverts + " vertices, " + tris.size() + " triangles");

        MemorySegment vSeg = sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, pts);
        Array1D vArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.FLOAT32_VEC3, nverts);
        MemorySegment iSeg = sceneArena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                                           DataType.UINT32_VEC3, tris.size());

        Geometry.Triangle geom = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);
        geom.commit();
        keepAlive.add(vArray);
        keepAlive.add(iArray);
        keepAlive.add(geom);
        addBounds(pts, m);
        return geom;
    }

    /**
     * Triangulates one (possibly concave) planar polygon by ear clipping.
     * The polygon is projected onto the plane's dominant axis (Newell normal).
     */
    private static void triangulate(float[] pts, List<Integer> face, List<int[]> out) {
        int n0 = face.size();
        if (n0 < 3) return;
        if (n0 == 3) { out.add(new int[]{ face.get(0), face.get(1), face.get(2) }); return; }

        // Newell normal
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n0; i++) {
            int a = face.get(i) * 3, b = face.get((i + 1) % n0) * 3;
            nx += (pts[a+1] - pts[b+1]) * (pts[a+2] + pts[b+2]);
            ny += (pts[a+2] - pts[b+2]) * (pts[a]   + pts[b]);
            nz += (pts[a]   - pts[b])   * (pts[a+1] + pts[b+1]);
        }
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        int drop = (ax >= ay && ax >= az) ? 0 : (ay >= az ? 1 : 2);
        double orient = (drop == 0 ? nx : drop == 1 ? ny : nz) >= 0 ? 1.0 : -1.0;

        List<Integer> v = new ArrayList<>(face);
        int guard = 0;
        while (v.size() > 3 && guard++ < 100000) {
            int n = v.size();
            boolean clipped = false;
            for (int i = 0; i < n; i++) {
                int a = v.get((i + n - 1) % n), b = v.get(i), c = v.get((i + 1) % n);
                double cr = cross2(pts, drop, a, b, c) * orient;
                if (cr < -1e-12) continue;                       // reflex vertex
                boolean inside = false;
                for (int k = 0; k < n && !inside; k++) {
                    int p = v.get(k);
                    if (p == a || p == b || p == c) continue;
                    inside = strictlyInside(pts, drop, p, a, b, c, orient);
                }
                if (inside) continue;
                if (cr > 1e-12) out.add(new int[]{ a, b, c });    // skip zero-area (collinear) ears
                v.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {                                       // degenerate polygon: force progress
                int n2 = v.size();
                out.add(new int[]{ v.get(n2 - 1), v.get(0), v.get(1) });
                v.remove(0);
            }
        }
        if (v.size() == 3) out.add(new int[]{ v.get(0), v.get(1), v.get(2) });
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
        } catch (Exception e) {}

        float[] vertices = { -x,-y,-z,  x,-y,-z,  x,y,-z,  -x,y,-z,
                             -x,-y, z,  x,-y, z,  x,y, z,  -x,y, z };
        int[] indices = { 4,5,6, 6,7,4,  1,0,3, 3,2,1,  5,1,2, 2,6,5,
                          0,4,7, 7,3,0,  7,6,2, 2,3,7,  0,1,5, 5,4,0 };

        addBounds(vertices, m);
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

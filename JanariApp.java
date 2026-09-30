import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
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

import org.web3d.x3d.jsail.Core.*;
import org.web3d.x3d.jsail.Geometry3D.*;
import org.web3d.x3d.jsail.Grouping.*;
import org.web3d.x3d.jsail.HAnim.*;
import org.web3d.x3d.jsail.Navigation.*;
import org.web3d.x3d.jsail.NURBS.*;
import org.web3d.x3d.jsail.EnvironmentalEffects.*;
import org.web3d.x3d.jsail.Rendering.*;
import org.web3d.x3d.jsail.Shape.*;
import org.web3d.x3d.jsail.Texturing.*;

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
import java.util.*;

public class JanariApp extends Application {
    private static String modelName = "rubik";

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
                "net.coderextreme." + which,
                "net.coderextreme.data.rubik",
                "net.coderextreme.data.rubikOnFire"
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
                roots = new net.coderextreme.data.rubik();
                modelName = "net.coderextreme.data.rubik";
            }

            org.web3d.x3d.jsail.Core.X3D x3dModel = roots.getRootNodeList().get(0);
            X3DAnariHandler handler = new X3DAnariHandler(x3dModel);
            anariPane.setHandler(handler);

            for (String mName : List.of("setAnimated", "startAnimation", "setContinuous", "start")) {
                try {
                    Method enableAnimMethod = anariPane.getClass().getMethod(mName, boolean.class);
                    enableAnimMethod.invoke(anariPane, true);
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
                                break;
                            } catch (Exception ignored) {}
                        }
                        if (repaintMethod == null) {
                            Class<?> cl = anariPane.getClass();
                            while (cl != null && cl != Object.class) {
                                for (Method m : cl.getDeclaredMethods()) {
                                    if (m.getParameterCount() == 0) {
                                        String n = m.getName().toLowerCase();
                                        if (n.contains("render") || n.contains("repaint")) {
                                            m.setAccessible(true);
                                            repaintMethod = m;
                                            break;
                                        }
                                    }
                                }
                                if (repaintMethod != null) break;
                                cl = cl.getSuperclass();
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

            StackPane root = new StackPane(anariPane);
            Scene scene = new Scene(root, 1024, 768);
            primaryStage.setTitle("Janari: " + modelName);
            primaryStage.setScene(scene);

            primaryStage.setOnCloseRequest(event -> {
                animTimer.stop();
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            modelName = args[0];
        }
        launch(args);
    }
}

// ============================================================================
// 3D MATH & GEOMETRIC ALGORITHMS
// ============================================================================

class AnariMath {
    static final int LINE_SIDES = 6;
    static final float[] LINE_COS = new float[LINE_SIDES];
    static final float[] LINE_SIN = new float[LINE_SIDES];

    static {
        for (int i = 0; i < LINE_SIDES; i++) {
            double angle = 2.0 * Math.PI * i / LINE_SIDES;
            LINE_COS[i] = (float) Math.cos(angle);
            LINE_SIN[i] = (float) Math.sin(angle);
        }
    }

    static void cross(float[] a, float[] b, float[] out) {
        out[0] = a[1] * b[2] - a[2] * b[1];
        out[1] = a[2] * b[0] - a[0] * b[2];
        out[2] = a[0] * b[1] - a[1] * b[0];
    }

    static void normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len > 1e-6f) {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        }
    }

    static float[] multiplyMatrix(float[] parent, float[] local) {
        float[] r = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                r[row + c * 4] = parent[row] * local[c * 4]
                               + parent[row + 4] * local[1 + c * 4]
                               + parent[row + 8] * local[2 + c * 4]
                               + parent[row + 12] * local[3 + c * 4];
            }
        }
        return r;
    }

    static float[] buildTransformMatrix(float[] tr, float[] sc, float[] rot) {
        float sx = (sc != null && sc.length >= 3) ? sc[0] : 1f;
        float sy = (sc != null && sc.length >= 3) ? sc[1] : 1f;
        float sz = (sc != null && sc.length >= 3) ? sc[2] : 1f;

        float[] m = { sx, 0, 0, 0,  0, sy, 0, 0,  0, 0, sz, 0,  0, 0, 0, 1 };

        if (rot != null && rot.length >= 4 && Math.abs(rot[3]) > 1e-6f) {
            float ax = rot[0], ay = rot[1], az = rot[2], angle = rot[3];
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (len > 1e-6f) {
                ax /= len; ay /= len; az /= len;
                float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
                float t = 1f - c;
                float[] r = {
                    t * ax * ax + c,       t * ax * ay + s * az, t * ax * az - s * ay, 0,
                    t * ax * ay - s * az, t * ay * ay + c,       t * ay * az + s * ax, 0,
                    t * ax * az + s * ay, t * ay * az - s * ax, t * az * az + c,       0,
                    0,                     0,                     0,                    1
                };
                m = multiplyMatrix(r, m);
            }
        }

        if (tr != null && tr.length >= 3) {
            m[12] += tr[0];
            m[13] += tr[1];
            m[14] += tr[2];
        }
        return m;
    }

    static float[] invertRigidMatrix(float[] m) {
        float[] r = {
            m[0], m[1], m[2], 0,
            m[4], m[5], m[6], 0,
            m[8], m[9], m[10], 0,
            0, 0, 0, 1
        };
        r[12] = -(r[0] * m[12] + r[4] * m[13] + r[8] * m[14]);
        r[13] = -(r[1] * m[12] + r[5] * m[13] + r[9] * m[14]);
        r[14] = -(r[2] * m[12] + r[6] * m[13] + r[10] * m[14]);
        return r;
    }

    static float[] transformPoint(float[] m, float x, float y, float z) {
        return new float[]{
            m[0] * x + m[4] * y + m[8] * z + m[12],
            m[1] * x + m[5] * y + m[9] * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14]
        };
    }

    static float[] axisAngleToQuat(float[] aa) {
        float ax = aa[0], ay = aa[1], az = aa[2], angle = aa[3];
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1e-8f || Math.abs(angle) < 1e-8f) return new float[]{1, 0, 0, 0};
        ax /= len; ay /= len; az /= len;
        float h = angle * 0.5f, s = (float) Math.sin(h);
        return new float[]{(float) Math.cos(h), ax * s, ay * s, az * s};
    }

    static float[] slerpAxisAngle(float[] a, float[] b, float t) {
        float[] qa = axisAngleToQuat(a), qb = axisAngleToQuat(b);
        float dot = qa[0] * qb[0] + qa[1] * qb[1] + qa[2] * qb[2] + qa[3] * qb[3];
        if (dot < 0f) {
            dot = -dot;
            for (int i = 0; i < 4; i++) qb[i] = -qb[i];
        }

        float w1, w2;
        if (dot > 0.9995f) {
            w1 = 1f - t; w2 = t;
        } else {
            double theta = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            double sinTheta = Math.sin(theta);
            w1 = (float) (Math.sin((1.0 - t) * theta) / sinTheta);
            w2 = (float) (Math.sin(t * theta) / sinTheta);
        }

        float w = w1 * qa[0] + w2 * qb[0];
        float x = w1 * qa[1] + w2 * qb[1];
        float y = w1 * qa[2] + w2 * qb[2];
        float z = w1 * qa[3] + w2 * qb[3];
        float len = (float) Math.sqrt(w * w + x * x + y * y + z * z);
        if (len > 1e-8f) { w /= len; x /= len; y /= len; z /= len; }

        float angle = 2f * (float) Math.acos(Math.max(-1f, Math.min(1f, w)));
        float sinHalf = (float) Math.sqrt(Math.max(0f, 1f - w * w));
        if (sinHalf < 1e-6f) return new float[]{1, 0, 0, 0};
        return new float[]{x / sinHalf, y / sinHalf, z / sinHalf, angle};
    }

    static void applyTextureTransform(float[] uvs, X3DAnariHandler.TextureTransformAnim tt) {
        if (uvs == null || tt == null) return;
        float cos = (float) Math.cos(tt.rotation), sin = (float) Math.sin(tt.rotation);
        float cx = tt.center[0], cy = tt.center[1];
        float sx = tt.scale[0], sy = tt.scale[1];
        float tx = tt.translation[0], ty = tt.translation[1];

        int n = uvs.length / 2;
        for (int i = 0; i < n; i++) {
            float u = uvs[i * 2], v = uvs[i * 2 + 1];
            float du = u - cx, dv = v - cy;
            uvs[i * 2]     = (du * cos - dv * sin) * sx + cx + tx;
            uvs[i * 2 + 1] = (du * sin + dv * cos) * sy + cy + ty;
        }
    }

    static List<float[]> recomputeFaceNormals(float[] pts, List<int[]> tris, List<Integer> triFaceId) {
        int maxFaceId = 0;
        for (int id : triFaceId) if (id > maxFaceId) maxFaceId = id;
        List<float[]> normals = new ArrayList<>(maxFaceId + 1);
        for (int i = 0; i <= maxFaceId; i++) normals.add(new float[3]);

        for (int t = 0; t < tris.size(); t++) {
            int[] tri = tris.get(t);
            int a = tri[0] * 3, b = tri[1] * 3, c = tri[2] * 3;
            float abx = pts[b] - pts[a], aby = pts[b + 1] - pts[a + 1], abz = pts[b + 2] - pts[a + 2];
            float acx = pts[c] - pts[a], acy = pts[c + 1] - pts[a + 1], acz = pts[c + 2] - pts[a + 2];
            float[] n = normals.get(triFaceId.get(t));
            n[0] += aby * acz - abz * acy;
            n[1] += abz * acx - abx * acz;
            n[2] += abx * acy - aby * acx;
        }
        for (float[] n : normals) normalize(n);
        return normals;
    }

    static float[] computeSmoothNormals(float[] pts, List<int[]> tris, List<Integer> triFaceId,
                                        List<float[]> faceNormals, List<Integer>[] vertFaces,
                                        float creaseAngle, int[] unrolledToOrigCoord) {
        int totalVerts = unrolledToOrigCoord.length;
        float[] unrolledNormals = new float[totalVerts * 3];
        float cosCrease = (float) Math.cos(Math.max(0.0, Math.min(Math.PI, creaseAngle)));

        int totalTris = tris.size();
        for (int t = 0; t < totalTris; t++) {
            int fId = triFaceId.get(t);
            float[] fn = faceNormals.get(fId);

            for (int corner = 0; corner < 3; corner++) {
                int vertIdx = t * 3 + corner;
                int cIdx = unrolledToOrigCoord[vertIdx];

                if (creaseAngle <= 1e-4f) {
                    unrolledNormals[vertIdx * 3]     = fn[0];
                    unrolledNormals[vertIdx * 3 + 1] = fn[1];
                    unrolledNormals[vertIdx * 3 + 2] = fn[2];
                } else {
                    float sx = 0f, sy = 0f, sz = 0f;
                    List<Integer> adjFaces = vertFaces[cIdx];
                    for (int otherFaceId : adjFaces) {
                        float[] otherFn = faceNormals.get(otherFaceId);
                        float dot = fn[0] * otherFn[0] + fn[1] * otherFn[1] + fn[2] * otherFn[2];
                        if (dot >= cosCrease) {
                            sx += otherFn[0]; sy += otherFn[1]; sz += otherFn[2];
                        }
                    }
                    float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
                    if (len > 1e-6f) { sx /= len; sy /= len; sz /= len; }
                    else { sx = fn[0]; sy = fn[1]; sz = fn[2]; }
                    unrolledNormals[vertIdx * 3]     = sx;
                    unrolledNormals[vertIdx * 3 + 1] = sy;
                    unrolledNormals[vertIdx * 3 + 2] = sz;
                }
            }
        }
        return unrolledNormals;
    }

    static void triangulatePolygon(float[] pts, List<Integer> faceCoord, List<Integer> faceUV, List<Integer> faceCol,
                                   List<int[]> outCoord, List<int[]> outUV, List<int[]> outCol) {
        int n0 = faceCoord.size();
        if (n0 < 3) return;

        if (n0 == 3) {
            outCoord.add(new int[]{ faceCoord.get(0), faceCoord.get(1), faceCoord.get(2) });
            if (outUV != null && faceUV != null && faceUV.size() >= 3) {
                outUV.add(new int[]{ faceUV.get(0), faceUV.get(1), faceUV.get(2) });
            }
            if (outCol != null && faceCol != null && faceCol.size() >= 3) {
                outCol.add(new int[]{ faceCol.get(0), faceCol.get(1), faceCol.get(2) });
            }
            return;
        }

        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n0; i++) {
            int a = faceCoord.get(i) * 3, b = faceCoord.get((i + 1) % n0) * 3;
            nx += (pts[a + 1] - pts[b + 1]) * (pts[a + 2] + pts[b + 2]);
            ny += (pts[a + 2] - pts[b + 2]) * (pts[a]     + pts[b]);
            nz += (pts[a]     - pts[b])     * (pts[a + 1] + pts[b + 1]);
        }
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        int drop = (ax >= ay && ax >= az) ? 0 : (ay >= az ? 1 : 2);
        double orient = (drop == 0 ? nx : drop == 1 ? ny : nz) >= 0 ? 1.0 : -1.0;

        List<Integer> v = new ArrayList<>(faceCoord);
        List<Integer> u = (faceUV != null) ? new ArrayList<>(faceUV) : null;
        List<Integer> cList = (faceCol != null) ? new ArrayList<>(faceCol) : null;
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
                    if (outCol != null && cList != null) {
                        outCol.add(new int[]{ cList.get((i + n - 1) % n), cList.get(i), cList.get((i + 1) % n) });
                    }
                }
                v.remove(i);
                if (u != null) u.remove(i);
                if (cList != null) cList.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) {
                int n2 = v.size();
                outCoord.add(new int[]{ v.get(n2 - 1), v.get(0), v.get(1) });
                if (outUV != null && u != null) {
                    outUV.add(new int[]{ u.get(n2 - 1), u.get(0), u.get(1) });
                }
                if (outCol != null && cList != null) {
                    outCol.add(new int[]{ cList.get(n2 - 1), cList.get(0), cList.get(1) });
                }
                v.remove(0);
                if (u != null) u.remove(0);
                if (cList != null) cList.remove(0);
            }
        }
        if (v.size() == 3) {
            outCoord.add(new int[]{ v.get(0), v.get(1), v.get(2) });
            if (outUV != null && u != null && u.size() >= 3) {
                outUV.add(new int[]{ u.get(0), u.get(1), u.get(2) });
            }
            if (outCol != null && cList != null && cList.size() >= 3) {
                outCol.add(new int[]{ cList.get(0), cList.get(1), cList.get(2) });
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
        return (cross2(p, d, a, b, q) * orient > e) && (cross2(p, d, b, c, q) * orient > e) && (cross2(p, d, c, a, q) * orient > e);
    }
}

// ============================================================================
// ANARI CONTEXT & INTERFACES
// ============================================================================

interface AnariNode {
    void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs);
}

interface AnariGeometry {
    Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable;
    boolean hasVertexColors();
}

class AnariContext {
    final Device device;
    final Arena arena;
    final List<Object> keepAlive;
    final List<Instance> anariInstances;
    final Map<Instance, float[]> instanceTransforms = new HashMap<>();
    final Map<String, Sampler> textureCache;
    final Map<String, Object> defMap;
    final Map<String, Object> protoMap;
    final Material<?> defaultMaterial;

    final float[] bmin = { Float.MAX_VALUE,  Float.MAX_VALUE,  Float.MAX_VALUE };
    final float[] bmax = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
    boolean viewpointSet = false;
    float[] cameraTarget = {0f, 0f, 0f};
    float cameraDistance = 12f;

    static final boolean FLIP_Y = true;
    static final boolean SWAP_RED_BLUE = true;
    static final boolean SRGB_ENCODE_COLORS = false;

    final List<X3DAnariHandler.DisplacerMeshBinding> activeBindings = new ArrayList<>();
    final List<X3DAnariHandler.SkinMeshBinding> skinBindings = new ArrayList<>();
    final List<X3DAnariHandler.TextureTransformBinding> textureBindings = new ArrayList<>();
    final Map<String, X3DAnariHandler.TransformAnim> animatedTransforms = new HashMap<>();
    final Map<String, X3DAnariHandler.JointAnim> joints = new HashMap<>();
    final Deque<X3DAnariHandler.JointAnim> jointStack = new ArrayDeque<>();
    final Map<String, X3DAnariHandler.DisplacerAnim> displacers = new HashMap<>();
    final Map<String, X3DAnariHandler.TextureTransformAnim> textureTransforms = new HashMap<>();

    AnariContext(Device device, Arena arena, List<Object> keepAlive,
                 List<Instance> anariInstances, Map<String, Sampler> textureCache,
                 Map<String, Object> defMap, Map<String, Object> protoMap,
                 Material<?> defaultMaterial) {
        this.device = device;
        this.arena = arena;
        this.keepAlive = keepAlive;
        this.anariInstances = anariInstances;
        this.textureCache = textureCache;
        this.defMap = defMap;
        this.protoMap = protoMap;
        this.defaultMaterial = defaultMaterial;
    }

    float[] displayColor(float r, float g, float b) {
        if (SRGB_ENCODE_COLORS) {
            r = r <= 0.04045f ? r / 12.92f : (float) Math.pow((r + 0.055) / 1.055, 2.4);
            g = g <= 0.04045f ? g / 12.92f : (float) Math.pow((g + 0.055) / 1.055, 2.4);
            b = b <= 0.04045f ? b / 12.92f : (float) Math.pow((b + 0.055) / 1.055, 2.4);
        }
        return SWAP_RED_BLUE ? new float[]{ b, g, r } : new float[]{ r, g, b };
    }

    float[] fallbackObjectColor(Object seed, boolean skin) {
        if (skin) return displayColor(0.86f, 0.68f, 0.54f);

        final float[][] palette = {
            {0.90f, 0.24f, 0.20f}, // red
            {0.96f, 0.55f, 0.16f}, // orange
            {0.95f, 0.78f, 0.20f}, // gold
            {0.34f, 0.78f, 0.36f}, // green
            {0.18f, 0.72f, 0.82f}, // cyan
            {0.22f, 0.42f, 0.88f}, // blue
            {0.52f, 0.34f, 0.86f}, // violet
            {0.82f, 0.32f, 0.68f}, // magenta
            {0.16f, 0.70f, 0.58f}, // teal
            {0.76f, 0.48f, 0.22f}  // copper
        };

        int hash = System.identityHashCode(seed);
        int index = Math.floorMod(hash, palette.length);
        float[] c = palette[index];
        return displayColor(c[0], c[1], c[2]);
    }

    void addBounds(float[] pts, float[] m) {
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

    Object resolveUse(Object node) {
        if (node == null) return null;
        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty() && defMap.containsKey(use)) return defMap.get(use);
        } catch (Exception ignored) {}
        return node;
    }

    void setAnariObjectParameter(org.codeberg.anari.api.Object<?> target, String paramName, DataType type, Object anariObj) {
        if (target == null || anariObj == null) return;
        String cleanParam = paramName.replace(".", "").replace("_", "").toLowerCase();

        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() == 1) {
                String mName = m.getName().toLowerCase();
                if (mName.endsWith(cleanParam) || mName.equals("set" + cleanParam)
                        || (cleanParam.contains("attribute0") && mName.contains("attribute0"))
                        || (cleanParam.contains("normal") && mName.contains("normal"))
                        || (cleanParam.contains("color") && (mName.contains("color") || mName.endsWith("color")))) {
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

        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals("set")) {
                if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class) {
                    try {
                        m.invoke(target, paramName, anariObj);
                        return;
                    } catch (Throwable ignored) {}
                } else if (m.getParameterCount() == 3 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1] == DataType.class) {
                    try {
                        m.invoke(target, paramName, type, anariObj);
                        return;
                    } catch (Throwable ignored) {}
                }
            }
        }

        MemorySegment h = getAnariHandle(anariObj);
        if (h != null) {
            MemorySegment ptr = arena.allocateFrom(ValueLayout.ADDRESS, h);
            try { target.set(paramName, type, ptr); return; } catch (Throwable ignored) {}
            try { target.set(paramName, type, h); } catch (Throwable ignored) {}
        }
    }

    MemorySegment getAnariHandle(Object anariObj) {
        if (anariObj == null) return null;
        if (anariObj instanceof MemorySegment) return (MemorySegment) anariObj;
        for (String name : List.of("handle", "segment", "getHandle", "getSegment", "address", "rawAddress")) {
            try {
                Method m = anariObj.getClass().getMethod(name);
                Object res = m.invoke(anariObj);
                if (res instanceof MemorySegment) return (MemorySegment) res;
                if (res instanceof Long) return MemorySegment.ofAddress((Long) res);
            } catch (Exception ignored) {}
        }
        Class<?> cl = anariObj.getClass();
        while (cl != null && cl != Object.class) {
            for (Field f : cl.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object res = f.get(anariObj);
                    if (res instanceof MemorySegment) return (MemorySegment) res;
                    if (res instanceof Long) return MemorySegment.ofAddress((Long) res);
                } catch (Exception ignored) {}
            }
            for (Method m : cl.getDeclaredMethods()) {
                if (m.getParameterCount() == 0) {
                    String mn = m.getName().toLowerCase();
                    if (mn.contains("handle") || mn.contains("segment") || mn.contains("address")) {
                        m.setAccessible(true);
                        try {
                            Object res = m.invoke(anariObj);
                            if (res instanceof MemorySegment) return (MemorySegment) res;
                            if (res instanceof Long) return MemorySegment.ofAddress((Long) res);
                        } catch (Exception ignored) {}
                    }
                }
            }
            cl = cl.getSuperclass();
        }
        return null;
    }

    X3DAnariHandler.JointAnim registerJoint(Object node) {
        String def = X3DTypeAdapter.asString(node, "getDEF");
        String name = X3DTypeAdapter.asString(node, "getName");
        if ((def == null || def.isEmpty()) && (name == null || name.isEmpty())) return null;

        String key = (def != null && !def.isEmpty()) ? def : name;
        X3DAnariHandler.JointAnim existing = joints.get(key);
        if (existing != null) return existing;

        float[] center = X3DTypeAdapter.asFloatArray(node, "getCenter");
        if (center == null || center.length < 3) center = new float[]{0f, 0f, 0f};

        float[] rotation = X3DTypeAdapter.asFloatArray(node, "getRotation");
        if (rotation == null || rotation.length < 4) rotation = new float[]{0f, 0f, 1f, 0f};

        float[] translation = X3DTypeAdapter.asFloatArray(node, "getTranslation");
        if (translation == null || translation.length < 3) translation = new float[]{0f, 0f, 0f};

        int[] skinIndex = X3DTypeAdapter.asIntArray(node, "getSkinCoordIndex");
        float[] skinWeight = X3DTypeAdapter.asFloatArray(node, "getSkinCoordWeight");

        X3DAnariHandler.JointAnim parent = jointStack.peek();
        X3DAnariHandler.JointAnim j = new X3DAnariHandler.JointAnim(def, name, center,
                                                                   rotation, translation, skinIndex, skinWeight, parent);
        if (def != null && !def.isEmpty()) joints.put(def, j);
        if (name != null && !name.isEmpty()) joints.put(name, j);
        return j;
    }

    void computeBindMatrices() {
        for (X3DAnariHandler.JointAnim j : joints.values()) {
            computeBindMatrix(j);
        }
    }

    float[] computeBindMatrix(X3DAnariHandler.JointAnim j) {
        if (j.bindMatrix != null) return j.bindMatrix;
        float[] local = X3DAnariHandler.buildHAnimJointMatrix(j);
        j.bindMatrix = j.parent == null
            ? local
            : AnariMath.multiplyMatrix(computeBindMatrix(j.parent), local);
        return j.bindMatrix;
    }

    float[] computeCurrentJointMatrix(X3DAnariHandler.JointAnim j, Map<X3DAnariHandler.JointAnim, float[]> cache) {
        float[] cached = cache.get(j);
        if (cached != null) return cached;

        float[] local = X3DAnariHandler.buildHAnimJointMatrix(j);
        float[] current = j.parent == null
            ? local
            : AnariMath.multiplyMatrix(computeCurrentJointMatrix(j.parent, cache), local);
        cache.put(j, current);
        return current;
    }

    X3DAnariHandler.SkinMeshBinding createSkinBinding(Geometry.Triangle geom, Array1D vArray,
                                                      MemorySegment vSeg, Array1D nArray,
                                                      MemorySegment nSeg, float[] pts,
                                                      int[] unrolledToOrigCoord,
                                                      List<int[]> tris, List<Integer> triFaceId,
                                                      List<Integer>[] vertFaces, float creaseAngle) {
        if (joints.isEmpty() || pts == null) return null;

        int nOriginal = pts.length / 3;
        @SuppressWarnings("unchecked")
        List<X3DAnariHandler.SkinInfluence>[] influences = new List[nOriginal];
        for (int i = 0; i < nOriginal; i++) influences[i] = new ArrayList<>();

        for (X3DAnariHandler.JointAnim j : joints.values()) {
            if (j.skinCoordIndex == null || j.skinCoordWeight == null) continue;
            int n = Math.min(j.skinCoordIndex.length, j.skinCoordWeight.length);
            for (int i = 0; i < n; i++) {
                int coordIndex = j.skinCoordIndex[i];
                if (coordIndex >= 0 && coordIndex < nOriginal) {
                    float w = j.skinCoordWeight[i];
                    if (Math.abs(w) > 1e-7f) influences[coordIndex].add(new X3DAnariHandler.SkinInfluence(j, w));
                }
            }
        }

        int influenced = 0;
        for (List<X3DAnariHandler.SkinInfluence> list : influences) {
            float sum = 0f;
            for (X3DAnariHandler.SkinInfluence inf : list) sum += inf.weight;
            if (sum > 1e-6f) {
                for (int i = 0; i < list.size(); i++) {
                    X3DAnariHandler.SkinInfluence inf = list.get(i);
                    list.set(i, new X3DAnariHandler.SkinInfluence(inf.joint, inf.weight / sum));
                }
                influenced++;
            }
        }

        if (influenced == 0) return null;
        computeBindMatrices();

        return new X3DAnariHandler.SkinMeshBinding(this, geom, vArray, vSeg, nArray, nSeg, pts,
                                                   unrolledToOrigCoord, tris, triFaceId, vertFaces,
                                                   creaseAngle, influences);
    }
}

// ============================================================================
// STRONG-TYPED FIELD CONVERTER & NODE UNWRAPPER
// ============================================================================

class X3DTypeAdapter {
    static String cleanQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        while (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s.replace("\"", "").trim();
    }

    static List<?> getListFromNode(Object node, String... candidateMethods) {
        if (node == null) return Collections.emptyList();
        for (String mName : candidateMethods) {
            try {
                Method m = node.getClass().getMethod(mName);
                if (m.getParameterCount() == 0) {
                    Object res = m.invoke(node);
                    if (res instanceof List<?>) return (List<?>) res;
                    if (res instanceof Object[]) return List.of((Object[]) res);
                }
            } catch (Exception ignored) {}
        }
        for (Method m : node.getClass().getMethods()) {
            if (m.getParameterCount() == 0) {
                String lc = m.getName().toLowerCase();
                for (String c : candidateMethods) {
                    if (lc.equals(c.toLowerCase()) || lc.equals("get" + c.toLowerCase())) {
                        try {
                            Object res = m.invoke(node);
                            if (res instanceof List<?>) return (List<?>) res;
                            if (res instanceof Object[]) return List.of((Object[]) res);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    static Object unwrapNode(Object o) {
        if (o == null) return null;
        if (o instanceof List<?>) {
            List<?> list = (List<?>) o;
            for (Object item : list) {
                Object unwrapped = unwrapNode(item);
                if (unwrapped != null) return unwrapped;
            }
            return null;
        }
        if (o instanceof Object[]) {
            Object[] arr = (Object[]) o;
            for (Object item : arr) {
                Object unwrapped = unwrapNode(item);
                if (unwrapped != null) return unwrapped;
            }
            return null;
        }
        return o;
    }

    static Object extractFieldValue(Object fv) {
        if (fv == null) return null;
        for (String m : List.of("getValue", "getValueString", "getValueStr")) {
            String s = asString(fv, m);
            if (s != null && !s.isEmpty()) return s;
        }
        for (String m : List.of("getChildren", "getChildrenList", "getChildList", "getNodes", "findChildren")) {
            List<?> list = getListFromNode(fv, m);
            if (!list.isEmpty()) {
                Object unwrapped = unwrapNode(list);
                if (unwrapped != null) return unwrapped;
            }
        }
        Class<?> cl = fv.getClass();
        while (cl != null && cl != Object.class) {
            for (Field f : cl.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object res = f.get(fv);
                    if (res != null) {
                        if (res instanceof String) {
                            String s = cleanQuotes((String) res);
                            if (!s.isEmpty()) return s;
                        } else {
                            Object unwrapped = unwrapNode(res);
                            if (unwrapped != null) return unwrapped;
                        }
                    }
                } catch (Exception ignored) {}
            }
            cl = cl.getSuperclass();
        }
        return null;
    }

    static float[] asFloatArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toFloatArray(res);
        } catch (Exception ignored) { return null; }
    }

    static double[] asDoubleArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toDoubleArray(res);
        } catch (Exception ignored) { return null; }
    }

    static int[] asIntArray(Object node, String method) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            return toIntArray(res);
        } catch (Exception ignored) { return null; }
    }

    static String asString(Object node, String method) {
        if (node == null) return null;
        try {
            Object res = null;
            try {
                Method m = node.getClass().getMethod(method);
                res = m.invoke(node);
            } catch (NoSuchMethodException e) {
                for (Method m : node.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getName().equalsIgnoreCase(method)) {
                        res = m.invoke(node);
                        break;
                    }
                }
            }
            if (res == null) return null;
            if (res instanceof String) return cleanQuotes((String) res);
            try {
                Method mVal = res.getClass().getMethod("getValue");
                Object v = mVal.invoke(res);
                if (v != null) return cleanQuotes(v.toString());
            } catch (Exception ignored) {}
            return cleanQuotes(res.toString());
        } catch (Exception ignored) { return null; }
    }

    static double asDouble(Object node, String method, double fallback) {
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

    static int asInt(Object node, String method, int fallback) {
        try {
            Object res = node.getClass().getMethod(method).invoke(node);
            if (res instanceof Number) return ((Number) res).intValue();
            if (res != null) {
                Method mVal = res.getClass().getMethod("getValue");
                Object v = mVal.invoke(res);
                if (v instanceof Number) return ((Number) v).intValue();
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    static boolean asBoolean(Object node, String method, boolean fallback) {
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

    static float[] toFloatArray(Object o) {
        if (o == null) return null;
        if (o instanceof float[]) return (float[]) o;
        if (o instanceof double[]) {
            double[] da = (double[]) o;
            float[] fa = new float[da.length];
            for (int i = 0; i < da.length; i++) fa[i] = (float) da[i];
            return fa;
        }
        for (String mName : List.of("getValue", "getArray")) {
            try {
                Object res = o.getClass().getMethod(mName).invoke(o);
                if (res instanceof float[]) return (float[]) res;
                if (res instanceof double[]) {
                    double[] da = (double[]) res;
                    float[] fa = new float[da.length];
                    for (int i = 0; i < da.length; i++) fa[i] = (float) da[i];
                    return fa;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    static double[] toDoubleArray(Object o) {
        if (o == null) return null;
        if (o instanceof double[]) return (double[]) o;
        if (o instanceof float[]) {
            float[] fa = (float[]) o;
            double[] da = new double[fa.length];
            for (int i = 0; i < fa.length; i++) da[i] = fa[i];
            return da;
        }
        for (String mName : List.of("getValue", "getArray")) {
            try {
                Object res = o.getClass().getMethod(mName).invoke(o);
                if (res instanceof double[]) return (double[]) res;
                if (res instanceof float[]) {
                    float[] fa = (float[]) res;
                    double[] da = new double[fa.length];
                    for (int i = 0; i < fa.length; i++) da[i] = fa[i];
                    return da;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    static int[] toIntArray(Object o) {
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

    static float[] textureCoordinatePoints(Object tcNode) {
        if (tcNode == null) return null;

        float[] direct = asFloatArray(tcNode, "getPoint");
        if (direct != null && direct.length >= 2) return direct;

        for (String method : List.of("getTexCoord", "getTexCoordList", "getTextureCoordinate", "getTextureCoordinateList")) {
            try {
                Object value = tcNode.getClass().getMethod(method).invoke(tcNode);
                float[] found = textureCoordinatePointsRecursive(value);
                if (found != null) return found;
            } catch (Exception ignored) {}
        }
        return textureCoordinatePointsRecursive(tcNode);
    }

    private static float[] textureCoordinatePointsRecursive(Object value) {
        if (value == null) return null;
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                float[] found = textureCoordinatePointsMapped(item, "TEXCOORD_0");
                if (found != null) return found;
            }
            for (Object item : (List<?>) value) {
                float[] found = textureCoordinatePointsRecursive(item);
                if (found != null) return found;
            }
            return null;
        }
        if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                float[] found = textureCoordinatePointsMapped(item, "TEXCOORD_0");
                if (found != null) return found;
            }
            for (Object item : (Object[]) value) {
                float[] found = textureCoordinatePointsRecursive(item);
                if (found != null) return found;
            }
            return null;
        }
        float[] mapped = textureCoordinatePointsMapped(value, "TEXCOORD_0");
        if (mapped != null) return mapped;
        float[] direct = asFloatArray(value, "getPoint");
        if (direct != null && direct.length >= 2) return direct;

        for (String m : List.of("getArray", "getValue", "getNodes", "getChildren", "getChildrenList")) {
            try {
                Object nested = value.getClass().getMethod(m).invoke(value);
                if (nested != value) {
                    float[] found = textureCoordinatePointsRecursive(nested);
                    if (found != null) return found;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static float[] textureCoordinatePointsMapped(Object node, String wantedMapping) {
        if (node == null) return null;
        String mapping = asString(node, "getMapping");
        if (mapping != null && !mapping.isEmpty() && !wantedMapping.equalsIgnoreCase(mapping)) return null;
        float[] points = asFloatArray(node, "getPoint");
        return (points != null && points.length >= 2) ? points : null;
    }

    static float[] parseVec3(Object val) {
        if (val == null) return null;
        if (val instanceof float[]) return (float[]) val;
        if (val instanceof double[]) {
            double[] d = (double[]) val;
            return new float[]{(float) d[0], (float) d[1], (float) d[2]};
        }
        try {
            Method mVal = val.getClass().getMethod("getValue");
            Object v = mVal.invoke(val);
            if (v instanceof float[]) return (float[]) v;
            if (v != null && v != val) return parseVec3(v);
        } catch (Exception ignored) {}
        String s = cleanQuotes(val.toString());
        String[] p = s.split("[,\\s]+");
        if (p.length >= 3) {
            try { return new float[]{ Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]) }; }
            catch (Exception ignored) {}
        }
        return null;
    }

    static float[] parseVec4(Object val) {
        if (val == null) return null;
        if (val instanceof float[]) return (float[]) val;
        if (val instanceof double[]) {
            double[] d = (double[]) val;
            return new float[]{(float) d[0], (float) d[1], (float) d[2], (float) d[3]};
        }
        try {
            Method mVal = val.getClass().getMethod("getValue");
            Object v = mVal.invoke(val);
            if (v instanceof float[]) return (float[]) v;
            if (v != null && v != val) return parseVec4(v);
        } catch (Exception ignored) {}
        String s = cleanQuotes(val.toString());
        String[] p = s.split("[,\\s]+");
        if (p.length >= 4) {
            try { return new float[]{ Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]) }; }
            catch (Exception ignored) {}
        }
        return null;
    }
}

// ============================================================================
// SUBCLASSED X3DJSAIL NODES WITH OBJECT-ORIENTED ANARI INTEGRATION
// ============================================================================

class AnariShape extends org.web3d.x3d.jsail.Shape.Shape implements AnariNode {
    private final org.web3d.x3d.jsail.Shape.Shape delegate;

    public AnariShape() { this.delegate = null; }
    public AnariShape(org.web3d.x3d.jsail.Shape.Shape delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        try {
            Object x3dGeom = (delegate != null) ? delegate.getGeometry() : getGeometry();
            try {
                Object isNode = (delegate != null) ? delegate.getIS() : getIS();
                if (isNode != null) {
                    List<?> connects = X3DTypeAdapter.getListFromNode(isNode, "getConnect", "getConnectList", "getConnects");
                    for (Object c : connects) {
                        String nField = X3DTypeAdapter.asString(c, "getNodeField");
                        String pField = X3DTypeAdapter.asString(c, "getProtoField");
                        if ("geometry".equals(nField) && protoArgs.containsKey(pField)) {
                            x3dGeom = protoArgs.get(pField);
                        }
                    }
                }
            } catch (Exception ignored) {}

            x3dGeom = X3DTypeAdapter.unwrapNode(ctx.resolveUse(x3dGeom));
            if (x3dGeom == null) return;

            Object app = (delegate != null) ? delegate.getAppearance() : getAppearance();
            app = ctx.resolveUse(app);

            Object mat = (app == null) ? null : app.getClass().getMethod("getMaterial").invoke(app);
            mat = ctx.resolveUse(mat);
            Object tex = null;
            if (app != null) {
                try { tex = app.getClass().getMethod("getTexture").invoke(app); } catch (Exception ignored) {}
            }
            mat = ctx.resolveUse(mat);
            if (tex == null && mat != null) {
                for (String tm : List.of("getBaseTexture", "getBaseTextureList", "getTexture")) {
                    try {
                        Object candidate = mat.getClass().getMethod(tm).invoke(mat);
                        candidate = ctx.resolveUse(candidate);
                        if (candidate != null) { tex = candidate; break; }
                    } catch (Exception ignored) {}
                }
            }
            if (tex != null) {
                String tDef = X3DTypeAdapter.asString(tex, "getDEF");
                if (tDef != null && !tDef.trim().isEmpty()) ctx.defMap.putIfAbsent(tDef.replace("\"", "").trim(), tex);
            }
            tex = ctx.resolveUse(tex);

            Object texTrans = null;
            if (app != null) {
                for (String m : List.of("getTextureTransform", "getTextureTransformList")) {
                    try { texTrans = ctx.resolveUse(app.getClass().getMethod(m).invoke(app)); if (texTrans != null) break; } catch (Exception ignored) {}
                }
            }
            X3DAnariHandler.TextureTransformAnim ttAnim = null;
            if (texTrans != null) {
                String ttDef = X3DTypeAdapter.asString(texTrans, "getDEF");
                if (ttDef != null && ctx.textureTransforms.containsKey(ttDef)) {
                    ttAnim = ctx.textureTransforms.get(ttDef);
                } else {
                    float[] center = X3DTypeAdapter.asFloatArray(texTrans, "getCenter");
                    float rot = (float) X3DTypeAdapter.asDouble(texTrans, "getRotation", 0.0);
                    float[] scale = X3DTypeAdapter.asFloatArray(texTrans, "getScale");
                    float[] trans = X3DTypeAdapter.asFloatArray(texTrans, "getTranslation");
                    String tName = (ttDef != null && !ttDef.isEmpty()) ? ttDef : ("tt_" + System.identityHashCode(texTrans));
                    ttAnim = new X3DAnariHandler.TextureTransformAnim(tName, center, rot, scale, trans);
                    ctx.textureTransforms.put(tName, ttAnim);
                }
            }

            String gName = x3dGeom.getClass().getSimpleName();
            if (gName.contains("IndexedLineSet")) {
                AnariIndexedLineSet lineSet = AnariNodeFactory.adaptIndexedLineSet(x3dGeom);
                lineSet.renderLines(ctx, parentTransform, mat, app);
                return;
            }
            if (gName.equals("LineSet") || x3dGeom instanceof org.web3d.x3d.jsail.Rendering.LineSet) {
                AnariLineSet lineSet = AnariNodeFactory.adaptLineSet(x3dGeom);
                lineSet.renderLines(ctx, parentTransform, mat, app);
                return;
            }
            if (x3dGeom.getClass().getSimpleName().contains("LineSet")) {
                AnariLineSet lineSet = AnariNodeFactory.adaptRegularLineSet(x3dGeom);
                lineSet.renderLines(ctx, parentTransform, mat, null);
                return;
            }

            Map<String, Object> geomArgs = new HashMap<>(protoArgs);
            if (tex != null) geomArgs.put("_hasTexture", Boolean.TRUE);
            if (ttAnim != null) geomArgs.put("_activeTextureTransform", ttAnim);

            AnariGeometry anariGeom = AnariNodeFactory.adaptGeometry(x3dGeom);
            if (anariGeom == null) return;

            Geometry.Triangle geometry = anariGeom.buildGeometry(ctx, parentTransform, geomArgs);
            if (geometry == null) return;

            Material<?> material = ctx.defaultMaterial;
            float[] d = null;
            float transparency = 0f;
            boolean isSkin = Boolean.TRUE.equals(protoArgs.get("_isSkin"));

            if (mat != null) {
                try { transparency = (float) X3DTypeAdapter.asDouble(mat, "getTransparency", 0.0); } catch (Exception ignored) {}
                d = X3DTypeAdapter.asFloatArray(mat, "getDiffuseColor");

                if (d == null) {
                    for (String cm : List.of("getBaseColor", "getBaseColorFactor", "getColor")) {
                        d = X3DTypeAdapter.asFloatArray(mat, cm);
                        if (d != null && d.length >= 3) break;
                    }
                }

                if (d == null && !mat.getClass().getSimpleName().contains("PhysicalMaterial")) {
                    float[] ec = X3DTypeAdapter.asFloatArray(mat, "getEmissiveColor");
                    if (ec != null && ec.length >= 3 && (ec[0] > 0f || ec[1] > 0f || ec[2] > 0f)) d = ec;
                }
            }

            Sampler sampler = null;
            if (tex != null && tex.getClass().getSimpleName().contains("ImageTexture")) {
                sampler = loadTextureSampler(ctx, tex);
            }

            if (sampler != null) {
                Material.Matte m = ctx.device.newMaterial(Material.SubType.MATTE);
                m.setColor(1f, 1f, 1f);
                ctx.setAnariObjectParameter(m, "color", DataType.SAMPLER, sampler);
                if (transparency > 0.001f) {
                    try { m.setFloat32("opacity", Math.max(0f, Math.min(1f, 1f - transparency))); } catch (Throwable ignored) {}
                }
                m.commit();
                ctx.keepAlive.add(m);
                material = m;
            } else if (anariGeom.hasVertexColors()) {
                Material.Matte m = ctx.device.newMaterial(Material.SubType.MATTE);
                m.setColor(1f, 1f, 1f);
                try {
                    m.set("color", DataType.STRING, ctx.arena.allocateFrom("color\0", StandardCharsets.UTF_8));
                } catch (Throwable t1) {
                    try {
                        m.set("color", DataType.STRING, ctx.arena.allocateFrom("attribute0\0", StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {}
                }
                m.commit();
                ctx.keepAlive.add(m);
                material = m;
            } else {
                if (d == null || (d.length >= 3 && (d[0] + d[1] + d[2] < 0.05f))) {
                    if (isSkin) {
                        d = new float[]{ 0.86f, 0.68f, 0.54f };
                    } else {
                        Object colorSeed = (app != null) ? app : x3dGeom;
                        d = ctx.fallbackObjectColor(colorSeed, false);
                    }
                }

                if (mat != null || isSkin || d != null) {
                    if (d == null) d = new float[]{ 0.32f, 0.36f, 0.44f };
                    float[] c = (d.length >= 3 && (isSkin || mat != null))
                                   ? ctx.displayColor(d[0], d[1], d[2]) : d;
                    float opacity = Math.max(0f, Math.min(1f, 1f - transparency));

                    Material.Matte m = ctx.device.newMaterial(Material.SubType.MATTE);
                    m.setColor(c[0], c[1], c[2]);
                    if (opacity < 0.999f) {
                        try { m.setFloat32("opacity", opacity); } catch (Throwable ignored) {}
                    }
                    m.commit();
                    ctx.keepAlive.add(m);
                    material = m;
                }
            }

            Surface surface = ctx.device.newSurface().setGeometry(geometry).setMaterial(material);
            surface.commit();
            ctx.keepAlive.add(surface);

            Group group = ctx.device.newGroup();
            Array1D surfArray = ctx.device.newArray1D(List.of(surface), DataType.SURFACE);
            surfArray.commit();
            group.setSurface(surfArray);
            group.commit();
            ctx.keepAlive.add(surfArray);
            ctx.keepAlive.add(group);

            Instance instance = ctx.device.newInstance(Instance.SubType.TRANSFORM);
            instance.setGroup(group);
            instance.setTransform(parentTransform);
            instance.commit();
            ctx.anariInstances.add(instance);
            ctx.instanceTransforms.put(instance, parentTransform.clone());

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static BufferedImage findTextureImage(String u) {
        String base = new File(u).getName();
        File dir = new File(".").getAbsoluteFile();
        for (int up = 0; up < 6 && dir != null; up++, dir = dir.getParentFile()) {
            for (String rel : List.of(u, "data/" + u, "src/main/data/" + u, "images/" + base, "data/images/" + base)) {
                File f = new File(dir, rel);
                if (f.isFile()) {
                    try { BufferedImage im = ImageIO.read(f); if (im != null) { System.err.println("[texture] found " + f); return im; } } catch (Exception ignored) {}
                }
            }
            for (String sub : List.of("data", "src/main/data", "images")) {
                File hit = searchFile(new File(dir, sub), base, 4);
                if (hit != null) {
                    try { BufferedImage im = ImageIO.read(hit); if (im != null) { System.err.println("[texture] found " + hit); return im; } } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private static File searchFile(File dir, String name, int depth) {
        if (dir == null || depth < 0 || !dir.isDirectory()) return null;
        File[] kids = dir.listFiles();
        if (kids == null) return null;
        for (File k : kids) if (k.isFile() && k.getName().equalsIgnoreCase(name)) return k;
        for (File k : kids) if (k.isDirectory()) { File r = searchFile(k, name, depth - 1); if (r != null) return r; }
        return null;
    }

    private static Sampler loadTextureSampler(AnariContext ctx, Object imageTexture) {
        try {
            String[] urls = null;
            for (String m : List.of("getUrl", "getUrlList", "getUrlArray")) {
                try {
                    Object res = imageTexture.getClass().getMethod(m).invoke(imageTexture);
                    if (res instanceof String[]) { urls = (String[]) res; break; }
                    if (res instanceof List<?>) {
                        List<?> l = (List<?>) res;
                        urls = new String[l.size()];
                        for (int i = 0; i < l.size(); i++) urls[i] = String.valueOf(l.get(i));
                        break;
                    }
                    if (res != null) {
                        for (String subM : List.of("getArray", "getValue", "getStrings")) {
                            try {
                                Object subRes = res.getClass().getMethod(subM).invoke(res);
                                if (subRes instanceof String[]) { urls = (String[]) subRes; break; }
                            } catch (Exception ignored) {}
                        }
                        if (urls != null) break;
                    }
                } catch (Exception ignored) {}
            }

            if (urls == null || urls.length == 0) return null;
            String firstUrl = urls[0];
            String cacheKey = firstUrl + "_raw";
            if (ctx.textureCache.containsKey(cacheKey)) return ctx.textureCache.get(cacheKey);

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
                    new File("src/main/data", u),
                    new File("../src/main/data", u),
                    new File(new File(u).getName()),
                    new File("data", new File(u).getName()),
                    new File("../data", new File(u).getName()),
                    new File("../../data", new File(u).getName())
                );
                for (File f : candidates) {
                    if (f.exists() && f.isFile()) {
                        try { img = ImageIO.read(f); if (img != null) break; } catch (Exception ignored) {}
                    }
                }
                if (img == null) img = findTextureImage(u);
                if (img != null) break;

                for (String resPath : List.of("/" + u, "/" + new File(u).getName(), "/data/" + u, "/data/" + new File(u).getName())) {
                    try {
                        InputStream stream = JanariApp.class.getResourceAsStream(resPath);
                        if (stream != null) {
                            try (InputStream is = stream) {
                                img = ImageIO.read(is);
                                if (img != null) break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (img != null) break;

                if (u.startsWith("http://") || u.startsWith("https://")) {
                    try {
                        java.net.URLConnection conn = URI.create(u).toURL().openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setConnectTimeout(8000);
                        try (InputStream is = conn.getInputStream()) {
                            img = ImageIO.read(is);
                            if (img != null) break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (img == null) {
                ctx.textureCache.put(cacheKey, null);
                return null;
            }

            int width = img.getWidth(), height = img.getHeight();
            byte[] rgba = new byte[width * height * 4];
            int k = 0;

            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int argb = img.getRGB(x, y);
                    byte r = (byte) ((argb >> 16) & 0xFF);
                    byte g = (byte) ((argb >> 8)  & 0xFF);
                    byte b = (byte) ((argb)       & 0xFF);
                    byte a = (byte) ((argb >> 24) & 0xFF);

                    if (AnariContext.SWAP_RED_BLUE) {
                        rgba[k++] = b; rgba[k++] = g; rgba[k++] = r;
                    } else {
                        rgba[k++] = r; rgba[k++] = g; rgba[k++] = b;
                    }
                    rgba[k++] = a;
                }
            }

            MemorySegment imgSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_BYTE, rgba);
            Array2D imgArray = ctx.device.newArray2D(imgSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UFIXED8_VEC4, width, height);
            imgArray.commit();
            ctx.keepAlive.add(imgArray);

            Sampler sampler = null;
            try {
                sampler = ctx.device.newSampler(Sampler.SubType.IMAGE2D);
            } catch (Throwable t) {
                Method mNew = ctx.device.getClass().getMethod("newSampler", String.class);
                sampler = (Sampler) mNew.invoke(ctx.device, "image2D");
            }

            if (sampler != null) {
                ctx.setAnariObjectParameter(sampler, "image", DataType.ARRAY2D, imgArray);

                try {
                    Method mAttr = sampler.getClass().getMethod("setInAttribute", String.class);
                    mAttr.invoke(sampler, "attribute0");
                } catch (Throwable ignored) {
                    try { sampler.set("inAttribute", DataType.STRING, ctx.arena.allocateFrom("attribute0\0", StandardCharsets.UTF_8)); } catch (Throwable ignored2) {}
                }

                boolean repeatS = X3DTypeAdapter.asBoolean(imageTexture, "getRepeatS", true);
                boolean repeatT = X3DTypeAdapter.asBoolean(imageTexture, "getRepeatT", true);
                try {
                    Method mWrap1 = null;
                    for (String wn : List.of("setWrapMode1", "setWrap1")) {
                        try { mWrap1 = sampler.getClass().getMethod(wn, String.class); break; } catch (NoSuchMethodException ignoredNs) {}
                    }
                    if (mWrap1 != null) mWrap1.invoke(sampler, repeatS ? "repeat" : "clampToEdge");
                } catch (Throwable ignored) {}
                try {
                    Method mWrap2 = null;
                    for (String wn : List.of("setWrapMode2", "setWrap2")) {
                        try { mWrap2 = sampler.getClass().getMethod(wn, String.class); break; } catch (NoSuchMethodException ignoredNs) {}
                    }
                    if (mWrap2 != null) mWrap2.invoke(sampler, repeatT ? "repeat" : "clampToEdge");
                } catch (Throwable ignored) {}

                sampler.commit();
                ctx.keepAlive.add(sampler);
                ctx.textureCache.put(cacheKey, sampler);
                return sampler;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}

class AnariHumanoid extends org.web3d.x3d.jsail.HAnim.HAnimHumanoid implements AnariNode {
    private final org.web3d.x3d.jsail.HAnim.HAnimHumanoid delegate;

    public AnariHumanoid() { this.delegate = null; }
    public AnariHumanoid(org.web3d.x3d.jsail.HAnim.HAnimHumanoid delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        Object h = (delegate != null) ? delegate : this;
        float[] tr = X3DTypeAdapter.asFloatArray(h, "getTranslation");
        float[] sc = X3DTypeAdapter.asFloatArray(h, "getScale");
        float[] rot = X3DTypeAdapter.asFloatArray(h, "getRotation");

        float[] localMat = AnariMath.buildTransformMatrix(tr, sc, rot);
        float[] humanoidMat = AnariMath.multiplyMatrix(parentTransform, localMat);

        Object skinCoord = null;
        for (String m : List.of("getSkinCoord", "getSkinCoordList")) {
            try { skinCoord = h.getClass().getMethod(m).invoke(h); if (skinCoord != null) break; } catch (Exception ignored) {}
        }
        skinCoord = ctx.resolveUse(skinCoord);

        Object skinNormal = null;
        for (String m : List.of("getSkinNormal", "getSkinNormalList")) {
            try { skinNormal = h.getClass().getMethod(m).invoke(h); if (skinNormal != null) break; } catch (Exception ignored) {}
        }
        skinNormal = ctx.resolveUse(skinNormal);

        Object skel = null;
        for (String m : List.of("getSkeleton", "getSkeletonList")) {
            try { skel = h.getClass().getMethod(m).invoke(h); if (skel != null) break; } catch (Exception ignored) {}
        }
        if (skel != null) {
            AnariNodeFactory.traverseList(ctx, skel, humanoidMat, protoArgs);
        }

        ctx.computeBindMatrices();

        Object skin = null;
        for (String m : List.of("getSkin", "getSkinList")) {
            try { skin = h.getClass().getMethod(m).invoke(h); if (skin != null) break; } catch (Exception ignored) {}
        }
        if (skin != null) {
            Map<String, Object> skinArgs = new HashMap<>(protoArgs);
            skinArgs.put("_isSkin", Boolean.TRUE);
            if (skinCoord != null) skinArgs.put("_skinCoord", skinCoord);
            if (skinNormal != null) skinArgs.put("_skinNormal", skinNormal);
            skinArgs.put("_humanoidMat", humanoidMat);
            AnariNodeFactory.traverseList(ctx, skin, humanoidMat, skinArgs);
        }
    }
}

class AnariJoint extends org.web3d.x3d.jsail.HAnim.HAnimJoint implements AnariNode {
    private final org.web3d.x3d.jsail.HAnim.HAnimJoint delegate;

    public AnariJoint() { this.delegate = null; }
    public AnariJoint(org.web3d.x3d.jsail.HAnim.HAnimJoint delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        Object jNode = (delegate != null) ? delegate : this;
        X3DAnariHandler.JointAnim joint = ctx.registerJoint(jNode);
        if (joint != null) ctx.jointStack.push(joint);

        float[] localMat = (joint != null)
                ? X3DAnariHandler.buildHAnimJointMatrix(joint)
                : AnariMath.buildTransformMatrix(null, null, null);
        float[] currentMat = AnariMath.multiplyMatrix(parentTransform, localMat);

        try {
            Object children = jNode.getClass().getMethod("getChildren").invoke(jNode);
            AnariNodeFactory.traverseList(ctx, children, currentMat, protoArgs);
        } catch (Exception ignored) {}

        if (joint != null && !ctx.jointStack.isEmpty()) {
            ctx.jointStack.pop();
        }
    }
}

class AnariSegment extends org.web3d.x3d.jsail.HAnim.HAnimSegment implements AnariNode {
    private final org.web3d.x3d.jsail.HAnim.HAnimSegment delegate;

    public AnariSegment() { this.delegate = null; }
    public AnariSegment(org.web3d.x3d.jsail.HAnim.HAnimSegment delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        Object seg = (delegate != null) ? delegate : this;
        List<X3DAnariHandler.DisplacerAnim> segDisplacers = new ArrayList<>();
        List<?> list = X3DTypeAdapter.getListFromNode(seg, "getDisplacers", "getDisplacerList");
        for (Object dObj : list) {
            String dDef = X3DTypeAdapter.asString(dObj, "getDEF");
            String dName = X3DTypeAdapter.asString(dObj, "getName");
            if (dDef != null && ctx.displacers.containsKey(dDef)) {
                segDisplacers.add(ctx.displacers.get(dDef));
            } else if (dName != null && ctx.displacers.containsKey(dName)) {
                segDisplacers.add(ctx.displacers.get(dName));
            }
        }
        Map<String, Object> childArgs = protoArgs;
        if (!segDisplacers.isEmpty()) {
            childArgs = new HashMap<>(protoArgs);
            childArgs.put("_activeDisplacers", segDisplacers);
        }
        try {
            Object children = seg.getClass().getMethod("getChildren").invoke(seg);
            AnariNodeFactory.traverseList(ctx, children, parentTransform, childArgs);
        } catch (Exception ignored) {}
    }
}

class AnariSite extends org.web3d.x3d.jsail.HAnim.HAnimSite implements AnariNode {
    private final org.web3d.x3d.jsail.HAnim.HAnimSite delegate;

    public AnariSite() { this.delegate = null; }
    public AnariSite(org.web3d.x3d.jsail.HAnim.HAnimSite delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        Object site = (delegate != null) ? delegate : this;
        float[] tr = X3DTypeAdapter.asFloatArray(site, "getTranslation");
        float[] sc = X3DTypeAdapter.asFloatArray(site, "getScale");
        float[] rot = X3DTypeAdapter.asFloatArray(site, "getRotation");
        float[] localMat = AnariMath.buildTransformMatrix(tr, sc, rot);
        float[] currentMat = AnariMath.multiplyMatrix(parentTransform, localMat);
        try {
            Object children = site.getClass().getMethod("getChildren").invoke(site);
            AnariNodeFactory.traverseList(ctx, children, currentMat, protoArgs);
        } catch (Exception ignored) {}
    }
}

class AnariTransform extends org.web3d.x3d.jsail.Grouping.Transform implements AnariNode {
    private final org.web3d.x3d.jsail.Grouping.Transform delegate;

    public AnariTransform() { this.delegate = null; }
    public AnariTransform(org.web3d.x3d.jsail.Grouping.Transform delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        Object node = (delegate != null) ? delegate : this;
        float[] tr = X3DTypeAdapter.asFloatArray(node, "getTranslation");
        float[] sc = X3DTypeAdapter.asFloatArray(node, "getScale");
        float[] rot = X3DTypeAdapter.asFloatArray(node, "getRotation");
        Object childrenToTraverse = null;

        try {
            Object isNode = null;
            for (String m : List.of("getIS", "getIs")) {
                try { isNode = node.getClass().getMethod(m).invoke(node); if (isNode != null) break; } catch (Exception ignored) {}
            }
            if (isNode != null) {
                List<?> connects = X3DTypeAdapter.getListFromNode(isNode, "getConnect", "getConnectList", "getConnects");
                for (Object c : connects) {
                    String nField = X3DTypeAdapter.asString(c, "getNodeField");
                    String pField = X3DTypeAdapter.asString(c, "getProtoField");
                    if (nField != null) nField = X3DTypeAdapter.cleanQuotes(nField);
                    if (pField != null) pField = X3DTypeAdapter.cleanQuotes(pField);
                    if (pField != null && protoArgs.containsKey(pField)) {
                        Object val = protoArgs.get(pField);
                        if ("translation".equals(nField)) tr = X3DTypeAdapter.parseVec3(val);
                        else if ("scale".equals(nField)) sc = X3DTypeAdapter.parseVec3(val);
                        else if ("rotation".equals(nField)) rot = X3DTypeAdapter.parseVec4(val);
                        else if ("children".equals(nField)) childrenToTraverse = val;
                    }
                }
            }
        } catch (Exception ignored) {}

        float[] localMat = AnariMath.buildTransformMatrix(tr, sc, rot);
        float[] currentMat = AnariMath.multiplyMatrix(parentTransform, localMat);

        int startIdx = ctx.anariInstances.size();

        try {
            if (childrenToTraverse == null) {
                childrenToTraverse = node.getClass().getMethod("getChildren").invoke(node);
            }
            AnariNodeFactory.traverseList(ctx, childrenToTraverse, currentMat, protoArgs);
        } catch (Exception ignored) {}

        int endIdx = ctx.anariInstances.size();
        String def = X3DTypeAdapter.asString(node, "getDEF");
        String name = X3DTypeAdapter.asString(node, "getName");
        if ((def != null && !def.isEmpty()) || (name != null && !name.isEmpty())) {
            List<X3DAnariHandler.InstanceBinding> bindings = new ArrayList<>();
            float[] invCurrent = AnariMath.invertRigidMatrix(currentMat);
            for (int i = startIdx; i < endIdx; i++) {
                Instance inst = ctx.anariInstances.get(i);
                float[] childMat = ctx.instanceTransforms.get(inst);
                float[] relMat = (childMat != null) ? AnariMath.multiplyMatrix(invCurrent, childMat) : AnariMath.buildTransformMatrix(null, null, null);
                bindings.add(new X3DAnariHandler.InstanceBinding(inst, relMat));
            }
            X3DAnariHandler.TransformAnim tAnim = new X3DAnariHandler.TransformAnim(def, parentTransform, tr, rot, sc, bindings);
            if (def != null && !def.isEmpty()) ctx.animatedTransforms.put(def, tAnim);
            if (name != null && !name.isEmpty()) ctx.animatedTransforms.put(name, tAnim);
        }
    }
}

class AnariGroup extends org.web3d.x3d.jsail.Grouping.Group implements AnariNode {
    private final org.web3d.x3d.jsail.Grouping.Group delegate;

    public AnariGroup() { this.delegate = null; }
    public AnariGroup(org.web3d.x3d.jsail.Grouping.Group delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        try {
            Object children = (delegate != null) ? delegate.getChildren() : getChildren();
            AnariNodeFactory.traverseList(ctx, children, parentTransform, protoArgs);
        } catch (Exception ignored) {}
    }
}

// ============================================================================
// SHARED LINE RENDERING HELPER (TUBES, CAPS, AND MATERIALS)
// ============================================================================

class AnariLineHelper {
    static class LineSegmentDef {
        final int p0, p1;
        final float[] c0, c1;
        LineSegmentDef(int p0, int p1, float[] c0, float[] c1) {
            this.p0 = p0; this.p1 = p1; this.c0 = c0; this.c1 = c1;
        }
    }

    static float[] getColorAt(float[] colors, int cIdx, int stride, AnariContext ctx) {
        if (colors == null || cIdx < 0 || (cIdx * stride + 2) >= colors.length) return null;
        return ctx.displayColor(colors[cIdx * stride], colors[cIdx * stride + 1], colors[cIdx * stride + 2]);
    }

    private static int colorKey(float[] c) {
        if (c == null || c.length < 3) return 0xFFFFFF;
        int r = Math.max(0, Math.min(255, Math.round(c[0] * 255f)));
        int g = Math.max(0, Math.min(255, Math.round(c[1] * 255f)));
        int b = Math.max(0, Math.min(255, Math.round(c[2] * 255f)));
        return (r << 16) | (g << 8) | b;
    }

    static void renderSegments(AnariContext ctx, float[] pts, List<LineSegmentDef> segments,
                               float[] m, float[] defaultLineCol, float lineWidthScale) {
        try {
            if (segments == null || segments.isEmpty()) return;

            // Batch segments by color so they share geometries and materials
            Map<Integer, List<LineSegmentDef>> colorGroups = new LinkedHashMap<>();
            Map<Integer, float[]> colorMap = new HashMap<>();

            for (LineSegmentDef seg : segments) {
                float[] col = seg.c0 != null ? seg.c0 : defaultLineCol;
                int key = colorKey(col);
                colorGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(seg);
                colorMap.putIfAbsent(key, col);
            }

            List<Surface> surfaces = new ArrayList<>();
            for (Map.Entry<Integer, List<LineSegmentDef>> entry : colorGroups.entrySet()) {
                List<LineSegmentDef> groupSegs = entry.getValue();
                Geometry.Triangle geom = buildTubeGeometry(ctx, pts, groupSegs, m, lineWidthScale);
                if (geom == null) continue;

                float[] col = colorMap.get(entry.getKey());
                Material.Matte segMat = ctx.device.newMaterial(Material.SubType.MATTE);
                segMat.setColor(col[0], col[1], col[2]);
                segMat.commit();
                ctx.keepAlive.add(segMat);

                Surface surface = ctx.device.newSurface().setGeometry(geom).setMaterial(segMat);
                surface.commit();
                ctx.keepAlive.add(surface);
                surfaces.add(surface);
            }

            if (!surfaces.isEmpty()) {
                Group group = ctx.device.newGroup();
                Array1D surfArray = ctx.device.newArray1D(surfaces, DataType.SURFACE);
                surfArray.commit();
                group.setSurface(surfArray);
                group.commit();
                ctx.keepAlive.add(surfArray);
                ctx.keepAlive.add(group);

                Instance instance = ctx.device.newInstance(Instance.SubType.TRANSFORM);
                instance.setGroup(group);
                instance.setTransform(m);
                instance.commit();
                ctx.anariInstances.add(instance);
                ctx.instanceTransforms.put(instance, m.clone());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static Geometry.Triangle buildTubeGeometry(AnariContext ctx, float[] pts, List<LineSegmentDef> segments,
                                              float[] m, float lineWidthScale) throws Throwable {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i + 2 < pts.length; i += 3) {
            minX = Math.min(minX, pts[i]);   maxX = Math.max(maxX, pts[i]);
            minY = Math.min(minY, pts[i+1]); maxY = Math.max(maxY, pts[i+1]);
            minZ = Math.min(minZ, pts[i+2]); maxZ = Math.max(maxZ, pts[i+2]);
        }
        float dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        float diag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float totalSegLen = 0f;
        int validSegCount = 0;
        for (LineSegmentDef seg : segments) {
            if (seg.p0 < 0 || seg.p1 < 0 || seg.p0 * 3 + 2 >= pts.length || seg.p1 * 3 + 2 >= pts.length) continue;
            int p0 = seg.p0 * 3, p1 = seg.p1 * 3;
            float sx = pts[p1] - pts[p0], sy = pts[p1 + 1] - pts[p0 + 1], sz = pts[p1 + 2] - pts[p0 + 2];
            float slen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slen > 1e-7f) { totalSegLen += slen; validSegCount++; }
        }
        if (validSegCount == 0) return null;

        float avgLen = totalSegLen / validSegCount;
        float radius = diag > 0f ? diag * 0.002f : 0.005f;
        if (avgLen > 1e-6f) radius = Math.min(radius, avgLen * 0.05f);
        if (radius < 1e-5f) radius = (avgLen > 1e-6f) ? avgLen * 0.02f : 0.002f;
        radius *= Math.max(0.1f, lineWidthScale);

        int totalVerts = validSegCount * 26;
        int totalTris = validSegCount * 24;

        float[] vertices = new float[totalVerts * 3];
        float[] normals = new float[totalVerts * 3];
        int[] indices = new int[totalTris * 3];

        int vIdx = 0, iIdx = 0;

        for (LineSegmentDef seg : segments) {
            if (seg.p0 < 0 || seg.p1 < 0 || seg.p0 * 3 + 2 >= pts.length || seg.p1 * 3 + 2 >= pts.length) continue;
            int p0 = seg.p0 * 3, p1 = seg.p1 * 3;
            float ax = pts[p0], ay = pts[p0 + 1], az = pts[p0 + 2];
            float bx = pts[p1], by = pts[p1 + 1], bz = pts[p1 + 2];

            float dirX = bx - ax, dirY = by - ay, dirZ = bz - az;
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (len < 1e-7f) continue;

            dirX /= len; dirY /= len; dirZ /= len;

            float ux, uy, uz;
            if (Math.abs(dirY) < 0.9f) {
                ux = -dirZ; uy = 0f; uz = dirX;
            } else {
                ux = 0f; uy = dirZ; uz = -dirY;
            }
            float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= uLen; uy /= uLen; uz /= uLen;

            float vx = dirY * uz - dirZ * uy;
            float vy = dirZ * ux - dirX * uz;
            float vz = dirX * uy - dirY * ux;

            int baseVert = vIdx / 3;

            int capACenter = baseVert;
            vertices[vIdx] = ax; vertices[vIdx + 1] = ay; vertices[vIdx + 2] = az;
            normals[vIdx] = -dirX; normals[vIdx + 1] = -dirY; normals[vIdx + 2] = -dirZ;
            vIdx += 3;

            int capARimBase = baseVert + 1;
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                vertices[vIdx] = ax + radius * (AnariMath.LINE_COS[j] * ux + AnariMath.LINE_SIN[j] * vx);
                vertices[vIdx + 1] = ay + radius * (AnariMath.LINE_COS[j] * uy + AnariMath.LINE_SIN[j] * vy);
                vertices[vIdx + 2] = az + radius * (AnariMath.LINE_COS[j] * uz + AnariMath.LINE_SIN[j] * vz);
                normals[vIdx] = -dirX; normals[vIdx + 1] = -dirY; normals[vIdx + 2] = -dirZ;
                vIdx += 3;
            }

            int bodyRingABase = baseVert + 7;
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                float rx = AnariMath.LINE_COS[j] * ux + AnariMath.LINE_SIN[j] * vx;
                float ry = AnariMath.LINE_COS[j] * uy + AnariMath.LINE_SIN[j] * vy;
                float rz = AnariMath.LINE_COS[j] * uz + AnariMath.LINE_SIN[j] * vz;
                vertices[vIdx] = ax + radius * rx; vertices[vIdx + 1] = ay + radius * ry; vertices[vIdx + 2] = az + radius * rz;
                normals[vIdx] = rx; normals[vIdx + 1] = ry; normals[vIdx + 2] = rz;
                vIdx += 3;
            }

            int bodyRingBBase = baseVert + 13;
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                float rx = AnariMath.LINE_COS[j] * ux + AnariMath.LINE_SIN[j] * vx;
                float ry = AnariMath.LINE_COS[j] * uy + AnariMath.LINE_SIN[j] * vy;
                float rz = AnariMath.LINE_COS[j] * uz + AnariMath.LINE_SIN[j] * vz;
                vertices[vIdx] = bx + radius * rx; vertices[vIdx + 1] = by + radius * ry; vertices[vIdx + 2] = bz + radius * rz;
                normals[vIdx] = rx; normals[vIdx + 1] = ry; normals[vIdx + 2] = rz;
                vIdx += 3;
            }

            int capBCenter = baseVert + 19;
            vertices[vIdx] = bx; vertices[vIdx + 1] = by; vertices[vIdx + 2] = bz;
            normals[vIdx] = dirX; normals[vIdx + 1] = dirY; normals[vIdx + 2] = dirZ;
            vIdx += 3;

            int capBRimBase = baseVert + 20;
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                vertices[vIdx] = bx + radius * (AnariMath.LINE_COS[j] * ux + AnariMath.LINE_SIN[j] * vx);
                vertices[vIdx + 1] = by + radius * (AnariMath.LINE_COS[j] * uy + AnariMath.LINE_SIN[j] * vy);
                vertices[vIdx + 2] = bz + radius * (AnariMath.LINE_COS[j] * uz + AnariMath.LINE_SIN[j] * vz);
                normals[vIdx] = dirX; normals[vIdx + 1] = dirY; normals[vIdx + 2] = dirZ;
                vIdx += 3;
            }

            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                int next = (j + 1) % AnariMath.LINE_SIDES;
                indices[iIdx++] = capACenter; indices[iIdx++] = capARimBase + next; indices[iIdx++] = capARimBase + j;
            }
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                int next = (j + 1) % AnariMath.LINE_SIDES;
                indices[iIdx++] = bodyRingABase + j; indices[iIdx++] = bodyRingABase + next; indices[iIdx++] = bodyRingBBase + j;
                indices[iIdx++] = bodyRingABase + next; indices[iIdx++] = bodyRingBBase + next; indices[iIdx++] = bodyRingBBase + j;
            }
            for (int j = 0; j < AnariMath.LINE_SIDES; j++) {
                int next = (j + 1) % AnariMath.LINE_SIDES;
                indices[iIdx++] = capBCenter; indices[iIdx++] = capBRimBase + j; indices[iIdx++] = capBRimBase + next;
            }
        }

        ctx.addBounds(vertices, m);

        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, totalTris);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, normals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }
}

// ============================================================================
// LINESET IMPLEMENTATION
// ============================================================================

class AnariLineSet extends org.web3d.x3d.jsail.Rendering.LineSet implements AnariNode {
    private final org.web3d.x3d.jsail.Rendering.LineSet delegate;

    public AnariLineSet() { this.delegate = null; }
    public AnariLineSet(org.web3d.x3d.jsail.Rendering.LineSet delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        renderLines(ctx, parentTransform, null, null);
    }

    public void renderLines(AnariContext ctx, float[] m, Object mat, Object app) {
        try {
            Object ls = (delegate != null) ? delegate : this;
            ls = ctx.resolveUse(ls);

            Object colorNode = null;
            for (String method : List.of("getColor", "getColorList")) {
                try { colorNode = ctx.resolveUse(ls.getClass().getMethod(method).invoke(ls)); if (colorNode != null) break; } catch (Exception ignored) {}
            }
            float[] colors = (colorNode != null) ? X3DTypeAdapter.asFloatArray(colorNode, "getColor") : null;
            if (colors == null && colorNode != null) {
                colors = X3DTypeAdapter.asFloatArray(colorNode, "getPoint");
            }
            int colorStride = (colorNode != null && colorNode.getClass().getSimpleName().contains("RGBA")) ? 4 : 3;
            boolean hasColor = (colors != null && colors.length >= colorStride);

            Object coord = null;
            for (String method : List.of("getCoord", "getCoordList")) {
                try { coord = ctx.resolveUse(ls.getClass().getMethod(method).invoke(ls)); if (coord != null) break; } catch (Exception ignored) {}
            }
            float[] pts = (coord == null) ? null : X3DTypeAdapter.asFloatArray(coord, "getPoint");
            if (pts == null || pts.length < 6) return;
            int nverts = pts.length / 3;

            int[] vc = X3DTypeAdapter.asIntArray(ls, "getVertexCount");
            boolean colorPerVertex = X3DTypeAdapter.asBoolean(ls, "getColorPerVertex", true);

            float lineWidthScale = 1.0f;
            if (app != null) {
                try {
                    Object lp = app.getClass().getMethod("getLineProperties").invoke(app);
                    lp = ctx.resolveUse(lp);
                    if (lp != null) {
                        lineWidthScale = (float) X3DTypeAdapter.asDouble(lp, "getLinewidthScaleFactor", 1.0);
                    }
                } catch (Exception ignored) {}
            }

            float[] defaultLineCol = null;
            if (mat != null) {
                float[] ec = X3DTypeAdapter.asFloatArray(mat, "getEmissiveColor");
                float[] dc = X3DTypeAdapter.asFloatArray(mat, "getDiffuseColor");
                if (ec != null && (ec[0] > 0f || ec[1] > 0f || ec[2] > 0f)) {
                    defaultLineCol = ctx.displayColor(ec[0], ec[1], ec[2]);
                } else if (dc != null && (dc[0] > 0f || dc[1] > 0f || dc[2] > 0f)) {
                    defaultLineCol = ctx.displayColor(dc[0], dc[1], dc[2]);
                }
            }
            if (defaultLineCol == null) {
                defaultLineCol = ctx.displayColor(1f, 1f, 1f);
            }

            List<AnariLineHelper.LineSegmentDef> segments = new ArrayList<>();
            if (vc == null || vc.length == 0) {
                for (int i = 0; i < nverts - 1; i++) {
                    float[] c0 = (hasColor && colorPerVertex) ? AnariLineHelper.getColorAt(colors, i, colorStride, ctx)
                            : (hasColor ? AnariLineHelper.getColorAt(colors, 0, colorStride, ctx) : defaultLineCol);
                    float[] c1 = (hasColor && colorPerVertex) ? AnariLineHelper.getColorAt(colors, i + 1, colorStride, ctx) : c0;
                    if (c0 == null) c0 = defaultLineCol;
                    if (c1 == null) c1 = defaultLineCol;
                    segments.add(new AnariLineHelper.LineSegmentDef(i, i + 1, c0, c1));
                }
            } else {
                int vOffset = 0;
                for (int poly = 0; poly < vc.length; poly++) {
                    int count = vc[poly];
                    for (int i = 0; i < count - 1; i++) {
                        int p0 = vOffset + i;
                        int p1 = vOffset + i + 1;
                        if (p0 < nverts && p1 < nverts) {
                            float[] c0 = defaultLineCol, c1 = defaultLineCol;
                            if (hasColor) {
                                if (colorPerVertex) {
                                    c0 = AnariLineHelper.getColorAt(colors, p0, colorStride, ctx);
                                    c1 = AnariLineHelper.getColorAt(colors, p1, colorStride, ctx);
                                } else {
                                    c0 = AnariLineHelper.getColorAt(colors, poly, colorStride, ctx);
                                    c1 = c0;
                                }
                            }
                            if (c0 == null) c0 = defaultLineCol;
                            if (c1 == null) c1 = defaultLineCol;
                            segments.add(new AnariLineHelper.LineSegmentDef(p0, p1, c0, c1));
                        }
                    }
                    vOffset += count;
                }
            }
            if (segments.isEmpty()) return;

            AnariLineHelper.renderSegments(ctx, pts, segments, m, defaultLineCol, lineWidthScale);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

// ============================================================================
// INDEXEDLINESET IMPLEMENTATION
// ============================================================================

class AnariIndexedLineSet extends org.web3d.x3d.jsail.Rendering.IndexedLineSet implements AnariNode {
    private final org.web3d.x3d.jsail.Rendering.IndexedLineSet delegate;

    public AnariIndexedLineSet() { this.delegate = null; }
    public AnariIndexedLineSet(org.web3d.x3d.jsail.Rendering.IndexedLineSet delegate) { this.delegate = delegate; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        renderLines(ctx, parentTransform, null, null);
    }

    public void renderLines(AnariContext ctx, float[] m, Object mat, Object app) {
        try {
            Object ils = (delegate != null) ? delegate : this;
            ils = ctx.resolveUse(ils);

            Object colorNode = null;
            for (String method : List.of("getColor", "getColorList")) {
                try { colorNode = ctx.resolveUse(ils.getClass().getMethod(method).invoke(ils)); if (colorNode != null) break; } catch (Exception ignored) {}
            }
            float[] colors = (colorNode != null) ? X3DTypeAdapter.asFloatArray(colorNode, "getColor") : null;
            if (colors == null && colorNode != null) {
                colors = X3DTypeAdapter.asFloatArray(colorNode, "getPoint");
            }
            int colorStride = (colorNode != null && colorNode.getClass().getSimpleName().contains("RGBA")) ? 4 : 3;
            boolean hasColor = (colors != null && colors.length >= colorStride);

            Object coord = null;
            for (String method : List.of("getCoord", "getCoordList")) {
                try { coord = ctx.resolveUse(ils.getClass().getMethod(method).invoke(ils)); if (coord != null) break; } catch (Exception ignored) {}
            }
            float[] pts = (coord == null) ? null : X3DTypeAdapter.asFloatArray(coord, "getPoint");
            if (pts == null || pts.length < 6) return;
            int nverts = pts.length / 3;

            int[] ci = X3DTypeAdapter.asIntArray(ils, "getCoordIndex");
            if (ci == null || ci.length < 2) return;

            int[] colorIndex = X3DTypeAdapter.asIntArray(ils, "getColorIndex");
            boolean colorPerVertex = X3DTypeAdapter.asBoolean(ils, "getColorPerVertex", true);

            float lineWidthScale = 1.0f;
            if (app != null) {
                try {
                    Object lp = app.getClass().getMethod("getLineProperties").invoke(app);
                    lp = ctx.resolveUse(lp);
                    if (lp != null) {
                        lineWidthScale = (float) X3DTypeAdapter.asDouble(lp, "getLinewidthScaleFactor", 1.0);
                    }
                } catch (Exception ignored) {}
            }

            float[] defaultLineCol = null;
            if (mat != null) {
                float[] ec = X3DTypeAdapter.asFloatArray(mat, "getEmissiveColor");
                float[] dc = X3DTypeAdapter.asFloatArray(mat, "getDiffuseColor");
                if (ec != null && (ec[0] > 0f || ec[1] > 0f || ec[2] > 0f)) {
                    defaultLineCol = ctx.displayColor(ec[0], ec[1], ec[2]);
                } else if (dc != null && (dc[0] > 0f || dc[1] > 0f || dc[2] > 0f)) {
                    defaultLineCol = ctx.displayColor(dc[0], dc[1], dc[2]);
                }
            }
            if (defaultLineCol == null) {
                defaultLineCol = ctx.displayColor(1f, 1f, 1f);
            }

            List<AnariLineHelper.LineSegmentDef> segments = new ArrayList<>();
            int polylineIdx = 0;
            int prevCoord = -1;
            int prevColorIdx = -1;

            for (int i = 0; i < ci.length; i++) {
                int cIdx = ci[i];
                if (cIdx < 0) {
                    prevCoord = -1;
                    prevColorIdx = -1;
                    polylineIdx++;
                    continue;
                }
                if (cIdx >= nverts) {
                    prevCoord = -1;
                    prevColorIdx = -1;
                    continue;
                }

                int curColorIdx = -1;
                if (hasColor) {
                    if (colorPerVertex) {
                        if (colorIndex != null && i < colorIndex.length && colorIndex[i] >= 0) {
                            curColorIdx = colorIndex[i];
                        } else {
                            curColorIdx = cIdx;
                        }
                    } else {
                        if (colorIndex != null && polylineIdx < colorIndex.length && colorIndex[polylineIdx] >= 0) {
                            curColorIdx = colorIndex[polylineIdx];
                        } else {
                            curColorIdx = polylineIdx;
                        }
                    }
                }

                if (prevCoord >= 0) {
                    float[] c0 = defaultLineCol;
                    float[] c1 = defaultLineCol;
                    if (hasColor) {
                        if (colorPerVertex) {
                            c0 = AnariLineHelper.getColorAt(colors, prevColorIdx, colorStride, ctx);
                            c1 = AnariLineHelper.getColorAt(colors, curColorIdx, colorStride, ctx);
                        } else {
                            c0 = AnariLineHelper.getColorAt(colors, curColorIdx, colorStride, ctx);
                            c1 = c0;
                        }
                        if (c0 == null) c0 = defaultLineCol;
                        if (c1 == null) c1 = defaultLineCol;
                    }
                    segments.add(new AnariLineHelper.LineSegmentDef(prevCoord, cIdx, c0, c1));
                }

                prevCoord = cIdx;
                prevColorIdx = curColorIdx;
            }

            if (segments.isEmpty()) return;

            AnariLineHelper.renderSegments(ctx, pts, segments, m, defaultLineCol, lineWidthScale);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

// ============================================================================
// NURBS PATCH SURFACE IMPLEMENTATION (DE BOOR EVALUATION & ANARI GEOMETRY)
// ============================================================================

class AnariNurbsPatchSurface extends org.web3d.x3d.jsail.NURBS.NurbsPatchSurface implements AnariGeometry, AnariNode {
    private final Object delegate;

    public AnariNurbsPatchSurface() { this.delegate = null; }
    public AnariNurbsPatchSurface(Object delegate) { this.delegate = delegate; }

    @Override
    public boolean hasVertexColors() { return false; }

    @Override
    public void render(AnariContext ctx, float[] parentTransform, Map<String, Object> protoArgs) {
        try {
            Geometry.Triangle geom = buildGeometry(ctx, parentTransform, protoArgs);
            if (geom == null) return;
            Surface surface = ctx.device.newSurface().setGeometry(geom).setMaterial(ctx.defaultMaterial);
            surface.commit();
            ctx.keepAlive.add(surface);

            Group group = ctx.device.newGroup();
            Array1D surfArray = ctx.device.newArray1D(List.of(surface), DataType.SURFACE);
            surfArray.commit();
            group.setSurface(surfArray);
            group.commit();
            ctx.keepAlive.add(surfArray);
            ctx.keepAlive.add(group);

            Instance instance = ctx.device.newInstance(Instance.SubType.TRANSFORM);
            instance.setGroup(group);
            instance.setTransform(parentTransform);
            instance.commit();
            ctx.anariInstances.add(instance);
            ctx.instanceTransforms.put(instance, parentTransform.clone());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        Object surf = (delegate != null) ? delegate : this;
        surf = ctx.resolveUse(surf);

        int uDimension = X3DTypeAdapter.asInt(surf, "getUDimension", 0);
        int vDimension = X3DTypeAdapter.asInt(surf, "getVDimension", 0);
        int uOrder = X3DTypeAdapter.asInt(surf, "getUOrder", 3);
        int vOrder = X3DTypeAdapter.asInt(surf, "getVOrder", 3);

        Object cpNode = null;
        for (String method : List.of("getControlPoint", "getControlPointList")) {
            try {
                cpNode = ctx.resolveUse(surf.getClass().getMethod(method).invoke(surf));
                if (cpNode != null) break;
            } catch (Exception ignored) {}
        }
        if (cpNode == null) cpNode = protoArgs.get("_skinCoord");
        float[] pts = (cpNode == null) ? null : X3DTypeAdapter.asFloatArray(cpNode, "getPoint");

        if (pts == null || uDimension < 2 || vDimension < 2 || pts.length < uDimension * vDimension * 3) {
            return null;
        }

        double[] uKnot = X3DTypeAdapter.asDoubleArray(surf, "getUKnot");
        double[] vKnot = X3DTypeAdapter.asDoubleArray(surf, "getVKnot");
        if (uKnot == null || uKnot.length < uDimension + uOrder) {
            uKnot = generateDefaultKnots(uDimension, uOrder);
        }
        if (vKnot == null || vKnot.length < vDimension + vOrder) {
            vKnot = generateDefaultKnots(vDimension, vOrder);
        }

        double[] weights = X3DTypeAdapter.asDoubleArray(surf, "getWeight");
        boolean solid = X3DTypeAdapter.asBoolean(surf, "getSolid", true);

        int uTess = X3DTypeAdapter.asInt(surf, "getUTessellation", 0);
        int vTess = X3DTypeAdapter.asInt(surf, "getVTessellation", 0);

        int stepsU;
        if (uTess > 0) {
            stepsU = Math.max(uTess, 4);
        } else if (uTess < 0) {
            int spans = Math.max(1, uDimension - uOrder + 1);
            stepsU = Math.max(Math.abs(uTess) * spans, 4);
        } else {
            stepsU = Math.max(uDimension * 2, 60);
        }

        int stepsV;
        if (vTess > 0) {
            stepsV = Math.max(vTess, 4);

        } else if (vTess < 0) {
            int spans = Math.max(1, vDimension - vOrder + 1);
            stepsV = Math.max(Math.abs(vTess) * spans, 4);
        } else {
            stepsV = Math.max(vDimension * 4, 16);
        }

        stepsU = Math.min(stepsU, 500);
        stepsV = Math.min(stepsV, 500);

        int gridVerts = (stepsU + 1) * (stepsV + 1);
        float[] gridPos = new float[gridVerts * 3];
        float[] gridNorm = new float[gridVerts * 3];
        float[] gridUV = new float[gridVerts * 2];

        double uMin = uKnot[uOrder - 1];
        double uMax = uKnot[uDimension];
        double vMin = vKnot[vOrder - 1];
        double vMax = vKnot[vDimension];
        if (uMax <= uMin) { uMin = uKnot[0]; uMax = uKnot[uKnot.length - 1]; }
        if (vMax <= vMin) { vMin = vKnot[0]; vMax = vKnot[vKnot.length - 1]; }
        if (uMax <= uMin) { uMin = 0.0; uMax = 1.0; }
        if (vMax <= vMin) { vMin = 0.0; vMax = 1.0; }

        double[] uBasis = new double[uOrder];
        double[] vBasis = new double[vOrder];

        for (int iv = 0; iv <= stepsV; iv++) {
            double vFrac = (double) iv / stepsV;
            double v = vMin + vFrac * (vMax - vMin);
            int vSpan = findSpan(vDimension - 1, vOrder - 1, v, vKnot);
            basisFuns(vSpan, v, vOrder - 1, vKnot, vBasis);

            for (int iu = 0; iu <= stepsU; iu++) {
                double uFrac = (double) iu / stepsU;
                double u = uMin + uFrac * (uMax - uMin);
                int uSpan = findSpan(uDimension - 1, uOrder - 1, u, uKnot);
                basisFuns(uSpan, u, uOrder - 1, uKnot, uBasis);

                double x = 0.0, y = 0.0, z = 0.0, w = 0.0;
                for (int s = 0; s < vOrder; s++) {
                    int j = vSpan - (vOrder - 1) + s;
                    double vB = vBasis[s];
                    for (int r = 0; r < uOrder; r++) {
                        int i = uSpan - (uOrder - 1) + r;
                        double uB = uBasis[r];
                        double b = uB * vB;
                        int cpIdx = i + j * uDimension;
                        double wt = (weights != null && cpIdx < weights.length) ? weights[cpIdx] : 1.0;
                        double bw = b * wt;
                        int ptIdx = cpIdx * 3;
                        x += bw * pts[ptIdx];
                        y += bw * pts[ptIdx + 1];
                        z += bw * pts[ptIdx + 2];
                        w += bw;
                    }
                }
                if (Math.abs(w) > 1e-12) {
                    x /= w; y /= w; z /= w;
                }

                int vIdx = (iv * (stepsU + 1) + iu) * 3;
                gridPos[vIdx] = (float) x;
                gridPos[vIdx + 1] = (float) y;
                gridPos[vIdx + 2] = (float) z;

                int uvIdx = (iv * (stepsU + 1) + iu) * 2;
                gridUV[uvIdx] = (float) uFrac;
                gridUV[uvIdx + 1] = (float) vFrac;
            }
        }

        for (int iv = 0; iv <= stepsV; iv++) {
            int prevV = Math.max(0, iv - 1);
            int nextV = Math.min(stepsV, iv + 1);
            for (int iu = 0; iu <= stepsU; iu++) {
                int prevU = Math.max(0, iu - 1);
                int nextU = Math.min(stepsU, iu + 1);

                int idxP_U0 = (iv * (stepsU + 1) + prevU) * 3;
                int idxP_U1 = (iv * (stepsU + 1) + nextU) * 3;
                float duX = gridPos[idxP_U1] - gridPos[idxP_U0];
                float duY = gridPos[idxP_U1 + 1] - gridPos[idxP_U0 + 1];
                float duZ = gridPos[idxP_U1 + 2] - gridPos[idxP_U0 + 2];

                int idxP_V0 = (prevV * (stepsU + 1) + iu) * 3;
                int idxP_V1 = (nextV * (stepsU + 1) + iu) * 3;
                float dvX = gridPos[idxP_V1] - gridPos[idxP_V0];
                float dvY = gridPos[idxP_V1 + 1] - gridPos[idxP_V0 + 1];
                float dvZ = gridPos[idxP_V1 + 2] - gridPos[idxP_V0 + 2];

                float nx = duY * dvZ - duZ * dvY;
                float ny = duZ * dvX - duX * dvZ;
                float nz = duX * dvY - duY * dvX;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1e-7f) { nx /= len; ny /= len; nz /= len; }
                else { nx = 0f; ny = 1f; nz = 0f; }

                int nIdx = (iv * (stepsU + 1) + iu) * 3;
                gridNorm[nIdx] = nx;
                gridNorm[nIdx + 1] = ny;
                gridNorm[nIdx + 2] = nz;
            }
        }

        int quads = stepsU * stepsV;
        int totalTris = solid ? (quads * 2) : (quads * 4);
        int totalVerts = solid ? gridVerts : (gridVerts * 2);

        float[] finalPos = new float[totalVerts * 3];
        float[] finalNorm = new float[totalVerts * 3];
        float[] finalUV = new float[totalVerts * 2];
        float[] baseUVs = new float[totalVerts * 2];
        int[] indices = new int[totalTris * 3];

        System.arraycopy(gridPos, 0, finalPos, 0, gridVerts * 3);
        System.arraycopy(gridNorm, 0, finalNorm, 0, gridVerts * 3);
        System.arraycopy(gridUV, 0, finalUV, 0, gridVerts * 2);
        System.arraycopy(gridUV, 0, baseUVs, 0, gridVerts * 2);

        if (!solid) {
            int offV = gridVerts * 3;
            int offUV = gridVerts * 2;
            for (int i = 0; i < gridVerts; i++) {
                finalPos[offV + i * 3]     = gridPos[i * 3];
                finalPos[offV + i * 3 + 1] = gridPos[i * 3 + 1];
                finalPos[offV + i * 3 + 2] = gridPos[i * 3 + 2];

                finalNorm[offV + i * 3]     = -gridNorm[i * 3];
                finalNorm[offV + i * 3 + 1] = -gridNorm[i * 3 + 1];
                finalNorm[offV + i * 3 + 2] = -gridNorm[i * 3 + 2];

                finalUV[offUV + i * 2]     = gridUV[i * 2];
                finalUV[offUV + i * 2 + 1] = gridUV[i * 2 + 1];
                baseUVs[offUV + i * 2]     = gridUV[i * 2];
                baseUVs[offUV + i * 2 + 1] = gridUV[i * 2 + 1];
            }
        }

        int iIdx = 0;
        for (int iv = 0; iv < stepsV; iv++) {
            for (int iu = 0; iu < stepsU; iu++) {
                int p00 = iv * (stepsU + 1) + iu;
                int p10 = iv * (stepsU + 1) + iu + 1;
                int p11 = (iv + 1) * (stepsU + 1) + iu + 1;
                int p01 = (iv + 1) * (stepsU + 1) + iu;

                indices[iIdx++] = p00; indices[iIdx++] = p10; indices[iIdx++] = p11;
                indices[iIdx++] = p00; indices[iIdx++] = p11; indices[iIdx++] = p01;

                if (!solid) {
                    int b00 = p00 + gridVerts;
                    int b10 = p10 + gridVerts;
                    int b11 = p11 + gridVerts;
                    int b01 = p01 + gridVerts;

                    indices[iIdx++] = b00; indices[iIdx++] = b11; indices[iIdx++] = b10;
                    indices[iIdx++] = b00; indices[iIdx++] = b01; indices[iIdx++] = b11;
                }
            }
        }

        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            AnariMath.applyTextureTransform(finalUV, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        ctx.addBounds(finalPos, m);

        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, finalPos);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, totalTris);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, finalNorm);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, finalUV);
        Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, totalVerts);
        uvArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
        ctx.keepAlive.add(uvArray);

        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
            X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
            ctx.textureBindings.add(ttb);
            ttb.apply(ctx.device);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }

    static double[] generateDefaultKnots(int dim, int order) {
        int nKnots = dim + order;
        double[] knots = new double[nKnots];
        int numInterior = dim - order;
        for (int i = 0; i < order; i++) knots[i] = 0.0;
        for (int i = 1; i <= numInterior; i++) {
            knots[order - 1 + i] = (double) i / (numInterior + 1);
        }
        for (int i = dim; i < nKnots; i++) knots[i] = 1.0;
        return knots;
    }

    static int findSpan(int n, int p, double u, double[] U) {
        if (u >= U[n + 1]) return n;
        if (u <= U[p]) return p;
        int low = p;
        int high = n + 1;
        int mid = (low + high) / 2;
        int guard = 0;
        while ((u < U[mid] || u >= U[mid + 1]) && guard++ < 1000) {
            if (u < U[mid]) high = mid;
            else low = mid;
            mid = (low + high) / 2;
        }
        return mid;
    }

    static void basisFuns(int span, double u, int p, double[] U, double[] N) {
        N[0] = 1.0;
        double[] left = new double[p + 1];
        double[] right = new double[p + 1];
        for (int j = 1; j <= p; j++) {
            left[j] = u - U[span + 1 - j];
            right[j] = U[span + j] - u;
            double saved = 0.0;
            for (int r = 0; r < j; r++) {
                double denom = right[r + 1] + left[j - r];
                if (denom != 0.0) {
                    double temp = N[r] / denom;
                    N[r] = saved + right[r + 1] * temp;
                    saved = left[j - r] * temp;
                } else {
                    N[r] = saved;
                    saved = 0.0;
                }
            }
            N[j] = saved;
        }
    }
}

class AnariIndexedFaceSet extends org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet implements AnariGeometry {
    private final org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet delegate;
    private boolean hasColors = false;

    public AnariIndexedFaceSet() { this.delegate = null; }
    public AnariIndexedFaceSet(org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet delegate) { this.delegate = delegate; }

    @Override
    public boolean hasVertexColors() { return hasColors; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        Object ifs = (delegate != null) ? delegate : this;

        int[] ci = X3DTypeAdapter.asIntArray(ifs, "getCoordIndex");
        Object coord = null;
        for (String method : List.of("getCoord", "getCoordList")) {
            try { coord = ctx.resolveUse(ifs.getClass().getMethod(method).invoke(ifs)); if (coord != null) break; } catch (Exception ignored) {}
        }
        if (coord == null) {
            coord = protoArgs.get("_skinCoord");
        }
        float[] pts = (coord == null) ? null : X3DTypeAdapter.asFloatArray(coord, "getPoint");
        if (ci == null || ci.length == 0 || pts == null || pts.length < 9) return null;
        int nverts = pts.length / 3;

        int[] tci = X3DTypeAdapter.asIntArray(ifs, "getTexCoordIndex");
        Object tcNode = null;
        for (String method : List.of("getTexCoord", "getTexCoordList")) {
            try { tcNode = ctx.resolveUse(ifs.getClass().getMethod(method).invoke(ifs)); if (tcNode != null) break; } catch (Exception ignored) {}
        }
        float[] uvs = (tcNode != null) ? X3DTypeAdapter.textureCoordinatePoints(tcNode) : null;
        boolean hasTexture = Boolean.TRUE.equals(protoArgs.get("_hasTexture"));
        boolean hasUV = (uvs != null && uvs.length >= 2) || hasTexture;

        Object colorNode = null;
        for (String method : List.of("getColor", "getColorList")) {
            try { colorNode = ctx.resolveUse(ifs.getClass().getMethod(method).invoke(ifs)); if (colorNode != null) break; } catch (Exception ignored) {}
        }
        float[] rawColors = (colorNode != null) ? X3DTypeAdapter.asFloatArray(colorNode, "getColor") : null;
        if (rawColors == null && colorNode != null) {
            rawColors = X3DTypeAdapter.asFloatArray(colorNode, "getPoint");
        }
        int colorStride = (colorNode != null && colorNode.getClass().getSimpleName().contains("RGBA")) ? 4 : 3;
        boolean hasColorNode = (rawColors != null && rawColors.length >= colorStride);
        int[] colorIndex = X3DTypeAdapter.asIntArray(ifs, "getColorIndex");
        boolean colorPerVertex = X3DTypeAdapter.asBoolean(ifs, "getColorPerVertex", true);
        this.hasColors = hasColorNode;

        float creaseAngle = (float) X3DTypeAdapter.asDouble(ifs, "getCreaseAngle", 0.0);

        List<int[]> tris = new ArrayList<>();
        List<int[]> uvTris = (hasUV) ? new ArrayList<>() : null;
        List<int[]> colorTris = hasColorNode ? new ArrayList<>() : null;
        List<Integer> triFaceId = new ArrayList<>();
        List<float[]> faceNormals = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Integer>[] vertFaces = new List[nverts];
        for (int i = 0; i < nverts; i++) vertFaces[i] = new ArrayList<>();

        List<Integer> faceCoord = new ArrayList<>();
        List<Integer> faceUV = (hasUV) ? new ArrayList<>() : null;
        List<Integer> faceCol = hasColorNode ? new ArrayList<>() : null;

        int currentFaceId = 0;
        for (int i = 0; i < ci.length; i++) {
            int idx = ci[i];
            if (idx < 0) {
                if (faceCoord.size() >= 3) {
                    processFace(pts, faceCoord, faceUV, faceCol, currentFaceId, tris, uvTris, colorTris, triFaceId, faceNormals, vertFaces);
                    currentFaceId++;
                }
                faceCoord.clear();
                if (faceUV != null) faceUV.clear();
                if (faceCol != null) faceCol.clear();
            } else if (idx < nverts) {
                faceCoord.add(idx);
                if (faceUV != null) {
                    int uvIdx = (tci != null && i < tci.length && tci[i] >= 0) ? tci[i] : idx;
                    faceUV.add(uvIdx);
                }
                if (hasColorNode) {
                    if (colorPerVertex) {
                        int cIdx = (colorIndex != null && i < colorIndex.length && colorIndex[i] >= 0) ? colorIndex[i] : idx;
                        faceCol.add(cIdx);
                    } else {
                        int cIdx = (colorIndex != null && currentFaceId < colorIndex.length && colorIndex[currentFaceId] >= 0)
                                ? colorIndex[currentFaceId] : currentFaceId;
                        faceCol.add(cIdx);
                    }
                }
            }
        }
        if (faceCoord.size() >= 3) {
            processFace(pts, faceCoord, faceUV, faceCol, currentFaceId, tris, uvTris, colorTris, triFaceId, faceNormals, vertFaces);
        }
        if (tris.isEmpty()) return null;

        int totalTris = tris.size();
        int totalVerts = totalTris * 3;
        float[] unrolledPts = new float[totalVerts * 3];
        float[] unrolledUV = hasUV ? new float[totalVerts * 2] : null;
        float[] baseUVs = hasUV ? new float[totalVerts * 2] : null;
        float[] unrolledColors = hasColorNode ? new float[totalVerts * 4] : null;
        int[] indices = new int[totalVerts];
        int[] unrolledToOrigCoord = new int[totalVerts];

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i + 2 < pts.length; i += 3) {
            minX = Math.min(minX, pts[i]);   maxX = Math.max(maxX, pts[i]);
            minY = Math.min(minY, pts[i+1]); maxY = Math.max(maxY, pts[i+1]);
        }
        float spanX = Math.max(maxX - minX, 1e-4f);
        float spanY = Math.max(maxY - minY, 1e-4f);

        int vK = 0, uvK = 0, colK = 0;
        for (int t = 0; t < totalTris; t++) {
            int[] cTri = tris.get(t);
            int[] uTri = (uvTris != null && t < uvTris.size()) ? uvTris.get(t) : null;
            int[] clrTri = (colorTris != null && t < colorTris.size()) ? colorTris.get(t) : null;

            for (int corner = 0; corner < 3; corner++) {
                int cIdx = cTri[corner];
                int vertIdx = t * 3 + corner;
                unrolledToOrigCoord[vertIdx] = cIdx;

                unrolledPts[vK++] = pts[3 * cIdx];
                unrolledPts[vK++] = pts[3 * cIdx + 1];
                unrolledPts[vK++] = pts[3 * cIdx + 2];

                if (hasUV) {
                    int uvIdx = (uTri != null) ? uTri[corner] : cIdx;
                    float u, v;
                    if (uvs != null && uvIdx * 2 + 1 < uvs.length) {
                        u = uvs[2 * uvIdx];
                        v = uvs[2 * uvIdx + 1];
                    } else {
                        u = (pts[3 * cIdx] - minX) / spanX;
                        v = (pts[3 * cIdx + 1] - minY) / spanY;
                    }
                    baseUVs[uvK] = u;
                    unrolledUV[uvK++] = u;
                    baseUVs[uvK] = v;
                    unrolledUV[uvK++] = v;
                }

                if (hasColorNode && clrTri != null) {
                    int clrIdx = clrTri[corner];
                    if (rawColors != null && clrIdx >= 0 && (clrIdx * colorStride + 2) < rawColors.length) {
                        float[] rgb = ctx.displayColor(rawColors[clrIdx * colorStride],
                                                       rawColors[clrIdx * colorStride + 1],
                                                       rawColors[clrIdx * colorStride + 2]);
                        unrolledColors[colK++] = rgb[0];
                        unrolledColors[colK++] = rgb[1];
                        unrolledColors[colK++] = rgb[2];
                        unrolledColors[colK++] = 1.0f;
                    } else {
                        unrolledColors[colK++] = 1f; unrolledColors[colK++] = 1f;
                        unrolledColors[colK++] = 1f; unrolledColors[colK++] = 1f;
                    }
                }

                indices[vertIdx] = vertIdx;
            }
        }

        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim && unrolledUV != null) {
            AnariMath.applyTextureTransform(unrolledUV, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        float[] unrolledNormals = AnariMath.computeSmoothNormals(pts, tris, triFaceId, faceNormals, vertFaces, creaseAngle, unrolledToOrigCoord);

        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledPts);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, totalTris);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledNormals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalVerts);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        if (hasUV && unrolledUV != null) {
            MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledUV);
            Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, totalVerts);
            uvArray.commit();
            ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
            ctx.keepAlive.add(uvArray);

            if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
                X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
                X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
                ctx.textureBindings.add(ttb);
                ttb.apply(ctx.device);
            }
        }

        if (hasColorNode && unrolledColors != null) {
            MemorySegment cSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledColors);
            Array1D cArray = ctx.device.newArray1D(cSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC4, totalVerts);
            cArray.commit();
            ctx.setAnariObjectParameter(geom, "vertex.color", DataType.ARRAY1D, cArray);
            if (!hasUV) {
                ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, cArray);
            }
            ctx.keepAlive.add(cArray);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        ctx.addBounds(unrolledPts, m);

        Object activeDispObj = protoArgs.get("_activeDisplacers");
        if (activeDispObj instanceof List<?>) {
            List<?> dList = (List<?>) activeDispObj;
            for (Object o : dList) {
                if (o instanceof X3DAnariHandler.DisplacerAnim) {
                    X3DAnariHandler.DisplacerAnim da = (X3DAnariHandler.DisplacerAnim) o;
                    if (da.displacements != null && da.displacements.length > 0) {
                        X3DAnariHandler.DisplacerMeshBinding binding = new X3DAnariHandler.DisplacerMeshBinding(
                            da, geom, vArray, vSeg, nArray, nSeg, pts,
                            unrolledToOrigCoord, tris, triFaceId, vertFaces, creaseAngle
                        );
                        ctx.activeBindings.add(binding);
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(protoArgs.get("_isSkin"))) {
            X3DAnariHandler.SkinMeshBinding binding = ctx.createSkinBinding(
                geom, vArray, vSeg, nArray, nSeg, pts,
                unrolledToOrigCoord, tris, triFaceId, vertFaces, creaseAngle
            );
            if (binding != null) {
                ctx.skinBindings.add(binding);
            }
        }

        return geom;
    }

    private static void processFace(float[] pts, List<Integer> faceCoord, List<Integer> faceUV, List<Integer> faceCol,
                                   int faceId, List<int[]> tris, List<int[]> uvTris, List<int[]> colorTris,
                                   List<Integer> triFaceId, List<float[]> faceNormals, List<Integer>[] vertFaces) {
        float[] fn = new float[3];
        int nPts = faceCoord.size();
        for (int i = 0; i < nPts; i++) {
            int a = faceCoord.get(i) * 3;
            int b = faceCoord.get((i + 1) % nPts) * 3;
            fn[0] += (pts[a + 1] - pts[b + 1]) * (pts[a + 2] + pts[b + 2]);
            fn[1] += (pts[a + 2] - pts[b + 2]) * (pts[a]     + pts[b]);
            fn[2] += (pts[a]     - pts[b])     * (pts[a + 1] + pts[b + 1]);
            vertFaces[faceCoord.get(i)].add(faceId);
        }
        AnariMath.normalize(fn);
        faceNormals.add(fn);

        int startTris = tris.size();
        AnariMath.triangulatePolygon(pts, faceCoord, faceUV, faceCol, tris, uvTris, colorTris);
        for (int k = startTris; k < tris.size(); k++) {
            triFaceId.add(faceId);
        }
    }
}

class AnariIndexedTriangleSet implements AnariGeometry {
    private final Object delegate;
    private boolean hasColors = false;

    public AnariIndexedTriangleSet(Object delegate) { this.delegate = delegate; }

    @Override
    public boolean hasVertexColors() { return hasColors; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        Object its = X3DTypeAdapter.unwrapNode(delegate);
        if (its == null) return null;

        int[] index = X3DTypeAdapter.asIntArray(its, "getIndex");

        Object coord = null;
        for (String method : List.of("getCoord", "getCoordList")) {
            try {
                coord = ctx.resolveUse(its.getClass().getMethod(method).invoke(its));
                if (coord != null) break;
            } catch (Exception ignored) {}
        }
        if (coord == null) coord = protoArgs.get("_skinCoord");

        float[] pts = (coord == null) ? null : X3DTypeAdapter.asFloatArray(coord, "getPoint");
        if (index == null || index.length < 3 || pts == null || pts.length < 9) return null;

        final int nverts = pts.length / 3;

        Object tcNode = null;
        for (String method : List.of("getTexCoord", "getTexCoordList")) {
            try {
                tcNode = ctx.resolveUse(its.getClass().getMethod(method).invoke(its));
                if (tcNode != null) break;
            } catch (Exception ignored) {}
        }
        float[] uvs = (tcNode != null) ? X3DTypeAdapter.textureCoordinatePoints(tcNode) : null;
        boolean hasTexture = Boolean.TRUE.equals(protoArgs.get("_hasTexture"));
        boolean hasUV = (uvs != null && uvs.length >= 2) || hasTexture;

        Object colorNode = null;
        for (String method : List.of("getColor", "getColorList")) {
            try {
                colorNode = ctx.resolveUse(its.getClass().getMethod(method).invoke(its));
                if (colorNode != null) break;
            } catch (Exception ignored) {}
        }
        float[] rawColors = (colorNode != null) ? X3DTypeAdapter.asFloatArray(colorNode, "getColor") : null;
        if (rawColors == null && colorNode != null) rawColors = X3DTypeAdapter.asFloatArray(colorNode, "getPoint");
        int colorStride = (colorNode != null && colorNode.getClass().getSimpleName().contains("RGBA")) ? 4 : 3;
        boolean hasColorNode = rawColors != null && rawColors.length >= colorStride;
        this.hasColors = hasColorNode;

        List<int[]> tris = new ArrayList<>();
        List<Integer> triFaceId = new ArrayList<>();
        List<float[]> faceNormals = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Integer>[] vertFaces = new List[nverts];
        for (int i = 0; i < nverts; i++) vertFaces[i] = new ArrayList<>();

        for (int i = 0; i + 2 < index.length; i += 3) {
            int a = index[i], b = index[i + 1], c = index[i + 2];
            if (a < 0 || b < 0 || c < 0 || a >= nverts || b >= nverts || c >= nverts) continue;
            if (a == b || b == c || c == a) continue;

            tris.add(new int[]{a, b, c});
            int faceId = faceNormals.size();
            triFaceId.add(faceId);
            vertFaces[a].add(faceId);
            vertFaces[b].add(faceId);
            vertFaces[c].add(faceId);

            int ai = 3 * a, bi = 3 * b, ci = 3 * c;
            float abx = pts[bi] - pts[ai], aby = pts[bi + 1] - pts[ai + 1], abz = pts[bi + 2] - pts[ai + 2];
            float acx = pts[ci] - pts[ai], acy = pts[ci + 1] - pts[ai + 1], acz = pts[ci + 2] - pts[ai + 2];
            float nx = aby * acz - abz * acy;
            float ny = abz * acx - abx * acz;
            float nz = abx * acy - aby * acx;
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-7f) { nx /= len; ny /= len; nz /= len; }
            else { nx = 0f; ny = 1f; nz = 0f; }
            faceNormals.add(new float[]{nx, ny, nz});
        }

        if (tris.isEmpty()) return null;

        int totalTris = tris.size();
        int totalVerts = totalTris * 3;
        float[] unrolledPts = new float[totalVerts * 3];
        float[] unrolledUV = hasUV ? new float[totalVerts * 2] : null;
        float[] baseUVs = hasUV ? new float[totalVerts * 2] : null;
        float[] unrolledColors = hasColorNode ? new float[totalVerts * 4] : null;
        int[] indices = new int[totalVerts];
        int[] unrolledToOrigCoord = new int[totalVerts];

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i + 2 < pts.length; i += 3) {
            minX = Math.min(minX, pts[i]);   maxX = Math.max(maxX, pts[i]);
            minY = Math.min(minY, pts[i + 1]); maxY = Math.max(maxY, pts[i + 1]);
        }
        float spanX = Math.max(maxX - minX, 1e-4f);
        float spanY = Math.max(maxY - minY, 1e-4f);

        int vK = 0, uvK = 0, colK = 0;
        for (int t = 0; t < totalTris; t++) {
            int[] tri = tris.get(t);
            for (int corner = 0; corner < 3; corner++) {
                int cIdx = tri[corner];
                int vertIdx = t * 3 + corner;
                unrolledToOrigCoord[vertIdx] = cIdx;

                unrolledPts[vK++] = pts[3 * cIdx];
                unrolledPts[vK++] = pts[3 * cIdx + 1];
                unrolledPts[vK++] = pts[3 * cIdx + 2];

                if (hasUV) {
                    float u, v;
                    if (uvs != null && cIdx * 2 + 1 < uvs.length) {
                        u = uvs[2 * cIdx];
                        v = uvs[2 * cIdx + 1];
                    } else {
                        u = (pts[3 * cIdx] - minX) / spanX;
                        v = (pts[3 * cIdx + 1] - minY) / spanY;
                    }
                    baseUVs[uvK] = u;
                    unrolledUV[uvK++] = u;
                    baseUVs[uvK] = v;
                    unrolledUV[uvK++] = v;
                }

                if (hasColorNode) {
                    int clrIdx = cIdx;
                    if (rawColors != null && clrIdx * colorStride + 2 < rawColors.length) {
                        float[] rgb = ctx.displayColor(rawColors[clrIdx * colorStride],
                                                       rawColors[clrIdx * colorStride + 1],
                                                       rawColors[clrIdx * colorStride + 2]);
                        unrolledColors[colK++] = rgb[0];
                        unrolledColors[colK++] = rgb[1];
                        unrolledColors[colK++] = rgb[2];
                        unrolledColors[colK++] = (colorStride == 4) ? rawColors[clrIdx * colorStride + 3] : 1f;
                    } else {
                        unrolledColors[colK++] = 1f;
                        unrolledColors[colK++] = 1f;
                        unrolledColors[colK++] = 1f;
                        unrolledColors[colK++] = 1f;
                    }
                }

                indices[vertIdx] = vertIdx;
            }
        }

        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim && unrolledUV != null) {
            AnariMath.applyTextureTransform(unrolledUV, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        float creaseAngle = (float) X3DTypeAdapter.asDouble(its, "getCreaseAngle", 0.0);
        float[] unrolledNormals = AnariMath.computeSmoothNormals(
                pts, tris, triFaceId, faceNormals, vertFaces, creaseAngle, unrolledToOrigCoord);

        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledPts);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL,
                DataType.FLOAT32_VEC3, totalVerts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL,
                DataType.UINT32_VEC3, totalTris);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledNormals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL,
                DataType.FLOAT32_VEC3, totalVerts);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        if (hasUV && unrolledUV != null) {
            MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledUV);
            Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL,
                    DataType.FLOAT32_VEC2, totalVerts);
            uvArray.commit();
            ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
            ctx.keepAlive.add(uvArray);

            if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
                X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
                X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(
                        ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
                ctx.textureBindings.add(ttb);
                ttb.apply(ctx.device);
            }
        }

        if (hasColorNode && unrolledColors != null) {
            MemorySegment cSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, unrolledColors);
            Array1D cArray = ctx.device.newArray1D(cSeg, MemorySegment.NULL, MemorySegment.NULL,
                    DataType.FLOAT32_VEC4, totalVerts);
            cArray.commit();
            ctx.setAnariObjectParameter(geom, "vertex.color", DataType.ARRAY1D, cArray);
            ctx.keepAlive.add(cArray);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        ctx.addBounds(unrolledPts, m);

        Object activeDispObj = protoArgs.get("_activeDisplacers");
        if (activeDispObj instanceof List<?>) {
            for (Object o : (List<?>) activeDispObj) {
                if (o instanceof X3DAnariHandler.DisplacerAnim) {
                    X3DAnariHandler.DisplacerAnim da = (X3DAnariHandler.DisplacerAnim) o;
                    if (da.displacements != null && da.displacements.length > 0) {
                        X3DAnariHandler.DisplacerMeshBinding binding = new X3DAnariHandler.DisplacerMeshBinding(
                                da, geom, vArray, vSeg, nArray, nSeg, pts,
                                unrolledToOrigCoord, tris, triFaceId, vertFaces, creaseAngle);
                        ctx.activeBindings.add(binding);
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(protoArgs.get("_isSkin"))) {
            X3DAnariHandler.SkinMeshBinding binding = ctx.createSkinBinding(
                    geom, vArray, vSeg, nArray, nSeg, pts,
                    unrolledToOrigCoord, tris, triFaceId, vertFaces, creaseAngle);
            if (binding != null) ctx.skinBindings.add(binding);
        }

        return geom;
    }
}

class AnariBox extends org.web3d.x3d.jsail.Geometry3D.Box implements AnariGeometry {
    private final org.web3d.x3d.jsail.Geometry3D.Box delegate;

    public AnariBox() { this.delegate = null; }
    public AnariBox(org.web3d.x3d.jsail.Geometry3D.Box delegate) { this.delegate = delegate; }

    @Override public boolean hasVertexColors() { return false; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        float[] s = (delegate != null) ? delegate.getSize() : getSize();
        float x = (s != null && s.length >= 3) ? s[0] / 2f : 0.5f;
        float y = (s != null && s.length >= 3) ? s[1] / 2f : 0.5f;
        float z = (s != null && s.length >= 3) ? s[2] / 2f : 0.5f;

        float[] rawVerts = {
            -x,-y, z,   x,-y, z,   x, y, z,  -x, y, z,
             x,-y,-z,  -x,-y,-z,  -x, y,-z,   x, y,-z,
            -x, y, z,   x, y, z,   x, y,-z,  -x, y,-z,
            -x,-y,-z,   x,-y,-z,   x,-y, z,  -x,-y, z,
             x,-y, z,   x,-y,-z,   x, y,-z,   x, y, z,
            -x,-y,-z,  -x,-y, z,  -x, y, z,  -x, y,-z
        };

        float[] rawNormals = {
             0, 0, 1,   0, 0, 1,   0, 0, 1,   0, 0, 1,
             0, 0,-1,   0, 0,-1,   0, 0,-1,   0, 0,-1,
             0, 1, 0,   0, 1, 0,   0, 1, 0,   0, 1, 0,
             0,-1, 0,   0,-1, 0,   0,-1, 0,   0,-1, 0,
             1, 0, 0,   1, 0, 0,   1, 0, 0,   1, 0, 0,
            -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0
        };

        float[] baseUVs = {
            0,0,  1,0,  1,1,  0,1,
            0,0,  1,0,  1,1,  0,1,
            0,0,  1,0,  1,1,  0,1,
            0,0,  1,0,  1,1,  0,1,
            0,0,  1,0,  1,1,  0,1,
            0,0,  1,0,  1,1,  0,1
        };

        int[] indices = new int[36];
        int iIdx = 0;
        for (int f = 0; f < 6; f++) {
            int base = f * 4;
            indices[iIdx++] = base;     indices[iIdx++] = base + 1; indices[iIdx++] = base + 2;
            indices[iIdx++] = base;     indices[iIdx++] = base + 2; indices[iIdx++] = base + 3;
        }

        float[] uvs = baseUVs.clone();
        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            AnariMath.applyTextureTransform(uvs, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        ctx.addBounds(rawVerts, m);
        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, rawVerts);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, 24);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, 12);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, rawNormals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, 24);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, uvs);
        Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, 24);
        uvArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
        ctx.keepAlive.add(uvArray);

        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
            X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
            ctx.textureBindings.add(ttb);
            ttb.apply(ctx.device);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }
}

class AnariSphere extends org.web3d.x3d.jsail.Geometry3D.Sphere implements AnariGeometry {
    private final org.web3d.x3d.jsail.Geometry3D.Sphere delegate;

    public AnariSphere() { this.delegate = null; }
    public AnariSphere(org.web3d.x3d.jsail.Geometry3D.Sphere delegate) { this.delegate = delegate; }

    @Override public boolean hasVertexColors() { return false; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        float radius = (delegate != null) ? delegate.getRadius() : getRadius();
        if (radius <= 0f) radius = 1.0f;

        int rings = 20, sectors = 32;
        int nverts = (rings + 1) * (sectors + 1);
        float[] vertices = new float[nverts * 3];
        float[] normals = new float[nverts * 3];
        float[] baseUVs = new float[nverts * 2];
        int vIdx = 0, uvIdx = 0;

        for (int r = 0; r <= rings; r++) {
            double phi = (double) r / rings * Math.PI;
            float y = (float) (radius * Math.cos(phi));
            float sinPhi = (float) Math.sin(phi);
            float v = 1.0f - (float) r / rings;

            for (int s = 0; s <= sectors; s++) {
                double theta = (double) s / sectors * (2.0 * Math.PI);
                float nx = (float) (sinPhi * Math.cos(theta));
                float ny = (float) Math.cos(phi);
                float nz = (float) (sinPhi * Math.sin(theta));

                vertices[vIdx] = radius * nx;
                vertices[vIdx + 1] = y;
                vertices[vIdx + 2] = radius * nz;

                normals[vIdx] = nx;
                normals[vIdx + 1] = ny;
                normals[vIdx + 2] = nz;
                vIdx += 3;

                baseUVs[uvIdx++] = (float) s / sectors;
                baseUVs[uvIdx++] = v;
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

        float[] uvs = baseUVs.clone();
        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            AnariMath.applyTextureTransform(uvs, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        ctx.addBounds(vertices, m);
        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, nverts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, ntris);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, normals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, nverts);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, uvs);
        Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, nverts);
        uvArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
        ctx.keepAlive.add(uvArray);

        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
            X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
            ctx.textureBindings.add(ttb);
            ttb.apply(ctx.device);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }
}

class AnariCylinder extends org.web3d.x3d.jsail.Geometry3D.Cylinder implements AnariGeometry {
    private final org.web3d.x3d.jsail.Geometry3D.Cylinder delegate;

    public AnariCylinder() { this.delegate = null; }
    public AnariCylinder(org.web3d.x3d.jsail.Geometry3D.Cylinder delegate) { this.delegate = delegate; }

    @Override public boolean hasVertexColors() { return false; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        float radius = (delegate != null) ? delegate.getRadius() : getRadius();
        float height = (delegate != null) ? delegate.getHeight() : getHeight();
        boolean top = (delegate != null) ? delegate.getTop() : getTop();
        boolean bottom = (delegate != null) ? delegate.getBottom() : getBottom();
        boolean side = (delegate != null) ? delegate.getSide() : getSide();

        int slices = 32;
        float halfH = height / 2.0f;
        List<Float> vList = new ArrayList<>();
        List<Float> nList = new ArrayList<>();
        List<Float> uvList = new ArrayList<>();
        List<Integer> iList = new ArrayList<>();

        if (side) {
            int baseIdx = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                float cos = (float) Math.cos(theta), sin = (float) Math.sin(theta);
                float x = radius * cos, z = radius * sin;
                float u = (float) i / slices;

                vList.add(x); vList.add(-halfH); vList.add(z);
                nList.add(cos); nList.add(0f); nList.add(sin);
                uvList.add(u); uvList.add(0f);

                vList.add(x); vList.add(halfH); vList.add(z);
                nList.add(cos); nList.add(0f); nList.add(sin);
                uvList.add(u); uvList.add(1f);
            }
            for (int i = 0; i < slices; i++) {
                int i0 = baseIdx + i * 2, i1 = baseIdx + i * 2 + 1;
                int i2 = baseIdx + (i + 1) * 2 + 1, i3 = baseIdx + (i + 1) * 2;
                iList.add(i0); iList.add(i2); iList.add(i1);
                iList.add(i0); iList.add(i3); iList.add(i2);
            }
        }
        if (top) {
            int centerIdx = vList.size() / 3;
            vList.add(0f); vList.add(halfH); vList.add(0f);
            nList.add(0f); nList.add(1f); nList.add(0f);
            uvList.add(0.5f); uvList.add(0.5f);

            int rimStart = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                float cos = (float) Math.cos(theta), sin = (float) Math.sin(theta);
                vList.add(radius * cos); vList.add(halfH); vList.add(radius * sin);
                nList.add(0f); nList.add(1f); nList.add(0f);
                uvList.add(0.5f + 0.5f * cos); uvList.add(0.5f + 0.5f * sin);
            }
            for (int i = 0; i < slices; i++) {
                iList.add(centerIdx); iList.add(rimStart + i + 1); iList.add(rimStart + i);
            }
        }
        if (bottom) {
            int centerIdx = vList.size() / 3;
            vList.add(0f); vList.add(-halfH); vList.add(0f);
            nList.add(0f); nList.add(-1f); nList.add(0f);
            uvList.add(0.5f); uvList.add(0.5f);

            int rimStart = vList.size() / 3;
            for (int i = 0; i <= slices; i++) {
                double theta = (double) i / slices * 2.0 * Math.PI;
                float cos = (float) Math.cos(theta), sin = (float) Math.sin(theta);
                vList.add(radius * cos); vList.add(-halfH); vList.add(radius * sin);
                nList.add(0f); nList.add(-1f); nList.add(0f);
                uvList.add(0.5f + 0.5f * cos); uvList.add(0.5f - 0.5f * sin);
            }
            for (int i = 0; i < slices; i++) {
                iList.add(centerIdx); iList.add(rimStart + i); iList.add(rimStart + i + 1);
            }
        }
        if (iList.isEmpty()) return null;

        float[] vertices = new float[vList.size()];
        for (int i = 0; i < vList.size(); i++) vertices[i] = vList.get(i);
        float[] normals = new float[nList.size()];
        for (int i = 0; i < nList.size(); i++) normals[i] = nList.get(i);
        float[] baseUVs = new float[uvList.size()];
        for (int i = 0; i < uvList.size(); i++) baseUVs[i] = uvList.get(i);
        int[] indices = new int[iList.size()];
        for (int i = 0; i < iList.size(); i++) indices[i] = iList.get(i);

        float[] uvs = baseUVs.clone();
        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            AnariMath.applyTextureTransform(uvs, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        ctx.addBounds(vertices, m);
        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, vertices.length / 3);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, indices.length / 3);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment nSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, normals);
        Array1D nArray = ctx.device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, normals.length / 3);
        nArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.normal", DataType.ARRAY1D, nArray);
        ctx.keepAlive.add(nArray);

        MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, uvs);
        Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, uvs.length / 2);
        uvArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
        ctx.keepAlive.add(uvArray);

        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
            X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
            ctx.textureBindings.add(ttb);
            ttb.apply(ctx.device);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }
}

class AnariExtrusion extends org.web3d.x3d.jsail.Geometry3D.Extrusion implements AnariGeometry {
    private final org.web3d.x3d.jsail.Geometry3D.Extrusion delegate;

    public AnariExtrusion() { this.delegate = null; }
    public AnariExtrusion(org.web3d.x3d.jsail.Geometry3D.Extrusion delegate) { this.delegate = delegate; }

    @Override public boolean hasVertexColors() { return false; }

    @Override
    public Geometry.Triangle buildGeometry(AnariContext ctx, float[] m, Map<String, Object> protoArgs) throws Throwable {
        float[] rawCS = (delegate != null) ? delegate.getCrossSection() : getCrossSection();
        if (rawCS == null || rawCS.length < 4) rawCS = new float[]{ 1f, 1f, 1f, -1f, -1f, -1f, -1f, 1f, 1f, 1f };
        int numCsPts = rawCS.length / 2;

        float[] rawSpine = (delegate != null) ? delegate.getSpine() : getSpine();
        if (rawSpine == null || rawSpine.length < 6) rawSpine = new float[]{ 0f, 0f, 0f, 0f, 1f, 0f };
        int numSpinePts = rawSpine.length / 3;

        float[] rawScale = (delegate != null) ? delegate.getScale() : getScale();

        float[][] scpX = new float[numSpinePts][3];
        float[][] scpY = new float[numSpinePts][3];
        float[][] scpZ = new float[numSpinePts][3];

        for (int i = 0; i < numSpinePts; i++) {
            float[] d = new float[3];
            if (i == 0) {
                d[0] = rawSpine[3] - rawSpine[0]; d[1] = rawSpine[4] - rawSpine[1]; d[2] = rawSpine[5] - rawSpine[2];
            } else if (i == numSpinePts - 1) {
                d[0] = rawSpine[i * 3] - rawSpine[(i - 1) * 3]; d[1] = rawSpine[i * 3 + 1] - rawSpine[(i - 1) * 3 + 1]; d[2] = rawSpine[i * 3 + 2] - rawSpine[(i - 1) * 3 + 2];
            } else {
                d[0] = rawSpine[(i + 1) * 3] - rawSpine[(i - 1) * 3]; d[1] = rawSpine[(i + 1) * 3 + 1] - rawSpine[(i - 1) * 3 + 1]; d[2] = rawSpine[(i + 1) * 3 + 2] - rawSpine[(i - 1) * 3 + 2];
            }
            AnariMath.normalize(d);
            scpY[i] = d;
        }

        for (int i = 0; i < numSpinePts; i++) {
            float[] y = scpY[i], z = new float[3];
            if (Math.abs(y[0]) < 1e-4f && Math.abs(y[2]) < 1e-4f) {
                z[0] = 0f; z[1] = 0f; z[2] = (y[1] > 0) ? -1f : 1f;
            } else {
                z[0] = -y[2]; z[1] = 0f; z[2] = y[0];
                AnariMath.normalize(z);
            }
            scpZ[i] = z;
            AnariMath.cross(y, z, scpX[i]);
            AnariMath.normalize(scpX[i]);
        }

        int totalRingVerts = numSpinePts * numCsPts;
        float[] vertices = new float[totalRingVerts * 3];
        float[] baseUVs = new float[totalRingVerts * 2];
        int vIdx = 0, uvIdx = 0;

        for (int s = 0; s < numSpinePts; s++) {
            float sx = 1f, sz = 1f;
            if (rawScale != null && s * 2 + 1 < rawScale.length) {
                sx = rawScale[s * 2]; sz = rawScale[s * 2 + 1];
            }
            float spX = rawSpine[s * 3], spY = rawSpine[s * 3 + 1], spZ = rawSpine[s * 3 + 2];
            float v = (float) s / (numSpinePts - 1);

            for (int c = 0; c < numCsPts; c++) {
                float cx = rawCS[c * 2] * sx, cz = rawCS[c * 2 + 1] * sz;
                vertices[vIdx++] = spX + cx * scpX[s][0] + cz * scpZ[s][0];
                vertices[vIdx++] = spY + cx * scpX[s][1] + cz * scpZ[s][1];
                vertices[vIdx++] = spZ + cx * scpX[s][2] + cz * scpZ[s][2];

                baseUVs[uvIdx++] = (float) c / (numCsPts - 1);
                baseUVs[uvIdx++] = v;
            }
        }

        List<Integer> indexList = new ArrayList<>();
        int spineSegments = numSpinePts - 1;
        for (int s = 0; s < spineSegments; s++) {
            int nextS = (s + 1) % numSpinePts;
            for (int c = 0; c < numCsPts - 1; c++) {
                int i0 = s * numCsPts + c, i1 = nextS * numCsPts + c;
                int i2 = nextS * numCsPts + (c + 1), i3 = s * numCsPts + (c + 1);
                indexList.add(i0); indexList.add(i1); indexList.add(i2);
                indexList.add(i0); indexList.add(i2); indexList.add(i3);
            }
        }

        int[] indices = new int[indexList.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = indexList.get(i);

        float[] uvs = baseUVs.clone();
        Object activeTT = protoArgs.get("_activeTextureTransform");
        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            AnariMath.applyTextureTransform(uvs, (X3DAnariHandler.TextureTransformAnim) activeTT);
        }

        ctx.addBounds(vertices, m);
        MemorySegment vSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, vertices);
        Array1D vArray = ctx.device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, totalRingVerts);
        vArray.commit();

        MemorySegment iSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_INT, indices);
        Array1D iArray = ctx.device.newArray1D(iSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.UINT32_VEC3, indices.length / 3);
        iArray.commit();

        Geometry.Triangle geom = ctx.device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(vArray)
                .setPrimitiveIndex(iArray);

        MemorySegment uvSeg = ctx.arena.allocateFrom(ValueLayout.JAVA_FLOAT, uvs);
        Array1D uvArray = ctx.device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, uvs.length / 2);
        uvArray.commit();
        ctx.setAnariObjectParameter(geom, "vertex.attribute0", DataType.ARRAY1D, uvArray);
        ctx.keepAlive.add(uvArray);

        if (activeTT instanceof X3DAnariHandler.TextureTransformAnim) {
            X3DAnariHandler.TextureTransformAnim ttAnim = (X3DAnariHandler.TextureTransformAnim) activeTT;
            X3DAnariHandler.TextureTransformBinding ttb = new X3DAnariHandler.TextureTransformBinding(ctx, ttAnim, geom, uvSeg, uvArray, baseUVs);
            ctx.textureBindings.add(ttb);
            ttb.apply(ctx.device);
        }

        geom.commit();
        ctx.keepAlive.add(vArray);
        ctx.keepAlive.add(iArray);
        ctx.keepAlive.add(geom);
        return geom;
    }
}

// ============================================================================
// NODE FACTORY & ADAPTER
// ============================================================================

class AnariNodeFactory {

    public static void traverseList(AnariContext ctx, Object children, float[] matrix, Map<String, Object> protoArgs) {
        if (children == null) return;
        if (children instanceof List<?>) {
            for (Object c : (List<?>) children) processNode(ctx, c, matrix, protoArgs);
        } else if (children instanceof Object[]) {
            for (Object c : (Object[]) children) processNode(ctx, c, matrix, protoArgs);
        } else {
            processNode(ctx, children, matrix, protoArgs);
        }
    }

    public static void processNode(AnariContext ctx, Object node, float[] parentTransform, Map<String, Object> protoArgs) {
        if (node == null) return;

        try {
            String def = (String) node.getClass().getMethod("getDEF").invoke(node);
            if (def != null && !def.trim().isEmpty()) ctx.defMap.putIfAbsent(X3DTypeAdapter.cleanQuotes(def), node);
        } catch (Exception ignored) {}

        try {
            String use = (String) node.getClass().getMethod("getUSE").invoke(node);
            if (use != null && !use.isEmpty()) {
                String cleanUse = X3DTypeAdapter.cleanQuotes(use);
                if (ctx.defMap.containsKey(cleanUse)) {
                    processNode(ctx, ctx.defMap.get(cleanUse), parentTransform, protoArgs);
                    return;
                }
            }
        } catch (Exception ignored) {}

        String cName = node.getClass().getSimpleName();
        if (cName.contains("ProtoDeclare") || cName.contains("ROUTE") || cName.contains("TimeSensor")
            || cName.contains("ScalarInterpolator") || cName.contains("OrientationInterpolator")
            || cName.contains("PositionInterpolator")) return;

        if (cName.contains("Background")) return;

        if (cName.contains("Viewpoint")) {
            if (!ctx.viewpointSet) {
                try {
                    float[] pos = X3DTypeAdapter.asFloatArray(node, "getPosition");
                    if (pos != null && pos.length >= 3) {
                        float vy = AnariContext.FLIP_Y ? -pos[1] : pos[1];
                        ctx.cameraTarget = new float[] { pos[0], vy, 0f };
                        ctx.cameraDistance = Math.abs(pos[2]) > 0.001f ? Math.abs(pos[2])
                                : (float) Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
                        ctx.viewpointSet = true;
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        if (cName.contains("ProtoInstance")) {
            try {
                String name = X3DTypeAdapter.asString(node, "getName");
                if (name != null) name = X3DTypeAdapter.cleanQuotes(name);
                Object protoDecl = ctx.protoMap.get(name);
                if (protoDecl != null) {
                    Map<String, Object> newArgs = new HashMap<>();

                    // 1. Defaults from ProtoInterface
                    Object pInterface = null;
                    for (String m : List.of("getProtoInterface", "getInterface")) {
                        try { pInterface = protoDecl.getClass().getMethod(m).invoke(protoDecl); if (pInterface != null) break; } catch (Exception ignored) {}
                    }
                    if (pInterface != null) {
                        List<?> fields = X3DTypeAdapter.getListFromNode(pInterface, "getField", "getFieldList", "getFields");
                        for (Object f : fields) {
                            String fName = X3DTypeAdapter.asString(f, "getName");
                            if (fName != null) {
                                fName = X3DTypeAdapter.cleanQuotes(fName);
                                if (!newArgs.containsKey(fName)) {
                                    Object defVal = X3DTypeAdapter.extractFieldValue(f);
                                    if (defVal != null) newArgs.put(fName, defVal);
                                }
                            }
                        }
                    }

                    // 2. IS / connect
                    Object isNode = null;
                    for (String m : List.of("getIS", "getIs")) {
                        try { isNode = node.getClass().getMethod(m).invoke(node); if (isNode != null) break; } catch (Exception ignored) {}
                    }
                    if (isNode != null) {
                        List<?> connects = X3DTypeAdapter.getListFromNode(isNode, "getConnect", "getConnectList", "getConnects");
                        for (Object c : connects) {
                            String nField = X3DTypeAdapter.asString(c, "getNodeField");
                            String pField = X3DTypeAdapter.asString(c, "getProtoField");
                            if (nField != null) nField = X3DTypeAdapter.cleanQuotes(nField);
                            if (pField != null) pField = X3DTypeAdapter.cleanQuotes(pField);
                            if (pField != null && protoArgs.containsKey(pField)) {
                                newArgs.put(nField, protoArgs.get(pField));
                            }
                        }
                    }

                    // 3. FieldValue overrides on this ProtoInstance
                    List<?> fvList = X3DTypeAdapter.getListFromNode(node, "getFieldValue", "getFieldValueList", "getFieldValues", "getFieldList");
                    for (Object fv : fvList) {
                        String fName = X3DTypeAdapter.asString(fv, "getName");
                        if (fName != null) {
                            fName = X3DTypeAdapter.cleanQuotes(fName);
                            Object fVal = X3DTypeAdapter.extractFieldValue(fv);
                            if (fVal != null) newArgs.put(fName, fVal);
                        }
                    }

                    // 4. Traverse ProtoBody
                    Object pBody = null;
                    for (String m : List.of("getProtoBody", "getBody")) {
                        try { pBody = protoDecl.getClass().getMethod(m).invoke(protoDecl); if (pBody != null) break; } catch (Exception ignored) {}
                    }
                    if (pBody != null) {
                        List<?> bodyChildren = X3DTypeAdapter.getListFromNode(pBody, "getChildren", "getChildrenList", "getChildList");
                        traverseList(ctx, bodyChildren, parentTransform, newArgs);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (node instanceof AnariNode) {
            ((AnariNode) node).render(ctx, parentTransform, protoArgs);
            return;
        }

        if (node instanceof org.web3d.x3d.jsail.HAnim.HAnimHumanoid || cName.contains("HAnimHumanoid")) {
            new AnariHumanoid((org.web3d.x3d.jsail.HAnim.HAnimHumanoid) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.HAnim.HAnimJoint || cName.contains("HAnimJoint")) {
            new AnariJoint((org.web3d.x3d.jsail.HAnim.HAnimJoint) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.HAnim.HAnimSegment || cName.contains("HAnimSegment")) {
            new AnariSegment((org.web3d.x3d.jsail.HAnim.HAnimSegment) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.HAnim.HAnimSite || cName.contains("HAnimSite")) {
            new AnariSite((org.web3d.x3d.jsail.HAnim.HAnimSite) node).render(ctx, parentTransform, protoArgs);
            return;
        }

        if (node instanceof org.web3d.x3d.jsail.Shape.Shape) {
            new AnariShape((org.web3d.x3d.jsail.Shape.Shape) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.Grouping.Transform) {
            new AnariTransform((org.web3d.x3d.jsail.Grouping.Transform) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.Grouping.Group) {
            new AnariGroup((org.web3d.x3d.jsail.Grouping.Group) node).render(ctx, parentTransform, protoArgs);
            return;
        }

        if (node instanceof org.web3d.x3d.jsail.Rendering.LineSet || cName.equals("LineSet")) {
            new AnariLineSet((org.web3d.x3d.jsail.Rendering.LineSet) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.Rendering.IndexedLineSet || cName.contains("IndexedLineSet")) {
            new AnariIndexedLineSet((org.web3d.x3d.jsail.Rendering.IndexedLineSet) node).render(ctx, parentTransform, protoArgs);
            return;
        }
        if (node instanceof org.web3d.x3d.jsail.NURBS.NurbsPatchSurface || cName.equals("NurbsPatchSurface")) {
            new AnariNurbsPatchSurface(node).render(ctx, parentTransform, protoArgs);
            return;
        }

        try {
            traverseList(ctx, node.getClass().getMethod("getChildren").invoke(node), parentTransform, protoArgs);
        } catch (Exception ignored) {}
    }

    public static AnariGeometry adaptGeometry(Object x3dGeom) {
        x3dGeom = X3DTypeAdapter.unwrapNode(x3dGeom);
        if (x3dGeom == null) return null;
        if (x3dGeom instanceof AnariGeometry) return (AnariGeometry) x3dGeom;
        if (x3dGeom instanceof org.web3d.x3d.jsail.NURBS.NurbsPatchSurface || x3dGeom.getClass().getSimpleName().equals("NurbsPatchSurface")) {
            return new AnariNurbsPatchSurface(x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet) {
            return new AnariIndexedFaceSet((org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet) x3dGeom);
        }
        if (x3dGeom.getClass().getSimpleName().equals("IndexedTriangleSet")) {
            return new AnariIndexedTriangleSet(x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.NURBS.NurbsPatchSurface) {
            return new AnariNurbsPatchSurface((org.web3d.x3d.jsail.NURBS.NurbsPatchSurface) x3dGeom);
        }
        if (x3dGeom.getClass().getSimpleName().contains("NurbsPatchSurface")) {
            return new AnariNurbsPatchSurface(x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.Geometry3D.Box) {
            return new AnariBox((org.web3d.x3d.jsail.Geometry3D.Box) x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.Geometry3D.Sphere) {
            return new AnariSphere((org.web3d.x3d.jsail.Geometry3D.Sphere) x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.Geometry3D.Cylinder) {
            return new AnariCylinder((org.web3d.x3d.jsail.Geometry3D.Cylinder) x3dGeom);
        }
        if (x3dGeom instanceof org.web3d.x3d.jsail.Geometry3D.Extrusion) {
            return new AnariExtrusion((org.web3d.x3d.jsail.Geometry3D.Extrusion) x3dGeom);
        }
        return null;
    }

    public static AnariLineSet adaptLineSet(Object x3dGeom) {
        x3dGeom = X3DTypeAdapter.unwrapNode(x3dGeom);
        if (x3dGeom instanceof AnariLineSet) return (AnariLineSet) x3dGeom;
        if (x3dGeom instanceof org.web3d.x3d.jsail.Rendering.LineSet) {
            return new AnariLineSet((org.web3d.x3d.jsail.Rendering.LineSet) x3dGeom);
        }
        return new AnariLineSet();
    }

    public static AnariIndexedLineSet adaptIndexedLineSet(Object x3dGeom) {
        x3dGeom = X3DTypeAdapter.unwrapNode(x3dGeom);
        if (x3dGeom instanceof AnariIndexedLineSet) return (AnariIndexedLineSet) x3dGeom;
        if (x3dGeom instanceof org.web3d.x3d.jsail.Rendering.IndexedLineSet) {
            return new AnariIndexedLineSet((org.web3d.x3d.jsail.Rendering.IndexedLineSet) x3dGeom);
        }
        return new AnariIndexedLineSet();
    }

    public static AnariLineSet adaptRegularLineSet(Object x3dGeom) {
        x3dGeom = X3DTypeAdapter.unwrapNode(x3dGeom);
        if (x3dGeom instanceof AnariLineSet) return (AnariLineSet) x3dGeom;
        if (x3dGeom instanceof org.web3d.x3d.jsail.Rendering.LineSet) {
            return new AnariLineSet((org.web3d.x3d.jsail.Rendering.LineSet) x3dGeom);
        }
        return new AnariLineSet();
    }
}

// ============================================================================
// MAIN ANARI SCENE HANDLER (ANIMATION, LIGHTS, AND RENDERING PIPELINE)
// ============================================================================

@SuppressWarnings({"rawtypes", "unchecked"})
class X3DAnariHandler extends AbstractHandler {
    private final Object x3dModel;
    private final Arena sceneArena = Arena.ofShared();
    private final List<Object> keepAlive = new ArrayList<>();
    private final List<Instance> anariInstances = new ArrayList<>();
    private final Map<String, Sampler> textureCache = new HashMap<>();
    private final Map<String, Object> defMap = new HashMap<>();
    private final Map<String, Object> protoMap = new HashMap<>();
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    private AnariContext context;
    private Material<?> defaultMaterial;
    private Device device;
    private Light.Directional headlight;
    private Light.Directional fillLight;
    private float[] backgroundColor = null;
    private float angleScale = 1f;
    private float lastAz = Float.NaN, lastEl = Float.NaN;
    private boolean built = false;

    private static final float HEADLIGHT_IRRADIANCE = 1.2f;
    private static final float AMBIENT_RADIANCE = 0.45f;

    private long animStartTime = 0;
    private final List<X3DRoute> routes = new ArrayList<>();
    private final List<TimeSensorAnim> timeSensors = new ArrayList<>();
    private final Map<String, ScalarInterpolatorAnim> interpolators = new HashMap<>();
    private final Map<String, OrientationInterpolatorAnim> orientationInterpolators = new HashMap<>();
    private final Map<String, PositionInterpolatorAnim> positionInterpolators = new HashMap<>();

    public X3DAnariHandler(Object x3dModel) {
        this.x3dModel = x3dModel;
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        this.device = device;
    }

    @Override
    protected void updateScene(TimerState state) throws Throwable {
        super.updateScene(state);
        if (!built && device != null && world != null) {
            built = true;
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
        if (!built || timeSensors.isEmpty()) return;
        if (animStartTime == 0) animStartTime = System.currentTimeMillis();

        double nowSec = Math.max(0.0, (System.currentTimeMillis() - animStartTime) / 1000.0);
        boolean meshUpdated = false;

        for (TimeSensorAnim ts : timeSensors) {
            if (!ts.enabled || !ts.isRunning) continue;
            if (!ts.loop && nowSec > ts.cycleInterval) {
                ts.isRunning = false;
                continue;
            }

            float frac = ts.loop
                ? (float) ((nowSec % ts.cycleInterval) / ts.cycleInterval)
                : (float) Math.min(1.0, nowSec / ts.cycleInterval);

            for (X3DRoute r : routes) {
                if (!r.fromNode.equals(ts.def)) continue;
                if (!"fraction_changed".equals(r.fromField) && !"fraction".equals(r.fromField)) continue;

                ScalarInterpolatorAnim si = interpolators.get(r.toNode);
                if (si != null) {
                    float weight = si.evaluate(frac);
                    for (X3DRoute r2 : routes) {
                        if (r2.fromNode.equals(si.def) && ("value_changed".equals(r2.fromField) || "value".equals(r2.fromField))) {
                            DisplacerAnim da = context.displacers.get(r2.toNode);
                            if (da != null) da.currentWeight = weight;

                            TextureTransformAnim tt = context.textureTransforms.get(r2.toNode);
                            if (tt != null && ("rotation".equals(r2.toField) || "set_rotation".equals(r2.toField))) {
                                tt.rotation = weight;
                            }
                        }
                    }
                }

                OrientationInterpolatorAnim oi = orientationInterpolators.get(r.toNode);
                if (oi != null) {
                    float[] rotation = oi.evaluate(frac);
                    for (X3DRoute r2 : routes) {
                        if (r2.fromNode.equals(oi.def) && ("value_changed".equals(r2.fromField) || "value".equals(r2.fromField))) {
                            applyJointAnimation(r2.toNode, r2.toField, rotation);
                            applyTransformAnimation(r2.toNode, r2.toField, rotation);
                            meshUpdated = true;
                        }
                    }
                }

                PositionInterpolatorAnim pi = positionInterpolators.get(r.toNode);
                if (pi != null) {
                    float[] position = pi.evaluate(frac);
                    for (X3DRoute r2 : routes) {
                        if (r2.fromNode.equals(pi.def) && ("value_changed".equals(r2.fromField) || "value".equals(r2.fromField))) {
                            applyJointAnimation(r2.toNode, r2.toField, position);
                            applyTransformAnimation(r2.toNode, r2.toField, position);
                            TextureTransformAnim tt = context.textureTransforms.get(r2.toNode);
                            if (tt != null) {
                                if ("translation".equals(r2.toField) || "set_translation".equals(r2.toField)) {
                                    tt.translation = new float[]{position[0], position[1]};
                                } else if ("scale".equals(r2.toField) || "set_scale".equals(r2.toField)) {
                                    tt.scale = new float[]{position[0], position[1]};
                                }
                            }
                            meshUpdated = true;
                        }
                    }
                }
            }
        }

        for (DisplacerMeshBinding binding : context.activeBindings) {
            float weight = binding.displacer.currentWeight;
            if (binding.lastWeight != weight) {
                binding.lastWeight = weight;
                binding.apply(device, sceneArena, weight);
                meshUpdated = true;
            }
        }

        if (!context.skinBindings.isEmpty() && !context.joints.isEmpty()) {
            for (SkinMeshBinding binding : context.skinBindings) {
                binding.apply(device);
                meshUpdated = true;
            }
        }

        for (TextureTransformBinding ttb : context.textureBindings) {
            if (ttb.apply(device)) meshUpdated = true;
        }

        if (meshUpdated && world != null) {
            try { world.commit(); } catch (Throwable ignored) {}
        }
    }

    private void applyJointAnimation(String targetDef, String field, float[] value) {
        JointAnim joint = context.joints.get(targetDef);
        if (joint == null || value == null) return;
        if ("set_rotation".equals(field) || "rotation".equals(field)) {
            if (value.length >= 4) joint.rotation = new float[]{value[0], value[1], value[2], value[3]};
        } else if ("set_translation".equals(field) || "translation".equals(field)) {
            if (value.length >= 3) joint.translation = new float[]{value[0], value[1], value[2]};
        }
    }

    private void applyTransformAnimation(String targetDef, String field, float[] value) {
        TransformAnim tAnim = context.animatedTransforms.get(targetDef);
        if (tAnim == null || value == null) return;
        if ("set_rotation".equals(field) || "rotation".equals(field)) {
            if (value.length >= 4) {
                tAnim.rotation = new float[]{value[0], value[1], value[2], value[3]};
                tAnim.updateTransform();
            }
        } else if ("set_translation".equals(field) || "translation".equals(field)) {
            if (value.length >= 3) {
                tAnim.translation = new float[]{value[0], value[1], value[2]};
                tAnim.updateTransform();
            }
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

    private void build(Device device) throws Throwable {
        float[] neutral = AnariContext.SWAP_RED_BLUE
            ? new float[]{ 0.28f, 0.24f, 0.20f }
            : new float[]{ 0.20f, 0.24f, 0.28f };
        defaultMaterial = device.newMaterial(Material.SubType.MATTE).setColor(neutral[0], neutral[1], neutral[2]);
        defaultMaterial.commit();
        keepAlive.add(defaultMaterial);

        context = new AnariContext(device, sceneArena, keepAlive, anariInstances,
                                   textureCache, defMap, protoMap, defaultMaterial);

        buildCache(x3dModel);

        float[] identity = { 1,0,0,0, 0,(AnariContext.FLIP_Y ? -1 : 1),0,0, 0,0,1,0, 0,0,0,1 };
        Object scene = null;
        for (String m : List.of("getScene")) {
            try { scene = x3dModel.getClass().getMethod(m).invoke(x3dModel); if (scene != null) break; } catch (Exception ignored) {}
        }
        if (scene != null) {
            List<?> sceneChildren = X3DTypeAdapter.getListFromNode(scene, "getChildren", "getChildrenList", "getChildList");
            AnariNodeFactory.traverseList(context, sceneChildren, identity, new HashMap<>());
        }

        if (context.bmin[0] <= context.bmax[0]) {
            float cx = (context.bmin[0] + context.bmax[0]) / 2;
            float cy = (context.bmin[1] + context.bmax[1]) / 2;
            float cz = (context.bmin[2] + context.bmax[2]) / 2;
            float dx = context.bmax[0] - context.bmin[0];
            float dy = context.bmax[1] - context.bmin[1];
            float dz = context.bmax[2] - context.bmin[2];
            float radius = 0.5f * (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (!context.viewpointSet) {
                cameraTarget = new float[] { cx, cy, cz };
                cameraDistance = Math.max(radius * 2.5f, 1f);
            } else {
                cameraTarget = context.cameraTarget;
                cameraDistance = context.cameraDistance;
            }
        }

        angleScale = Math.abs(cameraElevation) > 1.6f ? (float) (Math.PI / 180.0) : 1f;
        // Set an isometric-like perspective angle so the 3D cube structure is immediately visible
        cameraAzimuth = 0.45f;
        cameraElevation = 0.30f;

        if (renderer != null) {
            try {
                renderer.setFloat32("ambientRadiance", AMBIENT_RADIANCE);
                float[] bg = (backgroundColor != null) ? backgroundColor : new float[]{0f, 0f, 0f, 1f};
                renderer.set("background", DataType.FLOAT32_VEC4,
                             sceneArena.allocateFrom(ValueLayout.JAVA_FLOAT, bg[0], bg[1], bg[2], bg[3]));
                renderer.commit();
            } catch (Throwable t) { t.printStackTrace(); }
        }

        headlight = device.newLight(Light.SubType.DIRECTIONAL).setDirection(-0.4f, -0.6f, -0.7f);
        headlight.setIrradiance(HEADLIGHT_IRRADIANCE);
        headlight.commit();
        keepAlive.add(headlight);

        List<Light> allLights = new ArrayList<>();
        allLights.add(headlight);

        try {
            fillLight = device.newLight(Light.SubType.DIRECTIONAL).setDirection(0.5f, 0.7f, 0.5f);
            fillLight.setIrradiance(0.6f);
            fillLight.commit();
            keepAlive.add(fillLight);
            allLights.add(fillLight);
        } catch (Throwable ignored) {}

        if (!anariInstances.isEmpty()) {
            Array1D instArray = device.newArray1D(anariInstances, DataType.INSTANCE);
            instArray.commit();
            world.setInstance(instArray);
            keepAlive.add(instArray);
        }

        Array1D lightArray = device.newArray1D(allLights, DataType.LIGHT);
        lightArray.commit();
        world.setLight(lightArray);
        keepAlive.add(lightArray);

        world.commit();
    }

    private void buildCache(Object node) {
        if (node == null || !visited.add(node)) return;

        String def = X3DTypeAdapter.asString(node, "getDEF");
        if (def != null && !def.trim().isEmpty()) {
            defMap.put(X3DTypeAdapter.cleanQuotes(def), node);
        }

        String cName = node.getClass().getSimpleName();
        if (cName.contains("Background")) {
            float[] skyColor = X3DTypeAdapter.asFloatArray(node, "getSkyColor");
            if (skyColor != null && skyColor.length >= 3) {
                float[] disp = context != null ? context.displayColor(skyColor[0], skyColor[1], skyColor[2]) : new float[]{ skyColor[0], skyColor[1], skyColor[2] };
                this.backgroundColor = new float[]{ disp[0], disp[1], disp[2], 1.0f };
            }
        }

        if (cName.contains("ProtoDeclare")) {
            String name = X3DTypeAdapter.asString(node, "getName");
            if (name != null && !name.trim().isEmpty()) {
                String clean = X3DTypeAdapter.cleanQuotes(name);
                protoMap.put(clean, node);
            }
        }

        if (cName.contains("TimeSensor") && def != null) {
            boolean enabled = true;
            for (String m : List.of("getEnabled", "isEnabled")) {
                try { Object r = node.getClass().getMethod(m).invoke(node); if (r instanceof Boolean) { enabled = (Boolean) r; break; } } catch (Exception ignored) {}
            }
            double interval = Math.max(0.001, X3DTypeAdapter.asDouble(node, "getCycleInterval", 1.0));
            boolean loop = false;
            for (String m : List.of("getLoop", "isLoop")) {
                try { Object r = node.getClass().getMethod(m).invoke(node); if (r instanceof Boolean) { loop = (Boolean) r; break; } } catch (Exception ignored) {}
            }
            timeSensors.add(new TimeSensorAnim(def, interval, loop, enabled));
        }

        if (cName.contains("ScalarInterpolator") && def != null) {
            float[] key = X3DTypeAdapter.asFloatArray(node, "getKey");
            float[] val = X3DTypeAdapter.asFloatArray(node, "getKeyValue");
            if (val != null && val.length > 0) {
                if (key == null || key.length == 0) {
                    key = new float[val.length];
                    for (int i = 0; i < val.length; i++) key[i] = (val.length > 1) ? (float) i / (val.length - 1) : 0f;
                }
                interpolators.put(def, new ScalarInterpolatorAnim(def, key, val));
            }
        }

        if (cName.contains("OrientationInterpolator") && def != null) {
            float[] key = X3DTypeAdapter.asFloatArray(node, "getKey");
            float[] val = X3DTypeAdapter.asFloatArray(node, "getKeyValue");
            if (val != null && val.length >= 4) {
                int count = val.length / 4;
                if (key == null || key.length == 0) {
                    key = new float[count];
                    for (int i = 0; i < count; i++) key[i] = (count > 1) ? (float) i / (count - 1) : 0f;
                }
                orientationInterpolators.put(def, new OrientationInterpolatorAnim(def, key, val));
            }
        }

        if (cName.contains("PositionInterpolator") && def != null) {
            float[] key = X3DTypeAdapter.asFloatArray(node, "getKey");
            float[] val = X3DTypeAdapter.asFloatArray(node, "getKeyValue");
            if (val != null && val.length >= 3) {
                int count = val.length / 3;
                if (key == null || key.length == 0) {
                    key = new float[count];
                    for (int i = 0; i < count; i++) key[i] = (count > 1) ? (float) i / (count - 1) : 0f;
                }
                positionInterpolators.put(def, new PositionInterpolatorAnim(def, key, val));
            }
        }

        if (cName.contains("TextureTransform") && def != null) {
            float[] center = X3DTypeAdapter.asFloatArray(node, "getCenter");
            float rot = (float) X3DTypeAdapter.asDouble(node, "getRotation", 0.0);
            float[] scale = X3DTypeAdapter.asFloatArray(node, "getScale");
            float[] trans = X3DTypeAdapter.asFloatArray(node, "getTranslation");
            context.textureTransforms.put(def, new TextureTransformAnim(def, center, rot, scale, trans));
        }

        if (cName.contains("HAnimDisplacer") && def != null) {
            int[] ci = X3DTypeAdapter.asIntArray(node, "getCoordIndex");
            float[] d = X3DTypeAdapter.asFloatArray(node, "getDisplacements");
            if (ci != null && d != null && ci.length > 0 && d.length > 0) {
                DisplacerAnim da = new DisplacerAnim(def, ci, d);
                context.displacers.put(def, da);
                String dName = X3DTypeAdapter.asString(node, "getName");
                if (dName != null && !dName.isEmpty()) context.displacers.put(dName, da);
            }
        }

        if (cName.contains("ROUTE")) {
            String fNode = X3DTypeAdapter.asString(node, "getFromNode");
            String fField = X3DTypeAdapter.asString(node, "getFromField");
            String tNode = X3DTypeAdapter.asString(node, "getToNode");
            String tField = X3DTypeAdapter.asString(node, "getToField");
            if (fNode != null && tNode != null) {
                routes.add(new X3DRoute(X3DTypeAdapter.cleanQuotes(fNode),
                                       X3DTypeAdapter.cleanQuotes(fField),
                                       X3DTypeAdapter.cleanQuotes(tNode),
                                       X3DTypeAdapter.cleanQuotes(tField)));
            }
        }

        if (cName.equals("X3D")) {
            tryInvokeAndCache(node, "getScene");
            return;
        }

        for (String m : List.of("getChildren", "getChildrenList", "getChildList", "getProtoDeclare", "getProtoDeclareList", "getProtos",
                                "getRoutes", "getRouteList", "getAppearance", "getGeometry", "getMaterial", "getTexture", "getTextureTransform",
                                "getSkeleton", "getSkeletonList", "getSkin", "getSkinList",
                                "getJoints", "getSegments", "getSites", "getDisplacers", "getDisplacerList",
                                "getControlPoint", "getControlPointList",
                                "getProtoBody", "getProtoInterface", "getField", "getFieldList", "getFieldValue", "getFieldValueList")) {
            tryInvokeAndCache(node, m);
        }
    }

    private void tryInvokeAndCache(Object node, String methodName) {
        try {
            Method m = node.getClass().getMethod(methodName);
            if (m.getParameterCount() == 0) {
                Object res = m.invoke(node);
                if (res instanceof List<?>) {
                    for (Object child : (List<?>) res) if (child != null && isX3DNode(child)) buildCache(child);
                } else if (res instanceof Object[]) {
                    for (Object child : (Object[]) res) if (child != null && isX3DNode(child)) buildCache(child);
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

    static float[] buildHAnimJointMatrix(JointAnim j) {
        float[] c = j.center;
        float[] r = j.rotation != null ? j.rotation : new float[]{0f, 0f, 1f, 0f};
        float[] t = j.translation != null ? j.translation : new float[]{0f, 0f, 0f};

        float[] tc = translationMatrix(c[0], c[1], c[2]);
        float[] rr = AnariMath.buildTransformMatrix(null, null, r);
        float[] tnc = translationMatrix(-c[0], -c[1], -c[2]);
        float[] pivot = AnariMath.multiplyMatrix(AnariMath.multiplyMatrix(tc, rr), tnc);
        return AnariMath.multiplyMatrix(translationMatrix(t[0], t[1], t[2]), pivot);
    }

    private static float[] translationMatrix(float x, float y, float z) {
        return new float[]{
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            x,y,z,1
        };
    }

    static class X3DRoute {
        final String fromNode, fromField, toNode, toField;
        X3DRoute(String fn, String ff, String tn, String tf) {
            this.fromNode = fn; this.fromField = ff; this.toNode = tn; this.toField = tf;
        }
    }

    static class TimeSensorAnim {
        final String def; final double cycleInterval; final boolean loop, enabled; boolean isRunning;
        TimeSensorAnim(String def, double cycleInterval, boolean loop, boolean enabled) {
            this.def = def; this.cycleInterval = cycleInterval > 0 ? cycleInterval : 1.0;
            this.loop = loop; this.enabled = enabled; this.isRunning = enabled;
        }
    }

    static class ScalarInterpolatorAnim {
        final String def; final float[] key, keyValue;
        ScalarInterpolatorAnim(String def, float[] key, float[] keyValue) { this.def = def; this.key = key; this.keyValue = keyValue; }
        float evaluate(float fraction) {
            if (key == null || key.length == 0) return 0f;
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

    static class OrientationInterpolatorAnim {
        final String def; final float[] key, keyValue;
        OrientationInterpolatorAnim(String def, float[] key, float[] keyValue) { this.def = def; this.key = key; this.keyValue = keyValue; }
        float[] evaluate(float fraction) {
            int count = keyValue.length / 4;
            if (count == 0) return new float[]{0, 0, 1, 0};
            if (key == null || key.length == 0 || fraction <= key[0]) {
                return new float[]{keyValue[0], keyValue[1], keyValue[2], keyValue[3]};
            }
            if (fraction >= key[key.length - 1]) {
                int p = (count - 1) * 4;
                return new float[]{keyValue[p], keyValue[p + 1], keyValue[p + 2], keyValue[p + 3]};
            }

            for (int i = 0; i < key.length - 1; i++) {
                if (fraction >= key[i] && fraction <= key[i + 1]) {
                    float span = key[i + 1] - key[i];
                    float alpha = span > 1e-6f ? (fraction - key[i]) / span : 0f;
                    float[] a = new float[]{keyValue[i * 4], keyValue[i * 4 + 1], keyValue[i * 4 + 2], keyValue[i * 4 + 3]};
                    float[] b = new float[]{keyValue[(i + 1) * 4], keyValue[(i + 1) * 4 + 1], keyValue[(i + 1) * 4 + 2], keyValue[(i + 1) * 4 + 3]};
                    return AnariMath.slerpAxisAngle(a, b, alpha);
                }
            }
            int p = (count - 1) * 4;
            return new float[]{keyValue[p], keyValue[p + 1], keyValue[p + 2], keyValue[p + 3]};
        }
    }

    static class PositionInterpolatorAnim {
        final String def; final float[] key, keyValue;
        PositionInterpolatorAnim(String def, float[] key, float[] keyValue) { this.def = def; this.key = key; this.keyValue = keyValue; }
        float[] evaluate(float fraction) {
            int count = keyValue.length / 3;
            if (count == 0) return new float[]{0, 0, 0};
            if (key == null || key.length == 0 || fraction <= key[0]) {
                return new float[]{keyValue[0], keyValue[1], keyValue[2]};
            }
            if (fraction >= key[key.length - 1]) {
                int p = (count - 1) * 3;
                return new float[]{keyValue[p], keyValue[p + 1], keyValue[p + 2]};
            }

            for (int i = 0; i < key.length - 1; i++) {
                if (fraction >= key[i] && fraction <= key[i + 1]) {
                    float span = key[i + 1] - key[i];
                    float a = span > 1e-6f ? (fraction - key[i]) / span : 0f;
                    int p = i * 3, q = (i + 1) * 3;
                    return new float[]{
                        keyValue[p]     + a * (keyValue[q]     - keyValue[p]),
                        keyValue[p + 1] + a * (keyValue[q + 1] - keyValue[p + 1]),
                        keyValue[p + 2] + a * (keyValue[q + 2] - keyValue[p + 2])
                    };
                }
            }
            int p = (count - 1) * 3;
            return new float[]{keyValue[p], keyValue[p + 1], keyValue[p + 2]};
        }
    }

    static class TextureTransformAnim {
        final String def; float[] center; float rotation; float[] scale, translation;
        TextureTransformAnim(String def, float[] center, float rotation, float[] scale, float[] translation) {
            this.def = def;
            this.center = (center != null && center.length >= 2) ? center.clone() : new float[]{0f, 0f};
            this.rotation = rotation;
            this.scale = (scale != null && scale.length >= 2) ? scale.clone() : new float[]{1f, 1f};
            this.translation = (translation != null && translation.length >= 2) ? translation.clone() : new float[]{0f, 0f};
        }
    }

    static class TextureTransformBinding {
        final AnariContext context;
        final TextureTransformAnim transform;
        final Geometry.Triangle geometry;
        final MemorySegment uvSeg;
        Array1D uvArray;
        final float[] baseUVs;
        float lastRot = Float.NaN;
        float lastTx = Float.NaN, lastTy = Float.NaN;
        float lastSx = Float.NaN, lastSy = Float.NaN;

        TextureTransformBinding(AnariContext context, TextureTransformAnim transform, Geometry.Triangle geometry, MemorySegment uvSeg, Array1D uvArray, float[] baseUVs) {
            this.context = context;
            this.transform = transform;
            this.geometry = geometry;
            this.uvSeg = uvSeg;
            this.uvArray = uvArray;
            this.baseUVs = baseUVs.clone();
        }

        boolean apply(Device device) {
            float rot = transform.rotation;
            float tx = transform.translation[0], ty = transform.translation[1];
            float sx = transform.scale[0], sy = transform.scale[1];

            if (rot == lastRot && tx == lastTx && ty == lastTy && sx == lastSx && sy == lastSy) {
                return false;
            }
            lastRot = rot;
            lastTx = tx;
            lastTy = ty;
            lastSx = sx;
            lastSy = sy;

            float cos = (float) Math.cos(rot), sin = (float) Math.sin(rot);
            float cx = transform.center[0], cy = transform.center[1];
            int n = baseUVs.length / 2;
            for (int i = 0; i < n; i++) {
                float u0 = baseUVs[i * 2], v0 = baseUVs[i * 2 + 1];
                float du = u0 - cx, dv = v0 - cy;
                float u1 = (du * cos - dv * sin) * sx + cx + tx;
                float v1 = (du * sin + dv * cos) * sy + cy + ty;
                long off = (long) i * 2 * Float.BYTES;
                uvSeg.set(ValueLayout.JAVA_FLOAT, off, u1);
                uvSeg.set(ValueLayout.JAVA_FLOAT, off + Float.BYTES, v1);
            }
            try {
                Array1D fresh = device.newArray1D(uvSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC2, n);
                fresh.commit();
                context.setAnariObjectParameter(geometry, "vertex.attribute0", DataType.ARRAY1D, fresh);
                geometry.commit();
                uvArray = fresh;
            } catch (Throwable ignored) {}
            return true;
        }
    }

    static class InstanceBinding {
        final Instance instance;
        final float[] relativeMatrix;
        InstanceBinding(Instance instance, float[] relativeMatrix) {
            this.instance = instance;
            this.relativeMatrix = relativeMatrix.clone();
        }
    }

    static class TransformAnim {
        final String def;
        final float[] parentTransform;
        float[] translation = new float[]{0f, 0f, 0f};
        float[] rotation = new float[]{0f, 0f, 1f, 0f};
        float[] scale = new float[]{1f, 1f, 1f};
        final List<InstanceBinding> bindings;

        TransformAnim(String def, float[] parentTransform, float[] tr, float[] rot, float[] sc, List<InstanceBinding> bindings) {
            this.def = def;
            this.parentTransform = parentTransform.clone();
            if (tr != null && tr.length >= 3) this.translation = tr.clone();
            if (rot != null && rot.length >= 4) this.rotation = rot.clone();
            if (sc != null && sc.length >= 3) this.scale = sc.clone();
            this.bindings = bindings;
        }

        void updateTransform() {
            float[] local = AnariMath.buildTransformMatrix(translation, scale, rotation);
            float[] newCurrentMat = AnariMath.multiplyMatrix(parentTransform, local);
            for (InstanceBinding b : bindings) {
                try {
                    float[] newChildMat = AnariMath.multiplyMatrix(newCurrentMat, b.relativeMatrix);
                    b.instance.setTransform(newChildMat);
                    b.instance.commit();
                } catch (Throwable ignored) {}
            }
        }
    }

    static class DisplacerAnim {
        final String def; final int[] coordIndex; final float[] displacements; float currentWeight = 0f;
        DisplacerAnim(String def, int[] coordIndex, float[] displacements) {
            this.def = def; this.coordIndex = coordIndex; this.displacements = displacements;
        }
    }

    static class DisplacerMeshBinding {
        final DisplacerAnim displacer;
        final Geometry.Triangle geometry;
        Array1D vArray; final MemorySegment vSeg; Array1D nArray; final MemorySegment nSeg;
        final float[] baseCoords; final int[] unrolledToOrig; final List<int[]> tris;
        final List<Integer> triFaceId; final List<Integer>[] vertFaces; final float creaseAngle; float lastWeight = Float.NaN;

        DisplacerMeshBinding(DisplacerAnim displacer, Geometry.Triangle geometry, Array1D vArray, MemorySegment vSeg,
                             Array1D nArray, MemorySegment nSeg, float[] baseCoords, int[] unrolledToOrig,
                             List<int[]> tris, List<Integer> triFaceId, List<Integer>[] vertFaces, float creaseAngle) {
            this.displacer = displacer; this.geometry = geometry; this.vArray = vArray; this.vSeg = vSeg;
            this.nArray = nArray; this.nSeg = nSeg; this.baseCoords = baseCoords.clone(); this.unrolledToOrig = unrolledToOrig;
            this.tris = tris; this.triFaceId = triFaceId; this.vertFaces = vertFaces; this.creaseAngle = creaseAngle;
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
                vSeg.set(ValueLayout.JAVA_FLOAT, offset, deformed[cIdx * 3]);
                vSeg.set(ValueLayout.JAVA_FLOAT, offset + Float.BYTES, deformed[cIdx * 3 + 1]);
                vSeg.set(ValueLayout.JAVA_FLOAT, offset + 2 * Float.BYTES, deformed[cIdx * 3 + 2]);
            }
            try {
                Array1D freshArray = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, unrolledToOrig.length);
                freshArray.commit();
                geometry.setVertexPosition(freshArray);
                geometry.commit();
                vArray = freshArray;
            } catch (Throwable ignored) {}
        }
    }

    static class JointAnim {
        final String def, name; final float[] center; float[] rotation, translation;
        final int[] skinCoordIndex; final float[] skinCoordWeight; final JointAnim parent;
        float[] bindMatrix;

        JointAnim(String def, String name, float[] center, float[] rotation, float[] translation,
                  int[] skinCoordIndex, float[] skinCoordWeight, JointAnim parent) {
            this.def = def; this.name = name; this.center = center.clone();
            this.rotation = rotation.clone(); this.translation = translation.clone();
            this.skinCoordIndex = skinCoordIndex; this.skinCoordWeight = skinCoordWeight;
            this.parent = parent;
        }
    }

    static class SkinInfluence {
        final JointAnim joint; final float weight;
        SkinInfluence(JointAnim joint, float weight) { this.joint = joint; this.weight = weight; }
    }

    static class SkinMeshBinding {
        final AnariContext context;
        final Geometry.Triangle geometry;
        Array1D vArray; final MemorySegment vSeg;
        Array1D nArray; final MemorySegment nSeg;
        final float[] baseCoords; final int[] unrolledToOrig;
        final List<int[]> tris; final List<Integer> triFaceId;
        final List<Integer>[] vertFaces; final float creaseAngle;
        final List<SkinInfluence>[] influencesByVertex;

        SkinMeshBinding(AnariContext context, Geometry.Triangle geometry, Array1D vArray, MemorySegment vSeg,
                        Array1D nArray, MemorySegment nSeg, float[] baseCoords,
                        int[] unrolledToOrig, List<int[]> tris, List<Integer> triFaceId,
                        List<Integer>[] vertFaces, float creaseAngle,
                        List<SkinInfluence>[] influencesByVertex) {
            this.context = context; this.geometry = geometry; this.vArray = vArray; this.vSeg = vSeg;
            this.nArray = nArray; this.nSeg = nSeg; this.baseCoords = baseCoords.clone();
            this.unrolledToOrig = unrolledToOrig; this.tris = tris; this.triFaceId = triFaceId;
            this.vertFaces = vertFaces; this.creaseAngle = creaseAngle;
            this.influencesByVertex = influencesByVertex;
        }

        void apply(Device device) {
            Map<JointAnim, float[]> current = new HashMap<>();
            Map<JointAnim, float[]> skinMatrices = new HashMap<>();

            for (JointAnim j : context.joints.values()) {
                float[] currentMatrix = context.computeCurrentJointMatrix(j, current);
                float[] inverseBind = AnariMath.invertRigidMatrix(j.bindMatrix);
                skinMatrices.put(j, AnariMath.multiplyMatrix(currentMatrix, inverseBind));
            }

            float[] source = baseCoords.clone();
            for (DisplacerAnim da : context.displacers.values()) {
                if (da.currentWeight == 0f || da.coordIndex == null || da.displacements == null) continue;
                int n = Math.min(da.coordIndex.length, da.displacements.length / 3);
                for (int i = 0; i < n; i++) {
                    int c = da.coordIndex[i];
                    if (c >= 0 && c * 3 + 2 < source.length) {
                        source[c * 3]     += da.currentWeight * da.displacements[i * 3];
                        source[c * 3 + 1] += da.currentWeight * da.displacements[i * 3 + 1];
                        source[c * 3 + 2] += da.currentWeight * da.displacements[i * 3 + 2];
                    }
                }
            }

            float[] deformed = new float[baseCoords.length];
            for (int i = 0; i < influencesByVertex.length; i++) {
                float bx = source[i * 3], by = source[i * 3 + 1], bz = source[i * 3 + 2];
                List<SkinInfluence> infs = influencesByVertex[i];

                if (infs.isEmpty()) {
                    deformed[i * 3]     = bx;
                    deformed[i * 3 + 1] = by;
                    deformed[i * 3 + 2] = bz;
                    continue;
                }

                float x = 0, y = 0, z = 0, sum = 0;
                for (SkinInfluence inf : infs) {
                    float[] p = AnariMath.transformPoint(skinMatrices.get(inf.joint), bx, by, bz);
                    x += inf.weight * p[0];
                    y += inf.weight * p[1];
                    z += inf.weight * p[2];
                    sum += inf.weight;
                }
                if (sum > 1e-6f) {
                    deformed[i * 3]     = x / sum;
                    deformed[i * 3 + 1] = y / sum;
                    deformed[i * 3 + 2] = z / sum;
                } else {
                    deformed[i * 3]     = bx;
                    deformed[i * 3 + 1] = by;
                    deformed[i * 3 + 2] = bz;
                }
            }

            for (int v = 0; v < unrolledToOrig.length; v++) {
                int c = unrolledToOrig[v];
                long off = (long) v * 3 * Float.BYTES;
                vSeg.set(ValueLayout.JAVA_FLOAT, off, deformed[c * 3]);
                vSeg.set(ValueLayout.JAVA_FLOAT, off + Float.BYTES, deformed[c * 3 + 1]);
                vSeg.set(ValueLayout.JAVA_FLOAT, off + 2 * Float.BYTES, deformed[c * 3 + 2]);
            }

            float[] normals = AnariMath.computeSmoothNormals(deformed, tris, triFaceId,
                                                             AnariMath.recomputeFaceNormals(deformed, tris, triFaceId),
                                                             vertFaces, creaseAngle, unrolledToOrig);
            for (int v = 0; v < unrolledToOrig.length; v++) {
                long off = (long) v * 3 * Float.BYTES;
                nSeg.set(ValueLayout.JAVA_FLOAT, off, normals[v * 3]);
                nSeg.set(ValueLayout.JAVA_FLOAT, off + Float.BYTES, normals[v * 3 + 1]);
                nSeg.set(ValueLayout.JAVA_FLOAT, off + 2 * Float.BYTES, normals[v * 3 + 2]);
            }

            try {
                Array1D fresh = device.newArray1D(vSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, unrolledToOrig.length);
                fresh.commit();
                geometry.setVertexPosition(fresh);
                Array1D freshN = device.newArray1D(nSeg, MemorySegment.NULL, MemorySegment.NULL, DataType.FLOAT32_VEC3, unrolledToOrig.length);
                freshN.commit();
                context.setAnariObjectParameter(geometry, "vertex.normal", DataType.ARRAY1D, freshN);
                geometry.commit();
                vArray = fresh;
                nArray = freshN;
            } catch (Throwable ignored) {}
        }
    }
}

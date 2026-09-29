// package org.codeberg.anari.example;

import static java.lang.foreign.MemorySegment.NULL;

import org.codeberg.anari.api.Array1D;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Group;
import org.codeberg.anari.api.Instance;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Surface;
import static org.codeberg.anari.api.DataType.*;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.TimerState;

import org.web3d.x3d.sai.Core.X3DNode;
import org.web3d.x3d.sai.Grouping.X3DGroupingNode;

import org.web3d.x3d.jsail.X3DConcreteNode;
import org.web3d.x3d.jsail.Core.X3D;
import org.web3d.x3d.jsail.Core.Scene;
import org.web3d.x3d.jsail.Core.ProtoDeclare;
import org.web3d.x3d.jsail.Core.ProtoInstance;
import org.web3d.x3d.jsail.Core.ProtoBody;
import org.web3d.x3d.jsail.Core.ProtoInterface;
import org.web3d.x3d.jsail.Core.IS;
import org.web3d.x3d.jsail.Core.connect;
import org.web3d.x3d.jsail.Core.field;
import org.web3d.x3d.jsail.Core.fieldValue;
import org.web3d.x3d.jsail.Grouping.Transform;
import org.web3d.x3d.jsail.Grouping.Switch;
import org.web3d.x3d.jsail.HAnim.HAnimJoint;
import org.web3d.x3d.jsail.HAnim.HAnimHumanoid;
import org.web3d.x3d.jsail.Shape.Shape;
import org.web3d.x3d.jsail.Shape.Appearance;
import org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet;
import org.web3d.x3d.jsail.Geometry3D.Sphere;
import org.web3d.x3d.jsail.Geometry3D.Box;
import org.web3d.x3d.jsail.Geometry3D.Cylinder;
import org.web3d.x3d.jsail.Rendering.Coordinate;
import org.web3d.x3d.jsail.Navigation.Viewpoint;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NOTE ON API ASSUMPTIONS
 * ------------------------
 * The proto/DEF-USE resolution below calls a handful of JSAIL getters
 * (getDEF, getUSE, getIS, getConnect, getNodeField, getProtoField,
 * getFieldValue, getName, getValue, getField, getChildren on
 * ProtoBody/fieldValue/field, getSize/getRadius/getHeight on the
 * primitive geometries) that were inferred from JSAIL's normal
 * "getter mirrors setter, returns plain Java type" convention rather
 * than confirmed against the exact generated sources. If your JSAIL
 * jar differs slightly (e.g. getName() returns SFString instead of
 * String), you'll get compile errors pinpointing exactly which calls
 * to adjust -- the surrounding structure/logic doesn't depend on it.
 */
public class X3DAnariAdvancedHandler4 extends AbstractHandler {

    private final X3D rootX3d;

    // DEF registry: node name -> the actual concrete node instance it names.
    private final Map<String, X3DNode> defRegistry = new HashMap<>();
    // Proto registry: proto name -> its declaration (interface + body template).
    private final Map<String, ProtoDeclare> protoRegistry = new HashMap<>();

    // Parsed Viewpoint from X3D scene
    private ViewpointData x3dViewpoint = null;

    // Diagnostics: only print traversal/surface counts for the first few frames
    private int debugFrameCount = 0;

    @FunctionalInterface
    public interface AnariCleanupTask {
        void cleanup() throws Throwable;
    }

    // Queue for cleaning up objects from PREVIOUS frame
    // Current frame objects must stay alive until AFTER frame.render()
    private final List<AnariCleanupTask> prevFrameCleanupQueue = new ArrayList<>();

    private static class FlattenedRenderable {
        float[] absoluteTransform;
        X3DNode geometryNode;
        org.web3d.x3d.jsail.Shape.Material materialNode;
    }

    private static class MeshData {
        float[] vertices; // xyz triples
        int[] indices;    // triangle triples
        MeshData(float[] v, int[] i) { vertices = v; indices = i; }
    }

    public X3DAnariAdvancedHandler4() {
        super();
        this.rootX3d = new X3D();
    }

    public X3DAnariAdvancedHandler4(X3D rootX3d) {
        super();
        this.rootX3d = rootX3d;
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        // Parse Viewpoint from X3D scene once during initialization
        parseViewpoint();
    }

    /** Find and parse the first Viewpoint node in the scene */
    private void parseViewpoint() {
        Scene scene = rootX3d.getScene();
        if (scene != null && scene.getChildren() != null) {
            for (X3DNode child : scene.getChildren()) {
                ViewpointData vp = findViewpoint(child);
                if (vp != null) {
                    x3dViewpoint = vp;
                    System.out.println("Found X3D Viewpoint: pos=" + java.util.Arrays.toString(vp.position) + 
                        ", dir=" + java.util.Arrays.toString(vp.direction) + 
                        ", up=" + java.util.Arrays.toString(vp.up));
                    break;
                }
            }
        }
        // Set camera target to scene center (origin for BoxEm)
        cameraTarget = new float[]{0.0f, 0.0f, 0.0f};
    }

    private ViewpointData findViewpoint(X3DNode node) {
        if (node == null) return null;
        
        if (node instanceof Viewpoint) {
            Viewpoint vp = (Viewpoint) node;
            ViewpointData data = new ViewpointData();
            try {
                if (vp.getPosition() != null) {
                    data.position = vp.getPosition();
                }
                if (vp.getOrientation() != null) {
                    // X3D orientation is axis-angle: x, y, z, angle
                    // Convert to direction vector (simplified: use -Z rotated)
                    float[] orient = vp.getOrientation();
                    if (orient.length >= 4) {
                        // For simplicity, use default direction if orientation is identity
                        double angle = orient[3];
                        if (Math.abs(angle) < 0.001) {
                            data.direction = new float[]{0, 0, -1};
                        } else {
                            // Axis-angle to direction (approximate)
                            data.direction = new float[]{
                                (float) (orient[0] * Math.sin(angle)),
                                (float) (orient[1] * Math.sin(angle)),
                                (float) (orient[2] * Math.sin(angle))
                            };
                        }
                    }
                }
                if (vp.getFieldOfView() > 0) {
                    data.fieldOfView = vp.getFieldOfView();
                }
                // Up vector is typically (0,1,0) unless specified
                data.up = new float[]{0, 1, 0};
                return data;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Recurse into children
        if (node instanceof X3DGroupingNode) {
            X3DNode[] children = ((X3DGroupingNode) node).getChildren();
            if (children != null) {
                for (X3DNode child : children) {
                    ViewpointData result = findViewpoint(child);
                    if (result != null) return result;
                }
            }
        } else if (node instanceof ProtoInstance) {
            ProtoInstance pi = (ProtoInstance) node;
            if (pi.getFieldValueList() != null) {
                for (fieldValue fv : pi.getFieldValueList()) {
                    if (fv.getChildren() != null) {
                        for (X3DNode child : fv.getChildren()) {
                            ViewpointData result = findViewpoint(child);
                            if (result != null) return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected ViewpointData getViewpoint() {
        return x3dViewpoint;
    }

    @Override
    protected void updateScene(TimerState timer) throws Throwable {
        boolean debug = debugFrameCount < 5;
        try {
            List<FlattenedRenderable> renderables = new ArrayList<>();

            Scene scene = rootX3d.getScene();
            if (scene != null && scene.getChildren() != null) {
                // Pre-pass: collect every DEF'd node and every ProtoDeclare
                // reachable from the scene *before* we try to render anything,
                // since USE and ProtoInstance can both reference things that
                // are only reachable through field values / proto bodies.
                defRegistry.clear();
                protoRegistry.clear();
                for (X3DNode child : scene.getChildren()) {
                    registerDefsAndProtos(child);
                }

                for (X3DNode child : scene.getChildren()) {
                    traverseSceneGraph(child, getIdentityMatrix(), renderables, null);
                }
            } else if (debug) {
                System.out.println("[X3DAnariAdvancedHandler4] scene or scene.getChildren() is null -- nothing to traverse");
            }

            if (debug) {
                System.out.println("[X3DAnariAdvancedHandler4] renderables found after traversal: " + renderables.size());
            }

            List<Surface> surfaces = new ArrayList<>();
            List<AnariCleanupTask> memoryCleanupQueue = new ArrayList<>();

            for (FlattenedRenderable item : renderables) {
                if (debug) {
                    System.out.println("[X3DAnariAdvancedHandler4] renderable geometryNode=" +
                        (item.geometryNode == null ? "null" : item.geometryNode.getClass().getSimpleName()) +
                        " materialNode=" + (item.materialNode == null ? "null" : "present"));
                }
                Surface surface = buildAnariSurface(item.geometryNode, item.materialNode, item.absoluteTransform, memoryCleanupQueue);
                if (surface == null) {
                    if (debug) {
                        System.out.println("[X3DAnariAdvancedHandler4] buildAnariSurface returned null for this renderable -- skipped");
                    }
                    continue;
                }
                surfaces.add(surface);
            }

            if (debug) {
                System.out.println("[X3DAnariAdvancedHandler4] surfaces built: " + surfaces.size());
            }

            // Put surfaces directly on world (like JavaFxHandler)
            if (!surfaces.isEmpty()) {
                MemorySegment surfacePointers = arena.allocate(ValueLayout.ADDRESS, surfaces.size());
                for (int i = 0; i < surfaces.size(); i++) {
                    surfacePointers.setAtIndex(ValueLayout.ADDRESS, i, surfaces.get(i).getAddress(arena));
                }
                Array1D arraySurfaces = device.newArray1D(surfacePointers, NULL, NULL, SURFACE, surfaces.size()).commit();
                world.setSurface(arraySurfaces).commit();
            } else if (debug) {
                System.out.println("[X3DAnariAdvancedHandler4] surfaces list is empty -- world.setSurface was NOT called this frame");
            }

            Light light = device.newLight(Light.SubType.DIRECTIONAL).commit();

            MemorySegment lightPointers = arena.allocate(ValueLayout.ADDRESS, 1);
            lightPointers.setAtIndex(ValueLayout.ADDRESS, 0, light.getAddress(arena));

            Array1D arrayLights = device.newArray1D(lightPointers, NULL, NULL, LIGHT, 1).commit();
            world.setLight(arrayLights).commit();

            // Execute cleanup from PREVIOUS frame (not current frame!)
            // Current frame objects must stay alive until AFTER frame.render()
            for (AnariCleanupTask cleanup : prevFrameCleanupQueue) {
                cleanup.cleanup();
            }
            // Queue current frame objects for cleanup in NEXT frame
            prevFrameCleanupQueue.clear();
            prevFrameCleanupQueue.addAll(memoryCleanupQueue);
            memoryCleanupQueue.clear();

            if (debug) {
                System.out.println("[X3DAnariAdvancedHandler4] x3dViewpoint=" + (x3dViewpoint == null ? "null (using AbstractHandler default camera)" :
                    ("pos=" + java.util.Arrays.toString(x3dViewpoint.position) +
                     " dir=" + java.util.Arrays.toString(x3dViewpoint.direction) +
                     " up=" + java.util.Arrays.toString(x3dViewpoint.up) +
                     " fov=" + x3dViewpoint.fieldOfView)));
            }
        } catch (Throwable t) {
            // If something upstream in the render loop swallows exceptions from
            // updateScene(), this frame would otherwise fail silently with no
            // visible symptom other than "nothing rendered". Log it loudly instead.
            System.err.println("[X3DAnariAdvancedHandler4] updateScene() threw:");
            t.printStackTrace();
            throw t;
        } finally {
            debugFrameCount++;
        }
    }
    // ---------------------------------------------------------------

    private void registerDefsAndProtos(X3DNode node) {
        if (node == null) return;

        String def = getDEF(node);
        if (def != null && !def.isEmpty()) {
            defRegistry.put(def, node);
        }

        if (node instanceof ProtoDeclare) {
            ProtoDeclare decl = (ProtoDeclare) node;
            protoRegistry.put(decl.getName(), decl);
            if (decl.getProtoBody() != null && decl.getProtoBody().getChildren() != null) {
                for (X3DNode c : decl.getProtoBody().getChildren()) registerDefsAndProtos(c);
            }
            return;
        }

        if (node instanceof ProtoInstance) {
            ProtoInstance pi = (ProtoInstance) node;
            if (pi.getFieldValueList() != null) {
                for (fieldValue fv : pi.getFieldValueList()) {
                    if (fv.getChildren() != null) {
                        for (X3DNode c : fv.getChildren()) registerDefsAndProtos(c);
                    }
                }
            }
            return;
        }

        if (node instanceof X3DGroupingNode) {
            X3DNode[] children = ((X3DGroupingNode) node).getChildren();
            if (children != null) {
                for (X3DNode c : children) registerDefsAndProtos(c);
            }
        }
    }

    private String getDEF(X3DNode node) {
        if (node instanceof X3DConcreteNode) {
            try {
                return ((X3DConcreteNode) node).getDEF();
            } catch (Exception ignored) { /* node type may not carry a DEF */ }
        }
        return null;
    }

    private String getUSE(X3DNode node) {
        if (node instanceof X3DConcreteNode) {
            try {
                return ((X3DConcreteNode) node).getUSE();
            } catch (Exception ignored) { }
        }
        return null;
    }

    private IS getIS(X3DNode node) {
        if (node instanceof X3DConcreteNode) {
            try {
                return ((X3DConcreteNode) node).getIS();
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Resolves USE references against the DEF registry; returns node unchanged if it isn't a USE. */
    private X3DNode resolveUse(X3DNode node) {
        if (node == null) return null;
        String use = getUSE(node);
        if (use != null && !use.isEmpty()) {
            X3DNode target = defRegistry.get(use);
            if (target != null) return target;
            // Unresolved USE -- leave as-is; caller's later checks will simply find no usable geometry.
        }
        return node;
    }

    // ---------------------------------------------------------------
    // Scene graph traversal (now proto- and USE-aware)
    // ---------------------------------------------------------------

    /**
     * @param substitution the field-value bindings in effect if we're currently inside an
     *                      expanded proto body (protoField name -> String value or X3DNode[]),
     *                      or null when traversing plain (non-proto) scene content.
     */
    private void traverseSceneGraph(X3DNode node, float[] parentTransform, List<FlattenedRenderable> renderables,
                                     Map<String, Object> substitution) {
        if (node == null) return;
        node = resolveUse(node);

        if (node instanceof ProtoInstance) {
            expandProtoInstance((ProtoInstance) node, parentTransform, renderables, substitution);
            return;
        }

        float[] currentTransform = parentTransform;

        if (node instanceof Transform) {
            Transform t = (Transform) node;
            float[] translation = effectiveVec(node, "translation", substitution, t.getTranslation());
            float[] rotation = effectiveVec(node, "rotation", substitution, t.getRotation());
            float[] scale = effectiveVec(node, "scale", substitution, t.getScale());
            float[] localTransform = buildTransformMatrix(translation, rotation, scale);
            currentTransform = multiplyMatrices(parentTransform, localTransform);
        }
        else if (node instanceof HAnimJoint) {
            HAnimJoint joint = (HAnimJoint) node;
            float[] localTransform = buildTransformMatrix(joint.getTranslation(), joint.getRotation(), joint.getScale());
            currentTransform = multiplyMatrices(parentTransform, localTransform);
        }
        else if (node instanceof Switch) {
            Switch sw = (Switch) node;
            int choice = sw.getWhichChoice();
            X3DNode[] children = sw.getChildren();
            if (children != null && choice >= 0 && choice < children.length) {
                traverseSceneGraph(children[choice], currentTransform, renderables, substitution);
            }
            return;
        }

        if (node instanceof Shape) {
            Shape shape = (Shape) node;
            FlattenedRenderable fr = new FlattenedRenderable();
            fr.absoluteTransform = currentTransform;
            fr.geometryNode = resolveUse((X3DNode) shape.getGeometry());

            Appearance appearance = (Appearance) shape.getAppearance();
            if (appearance != null && appearance.getMaterial() != null) {
                fr.materialNode = (org.web3d.x3d.jsail.Shape.Material) appearance.getMaterial();
            }
            renderables.add(fr);
        }

        X3DNode[] children = effectiveChildren(node, substitution);
        if (children != null) {
            for (X3DNode child : children) {
                traverseSceneGraph(child, currentTransform, renderables, substitution);
            }
        } else if (node instanceof HAnimHumanoid) {
            HAnimHumanoid humanoid = (HAnimHumanoid) node;
            if (humanoid.getJoints() != null) {
                for (X3DNode child : humanoid.getJoints()) traverseSceneGraph(child, currentTransform, renderables, substitution);
            }
            if (humanoid.getSkeleton() != null) {
                for (X3DNode child : humanoid.getSkeleton()) traverseSceneGraph(child, currentTransform, renderables, substitution);
            }
        }
    }

    /**
     * Expands a ProtoInstance: resolves the field values it should be instantiated with
     * (interface defaults, overridden by any IS pass-through from an enclosing proto,
     * overridden again by explicit fieldValue on this instance), then traverses the
     * matching ProtoDeclare's body under that substitution.
     */
    private void expandProtoInstance(ProtoInstance pi, float[] parentTransform, List<FlattenedRenderable> renderables,
                                      Map<String, Object> enclosingSubstitution) {
        String protoName = pi.getName();
        ProtoDeclare decl = protoRegistry.get(protoName);
        if (decl == null) return; // unknown proto -- nothing we can expand

        Map<String, Object> substitution = new HashMap<>();

        // 1. Interface defaults.
        if (decl.getProtoInterface() != null && decl.getProtoInterface().getFieldList() != null) {
            for (field f : decl.getProtoInterface().getFieldList()) {
                if (f.getChildren() != null && !f.getChildren().isEmpty()) {
                    substitution.put(f.getName(), f.getChildren().toArray(new X3DNode[0]));
                } else if (f.getValue() != null) {
                    substitution.put(f.getName(), f.getValue());
                }
            }
        }

        // 2. IS pass-through: this ProtoInstance may itself sit inside another proto's
        //    body and expose one of its own fields to that outer proto's interface field.
        IS is = getIS(pi);
        if (is != null && is.getConnectList() != null && enclosingSubstitution != null) {
            for (connect c : is.getConnectList()) {
                Object v = enclosingSubstitution.get(c.getProtoField());
                if (v != null) substitution.put(c.getNodeField(), v);
            }
        }

        // 3. Explicit fieldValue overrides on this instance -- highest priority.
        if (pi.getFieldValueList() != null) {
            for (fieldValue fv : pi.getFieldValueList()) {
                if (fv.getChildren() != null && !fv.getChildren().isEmpty()) {
                    substitution.put(fv.getName(), fv.getChildren().toArray(new X3DNode[0]));
                } else if (fv.getValue() != null) {
                    substitution.put(fv.getName(), fv.getValue());
                }
            }
        }

        if (decl.getProtoBody() == null || decl.getProtoBody().getChildren() == null) return;
        for (X3DNode bodyChild : decl.getProtoBody().getChildren()) {
            traverseSceneGraph(bodyChild, parentTransform, renderables, substitution);
        }
    }

    /** Resolves a node's effective MFNode "children" content, honoring an IS->children binding. */
    private X3DNode[] effectiveChildren(X3DNode node, Map<String, Object> substitution) {
        if (substitution != null) {
            IS is = getIS(node);
            if (is != null && is.getConnectList() != null) {
                for (connect c : is.getConnectList()) {
                    if ("children".equals(c.getNodeField())) {
                        Object v = substitution.get(c.getProtoField());
                        if (v instanceof X3DNode[]) return (X3DNode[]) v;
                    }
                }
            }
        }
        if (node instanceof X3DGroupingNode) {
            return ((X3DGroupingNode) node).getChildren();
        }
        return null;
    }

    /** Resolves an SFVec field (translation/rotation/scale) honoring an IS binding, else falls back to literalValue. */
    private float[] effectiveVec(X3DNode node, String nodeFieldName, Map<String, Object> substitution, float[] literalValue) {
        if (substitution != null) {
            IS is = getIS(node);
            if (is != null && is.getConnectList() != null) {
                for (connect c : is.getConnectList()) {
                    if (nodeFieldName.equals(c.getNodeField())) {
                        Object v = substitution.get(c.getProtoField());
                        if (v instanceof String) return parseFloats((String) v);
                        if (v instanceof float[]) return (float[]) v;
                    }
                }
            }
        }
        return literalValue;
    }

    private float[] parseFloats(String s) {
        String[] parts = s.trim().split("\\s+");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Float.parseFloat(parts[i]);
        return out;
    }

    // ---------------------------------------------------------------
    // Geometry -> ANARI surfaces
    // ---------------------------------------------------------------

    private Surface buildAnariSurface(X3DNode x3dGeoIn, org.web3d.x3d.jsail.Shape.Material x3dMat, float[] absoluteTransform, List<AnariCleanupTask> cleanupQueue) throws Throwable {
        if (x3dGeoIn == null) return null;
        X3DNode x3dGeo = resolveUse(x3dGeoIn);

        float[] diffuse = {0.8f, 0.8f, 0.8f}; // Default gray
        if (x3dMat != null && x3dMat.getDiffuseColor() != null) {
            diffuse = x3dMat.getDiffuseColor();
        }

        MeshData mesh = null;

        if (x3dGeo instanceof IndexedFaceSet) {
            IndexedFaceSet ifs = (IndexedFaceSet) x3dGeo;
            if (ifs.getCoord() != null) {
                float[] vertices = ((Coordinate) ifs.getCoord()).getPoint();
                int[] indices = triangulatePolygonIndex(ifs.getCoordIndex());
                mesh = new MeshData(vertices, indices);
            }
        } else if (x3dGeo instanceof Box) {
            float[] size = ((Box) x3dGeo).getSize();
            if (size == null || size.length < 3) size = new float[] {2f, 2f, 2f}; // X3D default
            mesh = generateBoxMesh(size[0], size[1], size[2]);
        } else if (x3dGeo instanceof Sphere) {
            Float radiusObj = ((Sphere) x3dGeo).getRadius();
            float radius = (radiusObj == null) ? 1f : radiusObj; // X3D default radius = 1
            mesh = generateSphereMesh(radius, 16, 12);
        } else if (x3dGeo instanceof Cylinder) {
            Cylinder cyl = (Cylinder) x3dGeo;
            Float radiusObj = cyl.getRadius();
            Float heightObj = cyl.getHeight();
            float radius = (radiusObj == null) ? 1f : radiusObj; // X3D defaults
            float height = (heightObj == null) ? 2f : heightObj;
            mesh = generateCylinderMesh(radius, height, 16);
        }

        if (mesh == null || mesh.vertices == null || mesh.indices == null || mesh.vertices.length == 0) {
            System.out.println("[X3DAnariAdvancedHandler4] buildAnariSurface: no mesh produced for geometry type " +
                (x3dGeo == null ? "null" : x3dGeo.getClass().getName()) +
                (mesh != null ? " (mesh produced but empty vertices/indices)" : " (unrecognized/unhandled geometry type)"));
            return null;
        }

        float[] worldVertices = (absoluteTransform != null)
                ? transformVertices(mesh.vertices, absoluteTransform)
                : mesh.vertices;

        return createTriangleSurface(worldVertices, mesh.indices, diffuse, cleanupQueue);
    }

    /**
     * Applies the accumulated Transform/HAnimJoint matrix (row-major affine 4x4,
     * as produced by buildTransformMatrix/multiplyMatrices) to each local-space
     * vertex so geometry actually shows up where its ancestor Transforms put it,
     * instead of being rendered in raw local coordinates at the world origin.
     */
    private float[] transformVertices(float[] localVertices, float[] m) {
        float[] out = new float[localVertices.length];
        for (int i = 0; i < localVertices.length; i += 3) {
            float x = localVertices[i], y = localVertices[i + 1], z = localVertices[i + 2];
            out[i]     = m[0]*x + m[1]*y + m[2]*z  + m[3];
            out[i + 1] = m[4]*x + m[5]*y + m[6]*z  + m[7];
            out[i + 2] = m[8]*x + m[9]*y + m[10]*z + m[11];
        }
        return out;
    }

    private Surface createTriangleSurface(float[] vertices, int[] indices, float[] diffuse, List<AnariCleanupTask> cleanupQueue) throws Throwable {
        float[] colors = new float[(vertices.length / 3) * 4];
        for (int i = 0; i < colors.length; i += 4) {
            colors[i]   = diffuse[0];
            colors[i+1] = diffuse[1];
            colors[i+2] = diffuse[2];
            colors[i+3] = 1.0f;
        }

        // FIX: Use 'false' so ANARI copies the array data immediately.
        // This severs the tie to the Java object's GC lifecycle and avoids the use-after-free crash.
        Array1D arrayPos = device.newArray1D(FLOAT32_VEC3, vertices.length / 3).set(false, vertices).commit();
        Array1D arrayColors = device.newArray1D(FLOAT32_VEC4, colors.length / 4).set(false, colors).commit();
        Array1D arrayIdx = device.newArray1D(UINT32_VEC3, indices.length / 3).set(false, indices).commit();

        Geometry anariGeo = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(arrayPos)
                .setVertexColor(arrayColors)
                .setPrimitiveIndex(arrayIdx)
                .commit();

        cleanupQueue.add(() -> { arrayPos.release(); arrayIdx.release(); arrayColors.release(); });

        Material mat = device.newMaterial(Material.SubType.MATTE).setColor("color").commit();
        Surface surface = device.newSurface().setGeometry(anariGeo).setMaterial(mat).commit();

        final Geometry finalAnariGeo = anariGeo;
        final Material finalMat = mat;
        cleanupQueue.add(() -> { finalAnariGeo.release(); finalMat.release(); });

        return surface;
    }

    /**
     * Proper polygon triangulation for IndexedFaceSet's coordIndex: faces are
     * arbitrary polygons delimited by -1, not necessarily pre-triangulated
     * triples. This fan-triangulates each face from its first vertex.
     */
    private int[] triangulatePolygonIndex(int[] coordIndex) {
        List<Integer> tris = new ArrayList<>();
        List<Integer> face = new ArrayList<>();
        for (int idx : coordIndex) {
            if (idx == -1) {
                for (int i = 1; i + 1 < face.size(); i++) {
                    tris.add(face.get(0));
                    tris.add(face.get(i));
                    tris.add(face.get(i + 1));
                }
                face.clear();
            } else {
                face.add(idx);
            }
        }
        if (face.size() >= 3) { // handle a trailing face with no closing -1
            for (int i = 1; i + 1 < face.size(); i++) {
                tris.add(face.get(0));
                tris.add(face.get(i));
                tris.add(face.get(i + 1));
            }
        }
        int[] out = new int[tris.size()];
        for (int i = 0; i < out.length; i++) out[i] = tris.get(i);
        return out;
    }

    // ---------------------------------------------------------------
    // Primitive tessellation
    // ---------------------------------------------------------------

    private MeshData generateBoxMesh(float sx, float sy, float sz) {
        float x = sx / 2f, y = sy / 2f, z = sz / 2f;
        float[] vertices = {
            -x,-y,-z,  x,-y,-z,  x,y,-z,  -x,y,-z, // 0..3 back
            -x,-y, z,  x,-y, z,  x,y, z,  -x,y, z  // 4..7 front
        };
        int[] indices = {
            4,5,6, 6,7,4,   // front (+z)
            1,0,3, 3,2,1,   // back (-z)
            0,4,7, 7,3,0,   // left (-x)
            5,1,2, 2,6,5,   // right (+x)
            3,7,6, 6,2,3,   // top (+y)
            0,1,5, 5,4,0    // bottom (-y)
        };
        // NOTE: winding may need reversing (swap each triangle's last two indices)
        // if your ANARI renderer culls backfaces and boxes appear inside-out.
        return new MeshData(vertices, indices);
    }

    private MeshData generateSphereMesh(float radius, int slices, int stacks) {
        List<Float> verts = new ArrayList<>();
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks; // 0..PI
            for (int j = 0; j <= slices; j++) {
                double theta = 2 * Math.PI * j / slices;
                float vx = (float) (radius * Math.sin(phi) * Math.cos(theta));
                float vy = (float) (radius * Math.cos(phi));
                float vz = (float) (radius * Math.sin(phi) * Math.sin(theta));
                verts.add(vx); verts.add(vy); verts.add(vz);
            }
        }
        List<Integer> tris = new ArrayList<>();
        int rowLen = slices + 1;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = i * rowLen + j;
                int b = a + rowLen;
                tris.add(a); tris.add(b); tris.add(a + 1);
                tris.add(a + 1); tris.add(b); tris.add(b + 1);
            }
        }
        return toMeshData(verts, tris);
    }

    private MeshData generateCylinderMesh(float radius, float height, int sides) {
        List<Float> verts = new ArrayList<>();
        float halfH = height / 2f;

        int bottomCenter = 0;
        verts.add(0f); verts.add(-halfH); verts.add(0f);
        int topCenter = 1;
        verts.add(0f); verts.add(halfH); verts.add(0f);

        int ringStart = 2;
        for (int j = 0; j < sides; j++) {
            double theta = 2 * Math.PI * j / sides;
            float cx = (float) (radius * Math.cos(theta));
            float cz = (float) (radius * Math.sin(theta));
            verts.add(cx); verts.add(-halfH); verts.add(cz); // bottom ring
            verts.add(cx); verts.add(halfH);  verts.add(cz); // top ring
        }
        // vertex layout per side j (starting at ringStart): [bottom_j, top_j]

        List<Integer> tris = new ArrayList<>();
        for (int j = 0; j < sides; j++) {
            int jNext = (j + 1) % sides;
            int b0 = ringStart + j * 2;
            int t0 = b0 + 1;
            int b1 = ringStart + jNext * 2;
            int t1 = b1 + 1;

            // side wall
            tris.add(b0); tris.add(b1); tris.add(t0);
            tris.add(t0); tris.add(b1); tris.add(t1);

            // bottom cap (fan, facing -y)
            tris.add(bottomCenter); tris.add(b1); tris.add(b0);
            // top cap (fan, facing +y)
            tris.add(topCenter); tris.add(t0); tris.add(t1);
        }

        return toMeshData(verts, tris);
    }

    private MeshData toMeshData(List<Float> verts, List<Integer> tris) {
        float[] v = new float[verts.size()];
        for (int i = 0; i < v.length; i++) v[i] = verts.get(i);
        int[] t = new int[tris.size()];
        for (int i = 0; i < t.length; i++) t[i] = tris.get(i);
        return new MeshData(v, t);
    }

    // ---------------------------------------------------------------
    // Matrices & Transforms
    // ---------------------------------------------------------------

    private float[] getIdentityMatrix() {
        return new float[] {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
        };
    }

    private float[] multiplyMatrices(float[] m1, float[] m2) {
        float[] result = new float[16];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                result[r * 4 + c] = m1[r * 4 + 0] * m2[0 * 4 + c] +
                                    m1[r * 4 + 1] * m2[1 * 4 + c] +
                                    m1[r * 4 + 2] * m2[2 * 4 + c] +
                                    m1[r * 4 + 3] * m2[3 * 4 + c];
            }
        }
        return result;
    }

    private float[] buildTransformMatrix(float[] t, float[] r, float[] s) {
        float[] mat = getIdentityMatrix();

        float tx = (t != null && t.length >= 3) ? t[0] : 0;
        float ty = (t != null && t.length >= 3) ? t[1] : 0;
        float tz = (t != null && t.length >= 3) ? t[2] : 0;

        float sx = (s != null && s.length >= 3) ? s[0] : 1;
        float sy = (s != null && s.length >= 3) ? s[1] : 1;
        float sz = (s != null && s.length >= 3) ? s[2] : 1;

        float rx = (r != null && r.length >= 4) ? r[0] : 0;
        float ry = (r != null && r.length >= 4) ? r[1] : 1;
        float rz = (r != null && r.length >= 4) ? r[2] : 0;
        float angle = (r != null && r.length >= 4) ? r[3] : 0;

        float c = (float) Math.cos(angle);
        float si = (float) Math.sin(angle);
        float t_val = 1.0f - c;

        float len = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        if (len > 0) { rx /= len; ry /= len; rz /= len; }

        mat[0] = (t_val*rx*rx + c) * sx;     mat[1] = (t_val*rx*ry - si*rz) * sy; mat[2] = (t_val*rx*rz + si*ry) * sz; mat[3] = tx;
        mat[4] = (t_val*rx*ry + si*rz) * sx; mat[5] = (t_val*ry*ry + c) * sy;     mat[6] = (t_val*ry*rz - si*rx) * sz; mat[7] = ty;
        mat[8] = (t_val*rx*rz - si*ry) * sx; mat[9] = (t_val*ry*rz + si*rx) * sy; mat[10]= (t_val*rz*rz + c) * sz;   mat[11]= tz;

        return mat;
    }

    private float[] extractAffine3x4(float[] matrix4x4) {
        return new float[] {
            matrix4x4[0], matrix4x4[4], matrix4x4[8],
            matrix4x4[1], matrix4x4[5], matrix4x4[9],
            matrix4x4[2], matrix4x4[6], matrix4x4[10],
            matrix4x4[3], matrix4x4[7], matrix4x4[11]
        };
    }
}

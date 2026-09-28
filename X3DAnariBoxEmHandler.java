// package org.codeberg.anari.example;

import static java.lang.foreign.MemorySegment.NULL;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.codeberg.anari.api.AnariException;
import org.codeberg.anari.api.Array1D;
import org.codeberg.anari.api.DataType;
import static org.codeberg.anari.api.DataType.*;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.TimerState;

import org.web3d.x3d.jsail.Core.X3D;
import org.web3d.x3d.jsail.Core.Scene;
import org.web3d.x3d.jsail.fields.SFString;
import org.web3d.x3d.jsail.Navigation.NavigationInfo;
import org.web3d.x3d.jsail.Navigation.Viewpoint;
import org.web3d.x3d.jsail.Geometry3D.Box;
import org.web3d.x3d.jsail.Shape.Shape;
import org.web3d.x3d.jsail.Shape.Appearance;

/**
 * Simplified X3D to ANARI handler for BoxEm scenegraph.
 * Follows the pattern from X3DAnariHandler.java:
 * - Build X3D scenegraph once (from BoxEm)
 * - Pre-process geometry into ANARI arrays once
 * - In updateScene(): apply transforms and push to ANARI
 * - Uses set(false, ...) to avoid use-after-free
 */
public class X3DAnariBoxEmHandler extends AbstractHandler {

    // Pre-processed ANARI data
    private float[] boxVertices;
    private int[] boxIndices;
    private float[] boxColors;

    // ANARI objects (created once, reused)
    private Geometry.Triangle boxGeometry;
    private Material boxMaterial;
    private Surface boxSurface;

    // Viewpoint from X3D scene
    private ViewpointData x3dViewpoint = null;

    public X3DAnariBoxEmHandler() {
        super();
        buildBoxEmScene();
        preprocessGeometry();
    }

    /**
     * Builds the BoxEm X3D scenegraph programmatically.
     * Mirrors BoxEm.java: green box, Viewpoint at (0,0,12), EXAMINE navigation.
     */
    private void buildBoxEmScene() {
        X3D x3d = new X3D().setProfile(new SFString("Immersive"))
            .setVersion(new SFString("4.0"));

        Scene scene = new Scene()
            .addChild(new NavigationInfo()
                .setType(new SFString("\"EXAMINE\"")))
            .addChild(new Viewpoint()
                .setDescription(new SFString("Cubes on Fire"))
                .setPosition(new float[]{0f, 0f, 12f}))
            .addChild(new Shape().setDEF(new SFString("box"))
                .setGeometry(new Box().setSize(new float[]{1f, 1f, 1f}))
                .setAppearance(new Appearance()
                    .setMaterial(new org.web3d.x3d.jsail.Shape.Material().setDiffuseColor(new float[]{0f, 1f, 0f}))));

        x3d.setScene(scene);

        // Parse Viewpoint from the scene
        parseViewpoint(x3d);
        // Camera target at origin (box center)
        cameraTarget = new float[]{0.0f, 0.0f, 0.0f};
    }

    /**
     * Parse Viewpoint from X3D scene.
     */
    private void parseViewpoint(X3D x3d) {
        Scene scene = x3d.getScene();
        if (scene != null && scene.getChildren() != null) {
            for (var child : scene.getChildren()) {
                ViewpointData vp = findViewpoint(child);
                if (vp != null) {
                    x3dViewpoint = vp;
                    System.out.println("Found X3D Viewpoint: pos=" + java.util.Arrays.toString(vp.position) +
                        ", dir=" + java.util.Arrays.toString(vp.direction));
                    break;
                }
            }
        }
    }

    private ViewpointData findViewpoint(Object node) {
        if (node == null) return null;
        if (node instanceof Viewpoint) {
            Viewpoint vp = (Viewpoint) node;
            ViewpointData data = new ViewpointData();
            try {
                if (vp.getPosition() != null) {
                    data.position = vp.getPosition();
                }
                // Default direction for identity orientation
                data.direction = new float[]{0, 0, -1};
                if (vp.getFieldOfView() > 0) {
                    data.fieldOfView = vp.getFieldOfView();
                }
                data.up = new float[]{0, 1, 0};
                return data;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected ViewpointData getViewpoint() {
        // Disable Viewpoint to use EXAMINE mode (which worked before)
        System.out.println("getViewpoint() called, returning: " + x3dViewpoint);
        return null;
    }

    /**
     * Pre-process Box geometry into ANARI arrays.
     * Box is 1x1x1 centered at origin, so vertices are at +/-0.5.
     * Creates 12 triangles (2 per face * 6 faces).
     */
    private void preprocessGeometry() {
        // Box vertices (1 unit cube centered at origin)
        boxVertices = new float[]{
            -0.5f, -0.5f, -0.5f,   // 0
             0.5f, -0.5f, -0.5f,   // 1
             0.5f,  0.5f, -0.5f,   // 2
            -0.5f,  0.5f, -0.5f,   // 3
            -0.5f, -0.5f,  0.5f,   // 4
             0.5f, -0.5f,  0.5f,   // 5
             0.5f,  0.5f,  0.5f,   // 6
            -0.5f,  0.5f,  0.5f    // 7
        };

        // 12 triangles (2 per face * 6 faces), indices as triples
        boxIndices = new int[]{
            4, 5, 6,  6, 7, 4,   // front (+z)
            1, 0, 3,  3, 2, 1,   // back (-z)
            0, 4, 7,  7, 3, 0,   // left (-x)
            5, 1, 2,  2, 6, 5,   // right (+x)
            3, 7, 6,  6, 2, 3,   // top (+y)
            0, 1, 5,  5, 4, 0    // bottom (-y)
        };

        // Green color for all vertices (RGBA)
        int vertexCount = boxVertices.length / 3;
        boxColors = new float[vertexCount * 4];
        for (int i = 0; i < boxColors.length; i += 4) {
            boxColors[i]     = 0.0f; // R
            boxColors[i + 1] = 1.0f; // G
            boxColors[i + 2] = 0.0f; // B
            boxColors[i + 3] = 1.0f; // A
        }

        // Normals for each vertex (one per face, duplicated for shared vertices)
        // Each face has 4 vertices, but we need normals per vertex
        // For a cube, each vertex belongs to 3 faces, so we need to duplicate vertices
        // Actually, for simplicity, let's use flat shading: one normal per face, duplicate vertices
        // But the current indices share vertices, so we need per-vertex normals
        // For a cube with shared vertices, the normal at each vertex is the average of face normals
        // Front face (+z): vertices 4,5,6,7 -> normal (0,0,1)
        // Back face (-z): vertices 0,1,2,3 -> normal (0,0,-1)
        // Left face (-x): vertices 0,3,7,4 -> normal (-1,0,0)
        // Right face (+x): vertices 1,2,6,5 -> normal (1,0,0)
        // Top face (+y): vertices 3,2,6,7 -> normal (0,1,0)
        // Bottom face (-y): vertices 0,1,5,4 -> normal (0,-1,0)
        
        // Each vertex is shared by 3 faces, so average the normals
        // Vertex 0: back(-z) + left(-x) + bottom(-y) = (0,0,-1) + (-1,0,0) + (0,-1,0) = (-1,-1,-1) -> normalized
        // Vertex 1: back(-z) + right(+x) + bottom(-y) = (0,0,-1) + (1,0,0) + (0,-1,0) = (1,-1,-1)
        // Vertex 2: back(-z) + right(+x) + top(+y) = (0,0,-1) + (1,0,0) + (0,1,0) = (1,1,-1)
        // Vertex 3: back(-z) + left(-x) + top(+y) = (0,0,-1) + (-1,0,0) + (0,1,0) = (-1,1,-1)
        // Vertex 4: front(+z) + left(-x) + bottom(-y) = (0,0,1) + (-1,0,0) + (0,-1,0) = (-1,-1,1)
        // Vertex 5: front(+z) + right(+x) + bottom(-y) = (0,0,1) + (1,0,0) + (0,-1,0) = (1,-1,1)
        // Vertex 6: front(+z) + right(+x) + top(+y) = (0,0,1) + (1,0,0) + (0,1,0) = (1,1,1)
        // Vertex 7: front(+z) + left(-x) + top(+y) = (0,0,1) + (-1,0,0) + (0,1,0) = (-1,1,1)
        
        float invSqrt3 = 1.0f / (float) Math.sqrt(3.0);
/*
        boxNormals = new float[]{
            -invSqrt3, -invSqrt3, -invSqrt3,  // 0
             invSqrt3, -invSqrt3, -invSqrt3,  // 1
             invSqrt3,  invSqrt3, -invSqrt3,  // 2
            -invSqrt3,  invSqrt3, -invSqrt3,  // 3
            -invSqrt3, -invSqrt3,  invSqrt3,  // 4
             invSqrt3, -invSqrt3,  invSqrt3,  // 5
             invSqrt3,  invSqrt3,  invSqrt3,  // 6
            -invSqrt3,  invSqrt3,  invSqrt3   // 7
        };
*/
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        try {
            createAnariObjects();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create ANARI geometry, material, surface once.
     * Uses set(false, ...) to copy data immediately (avoids use-after-free).
     */
    private void createAnariObjects() throws AnariException, Throwable {
        // Create geometry with vertex positions, colors, indices (no normals for now)
        boxGeometry = device.newGeometry(Geometry.SubType.TRIANGLE)
            .setVertexPosition(createArray(FLOAT32_VEC3, boxVertices))
            .setVertexColor(createArray(FLOAT32_VEC4, boxColors))
            .setPrimitiveIndex(createArray(UINT32_VEC3, boxIndices))
            .commit();
        System.out.println("Box geometry created: " + boxGeometry + ", vertices=" + boxVertices.length/3 + ", triangles=" + boxIndices.length/3);

        // Green matte material - just set the color parameter directly (no setColor("color") which sets a string)
        boxMaterial = device.newMaterial(org.codeberg.anari.api.Material.SubType.MATTE)
            .setFloat32Vec3("color", 0.0f, 1.0f, 0.0f)
            .commit();
        System.out.println("Box material created: " + boxMaterial);

        // Surface combining geometry and material
        boxSurface = device.newSurface()
            .setGeometry(boxGeometry)
            .setMaterial(boxMaterial)
            .commit();
        System.out.println("Box surface created: " + boxSurface);
    }

    /**
     * Helper to create and commit an ANARI array with set(false, ...) for immediate copy.
     */
    private Array1D createArray(DataType type, float[] data) throws AnariException, Throwable {
        int tupleSize = switch (type) {
            case FLOAT32_VEC3 -> 3;
            case FLOAT32_VEC4 -> 4;
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
        return device.newArray1D(type, data.length / tupleSize)
            .set(false, data)
            .commit();
    }

    private Array1D createArray(DataType type, int[] data) throws AnariException, Throwable {
        int tupleSize = 3; // UINT32_VEC3
        return device.newArray1D(type, data.length / tupleSize)
            .set(false, data)
            .commit();
    }

@Override
    protected void updateScene(TimerState timer) throws Throwable {
        // Put surface on world
        if (boxSurface != null) {
            MemorySegment surfacePtr = arena.allocate(ValueLayout.ADDRESS, 1);
            surfacePtr.setAtIndex(ValueLayout.ADDRESS, 0, boxSurface.getAddress(arena));
            Array1D arraySurfaces = device.newArray1D(surfacePtr, NULL, NULL, SURFACE, 1).commit();
            world.setSurface(arraySurfaces).commit();
            System.out.println("World surface set: " + arraySurfaces);
        }

        // ADD LIGHT - MATTE material needs lights
        Light light = device.newLight(Light.SubType.DIRECTIONAL).commit();
        MemorySegment lightPointers = arena.allocate(ValueLayout.ADDRESS, 1);
        lightPointers.setAtIndex(ValueLayout.ADDRESS, 0, light.getAddress(arena));
        Array1D arrayLights = device.newArray1D(lightPointers, NULL, NULL, LIGHT, 1).commit();
        world.setLight(arrayLights).commit();
        System.out.println("Light set");

        System.out.println("=== updateScene end ===");
    }

    @Override
    public void release(Device device) {
        try {
            if (boxSurface != null) boxSurface.release();
            if (boxMaterial != null) boxMaterial.release();
            if (boxGeometry != null) boxGeometry.release();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        super.release(device);
    }
}

import org.codeberg.anari.api.*;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.TimerState;

import static org.codeberg.anari.api.DataType.*;

// X3DJSAIL - Massive imports for full node coverage
import org.web3d.x3d.sai.Core.*;
import org.web3d.x3d.sai.Grouping.*;
import org.web3d.x3d.sai.Shape.*;
import org.web3d.x3d.sai.Geometry3D.*;
import org.web3d.x3d.sai.Rendering.*;
import org.web3d.x3d.sai.Lighting.*;
import org.web3d.x3d.sai.Navigation.*;
import org.web3d.x3d.sai.EnvironmentalEffects.*;

import java.util.ArrayList;
import java.util.List;

public class X3DAnariAdvancedHandler5 extends AbstractHandler {

    private final X3D x3dModel;
    private final List<Instance> anariInstances = new ArrayList<>();
    private final List<Light> anariLights = new ArrayList<>();
    
    // Fallback/Default Materials
    private Material defaultMaterial;

    public X3DAnariAdvancedHandler5(X3D x3dModel) {
        this.x3dModel = x3dModel;
    }

    @Override
    public void init(Device device) throws AnariException {
        System.out.println("Initializing Janari Advanced Translation...");

        // Create a default matte material in case X3D shapes lack Appearance
        defaultMaterial = new Material(device, "matte");
        defaultMaterial.setParameter("color", new float[]{0.8f, 0.8f, 0.8f});
        defaultMaterial.commit();

        if (x3dModel != null && x3dModel.getScene() != null) {
            traverseNodes(device, x3dModel.getScene().getChildren(), null);
        }

        // Apply Instances to World
        if (!anariInstances.isEmpty()) {
            Array1D instArray = new Array1D(device, anariInstances.toArray(new Instance[0]), INSTANCE);
            // Assuming the base AbstractHandler gives access to 'world'
            world.setParameter("instance", instArray);
        }

        // Apply Lights to World
        if (!anariLights.isEmpty()) {
            Array1D lightArray = new Array1D(device, anariLights.toArray(new Light[0]), LIGHT);
            world.setParameter("light", lightArray);
        }

        world.commit();
        System.out.println("Janari Scene Loaded: " + anariInstances.size() + " instances, " + anariLights.size() + " lights.");
    }

    /**
     * Recursively traverses ALL possible grouping and child nodes.
     */
    private void traverseNodes(Device device, X3DNode[] nodes, float[] parentTransform) throws AnariException {
        if (nodes == null) return;

        for (X3DNode node : nodes) {
            // 1. Grouping and Transformations
            if (node instanceof Transform) {
                Transform transform = (Transform) node;
                // Combine parent matrix with current (Math omitted for brevity)
                float[] currentMatrix = transform.getMatrix(); 
                traverseNodes(device, transform.getChildren(), currentMatrix);
            } 
            else if (node instanceof Group) {
                traverseNodes(device, ((Group) node).getChildren(), parentTransform);
            }
            else if (node instanceof Switch) {
                Switch sw = (Switch) node;
                int whichChoice = sw.getWhichChoice();
                if (whichChoice >= 0 && sw.getChildren() != null && whichChoice < sw.getChildren().length) {
                    traverseNodes(device, new X3DNode[]{sw.getChildren()[whichChoice]}, parentTransform);
                }
            }

            // 2. Shapes & Geometry
            else if (node instanceof Shape) {
                Instance instance = buildShapeInstance(device, (Shape) node, parentTransform);
                if (instance != null) anariInstances.add(instance);
            }

            // 3. Lighting Environments
            else if (node instanceof PointLight) {
                anariLights.add(buildPointLight(device, (PointLight) node, parentTransform));
            } 
            else if (node instanceof DirectionalLight) {
                anariLights.add(buildDirectionalLight(device, (DirectionalLight) node));
            }

            // 4. Cameras
            else if (node instanceof Viewpoint) {
                configureCamera(device, (Viewpoint) node);
            }
        }
    }

    /**
     * Translates X3D Shape into ANARI Surface -> Group -> Instance hierarchy.
     */
    private Instance buildShapeInstance(Device device, Shape shape, float[] transformMatrix) throws AnariException {
        Geometry geometry = null;
        X3DNode x3dGeom = shape.getGeometry();

        // ---- GEOMETRY MAPPING ----
        if (x3dGeom instanceof Box) {
            geometry = createAnariBox(device, (Box) x3dGeom);
        } else if (x3dGeom instanceof Sphere) {
            geometry = new Geometry(device, "sphere");
            geometry.setParameter("radius", ((Sphere) x3dGeom).getRadius());
            // Need a single position for a basic sphere at origin
            geometry.setParameter("vertex.position", new Array1D(device, new float[]{0,0,0}, FLOAT32_VEC3));
        } else if (x3dGeom instanceof Cylinder) {
            geometry = new Geometry(device, "cylinder");
            geometry.setParameter("radius", ((Cylinder) x3dGeom).getRadius());
            // Map top and bottom caps based on height
            float h = ((Cylinder) x3dGeom).getHeight() / 2f;
            geometry.setParameter("vertex.position", new Array1D(device, new float[]{0,-h,0, 0,h,0}, FLOAT32_VEC3));
        } else if (x3dGeom instanceof TriangleSet) {
            geometry = new Geometry(device, "triangle");
            Coordinate coord = (Coordinate) ((TriangleSet) x3dGeom).getCoord();
            geometry.setParameter("vertex.position", new Array1D(device, coord.getPoint(), FLOAT32_VEC3));
        } else if (x3dGeom instanceof IndexedFaceSet) {
            // Note: In a true production app, you MUST tessellate an X3D IndexedFaceSet into Triangles here.
            geometry = parseIndexedFaceSet(device, (IndexedFaceSet) x3dGeom);
        }

        if (geometry != null) geometry.commit(); else return null;

        // ---- MATERIAL MAPPING ----
        Material material = defaultMaterial;
        Appearance app = shape.getAppearance();
        if (app != null && app.getMaterial() != null) {
            org.web3d.x3d.sai.Shape.Material x3dMat = (org.web3d.x3d.sai.Shape.Material) app.getMaterial();
            material = new Material(device, "physicallyBased");
            material.setParameter("baseColor", x3dMat.getDiffuseColor());
            material.setParameter("metallic", 0.0f);
            material.setParameter("roughness", 1.0f - x3dMat.getShininess());
            material.setParameter("opacity", 1.0f - x3dMat.getTransparency());
            material.commit();
        }

        // Assemble ANARI Surface
        Surface surface = new Surface(device);
        surface.setParameter("geometry", geometry);
        surface.setParameter("material", material);
        surface.commit();

        Group group = new Group(device);
        group.setParameter("surface", new Array1D(device, new Surface[]{surface}, SURFACE));
        group.commit();

        Instance instance = new Instance(device, "transform");
        instance.setParameter("group", group);
        if (transformMatrix != null) {
            instance.setParameter("transform", transformMatrix);
        }
        instance.commit();

        return instance;
    }

    /**
     * Converts an X3D Box to an ANARI Triangle Geometry (since ANARI doesn't have a native 'Box' primitive).
     */
    private Geometry createAnariBox(Device device, Box box) throws AnariException {
        float[] size = box.getSize();
        float x = size[0] / 2f, y = size[1] / 2f, z = size[2] / 2f;

        // 8 vertices of a box
        float[] vertices = {
            -x,-y,-z,  x,-y,-z,  x, y,-z, -x, y,-z, // Back face
            -x,-y, z,  x,-y, z,  x, y, z, -x, y, z  // Front face
        };

        // 12 triangles (36 indices) to make a box
        int[] indices = {
            0,1,2, 2,3,0, // Back
            4,5,6, 6,7,4, // Front
            0,4,7, 7,3,0, // Left
            1,5,6, 6,2,1, // Right
            3,2,6, 6,7,3, // Top
            0,1,5, 5,4,0  // Bottom
        };

        Geometry geom = new Geometry(device, "triangle");
        geom.setParameter("vertex.position", new Array1D(device, vertices, FLOAT32_VEC3));
        geom.setParameter("primitive.index", new Array1D(device, indices, UINT32_VEC3));
        return geom;
    }

    /**
     * Placeholder for IndexedFaceSet -> Triangles conversion.
     */
    private Geometry parseIndexedFaceSet(Device device, IndexedFaceSet ifs) throws AnariException {
        // Retrieve ifs.getCoordIndex() and triangulate polygons separated by -1
        // Create an Array1D of the triangulated indices and return Geometry("triangle").
        return null;
    }

    // ---- LIGHTING ----

    private Light buildPointLight(Device device, PointLight pLight, float[] transformMatrix) throws AnariException {
        Light light = new Light(device, "point");
        light.setParameter("color", pLight.getColor());
        light.setParameter("intensity", pLight.getIntensity() * 100f); // Scale to ANARI units
        light.setParameter("position", pLight.getLocation());
        // Map radius -> falloff if desired
        light.commit();
        return light;
    }

    private Light buildDirectionalLight(Device device, DirectionalLight dLight) throws AnariException {
        Light light = new Light(device, "directional");
        light.setParameter("color", dLight.getColor());
        light.setParameter("intensity", dLight.getIntensity());
        light.setParameter("direction", dLight.getDirection());
        light.commit();
        return light;
    }

    // ---- CAMERA MAPPER ----
    private void configureCamera(Device device, Viewpoint vp) throws AnariException {
        // If your AbstractHandler gives you a 'camera' object, configure it here:
        camera.setParameter("position", vp.getPosition());
        // vp.getOrientation() is axis-angle [x, y, z, angle]. 
        // Need to convert axis-angle to a direction/up vector for ANARI.
        camera.setParameter("fieldOfView", vp.getFieldOfView());
        camera.commit();
    }

    @Override
    public void update(TimerState state) {
        // Implement X3D animations via TimeSensor interpolation updates here
    }

    @Override
    public void cleanup(Device device) {
        // Standard cleanup logic
        for (Instance i : anariInstances) i.release();
        for (Light l : anariLights) l.release();
        if (defaultMaterial != null) defaultMaterial.release();
    }
}

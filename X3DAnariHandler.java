// package org.codeberg.anari.example;

import static java.lang.foreign.MemorySegment.NULL;

import org.codeberg.anari.api.Array1D;
import static org.codeberg.anari.api.DataType.FLOAT32_VEC3;
import static org.codeberg.anari.api.DataType.FLOAT32_VEC4;
import static org.codeberg.anari.api.DataType.LIGHT;
import static org.codeberg.anari.api.DataType.SURFACE;
import static org.codeberg.anari.api.DataType.UINT32_VEC3;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.TimerState;

// X3DJSAIL Imports
import org.web3d.x3d.jsail.Core.X3D;
import org.web3d.x3d.jsail.Core.Scene;
import org.web3d.x3d.jsail.fields.SFRotation;
import org.web3d.x3d.jsail.Rendering.Coordinate;
import org.web3d.x3d.jsail.Geometry3D.IndexedFaceSet;
import org.web3d.x3d.jsail.Grouping.Transform;
import org.web3d.x3d.jsail.Shape.Appearance;
import org.web3d.x3d.jsail.Shape.Shape;

public class X3DAnariHandler extends AbstractHandler {

    private X3D x3dScene;
    private float[] baseVertices;
    private int[] anariIndices;
    private float[] anariColors;

    public X3DAnariHandler(X3D scenegraph) {
        super();
        initX3DScene(scenegraph);
    }

    /**
     * Initializes the X3DJSAIL Scene structure once.
     */
    private void initX3DScene(X3D scenegraph) {

        org.web3d.x3d.jsail.Shape.Material material = null;
        IndexedFaceSet ifs = null;
	Coordinate coord = null;

	x3dScene = scenegraph;
        x3dScene = new X3D()
        	.setScene(new Scene()
        		.addChild(new Transform()
				.setDEF("AnimatedTransform")
        			.addChild(new Shape()
					.setGeometry(ifs = new IndexedFaceSet()
        					.setCoord(coord = new Coordinate().setPoint(new float[]{
							0.0f,  1.0f,  0.0f,
						       -1.0f, -1.0f,  1.0f,
							1.0f, -1.0f,  1.0f,
							0.0f, -1.0f, -1.0f
						}))
						.setCoordIndex(new int[]{
							0, 1, 2, -1,
							0, 2, 3, -1,
							0, 3, 1, -1,
							1, 3, 2, -1
						}))
					.setAppearance(new Appearance()
        					.setMaterial(material = new org.web3d.x3d.jsail.Shape.Material()
							.setDiffuseColor(0.2f, 0.8f, 0.2f)

						)
					)
				)
			)
		);

        // 4. Pre-process X3D data into ANARI-friendly arrays
        baseVertices = coord.getPoint();
        anariIndices = processX3dIndices(ifs.getCoordIndex());
        anariColors = processX3dColors(material.getDiffuseColor(), baseVertices.length / 3);
    }

    @Override
    protected void updateScene(TimerState timer) throws Throwable {
        // 1. Update X3D Scene (Rotate around Y-axis over time)
        // Using current time millis as an arbitrary time source for the angle
        float angle = (System.currentTimeMillis() % 10000) / 10000.0f * (float) (2 * Math.PI);
	Transform animatedTransform =  (Transform) x3dScene.findNodeByDEF("AnimatedTransform");
	System.err.println("AnimatedTransform " +  animatedTransform);
        animatedTransform.setRotation(new SFRotation(0.0f, 1.0f, 0.0f, angle));

        // 2. Apply Transform to vertices
        float[] currentVertices = applyTransform(baseVertices, angle);

        // 3. Push arrays to ANARI using FFM API mappings
        final Array1D arrayPos = device.newArray1D(FLOAT32_VEC3, currentVertices.length / 3)
                .set(true, currentVertices)
                .commit();

        final Array1D arrayColorsBuffer = device.newArray1D(FLOAT32_VEC4, anariColors.length / 4)
                .set(true, anariColors)
                .commit();

        final Array1D arrayIndex = device.newArray1D(UINT32_VEC3, anariIndices.length / 3)
                .set(true, anariIndices)
                .commit();

        // 4. Create ANARI Geometry
        final Geometry.Triangle mesh = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(arrayPos)
                .setVertexColor(arrayColorsBuffer)
                .setPrimitiveIndex(arrayIndex)
                .commit();
        
        arrayPos.release();
        arrayColorsBuffer.release();
        arrayIndex.release();

        // 5. Create ANARI Material
        final Material mat = device.newMaterial(Material.SubType.MATTE)
                .setColor("color") // Binds to the vertex colors we provided
                .commit();

        // 6. Create ANARI Surface
        final Surface surface = device.newSurface()
                .setGeometry(mesh)
                .setMaterial(mat)
                .commit();
        
        mesh.release();
        mat.release();

        // 7. Add Surface to World via Memory Segments (From anari-java example)
        final Array1D arraySurfaces = device.newArray1D(surface.getAddress(arena), NULL, NULL, SURFACE, 1)
                .commit();

        // 8. Create Light
        final Light light = device.newLight(Light.SubType.DIRECTIONAL)
                .commit();
        final Array1D arrayLights = device.newArray1D(light.getAddress(arena), NULL, NULL, LIGHT, 1)
                .commit();

        // 9. Update the World
        world.setSurface(arraySurfaces)
             .setLight(arrayLights)
             .commit();

        surface.release();
        arraySurfaces.release();
        light.release();
        arrayLights.release();
    }

    /**
     * Strips the X3D "-1" delimiter to create flat ANARI triangle indices.
     */
    private int[] processX3dIndices(int[] x3dIndices) {
        int validCount = 0;
        for (int i : x3dIndices) if (i != -1) validCount++;
        int[] indices = new int[validCount];
        int idx = 0;
        for (int i : x3dIndices) {
            if (i != -1) indices[idx++] = i;
        }
        return indices;
    }

    /**
     * Maps a single X3D diffuse color to a FLOAT32_VEC4 array for every vertex.
     */
    private float[] processX3dColors(float[] diffuseColor, int vertexCount) {
        float[] colors = new float[vertexCount * 4];
        for (int i = 0; i < colors.length; i += 4) {
            colors[i]     = diffuseColor[0]; // R
            colors[i + 1] = diffuseColor[1]; // G
            colors[i + 2] = diffuseColor[2]; // B
            colors[i + 3] = 1.0f;            // A
        }
        return colors;
    }

    /**
     * Utility method: Rotates vertices manually to mirror the X3D `Transform` updates.
     */
    private float[] applyTransform(float[] vertices, float angle) {
        float[] transformed = new float[vertices.length];
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Standard Y-axis rotation matrix
        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];
            
            // Push it back by 4 units in the Z direction so the camera can see it
            transformed[i]     = (x * cos + z * sin);
            transformed[i + 1] = y;
            transformed[i + 2] = (-x * sin + z * cos) + 4.0f;
        }
        return transformed;
    }
}

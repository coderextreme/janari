/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codeberg.anari.api;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import java.util.List;
import org.codeberg.anari.Anari;
import static org.codeberg.anari.api.AnariOO.isNull;

/**
 * The ANARI device is responsible for coordinating the sharing of execution
 * resources with the calling application, such as a CPU thread pool or GPU kernel queues.
 *
 * {@snippet lang=c :
 * typedef void* ANARIDevice;
 * }
 *
 * @author Johann Sorel
 */
public final class Device extends Object<Device> {

    final Library library;

    public Device(Library library, MemorySegment pointer) {
        super(null, DataType.DEVICE, null, pointer);
        this.library = library;
    }

    /**
     * Unique device version number, guaranteed to increase between versions,
     * queried as a mandatory property.
     */
    public int getVersion() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            getProperty(Properties.Device.QUERY_VERSION, DataType.INT32, mem, mem.byteSize());
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Semantic device version major value (major.minor.patch), queried as an
     * optional property. Returns null if not available.
     */
    public Integer getVersionMajor() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            final int res = getProperty(Properties.Device.QUERY_VERSION_MAJOR, DataType.INT32, mem, mem.byteSize(), WaitMask.NO_WAIT);
            if (res == 0) return null;
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Semantic device version minor value (major.minor.patch), queried as an
     * optional property. Returns null if not available.
     */
    public Integer getVersionMinor() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            final int res = getProperty(Properties.Device.QUERY_VERSION_MINOR, DataType.INT32, mem, mem.byteSize(), WaitMask.NO_WAIT);
            if (res == 0) return null;
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Semantic device version patch value (major.minor.patch), queried as an
     * optional property. Returns null if not available.
     */
    public Integer getVersionPatch() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            final int res = getProperty(Properties.Device.QUERY_VERSION_PATCH, DataType.INT32, mem, mem.byteSize(), WaitMask.NO_WAIT);
            if (res == 0) return null;
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Human readable device name/title, queried as an optional property.
     */
    public String getVersionName() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final long size = getPropertySize(Properties.Device.QUERY_VERSION_NAME);
            final MemorySegment mem = tempArena.allocate(size);
            getProperty(Properties.Device.QUERY_VERSION_NAME, DataType.STRING, mem, size);
            return mem.getString(0);
        }
    }

    /**
     * Targeted ANARI specification version major value (major.minor), queried
     * as a mandatory property.
     */
    public int getAnariVersionMajor() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            getProperty(Properties.Device.QUERY_ANARIVERSION_MAJOR, DataType.INT32, mem, mem.byteSize());
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Targeted ANARI specification version minor value (major.minor), queried
     * as a mandatory property.
     */
    public int getAnariVersionMinor() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_INT);
            getProperty(Properties.Device.QUERY_ANARIVERSION_MINOR, DataType.INT32, mem, mem.byteSize());
            return mem.get(JAVA_INT, 0);
        }
    }

    /**
     * Largest supported index into vertex arrays in geometries, queried as a
     * mandatory property.
     */
    public long getGeometryMaxIndex() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_LONG);
            getProperty(Properties.Device.QUERY_GEOMETRYMAXINDEX, DataType.UINT64, mem, mem.byteSize());
            return mem.get(JAVA_LONG, 0);
        }
    }

    /**
     * List of extensions supported by this device instance, queried as a
     * mandatory property.
     */
    public List<String> getExtensions() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(ADDRESS);
            getProperty(Properties.Device.QUERY_EXTENSION, DataType.STRING_LIST, mem, mem.byteSize());
            return AnariOO.fromNullTerminatedStrings(mem.get(ADDRESS, 0));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Create a new 1D array of object handles from a list of ANARI objects.
     */
    public Array1D newArray1D(List<? extends Object> list, DataType dataType) throws AnariException, Throwable {
        final long[] pointers = new long[list.size()];
        for (int i = 0; i < pointers.length; i++) pointers[i] = list.get(i).getPointer().address();
        Array1D array = newArray1D(dataType, list.size());
        array.set(true, pointers);
        return array;
    }

    /**
     * Create a new 1D array sharing application-owned memory. See
     * {@link Anari#newArray1D(MemorySegment, MemorySegment, MemorySegment, MemorySegment, int, long)}.
     */
    public Array1D newArray1D(MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, DataType dataType, long numElements1) throws AnariException, Throwable {
        try {
            return new Array1D(this,
                    library.anari.newArray1D(pointer, appMemory, deleter, userData, dataType.code, numElements1),
                    dataType,
                    numElements1
            );
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new 1D array whose backing memory is managed by the device
     * (write-only via mapping).
     */
    public Array1D newArray1D(DataType dataType, long numElements1) throws AnariException, Throwable {
        try {
            return new Array1D(this,
                    library.anari.newArray1D(pointer, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, dataType.code, numElements1),
                    dataType,
                    numElements1
            );
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new 2D array sharing application-owned memory. See
     * {@link Anari#newArray2D(MemorySegment, MemorySegment, MemorySegment, MemorySegment, int, long, long)}.
     */
    public Array2D newArray2D(MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, DataType dataType, long numElements1, long numElements2) throws AnariException, Throwable {
        try {
            return new Array2D(this,
                    library.anari.newArray2D(pointer, appMemory, deleter, userData, dataType.code, numElements1, numElements2),
                    dataType,
                    numElements1,
                    numElements2
            );
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new 3D array sharing application-owned memory. See
     * {@link Anari#newArray3D(MemorySegment, MemorySegment, MemorySegment, MemorySegment, int, long, long, long)}.
     */
    public Array3D newArray3D(MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, DataType dataType, long numElements1, long numElements2, long numElements3) throws AnariException, Throwable {
        try {
            return new Array3D(this,
                    library.anari.newArray3D(pointer, appMemory, deleter, userData, dataType.code, numElements1, numElements2, numElements3),
                    dataType,
                    numElements1,
                    numElements2,
                    numElements3
            );
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new light of the given subtype (e.g. "directional", "point", "spot").
     */
    public Light newLight(String subtype) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Light(
                    device,
                    subtype,
                    library.anari.newLight(pointer, tempArena.allocateFrom(subtype)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new light using a typed {@link Light.SubType} registry entry.
     */
    public <T extends Light> T newLight(Light.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newLight(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new camera of the given subtype (e.g. "perspective", "orthographic", "omnidirectional").
     */
    public Camera newCamera(String subtype) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Camera(
                    device,
                    subtype,
                    library.anari.newCamera(pointer, tempArena.allocateFrom(subtype)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new camera using a typed {@link Camera.SubType} registry entry.
     */
    public <T extends Camera> T newCamera(Camera.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newCamera(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new geometry of the given subtype (e.g. "triangle", "sphere", "isosurface").
     */
    public Geometry newGeometry(String type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Geometry(
                    device,
                    type,
                    library.anari.newGeometry(pointer, tempArena.allocateFrom(type)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new geometry using a typed {@link Geometry.SubType} registry entry.
     */
    public <T extends Geometry> T newGeometry(Geometry.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newGeometry(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new spatial field of the given subtype (e.g. "structuredRegular", "nanovdb", "unstructured").
     */
    public SpatialField newSpatialField(String subType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new SpatialField(
                    device,
                    subType,
                    library.anari.newSpatialField(pointer, tempArena.allocateFrom(subType)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new spatial field using a typed {@link SpatialField.SubType} registry entry.
     */
    public <T extends SpatialField> T newSpatialField(SpatialField.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newSpatialField(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new volume of the given subtype (e.g. "transferFunction1D").
     */
    public Volume newVolume(String subtype) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Volume(
                    device,
                    subtype,
                    library.anari.newVolume(pointer, tempArena.allocateFrom(subtype)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new volume using a typed {@link Volume.SubType} registry entry.
     */
    public <T extends Volume> T newVolume(Volume.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newVolume(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new surface, combining a geometry and a material.
     */
    public Surface newSurface() throws Throwable {
        try {
            return new Surface(
                    device,
                    library.anari.newSurface(pointer));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new material of the given subtype (e.g. "matte", "physicallyBased").
     */
    public Material newMaterial(String subtype) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Material(
                    device,
                    subtype,
                    library.anari.newMaterial(pointer, tempArena.allocateFrom(subtype)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new material using a typed {@link Material.SubType} registry entry.
     */
    public <T extends Material> T newMaterial(Material.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newMaterial(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new sampler of the given subtype (e.g. "image1D", "image2D", "image3D", "primitive", "transform").
     */
    public Sampler newSampler(String subType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Sampler(
                    device,
                    subType,
                    library.anari.newSampler(pointer, tempArena.allocateFrom(subType)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new sampler using a typed {@link Sampler.SubType} registry entry.
     */
    public <T extends Sampler> T newSampler(Sampler.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newSampler(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new group, a collection of surfaces, volumes, and lights sharing
     * a common local-space coordinate system.
     */
    public Group newGroup() throws Throwable {
        try {
            return new Group(
                    device,
                    library.anari.newGroup(pointer));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new instance of the given subtype (e.g. "transform", "motionTransform", "motionScaleRotationTranslation").
     */
    public Instance newInstance(String subType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Instance(
                    device,
                    subType,
                    library.anari.newInstance(pointer, tempArena.allocateFrom(subType)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new instance using a typed {@link Instance.SubType} registry entry.
     */
    public <T extends Instance> T newInstance(Instance.SubType<T> type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return type.create(
                    device,
                    library.anari.newInstance(pointer, tempArena.allocateFrom(type.name)));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new world, the top-level container of scene data.
     */
    public World newWorld() throws Throwable {
        try {
            return new World(
                    device,
                    library.anari.newWorld(pointer));
        } finally {
            checkForException();
        }
    }

    /**
     * Create a new frame, holding the objects necessary to render and the
     * resulting rendered image.
     */
    public Frame newFrame() throws Throwable {
        try {
            return new Frame(
                    device,
                    library.anari.newFrame(pointer));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Create a new renderer of the given subtype. Passing "default" always
     * results in a usable renderer with meaningful defaults for all parameters.
     */
    public Renderer newRenderer(String type) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Renderer(
                    device,
                    library.anari.newRenderer(pointer, tempArena.allocateFrom(type)));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Create a new extension-defined object of the given type and subtype.
     * Consult the extension documentation for supported type and subtype values.
     */
    public Object newObject(String objType, String subtype) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Object(device,
                    DataType.valueOf(objType),
                    subtype,
                    library.anari.newObject(pointer, tempArena.allocateFrom(objType), tempArena.allocateFrom(subtype)));
        } finally {
            checkForException();
        }
    }

    /**
     * Enumerate the subtypes of the given object type supported by this device.
     *
     * @return null if the object type can not have subtypes, empty if it has none.
     */
    public List<String> getObjectSubtypes(DataType objectType) throws AnariException, Throwable {
        try {
            final MemorySegment ptr = library.anari.getObjectSubtypes(pointer, objectType.code);
            if (isNull(ptr)) return null;
            return AnariOO.fromNullTerminatedStrings(ptr);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Query introspection information (e.g. "description" or "parameter") about
     * an object (sub)type. Pass null or an empty string for objectSubtype to
     * query an object type directly that does not have subtypes.
     */
    public MemorySegment getObjectInfo(DataType objectType, String objectSubtype, String infoName, DataType infoType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return library.anari.getObjectInfo(
                    pointer,
                    objectType.code,
                    (objectSubtype == null) ? MemorySegment.NULL : tempArena.allocateFrom(objectSubtype),
                    tempArena.allocateFrom(infoName),
                    infoType.code);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Query introspection information (e.g. "description", "minimum", "maximum",
     * "default", "required") about a specific parameter of an object (sub)type.
     */
    public MemorySegment getParameterInfo(DataType objectType, String objectSubtype, String parameterName, DataType parameterType, String infoName, DataType infoType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return library.anari.getParameterInfo(
                    pointer,
                    objectType.code,
                    (objectSubtype == null) ? MemorySegment.NULL : tempArena.allocateFrom(objectSubtype),
                    tempArena.allocateFrom(parameterName),
                    parameterType.code,
                    tempArena.allocateFrom(infoName),
                    infoType.code);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Rethrow as {@link AnariException} any error reported by the status
     * callback since the last check, for this device.
     */
    public void checkForException() throws AnariException {
        library.exceptionCatcher.checkException(pointer.address());
    }
}

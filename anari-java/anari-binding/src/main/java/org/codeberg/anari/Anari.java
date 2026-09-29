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
package org.codeberg.anari;

import java.io.File;
import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Linker;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.FunctionDescriptor.of;
import static java.lang.foreign.FunctionDescriptor.ofVoid;


/**
 * ANARI binding.
 *
 * @author Johann Sorel
 *
 * @see <a href="https://github.com/KhronosGroup/ANARI-SDK">ANARI SDK</a>
 */
public final class Anari implements AutoCloseable {

    private static final String LIBRARY_NAME = "anari";

    /**
     * The global instance, created when first needed.
     * This field shall be read and updated in a synchronized block.
     */
    private static Anari global;
    /**
     * Whether an error occurred during initialization of {@link #global}.
     * Shall be read and updated in the same synchronization block as {@link #global}.
     */
    private static String globalStatus;

    private final MethodHandle loadLibrary;
    private final MethodHandle unloadLibrary;
    private final MethodHandle loadModule;
    private final MethodHandle unloadModule;
    private final MethodHandle newDevice;
    private final MethodHandle newInitializedDevice;
    private final MethodHandle newArray1D;
    private final MethodHandle newArray2D;
    private final MethodHandle newArray3D;
    private final MethodHandle mapArray;
    private final MethodHandle unmapArray;
    private final MethodHandle newLight;
    private final MethodHandle newCamera;
    private final MethodHandle newGeometry;
    private final MethodHandle newSpatialField;
    private final MethodHandle newVolume;
    private final MethodHandle newSurface;
    private final MethodHandle newMaterial;
    private final MethodHandle newSampler;
    private final MethodHandle newGroup;
    private final MethodHandle newInstance;
    private final MethodHandle newWorld;
    private final MethodHandle newObject;
    private final MethodHandle setParameter;
    private final MethodHandle unsetParameter;
    private final MethodHandle unsetAllParameters;
    private final MethodHandle mapParameterArray1D;
    private final MethodHandle mapParameterArray2D;
    private final MethodHandle mapParameterArray3D;
    private final MethodHandle unmapParameterArray;
    private final MethodHandle commitParameters;
    private final MethodHandle release;
    private final MethodHandle retain;
    private final MethodHandle getDeviceSubtypes;
    private final MethodHandle getDeviceExtensions;
    private final MethodHandle getObjectSubtypes;
    private final MethodHandle getObjectInfo;
    private final MethodHandle getParameterInfo;
    private final MethodHandle getProperty;
    private final MethodHandle newFrame;
    private final MethodHandle mapFrame;
    private final MethodHandle unmapFrame;
    private final MethodHandle newRenderer;
    private final MethodHandle renderFrame;
    private final MethodHandle frameReady;
    private final MethodHandle discardFrame;

    /**
     * typedef void (*ANARIMemoryDeleter)(const void* userPtr, const void* appMemory)
     */
    public final FunctionDescriptor memoryDeleter;
    /**
     * typedef void (*ANARIStatusCallback)(const void* userPtr, ANARIDevice device, ANARIObject source, ANARIDataType sourceType, ANARIStatusSeverity severity, ANARIStatusCode code, const char* message);
     */
    public final FunctionDescriptor statusCallback;
    /**
     * typedef void (*ANARIFrameCompletionCallback)(const void* userPtr, ANARIDevice device, ANARIFrame frame);
     */
    public final FunctionDescriptor frameCompletionCallback;

    private final Arena arena;
    /**
     * The lookup for retrieving the address of a symbol in the native library.
     */
    private final SymbolLookup symbols;

    /**
     * The linker to use for fetching method handles from the {@linkplain #symbols}.
     * In current version, this is always {@link Linker#nativeLinker()}.
     */
    private final Linker linker;

    /**
     * Creates the handles for all <abbr>ANARI</abbr> functions which will be needed.
     *
     * @param  loader  the object used for loading the library.
     * @throws NoSuchElementException if an <abbr>ANARI</abbr> function has not been found in the library.
     */
    private Anari(Arena arena, SymbolLookup symbols) {
        this.arena = arena;
        this.symbols = symbols;
        this.linker = Linker.nativeLinker();

        loadLibrary         = lookup("anariLoadLibrary",         of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        unloadLibrary       = lookup("anariUnloadLibrary",       ofVoid(ADDRESS));
        loadModule          = lookup("anariLoadModule",          ofVoid(ADDRESS, ADDRESS));
        unloadModule        = lookup("anariUnloadModule",        ofVoid(ADDRESS, ADDRESS));
        newDevice           = lookup("anariNewDevice",           of(ADDRESS, ADDRESS, ADDRESS));
        newInitializedDevice = lookup("anariNewInitializedDevice", of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        newArray1D          = lookup("anariNewArray1D",          of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG));
        newArray2D          = lookup("anariNewArray2D",          of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG));
        newArray3D          = lookup("anariNewArray3D",          of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_LONG));
        mapArray            = lookup("anariMapArray",            of(ADDRESS, ADDRESS, ADDRESS));
        unmapArray          = lookup("anariUnmapArray",          ofVoid(ADDRESS, ADDRESS));
        newLight            = lookup("anariNewLight",            of(ADDRESS, ADDRESS, ADDRESS));
        newCamera           = lookup("anariNewCamera",           of(ADDRESS, ADDRESS, ADDRESS));
        newGeometry         = lookup("anariNewGeometry",         of(ADDRESS, ADDRESS, ADDRESS));
        newSpatialField     = lookup("anariNewSpatialField",     of(ADDRESS, ADDRESS, ADDRESS));
        newVolume           = lookup("anariNewVolume",           of(ADDRESS, ADDRESS, ADDRESS));
        newSurface          = lookup("anariNewSurface",          of(ADDRESS, ADDRESS));
        newMaterial         = lookup("anariNewMaterial",         of(ADDRESS, ADDRESS, ADDRESS));
        newSampler          = lookup("anariNewSampler",          of(ADDRESS, ADDRESS, ADDRESS));
        newGroup            = lookup("anariNewGroup",            of(ADDRESS, ADDRESS));
        newInstance         = lookup("anariNewInstance",         of(ADDRESS, ADDRESS, ADDRESS));
        newWorld            = lookup("anariNewWorld",            of(ADDRESS, ADDRESS));
        newObject           = lookup("anariNewObject",           of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        setParameter        = lookup("anariSetParameter",        ofVoid(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        unsetParameter      = lookup("anariUnsetParameter",      ofVoid(ADDRESS, ADDRESS, ADDRESS));
        unsetAllParameters  = lookup("anariUnsetAllParameters",  ofVoid(ADDRESS, ADDRESS));
        mapParameterArray1D = lookup("anariMapParameterArray1D", of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG, ADDRESS));
        mapParameterArray2D = lookup("anariMapParameterArray2D", of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS));
        mapParameterArray3D = lookup("anariMapParameterArray3D", of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_LONG, ADDRESS));
        unmapParameterArray = lookup("anariUnmapParameterArray", ofVoid(ADDRESS, ADDRESS, ADDRESS));
        commitParameters    = lookup("anariCommitParameters",    ofVoid(ADDRESS, ADDRESS));
        release             = lookup("anariRelease",             ofVoid(ADDRESS, ADDRESS));
        retain              = lookup("anariRetain",              ofVoid(ADDRESS, ADDRESS));
        getDeviceSubtypes   = lookup("anariGetDeviceSubtypes",   of(ADDRESS, ADDRESS));
        getDeviceExtensions = lookup("anariGetDeviceExtensions", of(ADDRESS, ADDRESS, ADDRESS));
        getObjectSubtypes   = lookup("anariGetObjectSubtypes",   of(ADDRESS, ADDRESS, JAVA_INT));
        getObjectInfo       = lookup("anariGetObjectInfo",       of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
        getParameterInfo    = lookup("anariGetParameterInfo",    of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        getProperty         = lookup("anariGetProperty",         of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT));
        newFrame            = lookup("anariNewFrame",            of(ADDRESS, ADDRESS));
        mapFrame            = lookup("anariMapFrame",            of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        unmapFrame          = lookup("anariUnmapFrame",          ofVoid(ADDRESS, ADDRESS, ADDRESS));
        newRenderer         = lookup("anariNewRenderer",         of(ADDRESS, ADDRESS, ADDRESS));
        renderFrame         = lookup("anariRenderFrame",         ofVoid(ADDRESS, ADDRESS));
        frameReady          = lookup("anariFrameReady",          of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
        discardFrame        = lookup("anariDiscardFrame",        ofVoid(ADDRESS, ADDRESS));

        memoryDeleter           = ofVoid(ADDRESS, ADDRESS);
        statusCallback          = ofVoid(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
        frameCompletionCallback = ofVoid(ADDRESS, ADDRESS, ADDRESS);

    }

    /**
     * Search for a native function.
     *
     * @param name ANARI C function name
     * @param desc C function arguments and return description
     * @return method handle for the ANARI function.
     * @throws IllegalArgumentException if function has not been found
     */
    private MethodHandle lookup(final String name, final FunctionDescriptor desc) {
        return symbols.find(name).map((method) -> linker.downcallHandle(method, desc)).orElseThrow();
    }

    /**
     * Load a library.
     *
     * This will look for a shared library named anari_library_[name], open it,
     * and look for entry points for (implementation defined) initialization.
     * The status callback passed is used as the default value for the statusCallback
     * parameter on devices created from the returned library object. Similarly,
     * the user pointer passed is used as the default value for the statusCallbackUserData device parameter.
     *
     * {@snippet lang=c :
     * ANARILibrary anariLoadLibrary(const char* name, ANARIStatusCallback statusCallback ANARI_DEFAULT_VAL(0), const void* statusCallbackUserData ANARI_DEFAULT_VAL(0));
     * }
     */
    public MemorySegment loadLibrary(MemorySegment name, MemorySegment callback, MemorySegment statusCallbackUserData) throws Throwable {
        return (MemorySegment) loadLibrary.invokeExact(name, callback, statusCallbackUserData);
    }

    /**
     * Unload a library (where library resource cleanup occurs).
     *
     * {@snippet lang=c :
     * void anariUnloadLibrary(ANARILibrary module);
     * }
     */
    public void unloadLibrary(MemorySegment library) throws Throwable {
        unloadLibrary.invokeExact(library);
    }

    /**
     * {@snippet lang=c :
     * void anariLoadModule(ANARILibrary library, const char* name);
     * }
     */
    public void loadModule(MemorySegment library, MemorySegment name) throws Throwable {
        loadModule.invokeExact(library, name);
    }

    /**
     * {@snippet lang=c :
     * void anariUnloadModule(ANARILibrary library, const char* name);
     * }
     */
    public void unloadModule(MemorySegment library, MemorySegment name) throws Throwable {
        unloadModule.invokeExact(library, name);
    }

    /**
     * Create a new device.
     *
     * {@snippet lang=c :
     * ANARIDevice anariNewDevice(ANARILibrary library, const char* type ANARI_DEFAULT_VAL("default"));
     * }
     */
    public MemorySegment newDevice(MemorySegment library, MemorySegment name) throws Throwable {
        return (MemorySegment) newDevice.invokeExact(library, name);
    }

    /**
     * Create a new device, passing immutable initializer parameters which can
     * only be assigned once, at device-creation time.
     *
     * {@snippet lang=c :
     * ANARIDevice anariNewInitializedDevice(ANARILibrary library, const char* type, ANARIParameterValue* initializers);
     * }
     */
    public MemorySegment newInitializedDevice(MemorySegment library, MemorySegment name, MemorySegment initializers) throws Throwable {
        return (MemorySegment) newInitializedDevice.invokeExact(library, name, initializers);
    }

    /**
     * To create a data array.
     *
     * The number of elements numElementsN must be positive (there cannot be an empty array object).
     * In each creation function a deleter can be passed in (with an associated pointer to any needed application data or state),
     * which ANARI will use to free the original pointer passed during construction. If the application passes NULL, ANARI will fully rely on the application to free the memory.
     *
     * When appMemory is not NULL, then the array is considered to be a shared array, where both the application and device observe the same memory.
     * Passing NULL in appMemory creates a managed array. The backing memory of the array is managed by the device and is only writable via mapping.
     *
     * Applications are permitted to release ANARIArray objects even if the device still contains internal references to it.
     * When releasing a shared array object ANARI relinquishes shared ownership of the memory, which may result in the creation of internal copies.
     * When a shared array is created with a deleter callback, it is implementation defined when an implementation frees host memory after
     * the array object has been released. Deleter callbacks are never called for managed arrays.
     *
     * Applications are only permitted to write to the memory visible to ANARIArray objects if all array objects involved are mapped.
     * Mapping a shared array object indicates that the device should not execute any rendering operations (or internal state updates,
     * such as building acceleration structures) of any parent objects to the mapped array object which is directly referencing mapped memory.
     *
     * {@snippet lang=c :
     * ANARIArray1D anariNewArray1D(ANARIDevice device, const void* appMemory, ANARIMemoryDeleter deleter, const void* userData, ANARIDataType dataType, uint64_t numElements1);
     * }
     */
    public MemorySegment newArray1D(MemorySegment device, MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, int dataType, long numElements1) throws Throwable {
        return (MemorySegment) newArray1D.invokeExact(device, appMemory, deleter, userData, dataType, numElements1);
    }

    /**
     * To create a data array.
     *
     * The number of elements numElementsN must be positive (there cannot be an empty array object).
     * In each creation function a deleter can be passed in (with an associated pointer to any needed application data or state),
     * which ANARI will use to free the original pointer passed during construction. If the application passes NULL, ANARI will fully rely on the application to free the memory.
     *
     * When appMemory is not NULL, then the array is considered to be a shared array, where both the application and device observe the same memory.
     * Passing NULL in appMemory creates a managed array. The backing memory of the array is managed by the device and is only writable via mapping.
     *
     * Applications are permitted to release ANARIArray objects even if the device still contains internal references to it.
     * When releasing a shared array object ANARI relinquishes shared ownership of the memory, which may result in the creation of internal copies.
     * When a shared array is created with a deleter callback, it is implementation defined when an implementation frees host memory after
     * the array object has been released. Deleter callbacks are never called for managed arrays.
     *
     * Applications are only permitted to write to the memory visible to ANARIArray objects if all array objects involved are mapped.
     * Mapping a shared array object indicates that the device should not execute any rendering operations (or internal state updates,
     * such as building acceleration structures) of any parent objects to the mapped array object which is directly referencing mapped memory.
     *
     * {@snippet lang=c :
     * ANARIArray2D anariNewArray2D(ANARIDevice device, const void* appMemory, ANARIMemoryDeleter deleter, const void* userData, ANARIDataType dataType, uint64_t numElements1, uint64_t numElements2);
     * }
     */
    public MemorySegment newArray2D(MemorySegment device, MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, int dataType, long numElements1, long numElements2) throws Throwable {
        return (MemorySegment) newArray2D.invokeExact(device, appMemory, deleter, userData, dataType, numElements1, numElements2);
    }

    /**
     * To create a data array.
     *
     * The number of elements numElementsN must be positive (there cannot be an empty array object).
     * In each creation function a deleter can be passed in (with an associated pointer to any needed application data or state),
     * which ANARI will use to free the original pointer passed during construction. If the application passes NULL, ANARI will fully rely on the application to free the memory.
     *
     * When appMemory is not NULL, then the array is considered to be a shared array, where both the application and device observe the same memory.
     * Passing NULL in appMemory creates a managed array. The backing memory of the array is managed by the device and is only writable via mapping.
     *
     * Applications are permitted to release ANARIArray objects even if the device still contains internal references to it.
     * When releasing a shared array object ANARI relinquishes shared ownership of the memory, which may result in the creation of internal copies.
     * When a shared array is created with a deleter callback, it is implementation defined when an implementation frees host memory after
     * the array object has been released. Deleter callbacks are never called for managed arrays.
     *
     * Applications are only permitted to write to the memory visible to ANARIArray objects if all array objects involved are mapped.
     * Mapping a shared array object indicates that the device should not execute any rendering operations (or internal state updates,
     * such as building acceleration structures) of any parent objects to the mapped array object which is directly referencing mapped memory.
     *
     * {@snippet lang=c :
     * ANARIArray3D anariNewArray3D(ANARIDevice device, const void* appMemory, ANARIMemoryDeleter deleter, const void* userData, ANARIDataType dataType, uint64_t numElements1, uint64_t numElements2, uint64_t numElements3);
     * }
     */
    public MemorySegment newArray3D(MemorySegment device, MemorySegment appMemory, MemorySegment deleter, MemorySegment userData, int dataType, long numElements1, long numElements2, long numElements3) throws Throwable {
        return (MemorySegment) newArray3D.invokeExact(device, appMemory, deleter, userData, dataType, numElements1, numElements2, numElements3);
    }

    /**
     * Map array object.
     *
     * Mapping a shared array will always result in the same address originally
     * used when constructing the array object. Mapping a managed array may return
     * a different pointer each time it is mapped. The contents of memory mapped
     * from managed arrays is undefined until written to and the entire mapped range
     * must be specified before unmapping to avoid populating the array with undefined values.
     *
     * ANARIArray objects containing object handles increase the ref count of all
     * objects in the array. Reference counts are updated on creation of the array object and when unmapping the array.
     *
     * {@snippet lang=c :
     * void* anariMapArray(ANARIDevice device, ANARIArray array);
     * }
     */
    public MemorySegment mapArray(MemorySegment device, MemorySegment array) throws Throwable {
        return (MemorySegment) mapArray.invokeExact(device, array);
    }

    /**
     * Unmap array object.
     *
     * {@snippet lang=c :
     * void anariUnmapArray(ANARIDevice device, ANARIArray array);
     * }
     */
    public void unmapArray(MemorySegment device, MemorySegment array) throws Throwable {
        unmapArray.invokeExact(device, array);
    }

    /**
     * {@snippet lang=c :
     * ANARILight anariNewLight(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newLight(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newLight.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARICamera anariNewCamera(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newCamera(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newCamera.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARIGeometry anariNewGeometry(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newGeometry(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newGeometry.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARISpatialField anariNewSpatialField(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newSpatialField(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newSpatialField.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARIVolume anariNewVolume(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newVolume(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newVolume.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARISurface anariNewSurface(ANARIDevice device);
     * }
     */
    public MemorySegment newSurface(MemorySegment device) throws Throwable {
        return (MemorySegment) newSurface.invokeExact(device);
    }

    /**
     * {@snippet lang=c :
     * ANARIMaterial anariNewMaterial(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newMaterial(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newMaterial.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARISampler anariNewSampler(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newSampler(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newSampler.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARIGroup anariNewGroup(ANARIDevice device);
     * }
     */
    public MemorySegment newGroup(MemorySegment device) throws Throwable {
        return (MemorySegment) newGroup.invokeExact(device);
    }

    /**
     * {@snippet lang=c :
     * ANARIInstance anariNewInstance(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newInstance(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newInstance.invokeExact(device, type);
    }

    /**
     * {@snippet lang=c :
     * ANARIWorld anariNewWorld(ANARIDevice device);
     * }
     */
    public MemorySegment newWorld(MemorySegment device) throws Throwable {
        return (MemorySegment) newWorld.invokeExact(device);
    }

    /**
     * {@snippet lang=c :
     * ANARIObject anariNewObject(ANARIDevice device, const char* objectType, const char* type);
     * }
     */
    public MemorySegment newObject(MemorySegment device, MemorySegment objType, MemorySegment type) throws Throwable {
        return (MemorySegment) newObject.invokeExact(device, objType, type);
    }

    /**
     * Objects are configured by parameters, which are identified by a string name and are set using anariSetParameter.
     *
     * Multiple types can be valid for a parameter, but only one type can be set for a particular parameter name at any given time.
     * Setting a parameter with a different type overwrites its previous value and type.
     * Attempting to set a parameter that is unknown to the implementation has no effect on object state,
     * but may cause an ANARI device implementation to emit warnings.
     * The same applies if an unsupported type for a parameter is used.
     *
     * {@snippet lang=c :
     * void anariSetParameter(ANARIDevice device, ANARIObject object, const char* name, ANARIDataType dataType, const void* mem);
     * }
     */
    public void setParameter(MemorySegment device, MemorySegment object, MemorySegment name, int dataType, MemorySegment mem) throws Throwable {
        setParameter.invokeExact(device, object, name, dataType, mem);
    }

    /**
     * Object parameters can be unset using anariUnsetParameter,
     * which returns the named parameter back to a state as if it had not been set.
     *
     * Just like with setting parameters, changes made by anariUnsetParameter must only be applied when the object is committed.
     *
     * {@snippet lang=c :
     * void anariUnsetParameter(ANARIDevice device, ANARIObject object, const char* name);
     * }
     */
    public void unsetParameter(MemorySegment device, MemorySegment object, MemorySegment name) throws Throwable {
        unsetParameter.invokeExact(device, object, name);
    }

    /**
     * Similarly, all parameters on an object can be simultaneously unset using anariUnsetAllParameters,
     * which returns the object back to a state as if no parameters had been set at all.
     * Just like with unsetting individual parameters, changes made by anariUnsetAllParameters must only be applied when the object is committed.
     *
     * {@snippet lang=c :
     * void anariUnsetAllParameters(ANARIDevice device, ANARIObject object);
     * }
     */
    public void unsetAllParameters(MemorySegment device, MemorySegment object) throws Throwable {
        unsetAllParameters.invokeExact(device, object);
    }

    /**
     * These functions return a write-only array for the application to fill, where the number of elements numElementsN must be positive (there cannot be an empty mapped array).
     * Whenever an array is directly mapped, any previous array configuration (size, dimensionality, etc.) is discarded in favor of the currently mapped array configuration.
     *
     * Devices are only required to allocate directly mapped arrays for parameters corresponding to extensions that the device implements.
     * In cases where the parameter is unknown by the device, the device is permitted to return NULL.
     *
     * Devices are permitted to have a non-dense element stride, which is returned by writing to elementStride.
     * The elementStride argument must not be NULL.
     *
     * {@snippet lang=c :
     * void* anariMapParameterArray1D(ANARIDevice device, ANARIObject object, const char* name, ANARIDataType dataType, uint64_t numElements1, uint64_t* elementStride);
     * }
     */
    public MemorySegment mapParameterArray1D(MemorySegment device, MemorySegment object, MemorySegment name, int datatype, long numElements1, MemorySegment elementStride) throws Throwable {
            return (MemorySegment) mapParameterArray1D.invokeExact(device, object, name, datatype, numElements1, elementStride);
    }

    /**
     * These functions return a write-only array for the application to fill, where the number of elements numElementsN must be positive (there cannot be an empty mapped array).
     * Whenever an array is directly mapped, any previous array configuration (size, dimensionality, etc.) is discarded in favor of the currently mapped array configuration.
     *
     * Devices are only required to allocate directly mapped arrays for parameters corresponding to extensions that the device implements.
     * In cases where the parameter is unknown by the device, the device is permitted to return NULL.
     *
     * Devices are permitted to have a non-dense element stride, which is returned by writing to elementStride.
     * The elementStride argument must not be NULL.
     *
     * {@snippet lang=c :
     * void* anariMapParameterArray2D(ANARIDevice device, ANARIObject object, const char* name, ANARIDataType dataType, uint64_t numElements1, uint64_t numElements2, uint64_t* elementStride);
     * }
     */
    public MemorySegment mapParameterArray2D(MemorySegment device, MemorySegment object, MemorySegment name, int datatype, long numElements1, long numElements2, MemorySegment elementStride) throws Throwable {
        return (MemorySegment) mapParameterArray2D.invokeExact(device, object, name, datatype, numElements1, numElements2, elementStride);
    }

    /**
     * These functions return a write-only array for the application to fill, where the number of elements numElementsN must be positive (there cannot be an empty mapped array).
     * Whenever an array is directly mapped, any previous array configuration (size, dimensionality, etc.) is discarded in favor of the currently mapped array configuration.
     *
     * Devices are only required to allocate directly mapped arrays for parameters corresponding to extensions that the device implements.
     * In cases where the parameter is unknown by the device, the device is permitted to return NULL.
     *
     * Devices are permitted to have a non-dense element stride, which is returned by writing to elementStride.
     * The elementStride argument must not be NULL.
     *
     * {@snippet lang=c :
     * void* anariMapParameterArray3D(ANARIDevice device, ANARIObject object, const char* name, ANARIDataType dataType, uint64_t numElements1, uint64_t numElements2, uint64_t numElements3, uint64_t* elementStride);
     * }
     */
    public MemorySegment mapParameterArray3D(MemorySegment device, MemorySegment object, MemorySegment name, int datatype, long numElements1, long numElements2, long numElements3, MemorySegment elementStride) throws Throwable {
        return (MemorySegment) mapParameterArray3D.invokeExact(device, object, name, datatype, numElements1, numElements2, numElements3, elementStride);
    }

    /**
     * Once the array has been filled, applications signal that the device is free to consume the data with this function.
     *
     * {@snippet lang=c :
     * void anariUnmapParameterArray(ANARIDevice device, ANARIObject object, const char* name);
     * }
     */
    public void unmapParameterArray(MemorySegment device, MemorySegment object, MemorySegment name) throws Throwable {
        unmapParameterArray.invokeExact(device, object, name);
    }

    /**
     * Changes to parameter values must only take effect once anariCommitParameters has been called on the object.
     *
     * {@snippet lang=c :
     * void anariCommitParameters(ANARIDevice device, ANARIObject object);
     * }
     */
    public void commitParameters(MemorySegment device, MemorySegment object) throws Throwable {
        commitParameters.invokeExact(device, object);
    }

    /**
     * Release an object reference.
     *
     * Object lifetime is managed by opaque reference counting. Objects have a public and internal reference count.
     * Objects are created with a public reference count of 1, which can be increased with anariRetain and decreased with anariRelease.
     * Once the public reference count has been decreased to 0, the object becomes inaccessible to host code,
     * where using its handle in subsequent API calls is invalid and results in undefined behavior.
     *
     * Calling anariRelease with a NULL object-handle for the second argument is not an error.
     * The internal reference count is managed by the API and will keep objects alive for as long
     * as the implementation needs them. Therefore, user code may release objects as soon as it no longer requires access to them.
     *
     * {@snippet lang=c :
     * void anariRelease(ANARIDevice device, ANARIObject object);
     * }
     */
    public void release(MemorySegment device, MemorySegment object) throws Throwable {
        release.invokeExact(device, object);
    }

    /**
     * Retain an object reference.
     *
     * Object lifetime is managed by opaque reference counting. Objects have a public and internal reference count.
     * Objects are created with a public reference count of 1, which can be increased with anariRetain and decreased with anariRelease.
     * Once the public reference count has been decreased to 0, the object becomes inaccessible to host code,
     * where using its handle in subsequent API calls is invalid and results in undefined behavior.
     *
     * Calling anariRelease with a NULL object-handle for the second argument is not an error.
     * The internal reference count is managed by the API and will keep objects alive for as long
     * as the implementation needs them. Therefore, user code may release objects as soon as it no longer requires access to them.
     *
     * {@snippet lang=c :
     * void anariRetain(ANARIDevice device, ANARIObject object);
     * }
     */
    public void retain(MemorySegment device, MemorySegment object) throws Throwable {
        retain.invokeExact(device, object);
    }

    /**
     * Device subtypes and their implemented extensions can be queried from a library before instantiating a device.
     * Object and parameter specific information can be queried from an instantiated device object.
     *
     * A list of device subtypes implemented in a library is retrieved by calling anariGetDeviceSubtypes.
     * It returns NULL if there are no devices, or a NULL-terminated list of 0-terminated C-strings with the names of the devices.
     * The first (if any) device is the default device.
     *
     * {@snippet lang=c :
     * const char ** anariGetDeviceSubtypes(ANARILibrary library);
     * }
     */
    public MemorySegment getDeviceSubtypes(MemorySegment library) throws Throwable {
        return (MemorySegment) getDeviceSubtypes.invokeExact(library);
    }

    /**
     * The list extensions implemented by a device is retrieved by calling anariGetDeviceExtensions
     * with a device subtype returned by anariGetDeviceSubtypes.
     * It returns a NULL-terminated list of 0-terminated C-strings with the names of the implemented extensions.
     *
     * {@snippet lang=c :
     * const char ** anariGetDeviceExtensions(ANARILibrary library, const char* deviceSubtype);
     * }
     */
    public MemorySegment getDeviceExtensions(MemorySegment library, MemorySegment deviceSubtype) throws Throwable {
        return (MemorySegment) getDeviceExtensions.invokeExact(library, deviceSubtype);
    }

    /**
     * To enumerate the subtypes of type objectType supported by device deviceSubtype call function anariGetObjectSubtypes.
     * It returns NULL if there are no subtypes, or a NULL-terminated list of 0-terminated C-strings with the names of the subtypes.
     *
     * {@snippet lang=c :
     *  const char ** anariGetObjectSubtypes(ANARIDevice device, ANARIDataType objectType);
     * }
     */
    public MemorySegment getObjectSubtypes(MemorySegment device, int objectType) throws Throwable {
        return (MemorySegment) getObjectSubtypes.invokeExact(device, objectType);
    }

    /**
     * {@snippet lang=c :
     * const void* anariGetObjectInfo(ANARIDevice device, ANARIDataType objectType, const char* objectSubtype, const char* infoName, ANARIDataType infoType);
     * }
     */
    public MemorySegment getObjectInfo(MemorySegment device, int objectType, MemorySegment objectSubtype, MemorySegment infoName, int infoType) throws Throwable {
        return (MemorySegment) getObjectInfo.invokeExact(device, objectType, objectSubtype, infoName, infoType);
    }

    /**
     * A parameter can be inspected with anariGetParameterInfo, returning the result for infoName of type infoType, or NULL if the info cannot be retrieved.
     *
     * {@snippet lang=c :
     * const void* anariGetParameterInfo(ANARIDevice device, ANARIDataType objectType, const char* objectSubtype, const char* parameterName, ANARIDataType parameterType, const char* infoName, ANARIDataType infoType);
     * }
     */
    public MemorySegment getParameterInfo(MemorySegment device, int objectType, MemorySegment objectSubtype, MemorySegment parameterName, int parameterType, MemorySegment infoName, int infoType) throws Throwable {
        return (MemorySegment) getParameterInfo.invokeExact(device, objectType, objectSubtype, parameterName, parameterType, infoName, infoType);
    }

    /**
     * Implementations may expose object properties through the object query interface.
     *
     * Properties are identified by a string name and type.
     * The waitMask indicates whether the property query will wait for the value to become available or should return instantly.
     *
     * If the property is available, the value will be written to memory, and the function returns 1.
     * Otherwise, the value is not written to memory, and the return value is 0.
     *
     * The length of a string property can be queried as an ANARI_ULONG by appending .size to the property name.
     *
     * {@snippet lang=c :
     * int anariGetProperty(ANARIDevice device, ANARIObject object, const char* name, ANARIDataType type, void* mem, uint64_t size, ANARIWaitMask ma
     * }
     */
    public int getProperty(MemorySegment device, MemorySegment object, MemorySegment name, int type, MemorySegment mem, long size, int mask) throws Throwable {
        return (int) getProperty.invokeExact(device, object, name, type, mem, size, mask);
    }

    /**
     * Create a new frame.
     *
     * {@snippet lang=c :
     * ANARIFrame anariNewFrame(ANARIDevice dev
     * }
     */
    public MemorySegment newFrame(MemorySegment device) throws Throwable {
        return (MemorySegment) newFrame.invokeExact(device);
    }

    /**
     * Map the given channel of a frame – and thus access the stored pixel information –.
     *
     * Only channels that have been set as parameters to the frame can be mapped,
     * the type of the pixels matches the corresponding parameter value. The arguments
     * width, height, and pixelType are output parameters for the application to validate
     * the exact dimensions and per-pixel data type in the mapped image.
     *
     *
     * {@snippet lang=c :
     * const void* anariMapFrame(ANARIDevice device, ANARIFrame frame, const char* channel, uint32_t* width, uint32_t* height, ANARIDataType* pixelType);
     * }
     */
    public MemorySegment mapFrame(MemorySegment device, MemorySegment frame, MemorySegment channel, MemorySegment width, MemorySegment height, MemorySegment pixelType) throws Throwable {
        return (MemorySegment) mapFrame.invokeExact(device, frame, channel, width, height, pixelType);
    }

    /**
     * A previously mapped channel of a frame can be unmapped with this function.
     *
     * {@snippet lang=c :
     * void anariUnmapFrame(ANARIDevice device, ANARIFrame frame, const char* channel);
     * }
     */
    public void unmapFrame(MemorySegment device, MemorySegment frame, MemorySegment channel) throws Throwable {
        unmapFrame.invokeExact(device, frame, channel);
    }

    /**
     * {@snippet lang=c :
     * ANARIRenderer anariNewRenderer(ANARIDevice device, const char* type);
     * }
     */
    public MemorySegment newRenderer(MemorySegment device, MemorySegment type) throws Throwable {
        return (MemorySegment) newRenderer.invokeExact(device, type);
    }

    /**
     * Rendering is asynchronous (non-blocking), and is done by combining a framebuffer,
     * renderer, camera, and world. The process of rendering a frame is known as a frame operation.
     *
     * This call may not block, and the ANARIFrame itself can be used to synchronize with the
     * application, cancel, or query for progress of the running task. When anariRenderFrame
     * is called, there is no guarantee when the associated task will begin execution.
     *
     * {@snippet lang=c :
     * void anariRenderFrame(ANARIDevice device, ANARIFrame frame);
     * }
     */
    public void renderFrame(MemorySegment device, MemorySegment frame) throws Throwable {
        renderFrame.invokeExact(device, frame);
    }

    /**
     * Query for the status of or wait on a running frame.
     *
     * If ANARI_NO_WAIT is passed as the wait mask, then the function returns true
     * if the frame has completed. Alternatively, passing ANARI_WAIT will block
     * the calling thread until the frame has completed and will always return true.
     *
     * Applications can query how long an async task ran with the duration property
     * on the ANARIFrame. If available, this returns the wall clock execution time
     * of the task in seconds. This is useful for applications to query exactly how
     * long an asynchronous task executed without the overhead of measuring both
     * task execution & synchronization by the calling application.
     *
     *
     * {@snippet lang=c :
     * int anariFrameReady(ANARIDevice device, ANARIFrame frame, ANARIWaitMask mask);
     * }
     */
    public int frameReady(MemorySegment device, MemorySegment frame, int waitMask) throws Throwable {
            return (int) frameReady.invokeExact(device, frame, waitMask);
    }

    /**
     * Signal that an in-flight frame should be cancelled if possible.
     *
     * This call is not required to block until the frame completes, rather it only
     * signals to the implementation to attempt cancelling the currently rendered
     * frame instead of going all the way to completion. The contents of a mapped
     * frame which has been discarded is undefined.
     *
     * {@snippet lang=c :
     * void anariDiscardFrame(ANARIDevice device, ANARIFrame frame);
     * }
     */
    public void discardFrame(MemorySegment device, MemorySegment frame) throws Throwable {
        discardFrame.invokeExact(device, frame);
    }

    @Override
    public void close() throws Exception {
        if (arena != null) {
            arena.close();
        }
    }

    /**
     * Loads the ANARI library from the given file path.
     *
     * @param library library path to load.
     * @return ANARI binding
     * @throws IllegalArgumentException if the ANARI library has not been found.
     * @throws NoSuchElementException if a ANARI function has not been found in the library.
     * @throws IllegalCallerException if this module is not authorized to call native methods.
     */
    public static Anari load(final Path library) {
        final Arena arena = Arena.ofShared();
        final Anari anari;
        try {
            final SymbolLookup symbols = SymbolLookup.libraryLookup(library, arena);
            anari = new Anari(arena, symbols);
        } catch (Throwable e) {
            arena.close();
            throw e;
        }
        return anari;
    }

    /**
     * Loads the ANARI library from the default library path.
     * The returned instance is valid for JVM lifetime.
     * This method returns a singleton.
     *
     * <p>If the <abbr>ANARI</abbr> library is not found, the current default is {@link SymbolLookup#loaderLookup()}
     * for allowing users to invoke {@link System#loadLibrary(String)} as a fallback.</p>
     *
     * @return ANARI binding
     * @throws IllegalStateException if the native library could not be loaded.
     */
    public static synchronized Anari global() throws IllegalStateException {
        if (globalStatus == null) {
            final String library = LIBRARY_NAME;
            String status = "Library not found";
            RuntimeException error = null;
            try {
                String filename = (File.separatorChar == '\\') ? (library + ".dll") : ("lib" + library + ".so");
                Anari instance  = null;
                SymbolLookup symbols;
                create: try {
                    try {
                        symbols = SymbolLookup.libraryLookup(filename, Arena.global());
                    } catch (IllegalArgumentException e) {
                        error    = e;
                        filename = "system";
                        symbols  = SymbolLookup.loaderLookup(); // In case user called `System.loadLibrary(…)`.
                    }
                    try {
                        instance = new Anari(null, symbols);
                        status   = "";
                    } catch (RuntimeException e) {
                        if (error != null) {
                            error.addSuppressed(e);
                        } else {
                            error = e;
                        }
                        break create;
                    }
                } catch (IllegalCallerException e) {
                    error  = e;
                    status = "Native access not allowed";
                } catch (NoSuchElementException e) {
                    error  = e;
                    status = "Function not found";
                }
                global = instance;
            } finally {
                globalStatus = status;
            }
            report(error);
        }
        report(null);
        return global;
    }

    /**
     * Throws an exception if the native library is not available.
     *
     * @param cause the cause of the error, or {@code null} if none.
     * @throws IllegalStateException if the status is not an empty string or if the given cause is not null.
     */
    private static void report(Exception cause) throws IllegalStateException {
        if (!globalStatus.isEmpty() || cause != null) {
            // Note: `NativeAccessNotAllowed` will ignore the `library` argument.
            String text = globalStatus;
            if (cause != null) {
                throw new IllegalStateException("ANARI: " + text, cause);
            } else {
                throw new IllegalStateException("ANARI: " + text);
            }
        }
    }
}

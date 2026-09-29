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

/**
 * {@snippet lang=c :
 * typedef int ANARIDataType;
 * }
 *
 * @author Johann Sorel
 */
public enum DataType {
    /**
     *
     */
    UNKNOWN(0, null, 1),
    /**
     * ANARIDataType = int32_t
     * describes data types in ANARI
     */
    DATA_TYPE(100, DataType.class, 1),
    /**
     * const char *<br>
     * 0-terminated string
     */
    STRING(101, String.class, 1),
    /**
     * void *
     * void pointer
     */
    VOID_POINTER(102, null, 1),
    /**
     * int32_t
     * boolean represented as int32_t
     */
    BOOL(103, int.class, 1),
    /**
     * const char **
     * array of 0-terminated strings terminated by NULL
     */
    STRING_LIST(150, long.class, 1),
    /**
     * ANARIDataType*
     * array of ANARIDataType values terminated by UNKNOWN
     */
    DATA_TYPE_LIST(151, null, 1),
    /**
     * ANARIParameter*
     * array of ANARIParameter structs terminated by {NULL, UNKNOWN}
     */
    PARAMETER_LIST(152, null, 1),
    /**
     * void(*)(void)
     * generic function pointer
     */
    FUNCTION_POINTER(200, null, 1),
    /**
     * ANARIMemoryDeleter
     * deleter function pointer
     */
    MEMORY_DELETER(201, null, 1),
    /**
     * ANARIStatusCallback
     * status callback function pointer
     */
    STATUS_CALLBACK(202, null, 1),
    /**
     * ANARIFrameCompletionCallback
     * extension KHR_FRAME_COMPLETION_CALLBACK frame completion callback function pointer
     */
    FRAME_COMPLETION_CALLBACK(203, null, 1),
    /**
     * ANARILibrary
     * library object handle
     */
    LIBRARY(500, Library.class, 1),
    /**
     * ANARIDevice
     * device object handle
     */
    DEVICE(501, Device.class, 1),
    /**
     * ANARIObject
     * generic object handle
     */
    OBJECT(502, Object.class, 1),
    /**
     * ANARIArray
     * generic array object handle
     */
    ARRAY(503, Array.class, 1),
    /**
     * ANARIArray1D
     * 1D array object handle
     */
    ARRAY1D(504, Array1D.class, 1),
    /**
     * ANARIArray2D
     * 2D array object handle
     */
    ARRAY2D(505, Array2D.class, 1),
    /**
     * ANARIArray3D
     * 3D array object handle
     */
    ARRAY3D(506, Array3D.class, 1),
    /**
     * ANARICamera
     * [camera] object handle
     */
    CAMERA(507, Camera.class, 1),
    /**
     * ANARIFrame
     * [frame] object handle
     */
    FRAME(508, Frame.class, 1),
    /**
     * ANARIGeometry
     * Geometry object handle
     */
    GEOMETRY(509, Geometry.class, 1),
    /**
     * ANARIGroup
     * [group] object handle
     */
    GROUP(510, Group.class, 1),
    /**
     * ANARIInstance
     * [instance] object handle
     */
    INSTANCE(511, Instance.class, 1),
    /**
     * ANARILight
     * [light] object handle
     */
    LIGHT(512, Light.class, 1),
    /**
     * ANARIMaterial
     * Material object handle
     */
    MATERIAL(513, Material.class, 1),
    /**
     * ANARIRenderer
     * [renderer] object handle
     */
    RENDERER(514, Renderer.class, 1),
    /**
     * ANARISurface
     * [surface] object handle
     */
    SURFACE(515, Surface.class, 1),
    /**
     * ANARISampler
     * [sampler] object handle
     */
    SAMPLER(516, Sampler.class, 1),
    /**
     * ANARISpatialField
     * Spatial Field object handle
     */
    SPATIAL_FIELD(517, SpatialField.class, 1),
    /**
     * ANARIVolume
     * [volume] object handle
     */
    VOLUME(518, Volume.class, 1),
    /**
     * ANARIWorld
     * [world] object handle
     */
    WORLD(519, World.class, 1),

    /**
     * int8_t
     * 8 bit signed integer
     */
    INT8(1000, byte.class,1),
    /**
     * int8_t[2]
     * two element 8 bit signed integer vector
     */
    INT8_VEC2(1001, byte.class,2),
    /**
     * int8_t[3]
     * three element 8 bit signed integer vector
     */
    INT8_VEC3(1002, byte.class,3),
    /**
     * int8_t[4]
     * four element 8 bit signed integer vector
     */
    INT8_VEC4(1003, byte.class,4),
    /**
     * uint8_t
     * 8 bit unsigned integer
     */
    UINT8(1004, byte.class,1),
    /**
     * uint8_t[2]
     * two element 8 bit unsigned integer vector
     */
    UINT8_VEC2(1005, byte.class,2),
    /**
     * uint8_t[3]
     * three element 8 bit unsigned integer vector
     */
    UINT8_VEC3(1006, byte.class,3),
    /**
     * uint8_t[4]
     * four element 8 bit unsigned integer vector
     */
    UINT8_VEC4(1007, byte.class,4),
    /**
     * int16_t
     * 16 bit signed integer
     */
    INT16(1008, short.class,1),
    /**
     * int16_t[2]
     * two element 16 bit signed integer vector
     */
    INT16_VEC2(1009, short.class,2),
    /**
     * int16_t[3]
     * three element 16 bit signed integer vector
     */
    INT16_VEC3(1010, short.class,3),
    /**
     * int16_t[4]
     * four element 16 bit signed integer vector
     */
    INT16_VEC4(1011, short.class,4),
    /**
     * uint16_t
     * 16 bit unsigned integer
     */
    UINT16(1012, short.class,1),
    /**
     * uint16_t[2]
     * two element 16 bit unsigned integer vector
     */
    UINT16_VEC2(1013, short.class,2),
    /**
     * uint16_t[3]
     * three element 16 bit unsigned integer vector
     */
    UINT16_VEC3(1014, short.class,3),
    /**
     * uint16_t[4]
     * four element 16 bit unsigned integer vector
     */
    UINT16_VEC4(1015, short.class,4),
    /**
     * int32_t
     * 32 bit signed integer
     */
    INT32(1016, int.class,1),
    /**
     * int32_t[2]
     * two element 32 bit signed integer vector
     */
    INT32_VEC2(1017, int.class,2),
    /**
     * int32_t[3]
     * three element 32 bit signed integer vector
     */
    INT32_VEC3(1018, int.class,3),
    /**
     * int32_t[4]
     * four element 32 bit signed integer vector
     */
    INT32_VEC4(1019, int.class,4),
    /**
     * uint32_t
     * 32 bit unsigned integer
     */
    UINT32(1020, int.class,1),
    /**
     * uint32_t[2]
     * two element 32 bit unsigned integer vector
     */
    UINT32_VEC2(1021, int.class,2),
    /**
     * uint32_t[3]
     * three element 32 bit unsigned integer vector
     */
    UINT32_VEC3(1022, int.class,3),
    /**
     * uint32_t[4]
     * four element 32 bit unsigned integer vector
     */
    UINT32_VEC4(1023, int.class,4),
    /**
     * int64_t
     * 64 bit signed integer
     */
    INT64(1024, long.class,1),
    /**
     * int64_t[2]
     * two element 64 bit signed integer vector
     */
    INT64_VEC2(1025, long.class,2),
    /**
     * int64_t[3]
     * three element 64 bit signed integer vector
     */
    INT64_VEC3(1026, long.class,3),
    /**
     * int64_t[4]
     * four element 64 bit signed integer vector
     */
    INT64_VEC4(1027, long.class,4),
    /**
     * uint64_t
     * 64 bit unsigned integer
     */
    UINT64(1028, long.class,1),
    /**
     * uint64_t[2]
     * two element vector 64 bit unsigned integer vector
     */
    UINT64_VEC2(1029, long.class,2),
    /**
     * uint64_t[3]
     * three element vector 64 bit unsigned integer vector
     */
    UINT64_VEC3(1030, long.class,3),
    /**
     * uint64_t[4]
     * four element 64 bit unsigned integer vector vector
     */
    UINT64_VEC4(1031, long.class,4),
    /**
     * int8_t
     * 8 bit signed normalized fixed point number
     */
    FIXED8(1032, byte.class,1),
    /**
     * int8_t[2]
     * two element 8 bit signed normalized fixed point vector
     */
    FIXED8_VEC2(1033, byte.class,2),
    /**
     * int8_t[3]
     * three element 8 bit signed normalized fixed point vector
     */
    FIXED8_VEC3(1034, byte.class,3),
    /**
     * int8_t[4]
     * four element 8 bit signed normalized fixed point vector
     */
    FIXED8_VEC4(1035, byte.class,4),
    /**
     * uint8_t
     * 8 bit unsigned normalized fixed point number
     */
    UFIXED8(1036, byte.class,1),
    /**
     * uint8_t[2]
     * two element 8 bit unsigned normalized fixed point vector
     */
    UFIXED8_VEC2(1037, byte.class,2),
    /**
     * uint8_t[3]
     * three element 8 bit unsigned normalized fixed point vector
     */
    UFIXED8_VEC3(1038, byte.class,3),
    /**
     * uint8_t[4]
     * four element 8 bit unsigned normalized fixed point vector
     */
    UFIXED8_VEC4(1039, byte.class,4),
    /**
     * int16_t
     * 16 bit signed normalized fixed point number
     */
    FIXED16(1040, short.class,1),
    /**
     * int16_t[2]
     * two element 16 bit signed normalized fixed point vector
     */
    FIXED16_VEC2(1041, short.class,2),
    /**
     * int16_t[3]
     * three element 16 bit signed normalized fixed point vector
     */
    FIXED16_VEC3(1042, short.class,3),
    /**
     * int16_t[4]
     * four element 16 bit signed normalized fixed point vector
     */
    FIXED16_VEC4(1043, short.class,4),
    /**
     * uint16_t
     * 16 bit unsigned normalized fixed point number
     */
    UFIXED16(1044, short.class,1),
    /**
     * uint16_t[2]
     * two element 16 bit unsigned normalized fixed point vector
     */
    UFIXED16_VEC2(1045, short.class,2),
    /**
     * uint16_t[3]
     * three element 16 bit unsigned normalized fixed point vector
     */
    UFIXED16_VEC3(1046, short.class,3),
    /**
     * uint16_t[4]
     * four element 16 bit unsigned normalized fixed point vector
     */
    UFIXED16_VEC4(1047, short.class,4),
    /**
     * int32_t
     * 32 bit signed normalized fixed point number
     */
    FIXED32(1048, int.class,1),
    /**
     * int32_t[2]
     * two element 32 bit signed normalized fixed point vector
     */
    FIXED32_VEC2(1049, int.class,2),
    /**
     * int32_t[3]
     * thre element 32 bit signed normalized fixed point vector
     */
    FIXED32_VEC3(1050, int.class,3),
    /**
     * int32_t[4]
     * four element 32 bit signed normalized fixed point vector
     */
    FIXED32_VEC4(1051, int.class,4),
    /**
     * uint32_t
     * 32 bit unsigned normalized fixed point number
     */
    UFIXED32(1052, int.class,1),
    /**
     * uint32_t[2]
     * two element 32 bit unsigned normalized fixed point vector
     */
    UFIXED32_VEC2(1053, int.class,2),
    /**
     * uint32_t[3]
     * three element 32 bit unsigned normalized fixed point vector
     */
    UFIXED32_VEC3(1054, int.class,3),
    /**
     * uint32_t[4]
     * four element 32 bit unsigned normalized fixed point vector
     */
    UFIXED32_VEC4(1055, int.class,4),
    /**
     * int64_t
     * 64 bit signed normalized fixed point number
     */
    FIXED64(1056, long.class,1),
    /**
     * int64_t[2]
     * two element 64 bit signed normalized fixed point vector
     */
    FIXED64_VEC2(1057, long.class,2),
    /**
     * int64_t[3]
     * three element 64 bit signed normalized fixed point vector
     */
    FIXED64_VEC3(1058, long.class,3),
    /**
     * int64_t[4]
     * four element 64 bit signed normalized fixed point vector
     */
    FIXED64_VEC4(1059, long.class,4),
    /**
     * uint64_t
     * 64 bit unsigned normalized fixed point number
     */
    UFIXED64(1060, long.class,1),
    /**
     * uint64_t[2]
     * two element 64 bit unsigned normalized fixed point vector
     */
    UFIXED64_VEC2(1061, long.class,2),
    /**
     * uint64_t[3]
     * three element 64 bit unsigned normalized fixed point vector
     */
    UFIXED64_VEC3(1062, long.class,3),
    /**
     * uint64_t[4]
     * four element 64 bit unsigned normalized fixed point vector
     */
    UFIXED64_VEC4(1063, long.class,4),
    /**
     * uint16_t
     * 16 bit floating point number
     */
    FLOAT16(1064, float.class,1),
    /**
     * uint16_t[2]
     * two element 16 bit floating point vector vector
     */
    FLOAT16_VEC2(1065, float.class,2),
    /**
     * uint16_t[3]
     * three element vector 16 bit floating point vector
     */
    FLOAT16_VEC3(1066, float.class,3),
    /**
     * uint16_t[4]
     * four element vector 16 bit floating point vector
     */
    FLOAT16_VEC4(1067, float.class,4),
    /**
     * float
     * 32 bit floating point number
     */
    FLOAT32(1068, float.class,1),
    /**
     * float[2]
     * two element 32 bit floating point vector vector
     */
    FLOAT32_VEC2(1069, float.class,2),
    /**
     * float[3]
     * three element vector 32 bit floating point vector
     */
    FLOAT32_VEC3(1070, float.class,3),
    /**
     * float[4]
     * four element vector 32 bit floating point vector
     */
    FLOAT32_VEC4(1071, float.class,4),
    /**
     * double
     * 64 bit floating point
     */
    FLOAT64(1072, double.class,1),
    /**
     * double[2]
     * two element vector 64 bit floating point vector
     */
    FLOAT64_VEC2(1073, double.class,2),
    /**
     * double[3]
     * three element vector 64 bit floating point vector
     */
    FLOAT64_VEC3(1074, double.class,3),
    /**
     * double[4]
     * four element vector 64 bit floating point vector
     */
    FLOAT64_VEC4(1075, double.class,4),
    /**
     * uint8_t[4]
     * three component sRGB color with linear alpha
     */
    UFIXED8_RGBA_SRGB(2003, byte.class,4),
    /**
     * uint8_t[3]
     * three component sRGB color
     */
    UFIXED8_RGB_SRGB(2002, byte.class,3),
    /**
     * uint8_t[2]
     * one componenet sRGB with linear alpha
     */
    UFIXED8_RA_SRGB(2001, byte.class,2),
    /**
     * uint8_t[1]
     * single component sRGB
     */
    UFIXED8_R_SRGB(2000, byte.class,1),
    /**
     * int32_t[2]
     * one dimensional 32 bit integer box (inclusive lower and upper bounds)
     */
    INT32_BOX1(2004, int.class,2),
    /**
     * int32_t[4]
     * two dimensional 32 bit integer box (inclusive lower and upper bound vector)
     */
    INT32_BOX2(2005, int.class,4),
    /**
     * int32_t[6]
     * three dimensional 32 bit integer box (inclusive lower and upper bound vector)
     */
    INT32_BOX3(2006, int.class,6),
    /**
     * int32_t[8]
     * four dimensional 32 bit integer box (inclusive lower and upper bound vector)
     */
    INT32_BOX4(2007, int.class,8),
    /**
     *  float[2]
     * one dimensional 32 bit float box (inclusive lower and upper bounds)
     */
    FLOAT32_BOX1(2008, float.class,2),
    /**
     * float[4]
     * two dimensional 32 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT32_BOX2(2009, float.class,4),
    /**
     * float[6]
     * three dimensional 32 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT32_BOX3(2010, float.class,6),
    /**
     * float[8]
     * four dimensional 32 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT32_BOX4(2011, float.class,8),
    /**
     * double[2]
     * one dimensional 64 bit float box (inclusive lower and upper bounds)
     */
    FLOAT64_BOX1(2208, double.class,2),
    /**
     * double[4]
     * two dimensional 64 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT64_BOX2(2209, double.class,4),
    /**
     * double[6]
     * three dimensional 64 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT64_BOX3(2210, double.class,6),
    /**
     * double[8]
     * four dimensional 64 bit float box (inclusive lower and upper bound vector)
     */
    FLOAT64_BOX4(2211, double.class,8),
    /**
     * uint64_t[2]
     * one dimensional 64 bit unsigned integer region (inclusive lower and exclusive upper bounds)
     */
    UINT64_REGION1(2104, long.class,2),
    /**
     * uint64_t[4]
     * two dimensional 64 bit unsigned integer region (inclusive lower and exclusive upper bound vector)
     */
    UINT64_REGION2(2105, long.class,4),
    /**
     * uint64_t[6]
     * three dimensional 64 bit unsigned integer region (inclusive lower and exclusive upper bound vector)
     */
    UINT64_REGION3(2106, long.class,6),
    /**
     * uint64_t[8]
     * four dimensional 64 bit unsigned integer region (inclusive lower and exclusive upper bound vector)
     */
    UINT64_REGION4(2107, long.class,8),
    /**
     * float[4]
     * two by two 32 bit float matrix in column-major order
     */
    FLOAT32_MAT2(2012, float.class,2*2),
    /**
     * float[9]
     * three by three 32 bit float matrix in column-major order
     */
    FLOAT32_MAT3(2013, float.class,3*3),
    /**
     * float[16]
     * four by four 32 bit float matrix in column-major order
     */
    FLOAT32_MAT4(2014, float.class,4*4),
    /**
     * float[6]
     * two by three 32 bit float matrix in column-major order
     */
    FLOAT32_MAT2x3(2015, float.class,2*3),
    /**
     * float[12]
     * three by four 32 bit float matrix in column-major order
     */
    FLOAT32_MAT3x4(2016, float.class,3*4),
    /**
     * float[4]
     * quaternion
     */
    FLOAT32_QUAT_IJKW(2017, float.class,4);

    public final int code;
    public final Class primitiveClass;
    public final int nbComponent;
    public final int byteSize;

    private DataType(int code, Class primitiveClass, int nbComponent) {
        this.code = code;
        this.primitiveClass = primitiveClass;
        this.nbComponent = nbComponent;

        final int nbByte;
        if (primitiveClass == byte.class) nbByte = 1;
        else if (primitiveClass == short.class) nbByte = 2;
        else if (primitiveClass == int.class) nbByte = 4;
        else if (primitiveClass == long.class) nbByte = 8;
        else if (primitiveClass == float.class) nbByte = 4;
        else if (primitiveClass == double.class) nbByte = 8;
        else nbByte = 8; //pointer
        this.byteSize = nbByte * nbComponent;
    }

    public static DataType forCode(int code) {
        for (DataType dt : DataType.values()) {
            if (dt.code == code) return dt;
        }
        throw new IllegalArgumentException("Unknown code " + code);
    }

}

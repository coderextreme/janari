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
 *
 * @author Johann Sorel
 */
public final class Properties {

    public static final class Object {

        /**
         * STRING optional explanation of the object, e.g., for a tooltip
         */
        public static final String QUERY_DESCRIPTION = "description";
        /**
         * PARAMETER_LIST mandatory list of supported parameters (array of ANARIParameter)
         */
        public static final String QUERY_PARAMETER = "parameter";
    }

    public static final class Parameter {

        /**
         * STRING optional explanation of the parameter, e.g., for a tooltip
         */
        public static final String QUERY_DESCRIPTION = "description";
        /**
         * type optional set values will be clamped to this minimum
         */
        public static final String QUERY_MINIMUM = "minimum";
        /**
         * type optional set values will be clamped to this maximum
         */
        public static final String QUERY_MAXIMUM = "maximum";
        /**
         * type optional default value, must be in minimum..maximum if present
         */
        public static final String QUERY_DEFAULT = "default";
        /**
         * DATA_TYPE_LIST mandatory for ARRAYS array of supported element types of the ANARIArray parameter, with last element UNKNOWN
         */
        public static final String QUERY_ELEMENTTYPE = "elementType";
        /**
         * BOOL mandatory whether the parameter must be set for an object to be valid, must be FALSE if a default is present
         */
        public static final String QUERY_REQUIRED = "required";
        /**
         * STRING_LIST or DATA_TYPE_LIST optional list of accepted strings or data types for parameters that only recognize specific values
         */
        public static final String QUERY_VALUE = "value";

    }

    public static final class Array1D {

        /**
         * Additional parameter accepted by ANARIArray1D with extension KHR_ARRAY1D_REGION
         * <br>
         * UINT64_REGION1 region of elements currently in use
         */
        public static final String PARAM_REGION = "region";
    }

    public static final class Frame {

        /**
         * STRING_LIST mandatory list of supported channels
         */
        public static final String QUERY_CHANNEL = "channel";
        /**
         * FLOAT32 mandatory time (in seconds) between start and completion of the frame
         */
        public static final String QUERY_DURATION = "duration";
        /**
         * FLOAT32 optional progress of the current frame task since the last call to anariRenderFrame, in [0..1]
         */
        public static final String QUERY_RENDERPROGRESS = "renderProgress";
        /**
         * FLOAT32 optional progress of frame refinement when using KHR_FRAME_ACCUMULATION renderers, in [0..1]
         */
        public static final String QUERY_REFINEMENTPROGRESS = "refinementProgress";
        /**
         * WORLD required world to be rendererd
         */
        public static final String PARAM_WORLD = "world";
        /**
         * CAMERA required camera used to render the world
         */
        public static final String PARAM_CAMERA = "camera";
        /**
         * RENDERER required renderer which renders the frame
         */
        public static final String PARAM_RENDERER = "renderer";
        /**
         * UINT32_VEC2 required size of the frame in pixels (width × height)
         */
        public static final String PARAM_SIZE = "size";
        /**
         * BOOL FALSE
         */
        public static final String PARAM_ACCUMULATION = "accumulation";
        /**
         * DATA_TYPE enable mapping the depth channel and specify its observable type; euclidean distance to the camera (not to the image plane), for multiple samples per pixel their minimum is taken; possible values: FLOAT32
         */
        public static final String PARAM_CHANNEL_COLOR = "channel.color";
        /**
         * DATA_TYPE enable mapping the depth channel and specify its observable type; euclidean distance to the camera (not to the image plane), for multiple samples per pixel their minimum is taken; possible values: FLOAT32         *
         */
        public static final String PARAM_CHANNEL_DEPTH = "channel.depth";
        /**
         * DATA_TYPE enable mapping the normal channel and specify its observable type; average world-space normal of the first hit; possible values: FIXED16_VEC3, FLOAT32_VEC3
         * <br>
         * Extension KHR_FRAME_CHANNEL_NORMAL
         */
        public static final String PARAM_CHANNEL_NORMAL = "channel.normal";
        /**
         * DATA_TYPE enable mapping the albedo channel and specify its observable type; average material albedo (color without illumination) at the first hit; possible values: UFIXED8_VEC3, UFIXED8_RGB_SRGB, FLOAT32_VEC3
         * <br>
         * Extension KHR_FRAME_CHANNEL_ALBEDO
         */
        public static final String PARAM_CHANNEL_ALBEDO = "channel.albedo";
        /**
         * DATA_TYPE enable mapping the primitiveId channel and specify its observable type; primitiveId attribute of the first hit; possible values: UINT32
         * <br>
         * Extension KHR_FRAME_CHANNEL_PRIMITIVE_ID
         */
        public static final String PARAM_CHANNEL_PRIMITIVEID = "channel.primitiveId";
        /**
         * DATA_TYPE enable mapping the objectId channel and specify its observable type; user defined Surface / Volume id, if specified, or index in Group of first hit; possible values: UINT32
         * <br>
         * Extension KHR_FRAME_CHANNEL_OBJECT_ID
         */
        public static final String PARAM_CHANNEL_OBJECTID = "channel.objectId";
        /**
         * DATA_TYPE enable mapping the instanceId channel and specify its observable type; user defined Instance id, if specified, or instance index of first hit; possible values: UINT32
         * <br>
         * Extension KHR_FRAME_CHANNEL_INSTANCE_ID
         */
        public static final String PARAM_CHANNEL_INSTANCEID = "channel.instanceId";
        /**
         * FRAME_COMPLETION_CALLBACK callback to invoke as continuation when the rendered frame is complete
         * <br>
         * Extension KHR_FRAME_COMPLETION_CALLBACK
         */
        public static final String PARAM_COMPLETIONCALLBACK = "frameCompletionCallback";
        /**
         * VOID_POINTER optional user pointer passed as the first argument of the frame completion callback
         * <br>
         * Extension KHR_FRAME_COMPLETION_CALLBACK
         */
        public static final String PARAM_COMPLETIONCALLBACKUSERDATA = "frameCompletionCallbackUserData";
        /**
         * VOID_POINTER default MPI_COMM_WORLD, the MPI communicator which the device should treat as the MPI world
         * <br>
         * Extension KHR_DATA_PARALLEL_MPI
         */
        public static final String PARAM_MPICOMMUNICATOR = "mpiCommunicator";
    }

    public static final class Renderer {

        /**
         * STRING_LIST mandatory list of supported extensions
         */
        public static final String QUERY_EXTENSION = "extension";

        public static final String PARAM_BACKGROUND = "background";
        public static final String PARAM_AMBIENTCOLOR = "ambientColor";
        public static final String PARAM_AMBIENTRADIANCE = "ambientRadiance";
        /**
         * BOOL default FALSE, whether the rendered image should be denoised
         * <br>
         * Extension KHR_RENDERER_DENOISE
         */
        public static final String PARAM_DENOISE = "denoise";
    }

    public static final class World {

        public static final String QUERY_BOUNDS = "bounds";

        public static final String PARAM_INSTANCE = "instance";
        public static final String PARAM_SURFACE = "surface";
        public static final String PARAM_VOLUME = "volume";
        public static final String PARAM_LIGHT = "light";
    }

    public static final class Device {

        /**
         * INT32 Mandatory unique version number guaranteed to increase between versions
         */
        public static final String QUERY_VERSION = "version";
        /**
         * INT32 Optional semantic version major value (major.minor.patch)
         */
        public static final String QUERY_VERSION_MAJOR = "version.major";
        /**
         * INT32 Optional semantic version minor value (major.minor.patch)
         */
        public static final String QUERY_VERSION_MINOR = "version.minor";
        /**
         * INT32 Optional semantic version patch value (major.minor.patch)
         */
        public static final String QUERY_VERSION_PATCH = "version.patch";
        /**
         * STRING Optional human readable name/title
         */
        public static final String QUERY_VERSION_NAME = "version.name";
        /**
         * INT32 Mandatory targeted ANARI specification version major value (major.minor)
         */
        public static final String QUERY_ANARIVERSION_MAJOR = "anariVersion.major";
        /**
         * INT32 Mandatory targeted ANARI specification version minor value (major.minor)
         */
        public static final String QUERY_ANARIVERSION_MINOR = "anariVersion.minor";
        /**
         * UINT64 Mandatory largest supported index into vertex arrays in geometries
         */
        public static final String QUERY_GEOMETRYMAXINDEX = "geometryMaxIndex";
        /**
         * STRING_LIST Mandatory list of supported extensions
         */
        public static final String QUERY_EXTENSION = "extension";
        /**
         * STATUS_CALLBACK callback used to report information to the application
         */
        public static final String PARAM_STATUSCALLBACK = "statusCallback";
        /**
         * VOID_POINTER optional pointer passed as the first argument of the status callback
         */
        public static final String PARAM_STATUSCALLBACKUSERDATA = "statusCallbackUserData";
    }

    public static final class Camera {
        public static final String PARAM_POSITION = "position";
        public static final String PARAM_DIRECTION = "direction";
        public static final String PARAM_UP = "up";
        public static final String PARAM_IMAGEREGION = "imageRegion";
        public static final String PARAM_APERTURERADIUS = "apertureRadius";
        public static final String PARAM_FOCUSDISTANCE = "focusDistance";
        public static final String PARAM_STEREOMODE = "stereoMode";
        public static final String PARAM_INTERPUPILLARYDISTANCE = "interpupillaryDistance";
        public static final String PARAM_MOTION_TRANSFORM = "motion.transform";
        public static final String PARAM_MOTION_SCALE = "motion.scale";
        public static final String PARAM_MOTION_ROTATION = "motion.rotation";
        public static final String PARAM_MOTION_TRANSLATION = "motion.translation";
        public static final String PARAM_TIME = "time";
        public static final String PARAM_SHUTTER = "shutter";
        public static final String PARAM_ROLLINGSHUTTERDIRECTION = "rollingShutterDirection";
        public static final String PARAM_ROLLINGSHUTTERDURATION = "rollingShutterDuration";
        public static final String PARAM_FOVY = "fovy";
        public static final String PARAM_ASPECT = "aspect";
        public static final String PARAM_NEAR = "near";
        public static final String PARAM_FAR = "far";
        public static final String PARAM_LAYOUT = "layout";
        public static final String PARAM_HEIGHT = "height";

    }

    public static final class Instance {
        public static final String QUERY_BOUNDS = "bounds";

        public static final String PARAM_GROUP = "group";
        public static final String PARAM_TRANSFORM = "transform";
        public static final String PARAM_ID = "id";
        public static final String PARAM_MOTION_TRANSFORM = "motion.transform";
        public static final String PARAM_TIME = "time";
        public static final String PARAM_MOTION_SCALE = "motion.scale";
        public static final String PARAM_MOTION_ROTATION = "motion.rotation";
        public static final String PARAM_MOTION_TRANSLATION = "motion.translation";
        /**
         * FLOAT32_VEC4 uniform color attribute, applies to all objects contained in the instance
         */
        public static final String PARAM_COLOR = "color";
        /**
         * FLOAT32_VEC4 uniform attribute 0, applies to all objects contained in the instance
         */
        public static final String PARAM_ATTRIBUTE0 = "attribute0";
        /**
         * FLOAT32_VEC4 uniform attribute 1, applies to all objects contained in the instance
         */
        public static final String PARAM_ATTRIBUTE1 = "attribute1";
        /**
         * FLOAT32_VEC4 uniform attribute 2, applies to all objects contained in the instance
         */
        public static final String PARAM_ATTRIBUTE2 = "attribute2";
        /**
         * FLOAT32_VEC4 uniform attribute 3, applies to all objects contained in the instance
         */
        public static final String PARAM_ATTRIBUTE3 = "attribute3";
    }

    public static final class Group {
        public static final String QUERY_BOUNDS = "bounds";

        public static final String PARAM_SURFACE = "surface";
        public static final String PARAM_VOLUME = "volume";
        public static final String PARAM_LIGHT = "light";
    }

    public static final class Light {

        public static final String PARAM_COLOR = "color";
        public static final String PARAM_VISIBLE = "visible";
        public static final String PARAM_DIRECTION = "direction";
        public static final String PARAM_IRRADIANCE = "irradiance";
        public static final String PARAM_ANGULARDIAMETER = "angularDiameter";
        public static final String PARAM_RADIANCE = "radiance";
        public static final String PARAM_UP = "up";
        public static final String PARAM_LAYOUT = "layout";
        public static final String PARAM_SCALE = "scale";
        public static final String PARAM_POSITION = "position";
        public static final String PARAM_INTENSITY = "intensity";
        public static final String PARAM_POWER = "power";
        public static final String PARAM_RADIUS = "radius";
        public static final String PARAM_EDGE1 = "edge1";
        public static final String PARAM_EDGE2 = "edge2";
        public static final String PARAM_SIDE = "side";
        public static final String PARAM_INTENSITYDISTRIBUTION = "intensityDistribution";
        public static final String PARAM_OPENINGANGLE = "openingAngle";
        public static final String PARAM_FALLOFFANGLE = "falloffAngle";
        public static final String PARAM_INNERRADIUS = "innerRadius";
        public static final String PARAM_C0 = "c0";
    }

    public static final class Surface {

        public static final String PARAM_GEOMETRY = "geometry";
        public static final String PARAM_MATERIAL = "material";
        /**
         * BOOL default TRUE, whether the surface is visible
         */
        public static final String PARAM_VISIBLE = "visible";
        public static final String PARAM_ID = "id";
    }

    public static final class Geometry {

        /**
         * FLOAT32_VEC4 uniform color attribute
         */
        public static final String PARAM_COLOR = "color";
        /**
         * FLOAT32_VEC4 uniform attribute 0
         */
        public static final String PARAM_ATTRIBUTE0 = "attribute0";
        /**
         * FLOAT32_VEC4 uniform attribute 1
         */
        public static final String PARAM_ATTRIBUTE1 = "attribute1";
        /**
         * FLOAT32_VEC4 uniform attribute 2
         */
        public static final String PARAM_ATTRIBUTE2 = "attribute2";
        /**
         * FLOAT32_VEC4 uniform attribute 3
         */
        public static final String PARAM_ATTRIBUTE3 = "attribute3";
        public static final String PARAM_PRIMITIVE_COLOR = "primitive.color";
        public static final String PARAM_PRIMITIVE_ATTRIBUTE0 = "primitive.attribute0";
        public static final String PARAM_PRIMITIVE_ATTRIBUTE1 = "primitive.attribute1";
        public static final String PARAM_PRIMITIVE_ATTRIBUTE2 = "primitive.attribute2";
        public static final String PARAM_PRIMITIVE_ATTRIBUTE3 = "primitive.attribute3";
        public static final String PARAM_PRIMITIVE_ID = "primitive.id";
        public static final String PARAM_VERTEX_POSITION = "vertex.position";
        public static final String PARAM_VERTEX_RADIUS = "vertex.radius";
        public static final String PARAM_VERTEX_CAP = "vertex.cap";
        public static final String PARAM_VERTEX_COLOR = "vertex.color";
        public static final String PARAM_VERTEX_ATTRIBUTE0 = "vertex.attribute0";
        public static final String PARAM_VERTEX_ATTRIBUTE1 = "vertex.attribute1";
        public static final String PARAM_VERTEX_ATTRIBUTE2 = "vertex.attribute2";
        public static final String PARAM_VERTEX_ATTRIBUTE3 = "vertex.attribute3";
        public static final String PARAM_PRIMITIVE_INDEX = "primitive.index";
        public static final String PARAM_CAPS = "caps";
        public static final String PARAM_RADIUS = "radius";
        public static final String PARAM_PRIMITIVE_RADIUS = "primitive.radius";
        public static final String PARAM_VERTEX_NORMAL = "vertex.normal";
        public static final String PARAM_VERTEX_TANGENT = "vertex.tangent";
        public static final String PARAM_MOTION_VERTEX_POSITION = "motion.vertex.position";
        public static final String PARAM_MOTION_VERTEX_NORMAL = "motion.vertex.normal";
        public static final String PARAM_MOTION_VERTEX_TANGENT = "motion.vertex.tangent";
        public static final String PARAM_TIME = "time";
        /**
         * ARRAY1D of FIXED16_VEC3 / FLOAT32_VEC3 face-varying normals
         */
        public static final String PARAM_FACEVARYING_NORMAL = "faceVarying.normal";
        /**
         * ARRAY1D of FIXED16_VEC3 / FIXED16_VEC4 / FLOAT32_VEC3 / FLOAT32_VEC4 face-varying tangents
         */
        public static final String PARAM_FACEVARYING_TANGENT = "faceVarying.tangent";
        /**
         * ARRAY1D of Color face-varying colors
         */
        public static final String PARAM_FACEVARYING_COLOR = "faceVarying.color";
        /**
         * ARRAY1D of FLOAT32 / FLOAT32_VEC2 / FLOAT32_VEC3 / FLOAT32_VEC4 face-varying attribute 0
         */
        public static final String PARAM_FACEVARYING_ATTRIBUTE0 = "faceVarying.attribute0";
        /**
         * ARRAY1D of FLOAT32 / FLOAT32_VEC2 / FLOAT32_VEC3 / FLOAT32_VEC4 face-varying attribute 1
         */
        public static final String PARAM_FACEVARYING_ATTRIBUTE1 = "faceVarying.attribute1";
        /**
         * ARRAY1D of FLOAT32 / FLOAT32_VEC2 / FLOAT32_VEC3 / FLOAT32_VEC4 face-varying attribute 2
         */
        public static final String PARAM_FACEVARYING_ATTRIBUTE2 = "faceVarying.attribute2";
        /**
         * ARRAY1D of FLOAT32 / FLOAT32_VEC2 / FLOAT32_VEC3 / FLOAT32_VEC4 face-varying attribute 3
         */
        public static final String PARAM_FACEVARYING_ATTRIBUTE3 = "faceVarying.attribute3";
        /**
         * FLOAT32 / ARRAY1D of FLOAT32 isovalue(s) defining the isosurface(s)
         * <br>
         * Extension KHR_GEOMETRY_ISOSURFACE
         */
        public static final String PARAM_ISOVALUE = "isovalue";
        /**
         * SPATIAL_FIELD, spatial field to be isosurfaced
         * <br>
         * Extension KHR_GEOMETRY_ISOSURFACE
         */
        public static final String PARAM_FIELD = "field";
    }

    public static final class Sampler {

        public static final String PARAM_INATTRIBUTE = "inAttribute";
        public static final String PARAM_INTRANSFORM = "inTransform";
        public static final String PARAM_INOFFSET = "inOffset";
        public static final String PARAM_IMAGE = "image";
        public static final String PARAM_FILTER = "filter";
        public static final String PARAM_WRAPMODE = "wrapMode";
        public static final String PARAM_OUTTRANSFORM = "outTransform";
        public static final String PARAM_OUTOFFSET = "outOffset";
        public static final String PARAM_WRAPMODE1 = "wrapMode1";
        public static final String PARAM_WRAPMODE2 = "wrapMode2";
        public static final String PARAM_WRAPMODE3 = "wrapMode3";
        public static final String PARAM_ARRAY = "array";
    }

    public static final class Material {

        public static final String PARAM_COLOR = "color";
        public static final String PARAM_OPACITY = "opacity";
        public static final String PARAM_ALPHAMODE = "alphaMode";
        public static final String PARAM_ALPHACUTOFF = "alphaCutoff";
        public static final String PARAM_BASECOLOR = "baseColor";
        public static final String PARAM_METALLIC = "metallic";
        public static final String PARAM_ROUGHNESS = "roughness";
        public static final String PARAM_NORMAL = "normal";
        public static final String PARAM_EMISSIVE = "emissive";
        public static final String PARAM_OCCLUSION = "occlusion";
        public static final String PARAM_SPECULAR = "specular";
        public static final String PARAM_SPECULARCOLOR = "specularColor";
        public static final String PARAM_CLEARCOAT = "clearcoat";
        public static final String PARAM_CLEARCOATROUGHNESS = "clearcoatRoughness";
        public static final String PARAM_CLEARCOATNORMAL = "clearcoatNormal";
        public static final String PARAM_TRANSMISSION = "transmission";
        public static final String PARAM_IOR = "ior";
        public static final String PARAM_THICKNESS = "thickness";
        public static final String PARAM_ATTENUATIONDISTANCE = "attenuationDistance";
        public static final String PARAM_ATTENUATIONCOLOR = "attenuationColor";
        public static final String PARAM_SHEENCOLOR = "sheenColor";
        public static final String PARAM_SHEENROUGHNESS = "sheenRoughness";
        public static final String PARAM_IRIDESCENCE = "iridescence";
        public static final String PARAM_IRIDESCENCEIOR = "iridescenceIor";
        public static final String PARAM_IRIDESCENCETHICKNESS = "iridescenceThickness";
    }

    public static final class Volume {

        /**
         * BOOL default TRUE, whether the volume is visible
         */
        public static final String PARAM_VISIBLE = "visible";
        public static final String PARAM_VALUE = "value";
        public static final String PARAM_VALUERANGE = "valueRange";
        public static final String PARAM_COLOR = "color";
        public static final String PARAM_OPACITY = "opacity";
        public static final String PARAM_UNITDISTANCE = "unitDistance";
        /**
         * UINT32 default -1u, optional user Id, for frame channel objectId
         * <br>
         * Extension KHR_FRAME_CHANNEL_OBJECT_ID
         */
        public static final String PARAM_ID = "id";
    }

    public static final class SpatialField {

        public static final String PARAM_DATA = "data";
        public static final String PARAM_ORIGIN = "origin";
        public static final String PARAM_SPACING = "spacing";
        public static final String PARAM_FILTER = "filter";
        /**
         * ARRAY1D of FLOAT32_VEC3 array of vertex positions
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_VERTEX_POSITION = "vertex.position";
        /**
         * ARRAY1D of UFIXED8 / FIXED16 / UFIXED16 / FLOAT32 / FLOAT64 array of values at vertices
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_VERTEX_DATA = "vertex.data";
        /**
         * ARRAY1D of UINT32 array of indices into the vertex.* arrays that form cells
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_INDEX = "index";
        /**
         * ARRAY1D of UFIXED8 / FIXED16 / UFIXED16 / FLOAT32 / FLOAT64, alternative specification of the values
         * (if vertex.data is not explicitly set): array of values in cells
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_CELL_DATA = "cell.data";
        /**
         * ARRAY1D of UINT8 array of cell types, where 10 encodes tetrahedral, 12 hexahedral, 13 wedge, and 14 pyramidal cells
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_CELL_TYPE = "cell.type";
        /**
         * ARRAY1D of UINT32 array of indices into the index array, specifying the first (index of the) vertex of each cell
         * <br>
         * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
         */
        public static final String PARAM_CELL_INDEX = "cell.index";
    }


    private Properties() {
    }


}

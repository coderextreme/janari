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

import java.lang.foreign.MemorySegment;
import java.util.function.BiFunction;

/**
 * Materials describe how light interacts with surfaces to give objects their
 * appearance. Most material parameters can be set to a constant, an
 * {@link Sampler}, or a String, in which case the string selects the surface
 * attribute that will be used to source the parameter during rendering.
 *
 * {@snippet lang=c :
 * typedef void* ANARIMaterial;
 * }
 *
 * @author Johann Sorel
 */
public class Material<T extends Material<T>> extends Object<T> {

    public static class SubType<T extends Material> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Matte> MATTE = new SubType<>("matte",Matte::new);
        public static final SubType<PhysicallyBased> PHYSICALLYBASED = new SubType<>("physicallyBased",PhysicallyBased::new);
    }

    public Material(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.MATERIAL, subtype, pointer);
    }

    /**
     * The matte material reflects light uniformly into the hemisphere
     * (Lambertian reflectance), so its apparent brightness is independent of
     * the viewing direction. Supports (partial) cut-out transparency
     * depending on alphaMode.
     * <br>
     * Extension KHR_MATERIAL_MATTE
     */
    public static class Matte extends Material<Matte> {

        public Matte(Device device, MemorySegment pointer) {
            super(device, SubType.MATTE.name, pointer);
        }

        /**
         * Diffuse color. Default: (0.8, 0.8, 0.8).
         */
        public Matte setColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_COLOR, r,g,b);
        }

        /**
         * Diffuse color, sourced from a sampler.
         */
        public Matte setColor(Sampler color) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_COLOR, DataType.SAMPLER, color);
        }

        /**
         * Diffuse color, sourced from the named surface attribute.
         */
        public Matte setColor(String color) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_COLOR, color);
        }

        /**
         * Opacity. Default: 1.0. If color is a sampler or string, the fourth
         * component of the fetched value is multiplied with this value.
         */
        public Matte setOpacity(float opacity) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_OPACITY, opacity);
        }

        /**
         * Opacity, sourced from a sampler.
         */
        public Matte setOpacity(Sampler opacity) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_OPACITY, DataType.SAMPLER, opacity);
        }

        /**
         * Opacity, sourced from the named surface attribute.
         */
        public Matte setOpacity(String opacity) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_OPACITY, opacity);
        }

        /**
         * Controls cut-out transparency, possible values: opaque, blend,
         * mask. Default: opaque.
         */
        public Matte setAlphaMode(String alphaMode) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_ALPHAMODE, alphaMode);
        }

        /**
         * Threshold used when alphaMode is mask. Default: 0.5.
         */
        public Matte setAlphaCutOff(float alphaCutoff) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_ALPHACUTOFF, alphaCutoff);
        }
    }

    /**
     * The physicallyBased material offers a wealth of extensions while being
     * user friendly. It aims to be compatible with glTF's pbrMetallicRoughness
     * material and ratified Khronos material extensions (KHR_materials_specular,
     * KHR_materials_clearcoat, KHR_materials_emissive_strength,
     * KHR_materials_ior, KHR_materials_transmission, KHR_materials_volume,
     * KHR_materials_sheen, and KHR_materials_iridescence).
     * <br>
     * Extension KHR_MATERIAL_PHYSICALLY_BASED
     */
    public static class PhysicallyBased extends Material<PhysicallyBased> {

        public PhysicallyBased(Device device, MemorySegment pointer) {
            super(device, SubType.PHYSICALLYBASED.name, pointer);
        }

        /**
         * Base color. Default: (1.0, 1.0, 1.0).
         */
        public PhysicallyBased setBaseColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_BASECOLOR, r,g,b);
        }

        /**
         * Base color, sourced from a sampler.
         */
        public PhysicallyBased setBaseColor(Sampler color) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_BASECOLOR, DataType.SAMPLER, color);
        }

        /**
         * Base color, sourced from the named surface attribute.
         */
        public PhysicallyBased setBaseColor(String color) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_BASECOLOR, color);
        }

        /**
         * Opacity. Default: 1.0. If baseColor is a sampler or string and
         * alphaMode is not opaque, the fourth component of the fetched value
         * is multiplied with this value.
         */
        public PhysicallyBased setOpacity(float opacity) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_OPACITY, opacity);
        }

        /**
         * Opacity, sourced from a sampler.
         */
        public PhysicallyBased setOpacity(Sampler opacity) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_OPACITY, DataType.SAMPLER, opacity);
        }

        /**
         * Opacity, sourced from the named surface attribute.
         */
        public PhysicallyBased setOpacity(String opacity) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_OPACITY, opacity);
        }

        /**
         * Metalness. Default: 1.0.
         */
        public PhysicallyBased setMetallic(float metallic) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_METALLIC, metallic);
        }

        /**
         * Metalness, sourced from a sampler.
         */
        public PhysicallyBased setMetallic(Sampler metallic) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_METALLIC, DataType.SAMPLER, metallic);
        }

        /**
         * Metalness, sourced from the named surface attribute.
         */
        public PhysicallyBased setMetallic(String metallic) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_METALLIC, metallic);
        }

        /**
         * Roughness. Default: 1.0.
         */
        public PhysicallyBased setRoughness(float roughness) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_ROUGHNESS, roughness);
        }

        /**
         * Roughness, sourced from a sampler.
         */
        public PhysicallyBased setRoughness(Sampler roughness) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_ROUGHNESS, DataType.SAMPLER, roughness);
        }

        /**
         * Roughness, sourced from the named surface attribute.
         */
        public PhysicallyBased setRoughness(String roughness) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_ROUGHNESS, roughness);
        }

        /**
         * Normal map for the base layer. Tangent-space normals: first two
         * components in [-1, 1], third component in [0, 1].
         */
        public PhysicallyBased setNormal(Sampler normal) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_NORMAL, DataType.SAMPLER, normal);
        }

        /**
         * Emissive color. Default: (0.0, 0.0, 0.0).
         */
        public PhysicallyBased setEmissive(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_EMISSIVE, r,g,b);
        }

        /**
         * Emissive color, sourced from a sampler.
         */
        public PhysicallyBased setEmissive(Sampler emissive) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_EMISSIVE, DataType.SAMPLER, emissive);
        }

        /**
         * Emissive color, sourced from the named surface attribute.
         */
        public PhysicallyBased setEmissive(String emissive) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_EMISSIVE, emissive);
        }

        /**
         * Occlusion map; implementations should apply the occlusion
         * attenuation only for otherwise unoccluded ambient lighting.
         */
        public PhysicallyBased setOcclusion(Sampler occlusion) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_OCCLUSION, DataType.SAMPLER, occlusion);
        }

        /**
         * Controls cut-out transparency, possible values: opaque, blend,
         * mask. Default: opaque.
         */
        public PhysicallyBased setAlphaMode(String alphaMode) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_ALPHAMODE, alphaMode);
        }

        /**
         * Threshold used when alphaMode is mask. Default: 0.5.
         */
        public PhysicallyBased setAlphaCutOff(float alphaCutoff) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_ALPHACUTOFF, alphaCutoff);
        }

        /**
         * Strength of the specular reflection. Default: 0.0.
         */
        public PhysicallyBased setSpecular(float specular) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_SPECULAR, specular);
        }

        /**
         * Strength of the specular reflection, sourced from a sampler.
         */
        public PhysicallyBased setSpecular(Sampler specular) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_SPECULAR, DataType.SAMPLER, specular);
        }

        /**
         * Strength of the specular reflection, sourced from the named surface attribute.
         */
        public PhysicallyBased setSpecular(String specular) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_SPECULAR, specular);
        }

        /**
         * Color of the specular reflection at normal incidence. Default: (1.0, 1.0, 1.0).
         */
        public PhysicallyBased setSpecularColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_SPECULARCOLOR, r,g,b);
        }

        /**
         * Color of the specular reflection at normal incidence, sourced from a sampler.
         */
        public PhysicallyBased setSpecularColor(Sampler color) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_SPECULARCOLOR, DataType.SAMPLER, color);
        }

        /**
         * Color of the specular reflection at normal incidence, sourced from
         * the named surface attribute.
         */
        public PhysicallyBased setSpecularColor(String color) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_SPECULARCOLOR, color);
        }

        /**
         * Strength of the clearcoat layer. Default: 0.0.
         */
        public PhysicallyBased setClearcoat(float clearcoat) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_CLEARCOAT, clearcoat);
        }

        /**
         * Strength of the clearcoat layer, sourced from a sampler.
         */
        public PhysicallyBased setClearcoat(Sampler clearcoat) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_CLEARCOAT, DataType.SAMPLER, clearcoat);
        }

        /**
         * Strength of the clearcoat layer, sourced from the named surface attribute.
         */
        public PhysicallyBased setClearcoat(String clearcoat) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_CLEARCOAT, clearcoat);
        }

        /**
         * Roughness of the clearcoat layer. Default: 0.0.
         */
        public PhysicallyBased setClearcoatRoughness(float clearcoatRoughness) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_CLEARCOATROUGHNESS, clearcoatRoughness);
        }

        /**
         * Roughness of the clearcoat layer, sourced from a sampler.
         */
        public PhysicallyBased setClearcoatRoughness(Sampler clearcoatRoughness) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_CLEARCOATROUGHNESS, DataType.SAMPLER, clearcoatRoughness);
        }

        /**
         * Roughness of the clearcoat layer, sourced from the named surface attribute.
         */
        public PhysicallyBased setClearcoatRoughness(String clearcoatRoughness) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_CLEARCOATROUGHNESS, clearcoatRoughness);
        }

        /**
         * Normal map for the clearcoat layer.
         */
        public PhysicallyBased setClearcoatNormal(Sampler clearcoatNormal) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_CLEARCOATNORMAL, DataType.SAMPLER, clearcoatNormal);
        }

        /**
         * Strength of the transmission. Default: 0.0.
         */
        public PhysicallyBased setTransmission(float transmission) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_TRANSMISSION, transmission);
        }

        /**
         * Strength of the transmission, sourced from a sampler.
         */
        public PhysicallyBased setTransmission(Sampler transmission) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_TRANSMISSION, DataType.SAMPLER, transmission);
        }

        /**
         * Strength of the transmission, sourced from the named surface attribute.
         */
        public PhysicallyBased setTransmission(String transmission) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_TRANSMISSION, transmission);
        }

        /**
         * Index of refraction. Default: 1.5.
         */
        public PhysicallyBased setIor(float ior) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_IOR, ior);
        }

        /**
         * Thickness of the volume beneath the surface (with 0 the material is
         * thin-walled). Default: 0.0.
         */
        public PhysicallyBased setThickness(float thickness) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_THICKNESS, thickness);
        }

        /**
         * Thickness of the volume beneath the surface, sourced from a sampler.
         */
        public PhysicallyBased setThickness(Sampler thickness) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_THICKNESS, DataType.SAMPLER, thickness);
        }

        /**
         * Thickness of the volume beneath the surface, sourced from the named
         * surface attribute.
         */
        public PhysicallyBased setThickness(String thickness) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_THICKNESS, thickness);
        }

        /**
         * Average distance that light travels in the medium before
         * interacting with a particle. Default: infinity.
         */
        public PhysicallyBased setAttenuationDistance(float attenuationDistance) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_ATTENUATIONDISTANCE, attenuationDistance);
        }

        /**
         * Color that white light turns into due to absorption when reaching
         * the attenuation distance. Default: (1.0, 1.0, 1.0).
         */
        public PhysicallyBased setAttenuationColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_ATTENUATIONCOLOR, r,g,b);
        }

        /**
         * Sheen color. Default: (0.0, 0.0, 0.0).
         */
        public PhysicallyBased setSheenColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Material.PARAM_SHEENCOLOR, r,g,b);
        }

        /**
         * Sheen color, sourced from a sampler.
         */
        public PhysicallyBased setSheenColor(Sampler sheenColor) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_SHEENCOLOR, DataType.SAMPLER, sheenColor);
        }

        /**
         * Sheen color, sourced from the named surface attribute.
         */
        public PhysicallyBased setSheenColor(String sheenColor) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_SHEENCOLOR, sheenColor);
        }

        /**
         * Sheen roughness. Default: 0.0.
         */
        public PhysicallyBased setSheenRoughness(float sheenRoughness) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_SHEENROUGHNESS, sheenRoughness);
        }

        /**
         * Sheen roughness, sourced from a sampler.
         */
        public PhysicallyBased setSheenRoughness(Sampler sheenRoughness) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_SHEENROUGHNESS, DataType.SAMPLER, sheenRoughness);
        }

        /**
         * Sheen roughness, sourced from the named surface attribute.
         */
        public PhysicallyBased setSheenRoughness(String sheenRoughness) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_SHEENROUGHNESS, sheenRoughness);
        }

        /**
         * Strength of the thin-film (iridescence) layer. Default: 0.0.
         */
        public PhysicallyBased setIridescence(float iridescence) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_IRIDESCENCE, iridescence);
        }

        /**
         * Strength of the thin-film layer, sourced from a sampler.
         */
        public PhysicallyBased setIridescence(Sampler iridescence) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_IRIDESCENCE, DataType.SAMPLER, iridescence);
        }

        /**
         * Strength of the thin-film layer, sourced from the named surface attribute.
         */
        public PhysicallyBased setIridescence(String iridescence) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_IRIDESCENCE, iridescence);
        }

        /**
         * Index of refraction of the thin-film layer. Default: 1.3.
         */
        public PhysicallyBased setIridescenceIor(float iridescenceIor) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_IRIDESCENCEIOR, iridescenceIor);
        }

        /**
         * Thickness of the thin-film layer. Default: 0.0.
         */
        public PhysicallyBased setIridescenceThickness(float iridescenceThickness) throws AnariException, Throwable {
            return setFloat32(Properties.Material.PARAM_IRIDESCENCETHICKNESS, iridescenceThickness);
        }

        /**
         * Thickness of the thin-film layer, sourced from a sampler.
         */
        public PhysicallyBased setIridescenceThickness(Sampler iridescenceThickness) throws AnariException, Throwable {
            return setObject(Properties.Material.PARAM_IRIDESCENCETHICKNESS, DataType.SAMPLER, iridescenceThickness);
        }

        /**
         * Thickness of the thin-film layer, sourced from the named surface attribute.
         */
        public PhysicallyBased setIridescenceThickness(String iridescenceThickness) throws AnariException, Throwable {
            return setString(Properties.Material.PARAM_IRIDESCENCETHICKNESS, iridescenceThickness);
        }
    }
}

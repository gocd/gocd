/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static org.apache.commons.lang3.StringUtils.join;

@Component
public class EntityHashes {
    public static final String SEP_CHAR = "/";

    private static final Gson GSON = new GsonBuilder().
            registerTypeAdapter(ConfigurationProperty.class, Serializers.CONFIGURATION_PROPERTY).
            registerTypeAdapter(EnvironmentVariableConfig.class, Serializers.ENVIRONMENT_VARIABLE).
            registerTypeAdapter(PluginInfo.class, Serializers.PLUGIN_INFO).
            create();

    private final MagicalGoConfigXmlWriter xmlSerializer;

    @Autowired
    public EntityHashes(ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        xmlSerializer = new MagicalGoConfigXmlWriter(configCache, registry);
    }

    private static String digestHex(String data) {
        return sha512_256Hex(data);
    }

    /**
     * Computes a digest of a {@link PartialConfig}.
     *
     * @param partial a {@link PartialConfig} to hash
     * @return a cryptographic digest that can be used for comparison.
     */
    public String digestPartial(PartialConfig partial) {
        if (null == partial) {
            return null;
        }

        return digestMany(
                digestMany(partial.getGroups()),
                digestMany(partial.getEnvironments()),
                digestMany(partial.getScms())
        );
    }

    public String digestMany(String... data) {
        return digestHex(join(data, SEP_CHAR));
    }

    /**
     * Computes a cryptographic digest of a collection's contents
     *
     * @param entities any {@link Collection} of config entities
     * @return a cryptographic hex digest ({@link String})
     */
    public String digestMany(Collection<?> entities) {
        if (null == entities) {
            return null;
        }

        return digestHex(entities.stream().
                map(this::digestDomainConfigEntity).
                collect(Collectors.joining(SEP_CHAR)));
    }

    public String digestDomainConfigEntity(Object entity) {
        return digestHex(serializeDomainEntity(entity));
    }

    public String digestDomainNonConfigEntity(Object entity) {
        return digestHex(GSON.toJson(entity));
    }

    protected String serializeDomainEntity(Object domainObject) {
        return xmlSerializer.toXmlPartial(domainObject);
    }

    private interface Serializers {
        /**
         * Custom serializer for encrypted data to ensure stable JSON output when crypto salt changes
         * over certain requests due to re-encryption
         */
        JsonSerializer<ConfigurationProperty> CONFIGURATION_PROPERTY = (src, typeOfSrc, context) -> {
            final JsonObject result = new JsonObject();
            result.addProperty("key", src.getConfigKeyName());
            result.addProperty("value", sha512_256Hex(src.getValue()));
            return result;
        };

        /**
         * Custom serializer for encrypted data to ensure stable JSON output when crypto salt changes
         * over certain requests due to re-encryption
         */
        JsonSerializer<EnvironmentVariableConfig> ENVIRONMENT_VARIABLE = (src, typeOfSrc, context) -> {
            final JsonObject result = new JsonObject();
            result.addProperty("key", src.getName());
            result.addProperty("value", sha512_256Hex(src.getValue()));
            return result;
        };

        /**
         * Custom serializer to simplify JSON and prevent errors while serializing {@link com.thoughtworks.go.plugin.api.info.PluginDescriptor}
         */
        JsonSerializer<PluginInfo> PLUGIN_INFO = (src, typeOfSrc, context) -> {
            final JsonObject result = new JsonObject();
            result.addProperty("id", src.getDescriptor().id());
            result.addProperty("extension", src.getExtensionName());
            result.add("settings", context.serialize(src.getPluginSettings()));
            return result;
        };
    }
}

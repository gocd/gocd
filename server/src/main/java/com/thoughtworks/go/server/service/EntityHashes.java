/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.server.util.DigestMixin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class EntityHashes implements DigestMixin {
    private static final Gson GSON = new GsonBuilder().
            registerTypeAdapter(ConfigurationProperty.class, Serializers.CONFIGURATION_PROPERTY).
            registerTypeAdapter(EnvironmentVariableConfig.class, Serializers.ENVIRONMENT_VARIABLE).
            registerTypeAdapter(PluginInfo.class, Serializers.PLUGIN_INFO).
            registerTypeAdapter(Modification.class, Serializers.MODIFICATION).
            create();

    private final MagicalGoConfigXmlWriter xmlSerializer;

    @Autowired
    public EntityHashes(ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        xmlSerializer = new MagicalGoConfigXmlWriter(configCache, registry);
    }

    /**
     * Computes a cryptographic digest of a collection's contents
     *
     * @param entities any {@link Collection} of config entities
     * @return a cryptographic hex digest ({@link String})
     */
    public String digest(Collection<?> entities) {
        if (null == entities) {
            return null;
        }

        return digest(entities.stream().
                map(this::digestDomainConfigEntity).
                collect(Collectors.joining(SEP_CHAR)));
    }

    public String digestDomainConfigEntity(Object entity) {
        return digest(serializeDomainEntity(entity));
    }

    public String digestDomainNonConfigEntity(Object entity) {
        return digest(GSON.toJson(entity));
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
            result.addProperty("value", DigestMixin.digestHex(src.getValue()));
            return result;
        };

        /**
         * Custom serializer for encrypted data to ensure stable JSON output when crypto salt changes
         * over certain requests due to re-encryption
         */
        JsonSerializer<EnvironmentVariableConfig> ENVIRONMENT_VARIABLE = (src, typeOfSrc, context) -> {
            final JsonObject result = new JsonObject();
            result.addProperty("key", src.getName());
            result.addProperty("value", DigestMixin.digestHex(src.getValue()));
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

        /**
         * Custom serializer for modifications
         */
        JsonSerializer<Modification> MODIFICATION = (src, typeOfSrc, context) -> {
            final JsonObject result = new JsonObject();
            result.addProperty("username", src.getUserName());
            result.addProperty("email_address", src.getEmailAddress());
            result.addProperty("revision", src.getRevision());
            result.addProperty("comment", src.getComment());
            result.add("modified_time", context.serialize(src.getModifiedTime()));
            return result;
        };
    }
}

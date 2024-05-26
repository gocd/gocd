/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.remote;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.*;
import com.thoughtworks.go.domain.builder.pluggableTask.PluggableTaskBuilder;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialInstance;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.domain.materials.tfs.TfsMaterialInstance;
import com.thoughtworks.go.remote.adapter.RuntimeTypeAdapterFactory;
import com.thoughtworks.go.remote.request.*;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.security.*;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

public class Serialization {

    private static final GoCipher DUMMY_CIPHER = new GoCipher(new DoNotEncrypter());

    private static class SingletonHolder {
        private static final Gson INSTANCE = new GsonBuilder()
            .registerTypeAdapter(Date.class, new IsoInstantDateTypeAdapter())
            .registerTypeAdapter(GoCipher.class, new SecurityRejectingAdapter<GoCipher>())
            .registerTypeAdapter(AESEncrypter.class, new SecurityRejectingAdapter<AESEncrypter>())
            .registerTypeAdapter(AESCipherProvider.class, new SecurityRejectingAdapter<AESCipherProvider>())
            .registerTypeAdapter(DESEncrypter.class, new SecurityRejectingAdapter<DESEncrypter>())
            .registerTypeAdapter(DESCipherProvider.class, new SecurityRejectingAdapter<DESCipherProvider>())
            .registerTypeAdapter(ArtifactStore.class, new ArtifactStoreAdapter())
            .registerTypeAdapter(ConfigurationProperty.class, new ConfigurationPropertyAdapter())
            .registerTypeAdapter(File.class, new FileAdapter())
            .registerTypeHierarchyAdapter(Charset.class, new CharsetAdapter())
            .registerTypeAdapterFactory(builderAdapter())
            .registerTypeAdapterFactory(materialAdapter())
            .registerTypeAdapterFactory(workAdapter())
            .registerTypeAdapterFactory(materialInstanceAdapter())
            .registerTypeAdapterFactory(agentRuntimeInfoAdapter())
            .registerTypeAdapterFactory(fetchHandlerAdapter())
            .registerTypeAdapterFactory(agentRequestAdapter())
            .create();
    }

    public static Gson instance() {
        return SingletonHolder.INSTANCE;
    }

    private static RuntimeTypeAdapterFactory<Builder> builderAdapter() {
        return RuntimeTypeAdapterFactory.of(Builder.class, "type")
                .registerSubtype(BuilderForKillAllChildTask.class, "BuilderForKillAllChildTask")
                .registerSubtype(CommandBuilder.class, "CommandBuilder")
                .registerSubtype(CommandBuilderWithArgList.class, "CommandBuilderWithArgList")
                .registerSubtype(FetchArtifactBuilder.class, "FetchArtifactBuilder")
                .registerSubtype(FetchPluggableArtifactBuilder.class, "FetchPluggableArtifactBuilder")
                .registerSubtype(NullBuilder.class, "NullBuilder")
                .registerSubtype(PluggableTaskBuilder.class, "PluggableTaskBuilder");
    }

    private static RuntimeTypeAdapterFactory<Work> workAdapter() {
        return RuntimeTypeAdapterFactory.of(Work.class, "type")
                .registerSubtype(NoWork.class, "NoWork")
                .registerSubtype(BuildWork.class, "BuildWork")
                .registerSubtype(DeniedAgentWork.class, "DeniedAgentWork")
                .registerSubtype(UnregisteredAgentWork.class, "UnregisteredAgentWork");
    }

    private static RuntimeTypeAdapterFactory<Material> materialAdapter() {
        return RuntimeTypeAdapterFactory.of(Material.class, "type")
                .registerSubtype(DependencyMaterial.class, "DependencyMaterial")
                .registerSubtype(GitMaterial.class, "GitMaterial")
                .registerSubtype(HgMaterial.class, "HgMaterial")
                .registerSubtype(P4Material.class, "P4Material")
                .registerSubtype(PackageMaterial.class, "PackageMaterial")
                .registerSubtype(PluggableSCMMaterial.class, "PluggableSCMMaterial")
                .registerSubtype(SvnMaterial.class, "SvnMaterial")
                .registerSubtype(TfsMaterial.class, "TfsMaterial");
    }

    private static RuntimeTypeAdapterFactory<MaterialInstance> materialInstanceAdapter() {
        return RuntimeTypeAdapterFactory.of(MaterialInstance.class, "type")
                .registerSubtype(DependencyMaterialInstance.class, "DependencyMaterialInstance")
                .registerSubtype(GitMaterialInstance.class, "GitMaterialInstance")
                .registerSubtype(HgMaterialInstance.class, "HgMaterialInstance")
                .registerSubtype(P4MaterialInstance.class, "P4MaterialInstance")
                .registerSubtype(PackageMaterialInstance.class, "PackageMaterialInstance")
                .registerSubtype(PluggableSCMMaterialInstance.class, "PluggableSCMMaterialInstance")
                .registerSubtype(SvnMaterialInstance.class, "SvnMaterialInstance")
                .registerSubtype(TfsMaterialInstance.class, "TfsMaterialInstance");
    }

    private static RuntimeTypeAdapterFactory<AgentRuntimeInfo> agentRuntimeInfoAdapter() {
        return RuntimeTypeAdapterFactory.of(AgentRuntimeInfo.class, "type")
                .registerSubtype(AgentRuntimeInfo.class, "AgentRuntimeInfo")
                .registerSubtype(ElasticAgentRuntimeInfo.class, "ElasticAgentRuntimeInfo");
    }

    private static RuntimeTypeAdapterFactory<FetchHandler> fetchHandlerAdapter() {
        return RuntimeTypeAdapterFactory.of(FetchHandler.class, "type")
                .registerSubtype(ChecksumFileHandler.class, "ChecksumFileHandler")
                .registerSubtype(DirHandler.class, "DirHandler")
                .registerSubtype(FileHandler.class, "FileHandler");
    }

    private static RuntimeTypeAdapterFactory<AgentRequest> agentRequestAdapter() {
        return RuntimeTypeAdapterFactory.of(AgentRequest.class, "type")
                .registerSubtype(GetCookieRequest.class, "GetCookieRequest")
                .registerSubtype(GetWorkRequest.class, "GetWorkRequest")
                .registerSubtype(IsIgnoredRequest.class, "IsIgnoredRequest")
                .registerSubtype(PingRequest.class, "PingRequest")
                .registerSubtype(ReportCompleteStatusRequest.class, "ReportCompleteStatusRequest")
                .registerSubtype(ReportCurrentStatusRequest.class, "ReportCurrentStatusRequest");
    }

    /**
     * Prevents serialization/deserialization of secret-related instances (e.g., {@link GoCipher}) which would leak
     * the AES keys, etc over the network
     */
    private static class SecurityRejectingAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            throw new IllegalArgumentException(format("Refusing to deserialize a %s in the JSON stream!", typeOfT.getTypeName()));
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            throw new IllegalArgumentException(format("Refusing to serialize a %s instance and leak security details!", typeOfSrc.getTypeName()));
        }
    }

    private static class FileAdapter implements JsonSerializer<File>, JsonDeserializer<File> {
        @Override
        public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String path = json.getAsJsonObject().get("path").getAsString();
            return new File(separatorsToSystem(path));
        }

        @Override
        public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            serialized.add("path", new JsonPrimitive(src.getPath()));
            return serialized;
        }
    }

    private static class CharsetAdapter implements JsonSerializer<Charset>, JsonDeserializer<Charset> {
        @Override
        public Charset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Charset.forName(json.getAsString());
        }

        @Override
        public JsonElement serialize(Charset src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    }

    private static class ConfigurationPropertyAdapter implements JsonSerializer<ConfigurationProperty>,
            JsonDeserializer<ConfigurationProperty> {
        @Override
        public JsonElement serialize(ConfigurationProperty src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            serialized.add("key", new JsonPrimitive(src.getConfigKeyName()));
            serialized.add("value", new JsonPrimitive(src.getResolvedValue()));

            return serialized;
        }

        @Override
        public ConfigurationProperty deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new ConfigurationProperty(DUMMY_CIPHER)
                    .withKey(json.getAsJsonObject().get("key").getAsString())
                    .withValue(json.getAsJsonObject().get("value").getAsString());
        }
    }

    private static class ArtifactStoreAdapter implements JsonSerializer<ArtifactStore>, JsonDeserializer<ArtifactStore> {
        @Override
        public JsonElement serialize(ArtifactStore src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("id", new JsonPrimitive(src.getId()));
            jsonObject.add("pluginId", new JsonPrimitive(src.getPluginId()));
            jsonObject.add("configuration", configurations(src));

            return jsonObject;
        }

        private JsonArray configurations(ArtifactStore store) {
            JsonArray jsonArray = new JsonArray();

            store.forEach(configurationProperty -> {
                JsonObject serialized = new JsonObject();
                serialized.add("key", new JsonPrimitive(configurationProperty.getConfigKeyName()));
                serialized.add("value", new JsonPrimitive(configurationProperty.getResolvedValue()));

                jsonArray.add(serialized);
            });
            return jsonArray;
        }

        @Override
        public ArtifactStore deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Configuration configuration = new Configuration();
            json.getAsJsonObject().get("configuration").getAsJsonArray().forEach(el ->
                    configuration.add(new ConfigurationProperty(DUMMY_CIPHER)
                            .withKey(el.getAsJsonObject().get("key").getAsString())
                            .withValue(el.getAsJsonObject().get("value").getAsString())
                    ));

            return new ArtifactStore(json.getAsJsonObject().get("id").getAsString(), json.getAsJsonObject().get("pluginId").getAsString(), configuration);
        }
    }

    /**
     * This ensures the agent will never try to create a new cipher file and also will detect any attempt to decrypt or
     * encrypt
     */
    private static class DoNotEncrypter implements Encrypter {
        @Override
        public boolean canDecrypt(String cipherText) {
            return true; // this is the isAES() check, allows us to throw the intended exception below
        }

        @Override
        public String encrypt(String plainText) throws CryptoException {
            throw new CryptoException("Agents should not be encrypting!");
        }

        @Override
        public String decrypt(String cipherText) throws CryptoException {
            throw new CryptoException("Agents should not be decrypting!");
        }
    }

    private static class IsoInstantDateTypeAdapter extends TypeAdapter<Date> {
        @Override
        public void write(JsonWriter out, Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                out.value(ISO_INSTANT.format(date.toInstant()));
            }
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            try {
                return new Date(ISO_INSTANT.parse(in.nextString(), Instant::from).toEpochMilli());
            } catch (DateTimeParseException e) {
                throw new JsonParseException(e);
            }
        }
    }
}

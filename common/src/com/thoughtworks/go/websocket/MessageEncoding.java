/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.websocket;

import com.google.gson.*;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class MessageEncoding {

    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(AgentRuntimeInfo.class, new AgentRuntimeInfoTypeAdapter()).create();

    public static String encodeWork(Work work) {
        try {
            try (ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectStream = new ObjectOutputStream(binaryOutput)) {
                    objectStream.writeObject(work);
                }
                return Base64.encodeBase64String(binaryOutput.toByteArray());
            }
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    public static Work decodeWork(String data) {
        try {
            byte[] binary = Base64.decodeBase64(data);
            try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(binary))) {
                return (Work) objectStream.readObject();
            }
        } catch (ClassNotFoundException | IOException e) {
            throw bomb(e);
        }
    }

    public static byte[] encodeMessage(Message msg) {
        String encode = gson.toJson(msg);
        try {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
                    out.write(encode.getBytes(StandardCharsets.UTF_8));
                    out.finish();
                }
                return bytes.toByteArray();
            }
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    public static Message decodeMessage(InputStream input) {
        try {
            try (GZIPInputStream zipStream = new GZIPInputStream(input)) {
                String jsonStr = new String(IOUtils.toByteArray(zipStream), StandardCharsets.UTF_8);
                return gson.fromJson(jsonStr, Message.class);
            }
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    public static String encodeData(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T decodeData(String data, Class<T> aClass) {
        return gson.fromJson(data, aClass);
    }

    // todo: Remove hand wrote deserialization after merging ElasticAgentRuntimeInfo class into AgentRuntimeInfo (@wpc)
    private static class AgentRuntimeInfoTypeAdapter implements JsonDeserializer<AgentRuntimeInfo> {
        @Override
        public AgentRuntimeInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            AgentIdentifier identifier = context.deserialize(jsonObject.get("identifier"), AgentIdentifier.class);
            AgentRuntimeStatus runtimeStatus = context.deserialize(jsonObject.get("runtimeStatus"), AgentRuntimeStatus.class);
            AgentBuildingInfo buildingInfo = context.deserialize(jsonObject.get("buildingInfo"), AgentBuildingInfo.class);
            String location = jsonObject.has("location") ? jsonObject.get("location").getAsString() : null;
            Long usableSpace = jsonObject.has("usableSpace") ? jsonObject.get("usableSpace").getAsLong() : null;
            String operatingSystemName = jsonObject.has("operatingSystemName") ? jsonObject.get("operatingSystemName").getAsString() : null;
            String cookie = jsonObject.has("cookie") ? jsonObject.get("cookie").getAsString() : null;
            String agentLauncherVersion = jsonObject.has("agentLauncherVersion") ? jsonObject.get("agentLauncherVersion").getAsString() : null;
            boolean supportsBuildCommandProtocol = jsonObject.has("supportsBuildCommandProtocol") && jsonObject.get("supportsBuildCommandProtocol").getAsBoolean();
            String elasticPluginId = jsonObject.has("elasticPluginId") ? jsonObject.get("elasticPluginId").getAsString() : null;
            String elasticAgentId = jsonObject.has("elasticAgentId") ? jsonObject.get("elasticAgentId").getAsString() : null;

            AgentRuntimeInfo info;
            if (elasticPluginId == null || StringUtil.isBlank(elasticPluginId)) {
                info = new AgentRuntimeInfo(identifier, runtimeStatus, location, cookie, agentLauncherVersion, supportsBuildCommandProtocol);
            } else {
                info = new ElasticAgentRuntimeInfo(identifier, runtimeStatus, location, cookie, agentLauncherVersion, elasticAgentId, elasticPluginId);
            }
            info.setUsableSpace(usableSpace);
            info.setOperatingSystem(operatingSystemName);
            info.setSupportsBuildCommandProtocol(supportsBuildCommandProtocol);
            info.setBuildingInfo(buildingInfo);
            return info;
        }
    }
}

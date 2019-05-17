/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.configrepo.v2;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMigrator;
import com.thoughtworks.go.plugin.access.configrepo.ExportedConfig;
import com.thoughtworks.go.plugin.access.configrepo.JsonMessageHandler;
import com.thoughtworks.go.plugin.access.configrepo.v2.messages.ParseDirectoryMessage;
import com.thoughtworks.go.plugin.access.configrepo.v2.messages.ParseDirectoryResponseMessage;
import com.thoughtworks.go.plugin.access.configrepo.v2.messages.PipelineExportMessage;
import com.thoughtworks.go.plugin.access.configrepo.v2.messages.PipelineExportResponseMessage;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Changes from V1:
 *   - Introduce parse-content (for validation + CLI).
 *   - Introduce get-icon (to show in the UI).
 *   - Introduce pipeline-export call (to allow export/conversion of pipeline back to the original format).
 *   - Introduce get-capabilities call (to tell whether the plugin supports exports and parse content).
 *
 *   - Introduce target_version of 4: Allow display_order_weight at pipeline level.
 */
public class JsonMessageHandler2_0 implements JsonMessageHandler {
    static final int CURRENT_CONTRACT_VERSION = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageHandler2_0.class);
    private final GsonCodec codec;
    private final ConfigRepoMigrator migrator;

    public JsonMessageHandler2_0(GsonCodec gsonCodec, ConfigRepoMigrator configRepoMigrator) {
        codec = gsonCodec;
        migrator = configRepoMigrator;
    }

    @Override
    public Capabilities getCapabilitiesFromResponse(String responseBody) {
        return com.thoughtworks.go.plugin.access.configrepo.v2.models.Capabilities.fromJSON(responseBody).toCapabilities();
    }

    @Override
    public Image getImageResponseFromBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    @Override
    public String requestMessageForPipelineExport(CRPipeline pipeline) {
        PipelineExportMessage requestMessage = new PipelineExportMessage(pipeline);
        return codec.getGson().toJson(requestMessage);
    }

    @Override
    public String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage requestMessage = prepareMessage_1(destinationFolder, configurations);
        return codec.getGson().toJson(requestMessage);
    }

    @Override
    public String requestMessageForParseContent(Map<String, String> contents) {
        return codec.getGson().toJson(Collections.singletonMap("contents", contents));
    }

    private ParseDirectoryMessage prepareMessage_1(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage requestMessage = new ParseDirectoryMessage(destinationFolder);
        for (CRConfigurationProperty conf : configurations) {
            requestMessage.addConfiguration(conf.getKey(), conf.getValue(), conf.getEncryptedValue());
        }
        return requestMessage;
    }

    private ResponseScratch parseResponseForMigration(String responseBody) {
        return new GsonBuilder().create().fromJson(responseBody, ResponseScratch.class);
    }

    @Override
    public ExportedConfig responseMessageForPipelineExport(String responseBody, Map<String, String> headers) {
        PipelineExportResponseMessage response = codec.getGson().fromJson(responseBody, PipelineExportResponseMessage.class);
        return ExportedConfig.from(response.getPipeline(), headers);
    }

    @Override
    public CRParseResult responseMessageForParseDirectory(String responseBody) {
        ErrorCollection errors = new ErrorCollection();
        try {
            ResponseScratch responseMap = parseResponseForMigration(responseBody);
            ParseDirectoryResponseMessage parseDirectoryResponseMessage;

            if (responseMap.target_version == null) {
                errors.addError("Plugin response message", "missing 'target_version' field");
                return new CRParseResult(errors);
            } else if (responseMap.target_version > CURRENT_CONTRACT_VERSION) {
                String message = String.format("'target_version' is %s but the GoCD Server supports %s",
                        responseMap.target_version, CURRENT_CONTRACT_VERSION);
                errors.addError("Plugin response message", message);
                return new CRParseResult(errors);
            } else {
                int version = responseMap.target_version;

                while (version < CURRENT_CONTRACT_VERSION) {
                    version++;
                    responseBody = migrate(responseBody, version);
                }
                // after migration, json should match contract
                parseDirectoryResponseMessage = codec.getGson().fromJson(responseBody, ParseDirectoryResponseMessage.class);
                parseDirectoryResponseMessage.validateResponse(errors);

                errors.addErrors(parseDirectoryResponseMessage.getPluginErrors());

                return new CRParseResult(parseDirectoryResponseMessage.getEnvironments(), parseDirectoryResponseMessage.getPipelines(), errors);
            }
        } catch (Exception ex) {
            StringBuilder builder = new StringBuilder();
            builder.append("Unexpected error when handling plugin response").append('\n');
            builder.append(ex);
            // "location" of error is runtime. This is what user will see in config repo errors list.
            errors.addError("runtime", builder.toString());
            LOGGER.error(builder.toString(), ex);
            return new CRParseResult(errors);
        }
    }

    @Override
    public CRParseResult responseMessageForParseContent(String responseBody) {
        return responseMessageForParseDirectory(responseBody);
    }

    private String migrate(String responseBody, int targetVersion) {
        if (targetVersion > CURRENT_CONTRACT_VERSION)
            throw new RuntimeException(String.format("Migration to %s is not supported", targetVersion));

        return migrator.migrate(responseBody, targetVersion);
    }

    class ResponseScratch {
        public Integer target_version;
    }
}

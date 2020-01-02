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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;

import java.util.Collection;
import java.util.Map;

public interface JsonMessageHandler {
    String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations);

    String requestMessageForParseContent(Map<String, String> contents);

    CRParseResult responseMessageForParseDirectory(String responseBody);

    CRParseResult responseMessageForParseContent(String responseBody);

    String requestMessageForPipelineExport(CRPipeline pipeline);

    ExportedConfig responseMessageForPipelineExport(String responseBody, Map<String, String> headers);

    Capabilities getCapabilitiesFromResponse(String responseBody);

    Image getImageResponseFromBody(String responseBody);

    String requestMessageConfigFiles(String destinationFolder, Collection<CRConfigurationProperty> configurations);

    ConfigFileList responseMessageForConfigFiles(String responseBody);
}

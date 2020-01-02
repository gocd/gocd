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
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;

import java.util.Collection;
import java.util.Map;

/**
 * Specifies contract between server and extension point.
 */
public interface ConfigRepoExtensionContract {

    CRParseResult parseDirectory(String pluginId, final String destinationFolder, final Collection<CRConfigurationProperty> configurations);

    ConfigFileList getConfigFiles(String pluginId, final String destinationFolder, final Collection<CRConfigurationProperty> configurations);

    CRParseResult parseContent(String pluginId, Map<String, String> content);

    ExportedConfig pipelineExport(String pluginId, final CRPipeline pipelineConfig);

    Capabilities getCapabilities(String pluginId);
}

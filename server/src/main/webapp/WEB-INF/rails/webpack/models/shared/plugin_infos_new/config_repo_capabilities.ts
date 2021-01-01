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

import {ConfigRepoCapabilitiesJSON} from "models/shared/plugin_infos_new/serialization";

export class ConfigRepoCapabilities {
  readonly supportsPipelineExport: boolean;
  readonly supportsParseContent: boolean;
  readonly supportsListConfigFiles: boolean;
  readonly supportsUserDefinedProperties: boolean;

  constructor(supportsPipelineExport: boolean, supportsParseContent: boolean, supportsListConfigFiles: boolean, supportsUserDefinedProperties: boolean) {
    this.supportsPipelineExport        = supportsPipelineExport;
    this.supportsParseContent          = supportsParseContent;
    this.supportsListConfigFiles       = supportsListConfigFiles;
    this.supportsUserDefinedProperties = supportsUserDefinedProperties;
  }

  static fromJSON(data: ConfigRepoCapabilitiesJSON): ConfigRepoCapabilities {
    return new ConfigRepoCapabilities(
      data && data.supports_pipeline_export,
      data && data.supports_parse_content,
      data && data.supports_list_config_files,
      data && data.supports_user_defined_properties,
    );
  }
}

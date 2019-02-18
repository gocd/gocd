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

export class ConfigRepoCapabilities {
  readonly supportsPipelineExport: boolean;
  readonly supportsParseContent: boolean;

  constructor(supportsPipelineExport: boolean, supportsParseContent: boolean) {
    this.supportsParseContent   = supportsParseContent;
    this.supportsPipelineExport = supportsPipelineExport;
  }

  static fromJSON(data: any) {
    return new ConfigRepoCapabilities(data.supports_pipeline_export, data.supports_parse_content);
  }
}

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
import {ElasticAgentCapabilitiesJSON} from "models/shared/plugin_infos_new/serialization";

export class ElasticPluginCapabilities {
  readonly supportsPluginStatusReport: boolean;
  readonly supportsAgentStatusReport: boolean;
  readonly supportsClusterStatusReport: boolean;

  constructor(supportsPluginStatusReport: boolean,
              supportsAgentStatusReport: boolean,
              supportsClusterStatusReport: boolean) {
    this.supportsPluginStatusReport  = supportsPluginStatusReport;
    this.supportsAgentStatusReport   = supportsAgentStatusReport;
    this.supportsClusterStatusReport = supportsClusterStatusReport;
  }

  static fromJSON(data: ElasticAgentCapabilitiesJSON) {
    return new ElasticPluginCapabilities(
      data && data.supports_plugin_status_report,
      data && data.supports_agent_status_report,
      data && data.supports_cluster_status_report);
  }
}

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
import Stream from "mithril/stream";
import {mixins as s} from "helpers/string-plus";

export const Capabilities = function (data) {
  this.supportsPluginStatusReport  = Stream(s.defaultToIfBlank(data.supportsPluginStatusReport, false));
  this.supportsAgentStatusReport   = Stream(s.defaultToIfBlank(data.supportsAgentStatusReport, false));
  this.supportsClusterStatusReport = Stream(s.defaultToIfBlank(data.supportsClusterStatusReport, false));
};

Capabilities.fromJSON = (data = {}) => new Capabilities({
  supportsPluginStatusReport:  data && data.supports_plugin_status_report,
  supportsAgentStatusReport:   data && data.supports_agent_status_report,
  supportsClusterStatusReport: data && data.supports_cluster_status_report
});


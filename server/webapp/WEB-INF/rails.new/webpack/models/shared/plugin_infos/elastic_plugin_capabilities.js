/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const Stream = require('mithril/stream');
const s      = require('string-plus');

const Capabilities = function (data) {
  this.supportsStatusReport      = Stream(s.defaultToIfBlank(data.supportsStatusReport, false));
  this.supportsAgentStatusReport = Stream(s.defaultToIfBlank(data.supportsAgentStatusReport, false));
};

Capabilities.fromJSON = (data = {}) => new Capabilities({
  supportsStatusReport:      data && data.supports_status_report,
  supportsAgentStatusReport: data && data.supports_agent_status_report
});

module.exports = Capabilities;

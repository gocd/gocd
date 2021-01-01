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
import {AnalyticsCapabilitiesJSON, AnalyticsCapabilityJSON} from "models/shared/plugin_infos_new/serialization";

export class AnalyticsCapability {
  readonly id: string;
  readonly type: string;
  readonly title: string;

  constructor(id: string, type: string, title: string) {
    this.id    = id;
    this.type  = type;
    this.title = title;
  }

  static fromJSON(data: AnalyticsCapabilityJSON) {
    return new AnalyticsCapability(data.id, data.type, data.title);
  }

}

export class AnalyticsCapabilities {
  readonly supportedAnalytics: AnalyticsCapability[];

  constructor(supportedAnalytics: AnalyticsCapability[]) {
    this.supportedAnalytics = supportedAnalytics;
  }

  static fromJSON(data: AnalyticsCapabilitiesJSON) {
    return new AnalyticsCapabilities(data.supported_analytics.map((it) => AnalyticsCapability.fromJSON(it)));
  }

  dashboardSupport() {
    return this.supportedAnalytics.filter((it) => it.type === "dashboard");
  }

  pipelineSupport() {
    return this.supportedAnalytics.filter((it) => it.type === "pipeline");
  }

  agentSupport() {
    return this.supportedAnalytics.filter((it) => it.type === "agent");
  }

}

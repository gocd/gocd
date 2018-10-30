/*
 * Copyright 2018 ThoughtWorks, Inc.
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

export class AnalyticsCapability {
  readonly id: string;
  //readonly title: string;
  readonly type: string;

  constructor(id: string, type: string) {
    this.id   = id;
    this.type = type;
  }

  static fromJSON(data: any) {
    return new AnalyticsCapability(data.id, data.type);
  }

}

export class AnalyticsCapabilities {
  readonly supportedAnalytics: AnalyticsCapability[];

  constructor(supportedAnalytics: AnalyticsCapability[]) {
    this.supportedAnalytics = supportedAnalytics;
  }

  static fromJSON(data: any) {
    return new AnalyticsCapabilities(data.supported_analytics.map((it: any) => AnalyticsCapability.fromJSON(it)));
  }

  dashboardSupport() {
    return this.supportedAnalytics.filter((it) => it.type === 'dashboard');
  }

  pipelineSupport() {
    return this.supportedAnalytics.filter((it) => it.type === 'pipeline');
  }

  agentSupport() {
    return this.supportedAnalytics.filter((it) => it.type === 'agent');
  }

}

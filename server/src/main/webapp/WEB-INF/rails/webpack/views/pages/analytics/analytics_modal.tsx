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

import _ from "lodash";
import m from "mithril";
import {Agent} from "models/agents/agents";
import {Frame} from "models/shared/analytics_frame";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";
import {Modal, Size} from "views/components/modal";
import {AnalyticsiFrameWidget} from "views/pages/analytics/analytics_iframe_widget";
import {AnalyticsNamespace} from "views/pages/analytics/analytics_namespace";

const PluginEndpoint = require("rails-shared/plugin-endpoint");

type SupportedAnalyticsTypes = Agent;

export abstract class AnalyticsModal<T extends SupportedAnalyticsTypes> extends Modal {
  protected entity: T;
  protected namespace: AnalyticsNamespace;
  private readonly supportedAnalytics: { [key: string]: AnalyticsCapability[] };

  constructor(entity: T,
              supportedAnalytics: { [key: string]: AnalyticsCapability[] },
              namespace: AnalyticsNamespace) {
    super(Size.large);
    this.entity             = entity;
    this.supportedAnalytics = supportedAnalytics;
    this.namespace          = namespace;
  }

  body(): m.Children {
    const models: Frame[] = [];
    _.each(this.supportedAnalytics, (agentAnalytics, pluginId) => {
      _.each(agentAnalytics, (metric: AnalyticsCapability, i: number) => {
        models.push(this.createModel(i, pluginId, metric));
      });
    });

    return models.map((model: Frame) => <AnalyticsiFrameWidget model={model} init={PluginEndpoint.init}/>);
  }

  createModel(index: number, pluginId: string, metric: AnalyticsCapability): Frame {
    const uid   = this.namespace.uid(index, pluginId, metric.type, metric),
          model = this.namespace.modelFor(uid);
    model.url(this.namespace.toUrl(uid, this.getUrlParams()));
    model.pluginId(pluginId);
    model.title(metric.title);
    return model;
  }

  buttons(): m.ChildArray {
    return [];
  }

  protected abstract getUrlParams(): { [key: string]: string | number };
}

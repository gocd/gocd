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

import {SinglePageAppBase} from "helpers/spa_base";
import {AgentsPage} from "views/pages/agents";

import {AjaxPoller} from "helpers/ajax_poller";
import m from "mithril";
import Stream from "mithril/stream";
// @ts-ignore
import {PluginInfos} from "models/shared/plugin_infos";
// @ts-ignore
import {AgentsWidget} from "views/agents/agents_widget";
// @ts-ignore
import {PageLoadError} from "views/shared/page_load_error";

const AgentsVM  = require("views/agents/models/agents_widget_view_model").VM;
const Agents    = require("models/agents/agents").Agents;
const SortOrder = require("views/agents/models/route_handler").SortOrder;

export class AgentsSPA extends SinglePageAppBase {
  constructor() {
    super(AgentsPage);
  }
}

$(() => {
  const agentsContainer = $("#agents");

  if (agentsContainer.get().length === 0) {
    return new AgentsSPA();
  }

  const isUserAdmin             = JSON.parse(agentsContainer.attr("data-is-current-user-an-admin")!);
  const shouldShowAnalyticsIcon = JSON.parse(agentsContainer.attr("data-should-show-analytics-icon")!);

  function createRepeater() {
    // @ts-ignore
    return new AjaxPoller((xhrCB) => Agents.all(xhrCB)
                                           .then((agentsData: any) => {
                                             agents(agentsData);
                                             agentsViewModel.initializeWith(agentsData);
                                             permanentMessage({});
                                           })
                                           .fail((errMsg: string) => {
                                             permanentMessage({type: "alert", message: errMsg});
                                           })
                                           .always(() => {
                                             showSpinner(false);
                                           }));
  }

  const agents           = Stream(new Agents());
  const showSpinner      = Stream(true);
  const sortOrder        = Stream(new SortOrder());
  const agentsViewModel  = new AgentsVM(sortOrder().searchText);
  const permanentMessage = Stream({});
  const currentRepeater  = Stream(createRepeater());

  const onPluginsInfoApiSuccess = (pluginInfos: any) => {
    const EAPluginInfos  = pluginInfos.filterByType("elastic-agent");
    const allPluginInfos = pluginInfos.filterByType("analytics");

    EAPluginInfos.eachPluginInfo((pluginInfo: any) => {
      allPluginInfos.addPluginInfo(pluginInfo);
    });

    const component = {
      view() {
        return <AgentsWidget vm={agentsViewModel} allAgents={agents}
                             isUserAdmin={isUserAdmin}
                             permanentMessage={permanentMessage}
                             showSpinner={showSpinner}
                             sortOrder={sortOrder}
                             shouldShowAnalyticsIcon={shouldShowAnalyticsIcon}
                             pluginInfos={typeof pluginInfos === "string" ? Stream() : Stream(allPluginInfos)}
                             doCancelPolling={() => currentRepeater().stop()}
                             doRefreshImmediately={() => {
                               currentRepeater().stop();
                               currentRepeater().start();
                             }}
        />;
      }
    };

    m.route(agentsContainer.get(0), "/", {
      "/": component,
      "/:sortBy/:orderBy": component,
      "/:sortBy/:orderBy/": component,
      "/:sortBy/:orderBy/:query": component
    });

    currentRepeater().start();

    sortOrder().initialize();
  };

  const onPluginInfoApiFailure = (response: string) => {
    m.mount(agentsContainer.get(0), {
      view() {
        return (<PageLoadError message={response}/>);
      }
    });
  };

  PluginInfos.all().then(onPluginsInfoApiSuccess, onPluginInfoApiFailure);
});

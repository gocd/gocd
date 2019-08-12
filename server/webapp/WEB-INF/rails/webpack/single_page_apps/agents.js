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
import $ from "jquery";
import m from "mithril";
import Stream from "mithril/stream";
import {Agents} from "models/agents/agents";
import {AgentsWidget} from "views/agents/agents_widget";
import {PageLoadError} from "views/shared/page_load_error";
import {VM as AgentsVM} from "views/agents/models/agents_widget_view_model";
import {SortOrder} from "views/agents/models/route_handler";
import {PluginInfos} from "models/shared/plugin_infos";
import {AjaxPoller} from "helpers/ajax_poller";

$(() => {
  const $agentElem = $('#agents');

  const isUserAdmin             = JSON.parse($agentElem.attr('data-is-current-user-an-admin'));
  const shouldShowAnalyticsIcon = JSON.parse($agentElem.attr('data-should-show-analytics-icon'));

  function createRepeater() {
    return new AjaxPoller((xhrCB) => Agents.all(xhrCB)
      .then((agentsData) => {
        agents(agentsData);
        agentsViewModel.initializeWith(agentsData);
        permanentMessage({});
      })
      .fail((errMsg) => {
        permanentMessage({type: 'alert', message: errMsg});
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

  const onPluginsInfoApiSuccess = (pluginInfos) => {
    const EAPluginInfos  = pluginInfos.filterByType('elastic-agent');
    const allPluginInfos = pluginInfos.filterByType('analytics');

    EAPluginInfos.eachPluginInfo((pluginInfo) => {
      allPluginInfos.addPluginInfo(pluginInfo);
    });

    const component = {
      view() {
        return m(AgentsWidget, {
          vm:                   agentsViewModel,
          allAgents:            agents,
          isUserAdmin,
          permanentMessage,
          showSpinner,
          sortOrder,
          shouldShowAnalyticsIcon,
          pluginInfos:          typeof pluginInfos === "string" ? Stream() : Stream(allPluginInfos),
          doCancelPolling:      () => currentRepeater().stop(),
          doRefreshImmediately: () => {
            currentRepeater().stop();
            currentRepeater().start();
          }
        });
      }
    };

    m.route($agentElem.get(0), '', {
      '':                         component,
      '/:sortBy/:orderBy':        component,
      '/:sortBy/:orderBy/':       component,
      '/:sortBy/:orderBy/:query': component
    });

    currentRepeater().start();

    sortOrder().initialize();
  };

  const onPluginInfoApiFailure = (response) => {
    m.mount($agentElem.get(0), {
      view() {
        return (<PageLoadError message={response}/>);
      }
    });
  };

  PluginInfos.all().then(onPluginsInfoApiSuccess, onPluginInfoApiFailure);
});


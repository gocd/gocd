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

const $              = require('jquery');
const m              = require('mithril');
const Stream         = require('mithril/stream');
const Agents         = require('models/agents/agents');
const AgentsWidget   = require('views/agents/agents_widget');
const AgentsVM       = require('views/agents/models/agents_widget_view_model');
const SortOrder      = require('views/agents/models/sort_order');
const VersionUpdater = require('models/shared/version_updater');
const PluginInfos    = require('models/shared/plugin_infos');
const AjaxPoller     = require('helpers/ajax_poller');

require('foundation-sites');

$(() => {
  new VersionUpdater().update();
  const $agentElem = $('#agents');

  const isUserAdmin = JSON.parse($agentElem.attr('data-is-current-user-an-admin'));

  $(document).foundation();

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
  const agentsViewModel  = new AgentsVM();
  const permanentMessage = Stream({});
  const currentRepeater  = Stream(createRepeater());
  const sortOrder        = Stream(new SortOrder());

  const onResponse        = (pluginInfos) => {
    const component = {
      view() {
        return m(AgentsWidget, {
          vm:                   agentsViewModel,
          allAgents:            agents,
          isUserAdmin,
          permanentMessage,
          showSpinner,
          sortOrder,
          pluginInfos: typeof pluginInfos === "string" ? Stream() : Stream(pluginInfos.filterByType('elastic-agent')),
          doCancelPolling:      () => currentRepeater().stop(),
          doRefreshImmediately: () => {
            currentRepeater().stop();
            currentRepeater(createRepeater().start());
          }
        });
      }
    };

    m.route($agentElem.get(0), '', {
      '':                  component,
      '/:sortBy/:orderBy': component
    });

    currentRepeater().start();

    sortOrder().initialize();
  };

  PluginInfos.all().then(onResponse, onResponse);
})
;


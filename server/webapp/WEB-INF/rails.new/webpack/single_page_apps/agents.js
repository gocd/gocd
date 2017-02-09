/*
 * Copyright 2016 ThoughtWorks, Inc.
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

var $              = require('jquery');
var m              = require('mithril');
var Stream         = require('mithril/stream');
var Agents         = require('models/agents/agents');
var AgentsWidget   = require('views/agents/agents_widget');
var AgentsVM       = require('views/agents/models/agents_widget_view_model');
var SortOrder      = require('views/agents/models/sort_order');
var VersionUpdater = require('models/shared/version_updater');
var AjaxPoller     = require('helpers/ajax_poller');

require('foundation-sites');

$(function () {
  new VersionUpdater().update();
  var $agentElem = $('#agents');

  var isUserAdmin = JSON.parse($agentElem.attr('data-is-current-user-an-admin'));

  $(document).foundation();

  function createRepeater() {
    return new AjaxPoller(function (xhrCB) {
      return Agents.all(xhrCB)
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
        });
    });
  }

  var agents           = Stream(new Agents());
  var showSpinner      = Stream(true);
  var agentsViewModel  = new AgentsVM();
  var permanentMessage = Stream({});
  var currentRepeater  = Stream(createRepeater());
  var sortOrder        = Stream(new SortOrder());

  var component = {
    view: function () {
      return m(AgentsWidget, {
        vm:                   agentsViewModel,
        allAgents:            agents,
        isUserAdmin:          isUserAdmin,
        permanentMessage:     permanentMessage,
        showSpinner:          showSpinner,
        sortOrder:            sortOrder,
        doCancelPolling:      () => currentRepeater().stop(),
        doRefreshImmediately: () => {
          currentRepeater().stop();
          currentRepeater(createRepeater().start());
        }
      });
    }
  };

  currentRepeater().start();

  m.route($agentElem.get(0), '', {
    '':                  component,
    '/:sortBy/:orderBy': component
  });
});


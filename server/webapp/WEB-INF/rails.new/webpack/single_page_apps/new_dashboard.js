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

const $               = require('jquery');
const m               = require('mithril');
const Stream          = require('mithril/stream');
const DashboardVM     = require('views/dashboard/models/dashboard_view_model');
const Dashboard       = require('models/dashboard/dashboard');
const DashboardWidget = require('views/dashboard/dashboard_widget');

const VersionUpdater = require('models/shared/version_updater');
const AjaxPoller     = require('helpers/ajax_poller');

require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  new VersionUpdater().update();
  const dashboardElem = $('#dashboard');

  const dashboardVM            = new DashboardVM();
  const isQuickEditPageEnabled = JSON.parse(dashboardElem.attr('data-is-quick-edit-page-enabled'));

  $(document).foundation();

  let dashboard;

  function onResponse(dashboardData) {
    dashboard = Dashboard.fromJSON(dashboardData);
    dashboardVM.initialize(dashboard.allPipelineNames());
  }

  function createRepeater() {
    return new AjaxPoller(() => Dashboard.get()
      .then(onResponse));
  }

  const repeater = Stream(createRepeater());

  const renderView = () => {
    const component = {
      view() {
        return m(DashboardWidget, {
          dashboard,
          isQuickEditPageEnabled,
          vm:                   dashboardVM,
          doCancelPolling:      () => repeater().stop(),
          doRefreshImmediately: () => {
            repeater().stop();
            repeater().start();
          }
        });
      }
    };

    m.mount($("#dashboard").get(0), component);
  };

  Dashboard.get().then(onResponse).then(repeater().start).then(renderView);
});

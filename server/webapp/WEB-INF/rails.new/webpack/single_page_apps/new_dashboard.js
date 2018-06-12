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
const PluginInfos     = require('models/shared/plugin_infos');

const VersionUpdater = require('models/shared/version_updater');
const AjaxPoller     = require('helpers/ajax_poller');

require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  new VersionUpdater().update();
  const dashboardElem              = $('#dashboard');

  const dashboardVM                = new DashboardVM();
  const isQuickEditPageEnabled     = JSON.parse(dashboardElem.attr('data-is-quick-edit-page-enabled'));
  const shouldShowAnalyticsIcon    = JSON.parse(dashboardElem.attr('data-should-show-analytics-icon'));
  const pluginsSupportingAnalytics = {};

  $(document).foundation();

  const dashboard = new Dashboard();

  function onResponse(dashboardData, message = undefined) {
    dashboard.initialize(dashboardData);
    dashboard.message(message);
  }

  function createRepeater() {
    const onsuccess = (data, _textStatus, jqXHR) => {
      if (jqXHR.status === 202) {
        const message = {
          type:    "info",
          content: data.message
        };
        onResponse({}, message);
        return;
      }
      onResponse(data);
    };
    const onerror   = (_jqXHR, textStatus, errorThrown) => {
      if (textStatus === 'parsererror') {
        const message = {
          type:    "alert",
          content: "Error occurred while parsing dashboard API response. Check server logs for more information."
        };
        onResponse({}, message);
        console.error(errorThrown); // eslint-disable-line no-console
        return;
      }

      const message = {
        type:    'warning',
        content: 'There was an unknown error ',
      };
      onResponse({}, message);
      return;
    };

    return new AjaxPoller(() => Dashboard.get()
      .then(onsuccess, onerror)
      .always(() => {
        showSpinner(false);
      }));
  }

  const repeater          = Stream(createRepeater());
  const showSpinner       = Stream(true);

  const renderView = () => {
    const component = {
      view() {
        return m(DashboardWidget, {
          dashboard,
          showSpinner,
          isQuickEditPageEnabled,
          pluginsSupportingAnalytics,
          shouldShowAnalyticsIcon,
          vm:                   dashboardVM,
          doCancelPolling:      () => repeater().stop(),
          doRefreshImmediately: () => {
            repeater().stop();
            repeater().start();
          }
        });
      }
    };

    m.route($("#dashboard").get(0), '', {
      '':             component,
      '/:searchedBy': component
    });

    dashboard.searchText(m.route.param('searchedBy') || '');
  };

  repeater().start();

  const onPluginInfosResponse = (pluginInfos) => {
    pluginInfos.eachPluginInfo((pluginInfo) => {
      const supportedPipelineAnalytics = pluginInfo.extensions().analytics.capabilities().supportedPipelineAnalytics();
      if(supportedPipelineAnalytics.length > 0) {
        pluginsSupportingAnalytics[pluginInfo.id()] = supportedPipelineAnalytics[0].id;
      }
    });

    renderView();
  };

  PluginInfos.all(null, {type: 'analytics'}).then(onPluginInfosResponse, onPluginInfosResponse);
});

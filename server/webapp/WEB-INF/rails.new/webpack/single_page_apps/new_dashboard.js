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
const AjaxPoller      = require('helpers/ajax_poller');
const PageLoadError   = require('views/shared/page_load_error');

const PersonalizeVM   = require('views/dashboard/models/personalization_vm');
const Personalization = require('models/dashboard/personalization');

$(() => {
  const dashboardElem              = $('#dashboard');
  const dashboardVM                = new DashboardVM();
  const isQuickEditPageEnabled     = JSON.parse(dashboardElem.attr('data-is-quick-edit-page-enabled'));
  const shouldShowAnalyticsIcon    = JSON.parse(dashboardElem.attr('data-should-show-analytics-icon'));
  const pluginsSupportingAnalytics = {};

  const dashboard = new Dashboard();
  const personalizeVM = new PersonalizeVM(currentView);

  let personalization;

  Personalization.get().then((selection) => {
    personalizeVM.names(selection.names());
    personalization = selection;
  });

  function currentView(viewName) { // a stream-like object/function
    if (viewName) {
      window.location.search = m.buildQueryString({ viewName });
      repeater().restart();
      return;
    }
    return $.trim(m.parseQueryString(window.location.search).viewName) || "Default";
  }

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
    const onerror   = (jqXHR, textStatus, errorThrown) => {
      if (textStatus === 'parsererror') {
        const message = {
          type:    "alert",
          content: "Error occurred while parsing dashboard API response. Check server logs for more information."
        };
        onResponse({}, message);
        console.error(errorThrown); // eslint-disable-line no-console
        return;
      }

      if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
        const message = {
          type:    'warning',
          content: jqXHR.responseJSON.message,
        };
        onResponse({}, message);
        return;
      }

      const message = {
        type:    'warning',
        content: 'There was an unknown error ',
      };
      onResponse({}, message);
    };

    return new AjaxPoller(() => Dashboard.get(currentView())
      .then(onsuccess, onerror)
      .always(() => {
        showSpinner(false);
      }));
  }

  const repeater    = Stream(createRepeater());
  const showSpinner = Stream(true);

  const renderView = () => {
    const component = {
      view() {
        return m(DashboardWidget, {
          dashboard,
          personalizeVM,
          personalization,
          showSpinner,
          isQuickEditPageEnabled,
          pluginsSupportingAnalytics,
          shouldShowAnalyticsIcon,
          vm:                   dashboardVM,
          doCancelPolling:      () => repeater().stop(),
          doRefreshImmediately: () => repeater().restart()
        });
      }
    };

    m.route(dashboardElem.get(0), '', {
      '':             component,
      '/:searchedBy': component
    });

    dashboard.searchText(m.route.param('searchedBy') || '');
  };

  repeater().start();

  const onPluginInfosResponse = (pluginInfos) => {
    pluginInfos.eachPluginInfo((pluginInfo) => {
      const supportedPipelineAnalytics = pluginInfo.extensions().analytics.capabilities().supportedPipelineAnalytics();
      if (supportedPipelineAnalytics.length > 0) {
        pluginsSupportingAnalytics[pluginInfo.id()] = supportedPipelineAnalytics[0].id;
      }
    });

    renderView();
  };

  const onPluginInfoApiFailure = (response) => {
    m.mount(dashboardElem.get(0), {
      view() {
        return (<PageLoadError message={response}/>);
      }
    });
  };

  PluginInfos.all(null, {type: 'analytics'}).then(onPluginInfosResponse, onPluginInfoApiFailure);
});

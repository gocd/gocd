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
const AnalyticsWidget = require('views/analytics/analytics_widget');
const PluginInfos     = require('models/shared/plugin_infos');
const VersionUpdater  = require('models/shared/version_updater');
const PluginEndpoint  = require('rails-shared/plugin-endpoint');
require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  $(document).foundation();
  new VersionUpdater().update();

  PluginEndpoint.ensure("v1");
  const analyticsElem = $('.analytics-container');
  const pipelines     = JSON.parse(analyticsElem.attr('data-pipeline-list'));

  const onSuccess = (pluginInfos) => {
    const plugins = pluginInfos.mapPluginInfos((p) => p.id());
    const metrics = {};
    pluginInfos.eachPluginInfo((p) => {
      metrics[p.id()] = p.extensions().analytics.capabilities().supportedAnalyticsDashboardMetrics();
    });

    const component = {
      view() {
        return m(AnalyticsWidget, {
          pluginInfos: Stream(pluginInfos),
          metrics,
          pipelines,
          plugins
        });
      }
    };

    m.mount(analyticsElem.get(0), component);
  };

  const onFailure = () => {
    analyticsElem.html($('<div class="alert callout">')
      .append('<h5>There was a problem fetching the analytics</h5>')
      .append('<p>Refresh <a href="javascript: window.location.reload()">this page</a> in some time, and if the problem persists, check the server logs.</p>')
    );
  };

  PluginInfos.all(null, {type: 'analytics'}).then(onSuccess, onFailure);
});

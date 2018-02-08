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

(function() {
  "use strict";

  const m = require("mithril");
  const $ = require("jquery");

  require("foundation-sites");
  require('helpers/server_health_messages_helper');

  const PluginEndpoint           = require('rails-shared/plugin-endpoint');
  const VersionUpdater           = require('models/shared/version_updater');
  const Frame                    = require('models/analytics/frame');
  const AnalyticsDashboardHeader = require('views/analytics/header');
  const PluginiFrameWidget       = require('views/analytics/plugin_iframe_widget');
  const Routes                   = require('gen/js-routes');

  const models = {};

  PluginEndpoint.ensure();

  PluginEndpoint.define({
    "analytics.pipeline": (message, reply) => { // eslint-disable-line no-unused-vars
      const model = models[message.uid];
      model.url(Routes.pipelineAnalyticsPath({plugin_id: message.pluginId, pipeline_name: message.data.pipelineName})); // eslint-disable-line camelcase
      model.load();
    }
  });

  document.addEventListener("DOMContentLoaded", () => {
    const main = document.querySelector("[data-supported-dashboard-metrics]");

    m.mount(main, {
      view() {
        const frames = [];
        frames.push(m(AnalyticsDashboardHeader));
        $.each($(main).data("supported-dashboard-metrics"), (pluginId, metrics) => {
          $.each(metrics, (idx, metric) => {
            const uid = `f-${pluginId}:${metric}:${idx}`;

            let model = models[uid];
            if (!model) {
              model = models[uid] = new Frame(m.redraw);
              model.url(Routes.dashboardAnalyticsPath({plugin_id: pluginId, metric})); // eslint-disable-line camelcase
            }
            frames.push(m(PluginiFrameWidget, {model: models[uid], pluginId, uid}));
          });
        });
        return frames;
      }
    });

    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });
})();

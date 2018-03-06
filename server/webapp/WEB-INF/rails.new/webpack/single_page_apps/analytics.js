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
  const MetricType               = require('models/analytics/metric_type');
  const Tabs                     = require('models/analytics/tabs');
  const AnalyticsDashboardHeader = require('views/analytics/header');
  const DashboardTabs            = require('views/analytics/tabs');
  const GlobalMetrics            = require('views/analytics/global_metrics');
  const PipelineMetrics          = require('views/analytics/pipeline_metrics');

  PluginEndpoint.ensure();

  document.addEventListener("DOMContentLoaded", () => {
    const main = document.querySelector("[data-supported-dashboard-metrics]");

    m.mount(main, {
      view() {
        const pageItems = [];
        const tabs = new Tabs(m.redraw);
        pageItems.push(m(AnalyticsDashboardHeader));
        tabs.push(new MetricType("Global", GlobalMetrics, $(main).data("supported-dashboard-metrics")));
        tabs.push(new MetricType("Pipeline", PipelineMetrics, {pipelines: $(main).data("pipeline-list"), plugins: $(main).data("supported-dashboard-metrics")}));
        pageItems.push(m(DashboardTabs, {tabs}));
        return pageItems;
      }
    });

    // boilerplate to init menus and check for updates
    $(document).foundation();
    new VersionUpdater().update();
  });
})();

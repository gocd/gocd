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
describe("Analytics Dashboard Tabs", () => {
  const m      = require('mithril');
  const $ = require('jquery');
  require('jasmine-jquery');

  const DashboardTabs = require('views/analytics/tabs');
  const GlobalMetrics = require('views/analytics/global_metrics');
  const PipelineMetrics = require('views/analytics/pipeline_metrics');
  const Tabs = require('models/analytics/tabs');
  const MetricType = require('models/analytics/metric_type');

  let $root, root;

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    unmount();
    window.destroyDomElementForTest();
  });

  it('should display given tabs', () => {
    const tabs = new Tabs(m.redraw);
    tabs.push(new MetricType("Global", GlobalMetrics, []));
    tabs.push(new MetricType("Pipeline", PipelineMetrics, {pipelines: [], plugins: []}));

    mount(tabs);
    expect($root.find(".dashboard-tabs li.current").text()).toBe("Global");
    expect($($root.find(".dashboard-tabs li")[1]).text()).toBe("Pipeline");
  });

  it('should change tabs when clicking inactive tab', () => {
    const tabs = new Tabs(m.redraw);
    tabs.push(new MetricType("Global", GlobalMetrics, []));
    tabs.push(new MetricType("Pipeline", PipelineMetrics, {pipelines: [], plugins: []}));

    mount(tabs);
    expect($root.find(".dashboard-tabs li.current").text()).toBe("Global");
    $($root.find(".dashboard-tabs li")[1]).click();
    expect($root.find(".dashboard-tabs li.current").text()).toBe("Pipeline");
  });

  const mount = (tabs) => {
    m.mount(root, {
      view() {
        return m(DashboardTabs, {tabs});
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});

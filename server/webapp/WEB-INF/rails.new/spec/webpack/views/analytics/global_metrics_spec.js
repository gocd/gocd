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
describe("Global Dashboard Metrics", () => {
  const m      = require('mithril');
  require('jasmine-jquery');

  const GlobalMetrics = require('views/analytics/global_metrics');

  function globalMetricTab(metrics) {
    const data = metrics;

    return {data};
  }

  let $root, root;
  const supportedMetrics = {
    "plugin-id-x": ["one", "two"],
    "plugin-id-y": ["three"]
  };

  beforeEach(() => {
    jasmine.Ajax.install();
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    unmount();
    window.destroyDomElementForTest();
  });

  it('Add a frame for each plugin metric', () => {
    jasmine.Ajax.stubRequest("/analytics/plugin-id-x/dashboard/one", undefined, 'GET').andReturn({ status: 200 });
    jasmine.Ajax.stubRequest("/analytics/plugin-id-x/dashboard/two", undefined, 'GET').andReturn({ status: 200 });
    jasmine.Ajax.stubRequest("/analytics/plugin-id-y/dashboard/three", undefined, 'GET').andReturn({ status: 200 });
    const model = globalMetricTab(supportedMetrics);

    mount(model);
    expect($root.find("iframe").length).toBe(3);
  });

  const mount = (model) => {
    m.mount(root, {
      view() {
        return m(GlobalMetrics, {model});
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});

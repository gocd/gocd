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
describe("Pipeline Dashboard Metrics", () => {
  const m      = require('mithril');
  require('jasmine-jquery');
  const $ = require("jquery");

  const PipelineMetrics = require('views/analytics/pipeline_metrics');

  let $root, root;
  const supportedMetrics = ["plugin-id-x", "plugin-id-y"];
  const pipelineList = ["p1", "p2", "p3"];

  beforeEach(() => {
    jasmine.Ajax.install();
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    unmount();
    window.destroyDomElementForTest();
  });

  it('should have a dropdown with each pipeline', () => {
    mount(pipelineList, supportedMetrics);
    const list = $root.find("select option");
    expect(list.length).toBe(3);
    expect($(list[0]).val()).toBe("p1");
    expect($(list[1]).val()).toBe("p2");
    expect($(list[2]).val()).toBe("p3");
  });

  it('Add a frame for each plugin', () => {
   mount(pipelineList, supportedMetrics);

    expect($root.find("iframe").length).toBe(2);

    const requests = jasmine.Ajax.requests;
    expect(requests.count()).toBe(2);
    expect(requests.at(0).url).toBe('/go/analytics/plugin-id-x/dashboard/pipeline_duration?pipeline_name=p1&context=dashboard');
    expect(requests.at(1).url).toBe('/go/analytics/plugin-id-y/dashboard/pipeline_duration?pipeline_name=p1&context=dashboard');
  });

  it('should change displayed graphs when new pipeline is selected', () => {
     mount(pipelineList, supportedMetrics);

    $root.find("select").val("p2").trigger("change");
    const requests = jasmine.Ajax.requests;
    expect(requests.count()).toBe(4);
    expect(requests.at(2).url).toBe('/go/analytics/plugin-id-x/dashboard/pipeline_duration?pipeline_name=p2&context=dashboard');
    expect(requests.at(3).url).toBe('/go/analytics/plugin-id-y/dashboard/pipeline_duration?pipeline_name=p2&context=dashboard');
  });

  const mount = (pipelines, plugins) => {
    m.mount(root, {
      view() {
        return m(PipelineMetrics, {
          pipelines,
          plugins
        });
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };
});

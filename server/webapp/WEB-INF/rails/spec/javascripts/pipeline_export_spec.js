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


describe("PipelineExport", function () {
  var c = crel, panel, link;
  var TEST_URL = "http://export.me";

  beforeEach(function setup() {
    panel = c("div"), link = c("a", {href: TEST_URL}, "click me");

    spyOn(PipelineExport, "plugins").and.returnValue([
      {id: "plug.1", about: {name: "plugin 1"}},
      {id: "plug.2", about: {name: "plugin 2"}}
    ]);
  });

  it("filterPlugins() returns only active plugins that support pipeline export", function() {
    var plugins = PipelineExport.filterPlugins(testPluginInfos)

    expect(plugins.length).toBe(1);
    expect(plugins[0].id).toBe("pos.test.id");
  });

  it("showExportOptions() displays plugin choices", function() {
    PipelineExport.showExportOptions(panel, link);

    var choices = panel.querySelectorAll("li a");

    expect(choices.length).toBe(2);
    expect(choices[0].textContent).toBe("plugin 1");
    expect(choices[0].getAttribute("data-plugin-id")).toBe("plug.1");
    expect(choices[1].textContent).toBe("plugin 2");
    expect(choices[1].getAttribute("data-plugin-id")).toBe("plug.2");
  });

  it("clicking on a plugin link causes a download", function(done) {
    if ("function" !== typeof Promise) {
      pending();
      return done();
    }

    PipelineExport.showExportOptions(panel, link);
    var choice = panel.querySelectorAll("li a")[0];

    jasmine.Ajax.withMock(function () {
      jasmine.Ajax.stubRequest(TEST_URL + "?plugin_id=" + choice.getAttribute("data-plugin-id"), undefined, "GET").andReturn({
        response: "content here",
        responseHeaders: {
          "Content-Type": "text/plain",
          "Content-Disposition": "attachment; filename=\"pipeline.txt\""
        },

        status:   200
      });

      spyOn(URL, "createObjectURL").and.returnValue("blob:uuid-of-pipeline-content");

      var download = spyOn(PipelineExport, "downloadAsFile").and.callFake(function (url, name) {
        expect(url).toBe("blob:uuid-of-pipeline-content");
        expect(name).toBe("pipeline.txt");
        done();
      });

      choice.click();
    });
  });

  var createPluginInfo = function(pluginId, stat, supportsExport) {
    return {
      id: pluginId,
      status: { state: stat },
      extensions: [{ type: "configrepo", capabilities: { supports_pipeline_export: supportsExport }}]
    }
  }

  var testPluginInfos = [createPluginInfo("pos.test.id", "active", true),
  createPluginInfo("invalid.id", "invalid", true),
  createPluginInfo("not.support.id", "active", false)]
});

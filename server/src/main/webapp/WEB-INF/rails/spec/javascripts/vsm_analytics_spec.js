/*
 * Copyright 2021 ThoughtWorks, Inc.
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
describe("vsm_analytics", function () {

  function VSMRenderer() {}

  AnalyticsPanel = {
    update: function downstream(source, destination, isSourceCurrent) {}
  };

  describe("selectPipeline", function () {
    it("should have current pipeline as source if a downstream pipeline is selected", function () {
      var graph = jQuery.extend({ "current_pipeline": "P3" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectPipeline("P4", 3);

      expect(vsmAnalytics.jsonData()["source"]).toBe("P3");
      expect(vsmAnalytics.jsonData()["destination"]).toBe("P4");
      expect(_.isEqual(AnalyticsPanel.update.calls.mostRecent().args, ["P3", "P4", true])).toBe(true);
    });

    it("should have current_pipeline as destination if an upstream node is selected", function () {
      var graph = jQuery.extend({ "current_pipeline": "P3" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectPipeline("P1", 1);

      expect(vsmAnalytics.jsonData()["source"]).toBe("P1");
      expect(vsmAnalytics.jsonData()["destination"]).toBe("P3");
      expect(_.isEqual(AnalyticsPanel.update.calls.mostRecent().args, ["P1", "P3", false])).toBe(true);
    });

    it("should have current_material as source and downstream pipeline as destination", function () {
      var graph = jQuery.extend({ "current_material": "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectPipeline("P1", 1);

      expect(vsmAnalytics.jsonData()["source"]).toBe("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmAnalytics.jsonData()["destination"]).toBe("P1");
      expect(_.isEqual(AnalyticsPanel.update.calls.mostRecent().args, ["material_name", "P1", true])).toBe(true);
    });

    it("should do nothing in absence of pipeline name", function () {
      var graph = jQuery.extend({ "current_pipeline": "P3" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectPipeline(undefined, 1);

      expect(AnalyticsPanel.update).not.toHaveBeenCalled();
    });
  });

  describe("selectMaterial", function () {
    it("should have current_pipeline as destination node if a scm node is selected", function () {
      var graph = jQuery.extend({ "current_pipeline": "P3" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectMaterial("name", "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629", 0);
      expect(vsmAnalytics.jsonData()["source"]).toBe("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmAnalytics.jsonData()["destination"]).toBe("P3");
      expect(_.isEqual(AnalyticsPanel.update.calls.mostRecent().args, ["name", "P3", false])).toBe(true);
    });

    it("should do nothing if showing vsm for material", function () {
      var graph = jQuery.extend({ "current_material": "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629" }, vsmGraphJSON());
      var vsmAnalytics = new VSMAnalytics(graph, new VSMRenderer(), "analytics_path", AnalyticsPanel, "button");
      spyOn(AnalyticsPanel, "update");

      vsmAnalytics.selectMaterial("name", "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629", 0);

      expect(AnalyticsPanel.update).not.toHaveBeenCalled();
    });
  });

  var vsmGraphJSON = function vsmGraphJSON() {
    return {
      "levels": [{
        "nodes": [{
          "dependents": ["P1", "P2", "8fe9bcb1-3d58-4e1a-89ce-83709609a8d9"],
          "depth": 1,
          "id": "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629",
          "instances": [],
          "locator": "",
          "material_revisions": [{
            "modifications": [{
              "comment": "rm lkjsdf",
              "locator": "/go/materials/value_stream_map/3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629/2a13c2e8cf1661d905099e7297dba3c5b58bce7c",
              "modified_time": "6 days ago",
              "revision": "2a13c2e8cf1661d905099e7297dba3c5b58bce7c",
              "user": "user_name"
            }]
          }],
          "name": "material_name",
          "node_type": "GIT",
          "parents": []
        }]
      }, {
        "nodes": [{
          "can_edit": true,
          "dependents": ["P3"],
          "depth": 1,
          "edit_path": "/go/admin/pipelines/P1/general",
          "id": "P1",
          "instances": [{
            "counter": 1,
            "label": "1",
            "locator": "/go/pipelines/value_stream_map/P1/1",
            "stages": [{
              "duration": 30,
              "locator": "/go/pipelines/P1/1/defaultStage/1",
              "name": "defaultStage",
              "status": "Passed"
            }]
          }],
          "locator": "/go/pipeline/activity/P1",
          "name": "P1",
          "node_type": "PIPELINE",
          "parents": ["3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"]
        }, {
          "can_edit": true,
          "dependents": ["P3"],
          "depth": 2,
          "edit_path": "/go/admin/pipelines/P2/general",
          "id": "P2",
          "instances": [{
            "counter": 1,
            "label": "1",
            "locator": "/go/pipelines/value_stream_map/P2/1",
            "stages": [{
              "duration": 52,
              "locator": "/go/pipelines/P2/1/defaultStage/1",
              "name": "defaultStage",
              "status": "Passed"
            }]
          }],
          "locator": "/go/pipeline/activity/P2",
          "name": "P2",
          "node_type": "PIPELINE",
          "parents": ["3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"]
        }, {
          "dependents": ["a5adb455-6f0a-4100-ae2b-c2bf0b863ca8"],
          "depth": 3,
          "id": "8fe9bcb1-3d58-4e1a-89ce-83709609a8d9",
          "instances": [],
          "locator": "",
          "name": "dummy-8fe9bcb1-3d58-4e1a-89ce-83709609a8d9",
          "node_type": "DUMMY",
          "parents": ["3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"]
        }]
      }, {
        "nodes": [{
          "can_edit": true,
          "dependents": ["P4"],
          "depth": 1,
          "edit_path": "/go/admin/pipelines/P3/general",
          "id": "P3",
          "instances": [{
            "counter": 1,
            "label": "1",
            "locator": "/go/pipelines/value_stream_map/P3/1",
            "stages": [{
              "duration": 13,
              "locator": "/go/pipelines/P3/1/defaultStage/1",
              "name": "defaultStage",
              "status": "Passed"
            }]
          }],
          "locator": "/go/pipeline/activity/P3",
          "name": "P3",
          "node_type": "PIPELINE",
          "parents": ["P1", "P2"]
        }, {
          "dependents": ["P4"],
          "depth": 2,
          "id": "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8",
          "instances": [],
          "locator": "",
          "material_revisions": [{
            "modifications": [{
              "comment": "add asd",
              "locator": "/go/materials/value_stream_map/9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8/ad67c8a52dd0ed18e722ef526b7818ad0959df19",
              "modified_time": "8 days ago",
              "revision": "ad67c8a52dd0ed18e722ef526b7818ad0959df19",
              "user": "user_name"
            }]
          }],
          "name": "/Users/maheshp/work/go/test_repo",
          "node_type": "GIT",
          "parents": []
        }, {
          "dependents": ["P4"],
          "depth": 3,
          "id": "a5adb455-6f0a-4100-ae2b-c2bf0b863ca8",
          "instances": [],
          "locator": "",
          "name": "dummy-a5adb455-6f0a-4100-ae2b-c2bf0b863ca8",
          "node_type": "DUMMY",
          "parents": ["8fe9bcb1-3d58-4e1a-89ce-83709609a8d9"]
        }]
      }, {
        "nodes": [{
          "can_edit": true,
          "dependents": [],
          "depth": 1,
          "edit_path": "/go/admin/pipelines/P4/general",
          "id": "P4",
          "instances": [{
            "counter": 3,
            "label": "3",
            "locator": "/go/pipelines/value_stream_map/P4/3",
            "stages": [{
              "duration": 17,
              "locator": "/go/pipelines/P4/3/defaultStage/1",
              "name": "defaultStage",
              "status": "Passed"
            }]
          }],
          "locator": "/go/pipeline/activity/P4",
          "name": "P4",
          "node_type": "PIPELINE",
          "parents": ["P3", "a5adb455-6f0a-4100-ae2b-c2bf0b863ca8", "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8"]
        }]
      }]
    };
  };
});

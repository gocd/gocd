/*
 * Copyright 2020 ThoughtWorks, Inc.
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
describe("vsm_graph", function () {

  describe("fromJSON", function () {
    it("should de-serialize graph from JSON", function () {
      var vsmGraph = VSMGraph.fromJSON(vsmGraphJSON());

      expect(vsmGraph.current_pipeline).toBe("P4");
      expect(vsmGraph.levels.size()).toBe(4);

      expect(vsmGraph.levels[0].nodes.size()).toBe(1);
      expect(vsmGraph.levels[0].nodes[0].id).toBe("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmGraph.levels[0].nodes[0].dependents.size()).toBe(3);
      expect(vsmGraph.levels[0].nodes[0].dependents).toContain("P1");
      expect(vsmGraph.levels[0].nodes[0].dependents).toContain("P2");
      expect(vsmGraph.levels[0].nodes[0].dependents).toContain("P4");
      expect(vsmGraph.levels[0].nodes[0].parents.size()).toBe(0);

      expect(vsmGraph.levels[1].nodes.size()).toBe(2);
      expect(vsmGraph.levels[1].nodes[0].id).toBe("P1");
      expect(vsmGraph.levels[1].nodes[0].parents.size()).toBe(1);
      expect(vsmGraph.levels[1].nodes[0].parents).toContain("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmGraph.levels[1].nodes[0].dependents.size()).toBe(1);
      expect(vsmGraph.levels[1].nodes[0].dependents).toContain("P3");
      expect(vsmGraph.levels[1].nodes[1].id).toBe("P2");
      expect(vsmGraph.levels[1].nodes[1].parents.size()).toBe(1);
      expect(vsmGraph.levels[1].nodes[1].parents).toContain("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmGraph.levels[1].nodes[1].dependents.size()).toBe(1);
      expect(vsmGraph.levels[1].nodes[1].dependents).toContain("P3");

      expect(vsmGraph.levels[2].nodes.size()).toBe(2);
      expect(vsmGraph.levels[2].nodes[0].id).toBe("P3");
      expect(vsmGraph.levels[2].nodes[0].parents.size()).toBe(2);
      expect(vsmGraph.levels[2].nodes[0].parents).toContain("P1");
      expect(vsmGraph.levels[2].nodes[0].parents).toContain("P2");
      expect(vsmGraph.levels[2].nodes[0].dependents.size()).toBe(1);
      expect(vsmGraph.levels[2].nodes[0].dependents).toContain("P4");
      expect(vsmGraph.levels[2].nodes[1].id).toBe("9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8");
      expect(vsmGraph.levels[2].nodes[1].parents.size()).toBe(0);
      expect(vsmGraph.levels[2].nodes[1].dependents.size()).toBe(1);
      expect(vsmGraph.levels[2].nodes[1].dependents).toContain("P4");

      expect(vsmGraph.levels[3].nodes.size()).toBe(1);
      expect(vsmGraph.levels[3].nodes[0].id).toBe("P4");
      expect(vsmGraph.levels[3].nodes[0].parents.size()).toBe(3);
      expect(vsmGraph.levels[3].nodes[0].parents).toContain("P3");
      expect(vsmGraph.levels[3].nodes[0].parents).toContain("3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629");
      expect(vsmGraph.levels[3].nodes[0].parents).toContain("9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8");
      expect(vsmGraph.levels[3].nodes[0].dependents.size()).toBe(0);
    });

    it("should leave the original JSON intact", function () {
      var graph = vsmGraphJSON();

      VSMGraph.fromJSON(graph);

      expect(_.isEqual(vsmGraphJSON(),graph)).toBe(true);
    });
  });

  describe("toJSON", function () {
    it("should serialize a vsm graph to JSON", function () {
      var vsmGraph = VSMGraph.fromJSON(vsmGraphJSON());

      var jsonString = JSON.stringify(vsmGraph);
      var json       = JSON.parse(jsonString);

      expect(json['current_pipeline']).toBe("P4");
      expect(_.isEqual(json, vsmGraphForAnalytics)).toBe(true);
    });
  });

  var vsmGraphForAnalytics = {
    "current_pipeline": "P4",
    "levels":           [
      {
        "nodes": [
          {
            "id":                 "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629",
            "name":               "material_name",
            "parents":            [],
            "dependents":         [
              "P1",
              "P2",
              "P4"
            ],
            "type":               "GIT",
            "material_revisions": [
              {
                "modifications": [
                  {
                    "revision": "2a13c2e8cf1661d905099e7297dba3c5b58bce7c",
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        "nodes": [
          {
            "dependents": [
              "P3"
            ],
            "id":         "P1",
            "instances":  [
              {
                "counter": 1,
                "label":   "1",
                "stages":  [
                  {
                    "duration": 30,
                    "name":     "defaultStage",
                    "status":   "Passed",
                    "counter":  1
                  }
                ]
              }
            ],
            "name":       "P1",
            "type":       "PIPELINE",
            "parents":    [
              "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"
            ]
          },
          {
            "dependents": [
              "P3"
            ],
            "id":         "P2",
            "instances":  [
              {
                "counter": 1,
                "label":   "1",
                "stages":  [
                  {
                    "duration": 52,
                    "name":     "defaultStage",
                    "status":   "Passed",
                    "counter":  1
                  }
                ]
              }
            ],
            "name":       "P2",
            "type":       "PIPELINE",
            "parents":    [
              "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"
            ]
          },
        ]
      },
      {
        "nodes": [
          {
            "dependents": [
              "P4"
            ],
            "id":         "P3",
            "instances":  [
              {
                "counter": 1,
                "label":   "1",
                "stages":  [
                  {
                    "duration": 13,
                    "name":     "defaultStage",
                    "status":   "Passed",
                    "counter":  1
                  }
                ]
              }
            ],
            "name":       "P3",
            "type":       "PIPELINE",
            "parents":    [
              "P1",
              "P2"
            ]
          },
          {
            "dependents":         [
              "P4"
            ],
            "id":                 "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8",
            "material_revisions": [
              {
                "modifications": [
                  {
                    "revision": "ad67c8a52dd0ed18e722ef526b7818ad0959df19",
                  }
                ]
              }
            ],
            "name":               "/Users/maheshp/work/go/test_repo",
            "type":               "GIT",
            "parents":            []
          }
        ]
      },
      {
        "nodes": [
          {
            "dependents": [],
            "id":         "P4",
            "instances":  [
              {
                "counter": 3,
                "label":   "3",
                "stages":  [
                  {
                    "duration": 17,
                    "name":     "defaultStage",
                    "status":   "Passed",
                    "counter":  1
                  }
                ]
              }
            ],
            "name":       "P4",
            "type":       "PIPELINE",
            "parents":    [
              "P3",
              "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629",
              "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8"
            ]
          }
        ]
      }
    ]
  };

  var vsmGraphJSON = function () {
    new PrototypeOverrides().overrideJSONStringify();
    return JSON.parse(JSON.stringify({
      "current_pipeline"
    :
      "P4",
        "levels"
    :
      [
        {
          "nodes": [
            {
              "dependents":         [
                "P1",
                "P2",
                "8fe9bcb1-3d58-4e1a-89ce-83709609a8d9"
              ],
              "depth":              1,
              "id":                 "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629",
              "instances":          [],
              "locator":            "",
              "material_revisions": [
                {
                  "modifications": [
                    {
                      "comment":       "rm lkjsdf",
                      "locator":       "/go/materials/value_stream_map/3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629/2a13c2e8cf1661d905099e7297dba3c5b58bce7c",
                      "modified_time": "6 days ago",
                      "revision":      "2a13c2e8cf1661d905099e7297dba3c5b58bce7c",
                      "user":          "user_name"
                    }
                  ]
                }
              ],
              "name":               "material_name",
              "node_type":          "GIT",
              "parents":            []
            }
          ]
        },
        {
          "nodes": [
            {
              "can_edit":   true,
              "dependents": [
                "P3"
              ],
              "depth":      1,
              "edit_path":  "/go/admin/pipelines/P1/general",
              "id":         "P1",
              "instances":  [
                {
                  "counter": 1,
                  "label":   "1",
                  "locator": "/go/pipelines/value_stream_map/P1/1",
                  "stages":  [
                    {
                      "duration": 30,
                      "locator":  "/go/pipelines/P1/1/defaultStage/1",
                      "name":     "defaultStage",
                      "status":   "Passed"
                    }
                  ]
                }
              ],
              "locator":    "/go/pipeline/activity/P1",
              "name":       "P1",
              "node_type":  "PIPELINE",
              "parents":    [
                "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"
              ]
            },
            {
              "can_edit":   true,
              "dependents": [
                "P3"
              ],
              "depth":      2,
              "edit_path":  "/go/admin/pipelines/P2/general",
              "id":         "P2",
              "instances":  [
                {
                  "counter": 1,
                  "label":   "1",
                  "locator": "/go/pipelines/value_stream_map/P2/1",
                  "stages":  [
                    {
                      "duration": 52,
                      "locator":  "/go/pipelines/P2/1/defaultStage/1",
                      "name":     "defaultStage",
                      "status":   "Passed"
                    }
                  ]
                }
              ],
              "locator":    "/go/pipeline/activity/P2",
              "name":       "P2",
              "node_type":  "PIPELINE",
              "parents":    [
                "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"
              ]
            },
            {
              "dependents": [
                "a5adb455-6f0a-4100-ae2b-c2bf0b863ca8"
              ],
              "depth":      3,
              "id":         "8fe9bcb1-3d58-4e1a-89ce-83709609a8d9",
              "instances":  [],
              "locator":    "",
              "name":       "dummy-8fe9bcb1-3d58-4e1a-89ce-83709609a8d9",
              "node_type":  "DUMMY",
              "parents":    [
                "3795dca7e793e62cfde2e8e2898efee05bde08c99700cff0ec96d68ad4522629"
              ]
            }
          ]
        },
        {
          "nodes": [
            {
              "can_edit":   true,
              "dependents": [
                "P4"
              ],
              "depth":      1,
              "edit_path":  "/go/admin/pipelines/P3/general",
              "id":         "P3",
              "instances":  [
                {
                  "counter": 1,
                  "label":   "1",
                  "locator": "/go/pipelines/value_stream_map/P3/1",
                  "stages":  [
                    {
                      "duration": 13,
                      "locator":  "/go/pipelines/P3/1/defaultStage/1",
                      "name":     "defaultStage",
                      "status":   "Passed"
                    }
                  ]
                }
              ],
              "locator":    "/go/pipeline/activity/P3",
              "name":       "P3",
              "node_type":  "PIPELINE",
              "parents":    [
                "P1",
                "P2"
              ]
            },
            {
              "dependents":         [
                "P4"
              ],
              "depth":              2,
              "id":                 "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8",
              "instances":          [],
              "locator":            "",
              "material_revisions": [
                {
                  "modifications": [
                    {
                      "comment":       "add asd",
                      "locator":       "/go/materials/value_stream_map/9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8/ad67c8a52dd0ed18e722ef526b7818ad0959df19",
                      "modified_time": "8 days ago",
                      "revision":      "ad67c8a52dd0ed18e722ef526b7818ad0959df19",
                      "user":          "user_name"
                    }
                  ]
                }
              ],
              "name":               "/Users/maheshp/work/go/test_repo",
              "node_type":          "GIT",
              "parents":            []
            },
            {
              "dependents": [
                "P4"
              ],
              "depth":      3,
              "id":         "a5adb455-6f0a-4100-ae2b-c2bf0b863ca8",
              "instances":  [],
              "locator":    "",
              "name":       "dummy-a5adb455-6f0a-4100-ae2b-c2bf0b863ca8",
              "node_type":  "DUMMY",
              "parents":    [
                "8fe9bcb1-3d58-4e1a-89ce-83709609a8d9"
              ]
            }
          ]
        },
        {
          "nodes": [
            {
              "can_edit":   true,
              "dependents": [],
              "depth":      1,
              "edit_path":  "/go/admin/pipelines/P4/general",
              "id":         "P4",
              "instances":  [
                {
                  "counter": 3,
                  "label":   "3",
                  "locator": "/go/pipelines/value_stream_map/P4/3",
                  "stages":  [
                    {
                      "duration": 17,
                      "locator":  "/go/pipelines/P4/3/defaultStage/1",
                      "name":     "defaultStage",
                      "status":   "Passed"
                    }
                  ]
                }
              ],
              "locator":    "/go/pipeline/activity/P4",
              "name":       "P4",
              "node_type":  "PIPELINE",
              "parents":    [
                "P3",
                "a5adb455-6f0a-4100-ae2b-c2bf0b863ca8",
                "9e02d1ae843b55f2cf77af4dbaba38e2dfaf8f86d4ca4c890a4ba9396bfc26c8"
              ]
            }
          ]
        }
      ]
    }));
  }

});

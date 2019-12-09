/*
 * Copyright 2019 ThoughtWorks, Inc.
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
describe("vsm_node", function () {

  describe("fromJSON", function () {
    it("should deserialize a pipeline dependency node from JSON", function () {
      var node = PipelineDependencyNode.fromJSON(nodeJSON);

      expect(node.id).toBe("build-linux");
      expect(node.name).toBe("build-linux");
      expect(node.type).toBe("PIPELINE");
      expect(node.parents).toContain("21f6ea93b7");
      expect(node.dependents).toContain("plugins");

      expect(node.instances.size()).toBe(1);
      expect(node.instances[0].counter).toBe(2590);
      expect(node.instances[0].label).toBe("2590");

      expect(node.instances[0].stages.size()).toBe(2);
      expect(node.instances[0].stages[0].name).toBe("build-non-server");
      expect(node.instances[0].stages[0].status).toBe("Passed");
      expect(node.instances[0].stages[0].counter).toBe(2);
      expect(node.instances[0].stages[0].duration).toBe(326);

      expect(node.instances[0].stages[1].name).toBe("build-server");
      expect(node.instances[0].stages[1].status).toBe("Passed");
      expect(node.instances[0].stages[1].counter).toBe(1);
      expect(node.instances[0].stages[1].duration).toBe(1239);
    });

    it("should deserialize a pipeline dependency node with no instances from JSON", function () {
      var node = PipelineDependencyNode.fromJSON(no_run);

      expect(node.id).toBe("post-release-github-activities");
      expect(node.name).toBe("post-release-github-activities");
      expect(node.type).toBe("PIPELINE");
      expect(node.parents).toContain("PublishStableRelease");
      expect(node.dependents.size()).toBe(0);
      expect(node.instances.size()).toBe(0);
    });

    it("should deserialize a scm material node from JSON", function () {
      var node = SCMDependencyNode.fromJSON(materialJSON);

      expect(node.id).toBe("21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b");
      expect(node.name).toBe("https://mirrors.gocd.org/git/gocd/gocd");
      expect(node.type).toBe("GIT");
      expect(node.dependents).toContain("519591c7-74ef-458d-b094-d4c15a80c0ab");
      expect(node.dependents).toContain("build-linux");

      expect(node.material_revisions.size()).toBe(2);
      expect(node.material_revisions[0].modifications.size()).toBe(1);
      expect(node.material_revisions[0].modifications[0].revision).toContain("878f029dd4fe4f9d36b575152a5086c2e0b1fc93");

      expect(node.material_revisions[1].modifications.size()).toBe(1);
      expect(node.material_revisions[1].modifications[0].revision).toContain("10b2c55bfc06c4c465447a5986f27a1d83570206");
    });

    it("should deserialize a dummy node from JSON", function () {
      var node = DummyNode.fromJSON(dummyJSON);

      expect(node.id).toBe("b667d782-64b0-475e-9bf2-d8db9a8c3b2c");
      expect(node.type).toBe("DUMMY");

      expect(node.dependents.size()).toBe(1);
      expect(node.dependents).toContain("e9448492-0d94-4c24-a7c4-a139e92dadff");

      expect(node.parents.size()).toBe(1);
      expect(node.parents).toContain("21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b");
    });
  });

  var nodeJSON = {
        "can_edit": true,
        "dependents": [
          "plugins"
        ],
        "depth": 1,
        "edit_path": "/go/admin/pipelines/build-linux/edit",
        "id": "build-linux",
        "instances": [
          {
            "counter": 2590,
            "label": "2590",
            "locator": "/go/pipelines/value_stream_map/build-linux/2590",
            "stages": [
              {
                "duration": 326,
                "locator": "/go/pipelines/build-linux/2590/build-non-server/2",
                "name": "build-non-server",
                "status": "Passed"
              },
              {
                "duration": 1239,
                "locator": "/go/pipelines/build-linux/2590/build-server/1",
                "name": "build-server",
                "status": "Passed"
              }
            ]
          }
        ],
        "locator": "/go/pipeline/activity/build-linux",
        "name": "build-linux",
        "node_type": "PIPELINE",
        "parents": [
          "21f6ea93b7"
        ]
      };

  var no_run =                 {
    "can_edit": true,
    "dependents": [],
    "depth": 3,
    "edit_path": "/go/admin/pipelines/post-release-github-activities/edit",
    "id": "post-release-github-activities",
    "instances": [
      {
        "counter": 0,
        "label": "",
        "locator": "",
        "stages": [
          {
            "duration": null,
            "locator": "",
            "name": "draft-release",
            "status": "Unknown"
          }
        ]
      }
    ],
    "locator": "/go/pipeline/activity/post-release-github-activities",
    "name": "post-release-github-activities",
    "node_type": "PIPELINE",
    "parents": [
      "PublishStableRelease"
    ]
  };

  var materialJSON =                 {
    "dependents": [
      "519591c7-74ef-458d-b094-d4c15a80c0ab",
      "build-linux"
    ],
    "depth": 1,
    "id": "21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b",
    "instances": [],
    "locator": "",
    "material_names": [
      "gocd"
    ],
    "material_revisions": [
      {
        "modifications": [
          {
            "comment": "Remove plugin info api v3",
            "locator": "/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/878f029dd4fe4f9d36b575152a5086c2e0b1fc93",
            "modified_time": "5 days ago",
            "revision": "878f029dd4fe4f9d36b575152a5086c2e0b1fc93",
            "user": "Ketan Padegaonkar <KetanPadegaonkar@gmail.com>"
          }
        ]
      },
      {
        "modifications": [
          {
            "comment": "Removed unused javascript (#4834)",
            "locator": "/go/materials/value_stream_map/21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b/10b2c55bfc06c4c465447a5986f27a1d83570206",
            "modified_time": "about 22 hours ago",
            "revision": "10b2c55bfc06c4c465447a5986f27a1d83570206",
            "user": "Bhupendra <bdpiparva@gmail.com>"
          }
        ]
      }
    ],
    "name": "https://mirrors.gocd.org/git/gocd/gocd",
    "node_type": "GIT",
    "parents": []
  };

  var dummyJSON =                 {
    "dependents": [
      "e9448492-0d94-4c24-a7c4-a139e92dadff"
    ],
    "depth": 5,
    "id": "b667d782-64b0-475e-9bf2-d8db9a8c3b2c",
    "instances": [],
    "locator": "",
    "name": "dummy-b667d782-64b0-475e-9bf2-d8db9a8c3b2c",
    "node_type": "DUMMY",
    "parents": [
      "21f6ea93b72144560a90ad9398e67c5c9f0f7c6cdf03e022a7fc2445bd8a3f2b"
    ]
  }

});

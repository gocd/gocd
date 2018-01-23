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

describe("Dashboard Material Revision Widget", () => {
  const m = require("mithril");

  const MaterialRevisionWidget = require("views/dashboard/material_revision_widget");
  const MaterialRevision       = require('models/dashboard/material_revision');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    window.destroyDomElementForTest();
  });

  describe("Git Material Revision View", () => {
    const gitRevisionJson = {
      "material_type": "Git",
      "material_name": "test-repo",
      "changed":       false,
      "modifications": [
        {
          "_links":        {
            "vsm": {
              "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
            }
          },
          "user_name":     "GoCD Test User <devnull@example.com>",
          "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
          "modified_time": "2017-12-19T05:30:32.000Z",
          "comment":       "Initial commit"
        }
      ]
    };

    beforeEach(() => {
      const revision = new MaterialRevision(gitRevisionJson);
      m.mount(root, {
        view() {
          return m(MaterialRevisionWidget, {
            revision,
          });
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it('should render material type and material name', () => {
      const widgetHeader = $root.find('.rev-head');
      expect(widgetHeader).toContainText(gitRevisionJson.material_type);
      expect(widgetHeader).toContainText(gitRevisionJson.material_name);
    });

    it('should render changes information related to the git material', () => {
      const modificationWidget = $root.find('.modifications');
      const modification       = gitRevisionJson.modifications[0];
      expect(modificationWidget).toContainText(modification.user_name);
      expect(modificationWidget).toContainText(modification.revision);
      expect(modificationWidget).toContainText(new Date(modification.modified_time));
      expect(modificationWidget).toContainText(modification.comment);
      expect(modificationWidget).toContainText("VSM");

      const vsmLink = $root.find('a');
      expect(vsmLink).toHaveAttr("href", modification._links.vsm.href);
    });

  });

  describe("Pipeline Material Revision View", () => {
    const pipelineRevisionJson = {
      "material_type": "Pipeline",
      "material_name": "up42",
      "changed":       false,
      "modifications": [{
        "_links":         {
          "vsm":               {
            "href": "http://localhost:8153/go/pipelines/value_stream_map/up42/2"
          },
          "stage_details_url": {
            "href": "http://localhost:8153/go/pipelines/up42/2/up42_stage/1"
          }
        },
        "revision":       "up42/2/up42_stage/1",
        "modified_time":  "2017-12-26T09:01:03.503Z",
        "pipeline_label": "2"
      }]
    };

    beforeEach(() => {
      const revision = new MaterialRevision(pipelineRevisionJson);
      m.mount(root, {
        view() {
          return m(MaterialRevisionWidget, {
            revision,
          });
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it('should render material type and pipeline material name', () => {
      const widgetHeader = $root.find('.rev-head');
      expect(widgetHeader).toContainText(pipelineRevisionJson.material_type);
      expect(widgetHeader).toContainText(pipelineRevisionJson.material_name);
    });

    it('should render changes information related to the pipeline material', () => {
      const modificationWidget = $root.find('.modifications');
      const modification       = pipelineRevisionJson.modifications[0];

      expect(modificationWidget).toContainText(modification.revision);
      expect(modificationWidget).toContainText(new Date(modification.modified_time));
      expect(modificationWidget).toContainText(modification.pipeline_label);
    });

    it('should contain a link to overview of stage details page of the stage that triggered the pipeline', () => {
      const stageDetailLink = $root.find('a')[0];
      const modification    = pipelineRevisionJson.modifications[0];
      expect(stageDetailLink).toHaveAttr("href", modification._links.stage_details_url.href);
    });

    it('should contain a link to stage details page showing pipeline dependencies of the run of the upstream pipeline run', () => {
      const pipelineDependencyLink = $root.find('a')[1];
      expect(pipelineDependencyLink).toHaveAttr("href", "http://localhost:8153/go/pipelines/up42/2/up42_stage/1/pipeline");
    });

  });

  describe("Package Material Revision View", () => {
    const packageRevisionJson = {
      "material_type": "Package",
      "material_name": "NPMPackage:node-http-api",
      "changed":       false,
      "modifications": [
        {
          "_links":        {
            "vsm": {
              "href": "http://localhost:8153/go/materials/value_stream_map/663e9d0907042bd7717e8de930ee7e2f9017c538698afe6a754bfea66845e7df/0.0.1"
            }
          },
          "user_name":     "Turixis",
          "revision":      "0.0.1",
          "modified_time": "2012-04-27T17:11:05.137Z",
          "comment":       "{\"TYPE\":\"PACKAGE_MATERIAL\"}"
        }
      ]
    };

    beforeEach(() => {
      const revision = new MaterialRevision(packageRevisionJson);
      m.mount(root, {
        view() {
          return m(MaterialRevisionWidget, {
            revision,
          });
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it("should render material type as Package and the correct package name", () => {
      const widgetHeader = $root.find('.rev-head');
      expect(widgetHeader).toContainText(packageRevisionJson.material_type);
      expect(widgetHeader).toContainText(packageRevisionJson.material_name);
    });

    it("should render changes information related to the package material", () => {
      const modificationWidget = $root.find('.modifications');
      const modification       = packageRevisionJson.modifications[0];

      expect(modificationWidget).toContainText(modification.user_name);
      expect(modificationWidget).toContainText(new Date(modification.modified_time));
      expect(modificationWidget).toContainText(modification.revision);
      expect(modificationWidget).toContainText("VSM");
      expect(modificationWidget).toContainText("Trackback: Not Provided");
    });

    it("should contain link to vsm of the package material", () => {
      const modificationWidget = $root.find('.modifications');
      const modification       = packageRevisionJson.modifications[0];

      const vsmLink = modificationWidget.find('a');
      expect(vsmLink).toHaveAttr("href", modification._links.vsm.href);
    });
  });

  describe("Color coding for modified revisions", () => {
    const changedMaterialJson = {
      "material_type": "Git",
      "material_name": "test-repo",
      "changed":       true,
      "modifications": [
        {
          "_links":        {
            "vsm": {
              "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
            }
          },
          "user_name":     "GoCD Test User <devnull@example.com>",
          "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
          "modified_time": "2017-12-19T05:30:32.000Z",
          "comment":       "Initial commit"
        }
      ]
    };

    beforeEach(() => {
      const revision = new MaterialRevision(changedMaterialJson);
      m.mount(root, {
        view() {
          return m(MaterialRevisionWidget, {
            revision,
          });
        }
      });
      m.redraw(true);
    });

    afterEach(() => {
      m.mount(root, null);
      m.redraw();
    });

    it("should highlight the revision that triggered the pipeline with yellow color", () => {
      const revisionWidget = $root.find('.changed');

      expect(revisionWidget).toExist();
    });

    describe("Color coding for unmodified revisions", () => {
      const unchangedMaterialJson = {
        "material_type": "Git",
        "material_name": "test-repo",
        "changed":       false,
        "modifications": [
          {
            "_links":        {
              "vsm": {
                "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
              }
            },
            "user_name":     "GoCD Test User <devnull@example.com>",
            "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
            "modified_time": "2017-12-19T05:30:32.000Z",
            "comment":       "Initial commit"
          }
        ]
      };

      beforeEach(() => {
        const revision = new MaterialRevision(unchangedMaterialJson);
        m.mount(root, {
          view() {
            return m(MaterialRevisionWidget, {
              revision,
            });
          }
        });
        m.redraw(true);
      });

      afterEach(() => {
        m.mount(root, null);
        m.redraw();
      });

      it("should not highlight revisions which aren't modified", () => {
        const revision = $root.find('.changed');

        expect(revision).not.toExist();
      });
    });
  });
});

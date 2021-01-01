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
import {MaterialRevision} from "models/dashboard/material_revision";

describe("Dashboard", () => {
  describe('Material Revision Model', () => {

    describe("scm material modifications", () => {
      it("should deserialize from json", () => {
        const scmRevision = new MaterialRevision(gitRevisionJson);

        expect(scmRevision.materialType).toBe(gitRevisionJson.material_type);
        expect(scmRevision.materialName).toBe(gitRevisionJson.material_name);
        expect(scmRevision.changed).toBe(gitRevisionJson.changed);

        expect(scmRevision.modifications.length).toBe(gitRevisionJson.modifications.length);
      });

      it("should deserialize material modifications", () => {
        const materialRevision = new MaterialRevision(gitRevisionJson);
        const gitModification  = materialRevision.modifications[0];

        const gitModificationJson = gitRevisionJson.modifications[0];

        expect(gitModification.username).toEqual(gitModificationJson.user_name);
        expect(gitModification.modifiedTime).toEqual(gitModificationJson.modified_time);
        expect(gitModification.revision).toEqual(gitModificationJson.revision);
        expect(gitModification.comment).toEqual(gitModificationJson.comment);
        expect(gitModification.vsmPath).toEqual(gitModificationJson._links.vsm.href);
      });

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
    });

    describe("pipeline material modifications", () => {
      const pipelineRevisionJson = {
        "material_type": "Pipeline",
        "material_name": "up42",
        "changed":       true,
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

      it("should deserialize information related to the revision from the json it receives", () => {
        const pipelineRevision = new MaterialRevision(pipelineRevisionJson);

        expect(pipelineRevision.materialName).toEqual(pipelineRevisionJson.material_name);
        expect(pipelineRevision.materialType).toEqual(pipelineRevisionJson.material_type);

        const pipelineModification         = pipelineRevision.modifications[0];
        const pipelineModificationFromJson = pipelineRevisionJson.modifications[0];

        expect(pipelineModification.revision).toEqual(pipelineModificationFromJson.revision);
        expect(pipelineModification.modifiedTime).toEqual(pipelineModificationFromJson.modified_time);
        expect(pipelineModification.pipelineLabel).toEqual(pipelineModificationFromJson.pipeline_label);
        expect(pipelineModification.stageDetailsUrl).toEqual(pipelineModificationFromJson._links.stage_details_url.href);
      });
    });

    describe("package material modifications", () => {
      const packageRevisionJson = {
        "material_type": "Package",
        "material_name": "NPMPackage:node-http-api",
        "changed":       true,
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

      const packageRevisionWithTrackbackJSON = {
        "material_type": "Package",
        "material_name": "NPMPackage:node-http-api",
        "changed":       true,
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
            "comment":       "{\"TYPE\":\"PACKAGE_MATERIAL\", \"TRACKBACK_URL\": \"trackback url\", \"COMMENT\": \"SOME COMMENT\"}"
          }]
      };

      it("should deserialize information related to the revision from the json it receives", () => {
        const packageRevision = new MaterialRevision(packageRevisionJson);

        expect(packageRevision.materialName).toEqual(packageRevisionJson.material_name);
        expect(packageRevision.materialType).toEqual(packageRevisionJson.material_type);
        expect(packageRevision.changed).toEqual(packageRevisionJson.changed);

        const packageModification          = packageRevision.modifications[0];
        const pipelineModificationFromJson = packageRevisionJson.modifications[0];

        expect(packageModification.username).toEqual(pipelineModificationFromJson.user_name);
        expect(packageModification.revision).toEqual(pipelineModificationFromJson.revision);
        expect(packageModification.modifiedTime).toEqual(pipelineModificationFromJson.modified_time);
        expect(packageModification.vsmPath).toEqual(pipelineModificationFromJson._links.vsm.href);
        expect(packageModification.comment).toEqual("Trackback: Not Provided");
      });

      it("should set appropriate comment", () => {
        const packageRevision = new MaterialRevision(packageRevisionWithTrackbackJSON);

        const packageModification = packageRevision.modifications[0];

        expect(packageModification.comment).toEqual("SOME COMMENTTrackback: trackback url");
      });
    });
  });
})
;

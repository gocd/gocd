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
import _ from "lodash";
import {TriggerWithOptionsInfo} from "models/dashboard/trigger_with_options_info";

describe("Dashboard", () => {
  describe('Trigger With Option Information Model', () => {

    it("should deserialize from json", () => {
      const info = TriggerWithOptionsInfo.fromJSON(json);

      expect(info.materials.length).toBe(json.materials.length);
      expect(info.plainTextVariables.length).toBe(2);
      expect(info.secureVariables.length).toBe(2);
    });

    it("should add material selection field for each material", () => {
      const info = TriggerWithOptionsInfo.fromJSON(json);

      expect(info.materials.length).toBe(json.materials.length);

      _.each(info.materials, (material) => {
        expect(_.isFunction(material.selection)).toBe(true);

        const fingerprint = 'selection fingerprint';
        material.selection(fingerprint);

        expect(material.selection()).toBe(fingerprint);
      });
    });

    describe("Trigger with Options Request JSON", () => {
      it("should set update_materials_before_scheduling to false", () => {
        const info = TriggerWithOptionsInfo.fromJSON(json);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['update_materials_before_scheduling']).toBe(false);
      });

      it("should not select any materials when none of the material revisions are selected and pipeline is never run", () => {
        const info = TriggerWithOptionsInfo.fromJSON(firstRunJson);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['materials']).toEqual([]);
      });

      it("should select last run revision when none of the material revisions are selected", () => {
        const info = TriggerWithOptionsInfo.fromJSON(json);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['materials']).toEqual([
          {
            fingerprint: info.materials[0].fingerprint,
            revision:    info.materials[0].revision.revision
          },
          {
            fingerprint: info.materials[1].fingerprint,
            revision:    info.materials[1].revision.revision
          }
        ]);
      });

      it("should not select any environment variables when none of the environment variables are overriden", () => {
        const info = TriggerWithOptionsInfo.fromJSON(json);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['environment_variables']).toEqual([]);
      });

      it("should contain selected materials", () => {
        const info     = TriggerWithOptionsInfo.fromJSON(json);
        const revision = "new revision";
        info.materials[0].selection(revision);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['materials']).toEqual([
          {
          fingerprint: info.materials[0].fingerprint,
          revision
          },
          {
            fingerprint: info.materials[1].fingerprint,
            revision:    info.materials[1].revision.revision
          }]);
      });

      it("should contain overriden environment variables", () => {
        const info     = TriggerWithOptionsInfo.fromJSON(json);
        const newValue = "some value";
        const variable = info.plainTextVariables[0];
        variable.value(newValue);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['environment_variables']).toEqual([{
          name:   variable.name,
          value:  variable.value(),
          secure: variable.isSecureValue()
        }]);
      });

      it("should contain overridden secure environment variables", () => {
        const info     = TriggerWithOptionsInfo.fromJSON(json);
        const newValue = "some value";
        const variable = info.secureVariables[0];
        variable.editValue();
        variable.value(newValue);

        const triggerOptionsJSON = info.getTriggerOptionsJSON();
        expect(triggerOptionsJSON['environment_variables']).toEqual([{
          name:   variable.name,
          value:  variable.value(),
          secure: variable.isSecureValue()
        }]);
      });
    });

    it('should fetch trigger options for the specified pipeline name', () => {
      const pipelineName = 'up42';

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineName}/trigger_options`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(json),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((info) => {
          expect(info.materials.length).toBe(json.materials.length);
          expect(info.plainTextVariables.length).toBe(2);
          expect(info.secureVariables.length).toBe(2);
        });

        TriggerWithOptionsInfo.all(pipelineName).then(successCallback);
        expect(successCallback).toHaveBeenCalled();

        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/pipelines/${pipelineName}/trigger_options`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    const json = {
      "variables": [
        {
          "name":   "version",
          "secure": false,
          "value":  "asdf"
        },
        {
          "name":   "foobar",
          "secure": false,
          "value":  "asdf"
        },
        {
          "name":   "secure1",
          "secure": true,
          "value":  "****"
        },
        {
          "name":   "highly secure",
          "secure": true,
          "value":  "****"
        }
      ],

      "materials": [
        {
          "type":        "Git",
          "name":        "https://github.com/ganeshspatil/gocd",
          "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
          "revision":    {
            "date":              "2018-02-08T04:32:11Z",
            "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
            "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
            "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
          }
        },
        {
          "type":        "Git",
          "name":        "https://github.com/ganeshspatil/gocd2",
          "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd810",
          "revision":    {
            "date":              "2018-02-08T04:32:11Z",
            "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
            "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
            "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d6"
          }
        }
      ]
    };

    const firstRunJson = {
      "variables": [
        {
          "name":   "version",
          "secure": false,
          "value":  "asdf"
        },
        {
          "name":   "foobar",
          "secure": false,
          "value":  "asdf"
        },
        {
          "name":   "secure1",
          "secure": true,
          "value":  "****"
        },
        {
          "name":   "highly secure",
          "secure": true,
          "value":  "****"
        }
      ],

      "materials": [
        {
          "type":        "Git",
          "name":        "https://github.com/ganeshspatil/gocd",
          "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
          "revision":    {}
        },
        {
          "type":        "Git",
          "name":        "https://github.com/ganeshspatil/gocd2",
          "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd810",
          "revision":    {}
        }
      ]
    };
  });
});

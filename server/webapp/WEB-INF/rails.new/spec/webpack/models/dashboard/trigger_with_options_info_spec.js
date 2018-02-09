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

describe("Dashboard", () => {
  describe('Trigger With Option Information Model', () => {

    const TriggerWithOptionsInfo = require('models/dashboard/trigger_with_options_info');

    it("should deserialize from json", () => {
      const info = TriggerWithOptionsInfo.fromJSON(json);

      expect(info.materials.length).toBe(json.materials.length);
      expect(info.plainTextVariables.length).toBe(2);
      expect(info.secureVariables.length).toBe(2);
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
        }
      ]
    };
  });
});

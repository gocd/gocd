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

describe("Dashboard Trigger With Options View Model", () => {
  const TriggerWithOptionsVM   = require("views/dashboard/models/trigger_with_options_vm");
  const TriggerWithOptionsInfo = require('models/dashboard/trigger_with_options_info');

  describe('initialize', () => {
    let vm;
    beforeEach(() => {
      vm = new TriggerWithOptionsVM();
      vm.initialize(TriggerWithOptionsInfo.fromJSON(json));
    });

    it('should contain tab keys on the vm', () => {
      expect(vm.MATERIALS_TAB_KEY).toBe('materials');
      expect(vm.ENVIRONMENT_VARIABLES_TAB_KEY).toBe('environment-variables');
      expect(vm.SECURE_ENVIRONMENT_VARIABLES_TAB_KEY).toBe('secure-environment-variables');
    });

    it('should initialize VM with tab states', () => {
      expect(vm.isTabSelected(vm.MATERIALS_TAB_KEY)).toBe(true);
      expect(vm.isTabSelected(vm.ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(false);
      expect(vm.isTabSelected(vm.SECURE_ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(false);
    });

    it("should select the materials tab by default", () => {
      expect(vm.isTabSelected(vm.MATERIALS_TAB_KEY)).toBe(true);
    });

    it("should select a tab", () => {
      expect(vm.isTabSelected(vm.MATERIALS_TAB_KEY)).toBe(true);
      expect(vm.isTabSelected(vm.ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(false);
      expect(vm.isTabSelected(vm.SECURE_ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(false);

      vm.selectTab(vm.ENVIRONMENT_VARIABLES_TAB_KEY);

      expect(vm.isTabSelected(vm.MATERIALS_TAB_KEY)).toBe(false);
      expect(vm.isTabSelected(vm.ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(true);
      expect(vm.isTabSelected(vm.SECURE_ENVIRONMENT_VARIABLES_TAB_KEY)).toBe(false);
    });
  });

  const json = {
    "environment_variables":        [
      {
        "name":  "version",
        "value": "asdf"
      },
      {
        "name":  "foobar",
        "value": "asdf"
      }
    ],
    "secure_environment_variables": [
      {
        "name":  "secure1",
        "value": "****"
      },
      {
        "name":  "highly secure",
        "value": "****"
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

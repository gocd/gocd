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
import {TriggerWithOptionsVM} from "views/dashboard/models/trigger_with_options_vm";
import {TriggerWithOptionsInfo} from "models/dashboard/trigger_with_options_info";

describe("Dashboard Trigger With Options View Model", () => {

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

    it("it should initialize vm with materials state", () => {
      expect(vm.isMaterialSelected(json.materials[0].name)).toBe(true);
      expect(vm.isMaterialSelected(json.materials[1].name)).toBe(false);
    });

    it("it should select a material", () => {
      expect(vm.isMaterialSelected(json.materials[0].name)).toBe(true);
      expect(vm.isMaterialSelected(json.materials[1].name)).toBe(false);

      vm.selectMaterial(json.materials[1].name);

      expect(vm.isMaterialSelected(json.materials[0].name)).toBe(false);
      expect(vm.isMaterialSelected(json.materials[1].name)).toBe(true);
    });
  });

  const json = {
    "materials": [
      {
        "name": "material1"
      },
      {
        "name": "material2"
      }
    ]
  };
})
;

/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['lodash', 'string-plus', 'models/pipeline_configs/tabs'], function (_, s, Tabs) {
  describe("Tabs Model", function () {
    var tabs, tab;
    beforeEach(function () {
      tabs = new Tabs();

      tab = tabs.createTab({
        name: 'tab_name',
        path: 'tab_path'
      });
    });

    it('should initialize model with name', function () {
      expect(tab.name()).toBe('tab_name');
    });

    it("should initialize model with path", function () {
      expect(tab.path()).toBe('tab_path');
    });

    describe('validations', function () {
      it("should add error when name is blank but path is not", function () {
        tab.name('');

        var errors = tab.validate();

        expect(errors.errors('name')).toEqual(['Name must be present']);
      });

      it("should NOT add error when both name and path are blank", function () {
        tab.name('');
        tab.path('');

        var errors = tab.validate();

        expect(errors._isEmpty()).toBe(true);
      });

      it("should not allow tabs with duplicate names", function () {
        var errorsOnOriginal = tab.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateTab = tabs.createTab({
          name: tab.name()
        });

        errorsOnOriginal = tab.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateTab.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });
    });


    describe("Deserialization from JSON", function () {
      beforeEach(function () {
        tab = Tabs.Tab.fromJSON(sampleJSON());
      });

      it("should initialize from json", function () {
        expect(tab.name()).toBe('tab_name');
        expect(tab.path()).toBe('tab_path');
      });

      function sampleJSON() {
        return {
          name: 'tab_name',
          path: 'tab_path'
        };
      }
    });
  });
});

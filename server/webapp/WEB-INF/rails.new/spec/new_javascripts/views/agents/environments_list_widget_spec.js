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

define(["jquery", "mithril", 'models/agents/environments', "views/agents/environments_list_widget", "foundation.dropdown"], function ($, m, Environments, EnvironmentsListWidget) {
  describe("Environments List Widget", function () {
    var $root = $(  '#mithril-mount-point'), root = $root.get(0);

    beforeAll(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/environments/).andReturn({
        "responseText": JSON.stringify(['Dev', 'Build', 'Testing', 'Deploy']),
        "status":       200
      });
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({"status": 304});

      var selectedAgents = [
        {
          uuid:         '1',
          environments: function () {
            return ['Dev', 'Testing'];
          }
        },
        {
          uuid:         '2',
          environments: function () {
            return ['Build', 'Testing'];
          }
        }
      ];
      Environments.init(selectedAgents);

      mount();
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
      unmount();
    });

    it('should contain all the environments checkbox', function () {
      var allEnvironments = $.find('.resources-items :checkbox');
      expect(allEnvironments).toHaveLength(4);
      expect(allEnvironments[0]).toHaveValue('Dev');
      expect(allEnvironments[1]).toHaveValue('Build');
      expect(allEnvironments[2]).toHaveValue('Testing');
      expect(allEnvironments[3]).toHaveValue('Deploy');
    });

    it('should check environments that are present on all the agents', function () {
      var allEnvironments = $.find('.resources-items :checkbox');
      expect(allEnvironments[2]).toHaveValue('Testing');
      expect(allEnvironments[2]).toBeChecked();
    });

    it('should select environments as indeterminate that are present on some of the agents', function () {
      var allEnvironments = $.find('.resources-items :checkbox');
      expect(allEnvironments[0]).toHaveValue('Dev');
      expect(allEnvironments[0].indeterminate).toBe(true);

      expect(allEnvironments[1]).toHaveValue('Build');
      expect(allEnvironments[1].indeterminate).toBe(true);
    });

    it('should uncheck environments that are not present on any the agents', function () {
      var allEnvironments = $.find('.resources-items :checkbox');
      expect(allEnvironments[3]).toHaveValue('Deploy');
      expect(allEnvironments[3]).not.toBeChecked();
      expect(allEnvironments[3].indeterminate).toBe(false);
    });

    var mount = function () {
      m.mount(root,
          m.component(EnvironmentsListWidget, {
            hideDropDown:         hideDropDown,
            dropDownReset:        dropDownReset,
            onEnvironmentsUpdate: onEnvironmentsUpdate,
          })
      );
      m.redraw(true);
    };

    var hideDropDown  = function () {
    };
    var dropDownReset = function () {
    };
    var onEnvironmentsUpdate = function () {
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };
    
  });
});

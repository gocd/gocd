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

define(["jquery", "mithril", 'models/agents/agents', 'models/agents/resources', "views/agents/resources_list_widget", "foundation.dropdown"], function ($, m, Agents, Resources, ResourcesListWidget) {
  describe("Resources List Widget", function () {

    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var vm = {
      agentsCheckedState: {}
    };

    vm.dropdown = {
      reset:  m.prop(true),
      states: {},
      add:    function () {
      },

      hide: function (name) {
        this.states[name](false);
      }
    };

    vm.dropdown.states['environment'] = m.prop(false);
    vm.dropdown.states['resource']    = m.prop(false);

    beforeAll(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/resources/).andReturn({
        "responseText": JSON.stringify(['Linux', 'Gauge', 'Java', 'Windows']),
        "status":       200
      });
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({"status": 304});
    });

    beforeEach(function () {
      var selectedAgents = [
        {
          uuid:      '1',
          resources: function () {
            return ['Linux', 'Java'];
          }
        },
        {
          uuid:      '2',
          resources: function () {
            return ['Gauge', 'Java'];
          }
        }
      ];

      Resources.init(selectedAgents);

      mount();
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
      unmount();
    });

    it('should contain all the resources checkbox', function () {
      var allResources = $.find('.resources-items :checkbox');
      expect(allResources).toHaveLength(4);
      expect(allResources[0]).toHaveValue('Linux');
      expect(allResources[1]).toHaveValue('Gauge');
      expect(allResources[2]).toHaveValue('Java');
      expect(allResources[3]).toHaveValue('Windows');
    });

    it('should check resources that are present on all the agents', function () {
      var allResources = $.find('.resources-items :checkbox');
      expect(allResources[2]).toHaveValue('Java');
      expect(allResources[2]).toBeChecked();
    });

    it('should select resources as indeterminate that are present on some of the agents', function () {
      var allResources = $.find('.resources-items :checkbox');
      expect(allResources[0]).toHaveValue('Linux');
      expect(allResources[0].indeterminate).toBe(true);

      expect(allResources[1]).toHaveValue('Gauge');
      expect(allResources[1].indeterminate).toBe(true);
    });

    it('should uncheck resources that are not present on any the agents', function () {
      var allResources = $.find('.resources-items :checkbox');
      expect(allResources[3]).toHaveValue('Windows');
      expect(allResources[3]).not.toBeChecked();
      expect(allResources[3].indeterminate).toBe(false);
    });

    it('should have button to add resources', function () {
      var addButton = $root.find('.add-resource :button')[0];
      expect(addButton).toHaveText("Add");
    });

    it('should have button to apply resources', function () {
      var applyButton = $root.find('.add-resource :button')[1];
      expect(applyButton).toHaveText("Apply");
    });

    it('should add resource after invoking add button', function () {
      var allResources = $root.find('.resources-items :checkbox');
      expect(allResources).toHaveLength(4);

      var inputBox = $root.find('.add-resource :text')[0];
      $(inputBox).val('Chrome').trigger('input');


      var addButton = $root.find('.add-resource :button')[0];
      addButton.click();
      m.redraw(true);

      allResources = $root.find('.resources-items :checkbox');
      expect(allResources).toHaveLength(5);
    });


    it('should clear input-text box after adding resource', function () {
      var inputBox = $root.find('.add-resource input');
      $(inputBox).val('Chrome').trigger('change');

      expect(inputBox).toHaveValue('Chrome');
      var addButton = $root.find('.add-resource button')[0];
      addButton.click();
      m.redraw(true);

      inputBox = $root.find('.add-resource input');
      expect(inputBox).toHaveValue('');
    });

    it('should not add duplicate resources', function () {
      var allResources = $root.find('.resources-items input[type="Checkbox"]');
      expect(allResources).toHaveLength(4);
      expect(allResources[0]).toHaveValue('Linux');

      var inputBox = $root.find('.add-resource :input')[0];
      $(inputBox).val('Linux').trigger('change');

      var addButton = $root.find('.add-resource :button')[1];
      addButton.click();
      m.redraw(true);

      allResources = $root.find('.resources-items input[type="Checkbox"]');
      expect(allResources).toHaveLength(4);
    });

    var mount = function () {
      m.mount(root,
        m.component(ResourcesListWidget, {
          'hideDropDown':      hideDropDown,
          'dropDownReset':     dropDownReset,
          'onResourcesUpdate': m.prop()
        })
      );
      m.redraw(true);
    };

    var hideDropDown = function () {
    };

    var dropDownReset = function () {
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };

  });
});

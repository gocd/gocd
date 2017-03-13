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

describe("ResourcesListWidget", () => {
  var $                = require("jquery");
  var simulateEvent    = require('simulate-event');
  var m                = require('mithril');
  var Stream           = require('mithril/stream');
  var TriStateCheckbox = require('models/agents/tri_state_checkbox');
  require("foundation-sites");

  var Resources           = require('models/agents/resources');
  var ResourcesListWidget = require("views/agents/resources_list_widget");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  var vm;
  beforeEach(() => {
    vm = {
      agentsCheckedState: {}
    };

    vm.dropdown = {
      reset:  Stream(true),
      states: {},
      add:    function () {
      },

      hide: function (name) {
        this.states[name](false);
      }
    };

    vm.dropdown.states['environment'] = Stream(false);
    vm.dropdown.states['resource']    = Stream(false);

    var selectedAgentsResources = [['Linux', 'Java'], ['Gauge', 'Java']];

    var allResources = [
      new TriStateCheckbox('Gauge', selectedAgentsResources),
      new TriStateCheckbox('Java', selectedAgentsResources),
      new TriStateCheckbox('Linux', selectedAgentsResources),
      new TriStateCheckbox('Windows', selectedAgentsResources),
    ];

    Resources.list = allResources;
    mount();
  });

  afterEach(() => {
    unmount();
    Resources.list = [];
  });

  it('should contain all the resources checkbox', () => {
    var allResources = $.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(4);
    expect(allResources[0]).toHaveValue('Gauge');
    expect(allResources[1]).toHaveValue('Java');
    expect(allResources[2]).toHaveValue('Linux');
    expect(allResources[3]).toHaveValue('Windows');
  });

  it('should check resources that are present on all the agents', () => {
    var allResources = $.find('.resources-items :checkbox');
    expect(allResources[1]).toHaveValue('Java');
    expect(allResources[1]).toBeChecked();
  });

  it('should select resources as indeterminate that are present on some of the agents', () => {
    var allResources = $.find('.resources-items :checkbox');
    expect(allResources[2]).toHaveValue('Linux');
    expect(allResources[2].indeterminate).toBe(true);

    expect(allResources[0]).toHaveValue('Gauge');
    expect(allResources[0].indeterminate).toBe(true);
  });

  it('should uncheck resources that are not present on any the agents', () => {
    var allResources = $.find('.resources-items :checkbox');
    expect(allResources[3]).toHaveValue('Windows');
    expect(allResources[3]).not.toBeChecked();
    expect(allResources[3].indeterminate).toBe(false);
  });

  it('should have button to add resources', () => {
    var addButton = $root.find('.add-resource :button')[0];
    expect(addButton).toHaveText("Add");
  });

  it('should have button to apply resources', () => {
    var applyButton = $root.find('.add-resource :button')[1];
    expect(applyButton).toHaveText("Apply");
  });

  it('should add resource after invoking add button', () => {
    var allResources = $root.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(4);

    var inputBox = $root.find('.add-resource-input').get(0);
    $(inputBox).val('Chrome');
    simulateEvent.simulate(inputBox, 'input');

    expect(inputBox).toHaveValue('Chrome');
    var addButton = $root.find('.add-resource .add-resource-btn')[0];
    simulateEvent.simulate(addButton, 'click');
    m.redraw();

    allResources = $root.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(5);
  });


  it('should clear input-text box after adding resource', () => {
    var inputBox = $root.find('.add-resource input');
    $(inputBox).val('Chrome').trigger('change');

    expect(inputBox).toHaveValue('Chrome');
    var addButton = $root.find('.add-resource button')[0];
    addButton.click();
    m.redraw();

    inputBox = $root.find('.add-resource input');
    expect(inputBox).toHaveValue('');
  });

  it('should not add duplicate resources', () => {
    var allResources = $root.find('.resources-items input[type="Checkbox"]');
    expect(allResources).toHaveLength(4);
    expect(allResources[2]).toHaveValue('Linux');

    var inputBox = $root.find('.add-resource :input')[0];
    $(inputBox).val('Linux').trigger('change');

    var addButton = $root.find('.add-resource :button')[1];
    addButton.click();
    m.redraw();

    allResources = $root.find('.resources-items input[type="Checkbox"]');
    expect(allResources).toHaveLength(4);
  });

  var mount = () => {
    m.mount(root, {
      view: function () {
        return m(ResourcesListWidget, {
          'hideDropDown':      hideDropDown,
          'dropDownReset':     dropDownReset,
          'onResourcesUpdate': Stream()
        });
      }
    });

    m.redraw();
  };

  var hideDropDown = () => {
  };

  var dropDownReset = () => {
  };

  var unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

});

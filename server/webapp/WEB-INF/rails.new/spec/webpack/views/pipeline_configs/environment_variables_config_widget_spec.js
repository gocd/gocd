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
describe("EnvironmentVariable Widget", () => {

  var $             = require("jquery");
  var m             = require('mithril');
  var Stream        = require('mithril/stream');
  var simulateEvent = require('simulate-event');

  require('jasmine-jquery');

  var EnvironmentVariables      = require("models/pipeline_configs/environment_variables");
  var EnvironmentVariableWidget = require("views/pipeline_configs/environment_variables_config_widget");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  var variables;

  beforeEach(() => {
    variables = Stream(EnvironmentVariables.fromJSON([
      {
        name:  "COMMAND",
        value: "echo"
      }, {
        name:   "PASSWORD",
        value:  "s3cr3t",
        secure: true
      }
    ]));

    m.mount(root, {
      view: function () {
        return m(EnvironmentVariableWidget, {variables});
      }
    });

    m.redraw();

    simulateEvent.simulate($root.find('.environment-variables .accordion-item > a').get(0), 'click');
    m.redraw();
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should display environment variables", () => {
    var environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=COMMAND]');
    var variableName              = environmentVariableFields.find("input[data-prop-name=name]");
    var variableValue             = environmentVariableFields.find("input[data-prop-name=value]");

    expect(variableName).toHaveValue("COMMAND");
    expect(variableValue).toHaveValue("echo");
  });

  it("should display normal text field for non-secure variables", () => {
    var environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=plain][data-variable-name=COMMAND]');
    var valueField               = environmentVariableField.find("input[data-prop-name=value]");

    expect(valueField.attr('type')).toBe('text');
  });

  it("should display edit link for secure variable", () => {
    var environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=secure][data-variable-name=PASSWORD]');
    var editLink                 = environmentVariableField.find("button.edit-secure-variable");

    expect(editLink.length).toBe(1);

    simulateEvent.simulate(editLink.get(0), 'click');
    m.redraw();

    expect($('button.edit-secure-variable').length).toBe(0);
  });
});

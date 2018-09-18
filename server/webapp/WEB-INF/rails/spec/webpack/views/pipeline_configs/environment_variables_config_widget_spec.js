/*
 * Copyright 2017 ThoughtWorks, Inc.
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

  const $             = require("jquery");
  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');

  const EnvironmentVariables      = require("models/pipeline_configs/environment_variables");
  const EnvironmentVariableWidget = require("views/pipeline_configs/environment_variables_config_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let variables;

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
      view() {
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
    const environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=COMMAND]');
    const variableName              = environmentVariableFields.find("input[data-prop-name=name]");
    const variableValue             = environmentVariableFields.find("input[data-prop-name=value]");

    expect(variableName).toHaveValue("COMMAND");
    expect(variableValue).toHaveValue("echo");
  });

  it("should display normal text field for non-secure variables", () => {
    const environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=plain][data-variable-name=COMMAND]');
    const valueField               = environmentVariableField.find("input[data-prop-name=value]");

    expect(valueField.attr('type')).toBe('text');
  });

  it("should display edit link for secure variable", () => {
    const environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=secure][data-variable-name=PASSWORD]');
    const editLink                 = environmentVariableField.find("button.edit-secure-variable");

    expect(editLink.length).toBe(1);

    simulateEvent.simulate(editLink.get(0), 'click');
    m.redraw();

    expect($('button.edit-secure-variable').length).toBe(0);
  });
});

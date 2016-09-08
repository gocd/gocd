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

define(["jquery", "mithril", "models/pipeline_configs/environment_variables", "views/pipeline_configs/environment_variables_config_widget"], function ($, m, EnvironmentVariables, EnvironmentVariableWidget) {
  describe("EnvironmentVariable Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var variables;

    beforeEach(function () {
      variables = m.prop(EnvironmentVariables.fromJSON([
        {
          name:  "COMMAND",
          value: "echo"
        }, {
          name:   "PASSWORD",
          value:  "s3cr3t",
          secure: true
        }
      ]));

      m.mount(root,
        m.component(EnvironmentVariableWidget, {variables: variables})
      );

      m.redraw(true);

      var accordion = $root.find('.environment-variables .accordion-item > a').get(0);
      var evObj     = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);
    });

    it("should display environment variables", function () {
      var environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=COMMAND]');
      var variableName              = environmentVariableFields.find("input[data-prop-name=name]").val();
      var variableValue             = environmentVariableFields.find("input[data-prop-name=value]").val();

      expect(variableName).toBe("COMMAND");
      expect(variableValue).toBe("echo");
    });

    it("should display normal text field for non-secure variables", function () {
      var environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=plain][data-variable-name=COMMAND]');
      var valueField               = environmentVariableField.find("input[data-prop-name=value]");

      expect(valueField.attr('type')).toBe('text');
    });

    it("should display edit link for secure variable", function () {
      var environmentVariableField = $root.find('.environment-variables div.environment-variable[data-variable-type=secure][data-variable-name=PASSWORD]');
      var editLink                 = environmentVariableField.find("a.edit-secure-variable");

      expect(editLink.text()).toBe('Edit');

      var evObj = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      editLink.get(0).onclick(evObj);
      m.redraw(true);

      editLink = environmentVariableField.find("a.edit-secure-variable");
      expect(editLink.length).toBe(0);
    });
  });
});

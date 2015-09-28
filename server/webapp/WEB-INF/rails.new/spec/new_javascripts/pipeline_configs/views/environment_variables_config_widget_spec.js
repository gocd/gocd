/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(["jquery", "mithril", "pipeline_configs/models/environment_variables", "pipeline_configs/views/environment_variables_config_widget"], function ($, m, EnvironmentVariables, EnvironmentVariableWidget) {
  describe("Environment Variable Widget", function () {
    var $root;
    beforeEach(function () {
      var variables = EnvironmentVariables.fromJSON([
        {
          name:  "COMMAND",
          value: "echo"
        }, {
          name:   "PASSWORD",
          value:  "s3cr3t",
          secure: true
        }
      ]);
      root  = document.createElement("div");
      document.body.appendChild(root);
      $root         = $(root);

      m.mount(root,
        m.component(EnvironmentVariableWidget, {variables: variables})
      );

      m.redraw(true);

      var accordion = $root.find('.environment-variables .accordion-navigation > a').get(0);
      var evObj     = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);
    });

    afterEach(function () {
      root.parentNode.removeChild(root);
    });

    it("should display environment variables", function () {
      var environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=COMMAND]');
      var variableName              = environmentVariableFields.find("input[data-prop-name=name]").val();
      var variableValue             = environmentVariableFields.find("input[data-prop-name=value]").val();

      expect(variableName).toBe("COMMAND");
      expect(variableValue).toBe("echo");
    });

    it("should display normal text field for non-secure variables", function () {
      var environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=COMMAND]');
      var valueField                = environmentVariableFields.find("input[data-prop-name=value]");

      expect(valueField.attr('type')).toBe('text');
    });

    it("should display password field for secure variable", function () {
      var environmentVariableFields = $root.find('.environment-variables div.environment-variable[data-variable-name=PASSWORD]');
      var valueField                = environmentVariableFields.find("input[data-prop-name=value]");

      expect(valueField.attr('type')).toBe('password');
    });
  });
});

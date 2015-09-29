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

define(["jquery", "mithril", "pipeline_configs/models/parameters", "pipeline_configs/views/parameters_config_widget"], function ($, m, Parameters, ParametersConfigWidget) {
  describe("Parameter Widget", function () {
    var root, $root;
    beforeEach(function () {
      var parameters = new Parameters.fromJSON([
        {name: "COMMAND", value: "echo"}
      ]);

      root  = document.createElement("div");
      document.body.appendChild(root);
      $root = $(root);

      m.mount(root,
        m.component(ParametersConfigWidget, {parameters: parameters})
      );

      m.redraw(true);

      var accordion = $root.find('.parameters .accordion-navigation > a').get(0);
      var evObj     = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);
    });

    afterEach(function () {
      root.parentNode.removeChild(root);
    });

    it("should display parameters", function () {
      var paramField = $root.find('.parameters div.parameter[data-parameter-name=COMMAND]');
      var paramName  = paramField.find("input[data-prop-name=name]").val();
      var paramValue = paramField.find("input[data-prop-name=value]").val();

      expect(paramName).toBe("COMMAND");
      expect(paramValue).toBe("echo");
    });
  });
});

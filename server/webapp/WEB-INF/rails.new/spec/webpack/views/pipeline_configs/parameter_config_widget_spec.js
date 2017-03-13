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

describe("Parameter Widget", () => {

  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const simulateEvent = require('simulate-event');

  require("jasmine-jquery");

  const Parameters             = require("models/pipeline_configs/parameters");
  const ParametersConfigWidget = require("views/pipeline_configs/parameters_config_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let parameters;
  beforeEach(() => {
    parameters = Stream(new Parameters.fromJSON([
      {name: "COMMAND", value: "echo"}
    ]));

    m.mount(root, {
      view() {
        return m(ParametersConfigWidget, {parameters});
      }
    });
    m.redraw();
    simulateEvent.simulate($root.find('.parameters .accordion-item > a').get(0), 'click');
    m.redraw();
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should display parameters", (done) => {
    setTimeout(() => {
      const paramField = $root.find('.parameters div.parameter[data-parameter-name=COMMAND]');
      const paramName  = paramField.find("input[data-prop-name=name]");
      const paramValue = paramField.find("input[data-prop-name=value]");

      expect(paramName).toHaveValue("COMMAND");
      expect(paramValue).toHaveValue("echo");
      done();
    }, 100);
  });

});

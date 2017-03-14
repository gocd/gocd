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

describe("Read Only Environment Variables Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const EnvironmentVariablesWidget = require("views/pipeline_configs/read_only/environment_variables_widget");
  const Template                   = require("models/pipeline_configs/template");

  let $root, root, template;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe("Plain Text Variables", () => {
    beforeEach(() => {
      template = Template.fromJSON(rawTemplateJSONWithPlainEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', () => {
      expect($('h5')).toContainText('Environment Variables:');
    });


    it('should render environment variables', () => {
      const variable = rawTemplateJSONWithPlainEnvironmentVariables().stages[0].environment_variables[0];
      expect($root).toContainText(variable.name);
      expect($root).toContainText(variable.value);
    });
  });

  describe("Secure Variables", () => {
    beforeEach(() => {
      template = Template.fromJSON(rawTemplateJSONWithSecureEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', () => {
      expect($('h5')).toContainText('Environment Variables:');
    });


    it('should render environment variables', () => {
      const variable = rawTemplateJSONWithSecureEnvironmentVariables().stages[0].environment_variables[0];
      expect($root).toContainText(variable.name);
      expect($root).toContainText('******');
    });
  });

  describe("No Variables Message", () => {
    beforeEach(() => {
      template = Template.fromJSON(rawTemplateJSONWithNoEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', () => {
      expect($('h5')).toContainText('Environment Variables:');
    });

    it('should render no environment variable message', () => {
      expect($root).toContainText('No Environment Variables have been configured.');
    });
  });


  const mount = function () {
    m.mount(root, {
      view () {
        return m(EnvironmentVariablesWidget, {environmentVariables: template.stages().firstStage().environmentVariables});
      }
    });
    m.redraw();
  };

  const unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  const rawTemplateJSONWithPlainEnvironmentVariables = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":                  "up42_stage",
          "environment_variables": [
            {
              "name":   "USERNAME",
              "value":  "admin",
              "secure": false
            }
          ]
        }
      ]
    };
  };

  const rawTemplateJSONWithSecureEnvironmentVariables = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":                  "up42_stage",
          "environment_variables": [
            {
              "name":            "PASSWORD",
              "encrypted_value": "rs9n6d",
              "secure":          true
            }
          ]
        }
      ]
    };
  };

  const rawTemplateJSONWithNoEnvironmentVariables = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":                  "up42_stage",
          "environment_variables": []
        }
      ]
    };
  };

});

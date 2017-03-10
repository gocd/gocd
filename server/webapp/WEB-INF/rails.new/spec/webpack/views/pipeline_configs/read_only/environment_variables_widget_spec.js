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

describe("Read Only Environment Variables Widget", function () {
  var $ = require("jquery");
  var m = require("mithril");
  require('jasmine-jquery');

  var EnvironmentVariablesWidget = require("views/pipeline_configs/read_only/environment_variables_widget");
  var Template                   = require("models/pipeline_configs/template");

  var $root, root, template;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe("Plain Text Variables", function () {
    beforeEach(function () {
      template = Template.fromJSON(rawTemplateJSONWithPlainEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', function () {
      expect($('h5')).toContainText('Environment Variables:');
    });


    it('should render environment variables', function () {
      let variable = rawTemplateJSONWithPlainEnvironmentVariables().stages[0].environment_variables[0];
      expect($root).toContainText(variable.name);
      expect($root).toContainText(variable.value);
    });
  });

  describe("Secure Variables", function () {
    beforeEach(function () {
      template = Template.fromJSON(rawTemplateJSONWithSecureEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', function () {
      expect($('h5')).toContainText('Environment Variables:');
    });


    it('should render environment variables', function () {
      let variable = rawTemplateJSONWithSecureEnvironmentVariables().stages[0].environment_variables[0];
      expect($root).toContainText(variable.name);
      expect($root).toContainText('******');
    });
  });

  describe("No Variables Message", function () {
    beforeEach(function () {
      template = Template.fromJSON(rawTemplateJSONWithNoEnvironmentVariables());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage environment variables heading', function () {
      expect($('h5')).toContainText('Environment Variables:');
    });

    it('should render no environment variable message', function () {
      expect($root).toContainText('No Environment Variables have been configured.');
    });
  });


  var mount = function () {
    m.mount(root, {
      view: function () {
        return m(EnvironmentVariablesWidget, {stage: template.stages().firstStage});
      }
    });
    m.redraw();
  };

  var unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  var rawTemplateJSONWithPlainEnvironmentVariables = function () {
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

  var rawTemplateJSONWithSecureEnvironmentVariables = function () {
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

  var rawTemplateJSONWithNoEnvironmentVariables = function () {
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

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

var $                       = require('jquery');
var m                       = require('mithril');
var Stream                  = require('mithril/stream');
var NewTemplateConfigWidget = require('views/template_configs/new_template_config_widget');
var ElasticProfiles         = require('models/elastic_profiles/elastic_profiles');
var simulateEvent           = require('simulate-event');

describe("NeWTemplateConfigWidget", function () {
  var templateJSON = {
    "_links":        {
      "self": {
        "href": "https://ci.example.com/go/api/admin/templates/baz"
      },
      "doc":  {
        "href": "https://api.go.cd/#template-config"
      },
      "find": {
        "href": "https://ci.example.com/go/api/admin/templates/:template_name"
      }
    },
    "name":          "baz",
    "authorization": {
      "admins": {
        "roles": [],
        "users": []
      }
    },
    "stages":        [
      {
        "name":                    "defaultStage",
        "fetch_materials":         true,
        "clean_working_directory": false,
        "never_cleanup_artifacts": false,
        "approval":                {
          "type":          "success",
          "authorization": {
            "roles": [],
            "users": []
          }
        },
        "environment_variables":   [],
        "jobs":                    [
          {
            "name":                  "defaultJob",
            "run_instance_count":    null,
            "timeout":               null,
            "environment_variables": [],
            "resources":             [],
            "tasks":                 [],
            "tabs":                  [],
            "artifacts":             [],
            "properties":            null
          }
        ]
      }
    ]
  };

  var allTemplatesJSON = {
    "_embedded": {
      "templates": []
    }
  };

  var profileJSON     = {
    "id":         "unit-tests",
    "plugin_id":  "cd.go.contrib.elastic-agent.docker",
    "properties": [
      {
        "key":   "Image",
        "value": "gocdcontrib/gocd-dev-build"
      },
      {
        "key":   "Environment",
        "value": "JAVA_HOME=/opt/java\nMAKE_OPTS=-j8"
      }
    ]
  };
  var allProfilesJSON = {
    "_embedded": {
      "profiles": [profileJSON]
    }
  };

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(function () {
    jasmine.Ajax.install();

    jasmine.Ajax.stubRequest('/go/api/admin/templates', undefined, 'GET').andReturn({
      responseText: JSON.stringify(allTemplatesJSON),
      status:       200
    });

    jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
      responseText: JSON.stringify(allProfilesJSON),
      status:       200
    });

    jasmine.Ajax.stubRequest('/go/api/admin/templates', undefined, 'POST').andReturn({
      responseText: JSON.stringify(templateJSON),
      status:       200
    });

    var elasticProfiles = Stream(ElasticProfiles.fromJSON(allProfilesJSON));

    m.mount(root, {
      view: function () {
        return m(NewTemplateConfigWidget, {elasticProfiles: elasticProfiles});
      }
    });
    m.redraw();
  });

  afterEach(function () {
    jasmine.Ajax.uninstall();

    m.mount(root, null);
    m.redraw();
  });

  describe('Headers', function () {
    it('should show add template heading', function () {
      expect($('.page-header h1')).toContainText('Add Template');
    });
  });

  describe('Extract from Pipelines', function () {
    it('should show the extract from pipelines option', function () {
      expect($('.extract-from-pipeline')).toContainText('Extract From Pipeline');
    });

    it('should show select pipeline widget if template is extracted from a pipeline', function () {
      expect($('.extract-from-pipeline input')[0].checked).toBe(false);
      $('.extract-from-pipeline input')[0].click();
      m.redraw();
      expect($('.select-pipeline')).toBeInDOM();
      expect($('.extract-from-pipeline input')[0].checked).toBe(true);
    });

    it('should show new stages widget if template is not extracted from a pipeline', function () {
      expect($('.extract-from-pipeline input')[0].checked).toBe(false);
      expect($('.select-pipeline')).not.toBeInDOM();
      expect($('.stages')).toBeInDOM();
    });
  });

  describe('Save', function () {
    it('should create a new template', function () {
      spyOn(m.route, 'set');

      var templateName = $($('.template-body input:text')[0]).val(templateJSON.name);
      templateName.val(templateJSON.name);
      var e = $.Event('input', {currentTarget: templateName.get(0)});
      templateName.trigger(e);
      m.redraw();
      $root.find('.save-template').click();
      m.redraw();

      expect(m.route.set).toHaveBeenCalledWith('/' + templateJSON.name);
    });

    it('should not create a template when validation fails', function () {
      $root.find('.save-template').click();
      m.redraw();

      expect($('.template-body .alert')).toContainText('There are errors on the page, fix them and save');
    });

  });

  describe('Back', function () {
    it('should route back to templates page', function () {
      spyOn(m.route, 'set');

      simulateEvent.simulate($('.button').get(0), 'click');
      m.redraw();

      expect(m.route.set).toHaveBeenCalledWith('default');
    });
  });

  describe("Permissions", function () {
    it('should show permissions tab for admin users', function () {
      expect($('.permissions')).toBeInDOM();
    });
  });

  describe("Stages", function () {
    it('should show stages of the template', function () {
      expect($('.stages')).toBeInDOM();
    });
  });

});
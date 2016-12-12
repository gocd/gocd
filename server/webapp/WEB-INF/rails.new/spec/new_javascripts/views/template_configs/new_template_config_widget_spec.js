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

define(["jquery", "mithril",
    "views/template_configs/new_template_config_widget",
    "views/template_configs/template_config_widget",
    "views/template_configs/templates_config_widget",
    "models/elastic_profiles/elastic_profiles"],
  function ($, m, NewTemplateConfigWidget, TemplateConfigWidget, TemplatesConfigWidget, ElasticProfiles) {

    describe("NeWTemplateConfigWidget", function () {
      var $root = $('#mithril-mount-point'), root = $root.get(0);

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

        var elasticProfiles = m.prop(ElasticProfiles.fromJSON(allProfilesJSON));

        m.mount(root, m.component(TemplatesConfigWidget));
        m.route(root, '', {
          '':               m.component(TemplatesConfigWidget, {isUserAdmin: true}),
          '/:templateName': m.component(TemplateConfigWidget, {elasticProfiles: elasticProfiles, isUserAdmin: true}),
          '/:new':          m.component(NewTemplateConfigWidget, {elasticProfiles: elasticProfiles})
        });
        m.route('/:new');
        m.redraw(true);
      });

      afterEach(function () {
        jasmine.Ajax.uninstall();

        m.mount(root, null);
        m.route('');
        m.redraw(true);
      });

      describe('Headers', function () {
        it('should show add template heading', function () {
          expect($('.page-header')).toContainText('Add Template');
        });
      });

      describe('Extract from Pipelines', function () {
        it('should show the extract from pipelines option', function () {
          expect($('.extract-from-pipeline')).toContainText('Extract From Pipeline');
        });

        it('should show select pipeline widget if template is extracted from a pipeline', function () {
          expect($('.extract-from-pipeline input')[0].checked).toBe(false);
          $('.extract-from-pipeline input')[0].click();
          m.redraw(true);
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
          spyOn(m, 'route');

          var templateName = $($('.template-body input:text')[0]).val(templateJSON.name);
          templateName.val(templateJSON.name);
          var e = $.Event('input', {currentTarget: templateName.get(0)});
          templateName.trigger(e);
          m.redraw(true);
          $root.find('.save-template').click();
          m.redraw(true);

          expect(m.route).toHaveBeenCalledWith('/' + templateJSON.name);
        });

        it('should not create a template when validation fails', function () {
          $root.find('.save-template').click();
          m.redraw(true);

          expect($('.template-body .alert')).toContainText('There are errors on the page, fix them and save');
        });

      });

      describe('Back', function () {
        it('should route back to templates page', function () {
          spyOn(m, 'route');

          $('.button')[0].click();

          expect(m.route).toHaveBeenCalledWith('default');
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
  });
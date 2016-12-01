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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/repository_config_new_widget", "models/pipeline_configs/materials",
    'models/pipeline_configs/repositories', 'models/pipeline_configs/plugin_infos'],
  function ($, m, RepositoryConfigNewWidget, Materials, Repositories, PluginInfos) {

    describe("RepositoryConfigNewWidget", function () {
      var $root = $('#mithril-mount-point'), root = $root.get(0);

      var debPluginInfoJSON = {
        "id":                          "deb",
        "name":                        "Deb plugin",
        "version":                     "13.4.1",
        "type":                        "package-repository",
        "pluggable_instance_settings": {
          "configurations": [
            {
              "key":      "PACKAGE_NAME",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         true,
                "part_of_identity": true
              }
            },
            {
              "key":      "VERSION_SPEC",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         false,
                "part_of_identity": true
              }
            },
            {
              "key":      "ARCHITECTURE",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         false,
                "part_of_identity": true
              }
            },
            {
              "key":      "REPO_URL",
              "type":     "repository",
              "metadata": {
                "secure":           false,
                "required":         true,
                "part_of_identity": true
              }
            }
          ]
        }
      };

      var npmPluginInfoJSON = {
        "id":                          "npm",
        "name":                        "Npm plugin",
        "version":                     "13.4.1",
        "type":                        "package-repository",
        "pluggable_instance_settings": {
          "configurations": [
            {
              "key":      "PACKAGE_NAME",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         true,
                "part_of_identity": true
              }
            },
            {
              "key":      "VERSION_SPEC",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         false,
                "part_of_identity": true
              }
            },
            {
              "key":      "ARCHITECTURE",
              "type":     "package",
              "metadata": {
                "secure":           false,
                "required":         false,
                "part_of_identity": true
              }
            },
            {
              "key":      "REPO_URL",
              "type":     "repository",
              "metadata": {
                "secure":           false,
                "required":         true,
                "part_of_identity": true
              }
            }
          ]
        }
      };


      var mount = function (repository) {
        m.mount(root,
          m.component(RepositoryConfigNewWidget,
            {
              'repoForEdit': repository,
              'vm':          new Repositories.vm()
            })
        );
        m.redraw(true);
      };


      beforeEach(function () {
        jasmine.Ajax.install();
        jasmine.Ajax.stubRequest('/go/api/admin/plugin_info/deb', undefined, 'GET').andReturn({
          responseText: JSON.stringify(debPluginInfoJSON),
          status:       200
        });

        jasmine.Ajax.stubRequest('/go/api/admin/plugin_info/npm', undefined, 'GET').andReturn({
          responseText: JSON.stringify(npmPluginInfoJSON),
          status:       200
        });

        var debPluginInfo = new PluginInfos.PluginInfo(debPluginInfoJSON);
        var npmPluginInfo = new PluginInfos.PluginInfo(npmPluginInfoJSON);

        PluginInfos([debPluginInfo, npmPluginInfo]);
        var repository = m.prop(new Repositories.Repository({}));
        mount(repository);
      });

      afterEach(function () {
        jasmine.Ajax.uninstall();
        PluginInfos([]);
        m.mount(root, null);
        m.redraw(true);
      });

      describe("Repository New Widget", function () {
        it("should have input for repository name", function () {
          var modal = $root.find('.modal-content');
          expect(modal).toContainElement("input[data-prop-name='name']");
          var labels = $(modal).find('label');
          expect(labels[0]).toContainText("Name");
        });

        it("should have a dropdown for repository plugins", function () {
          var selector = $root.find(".modal-content select[data-prop-name='plugin']");
          var labels = $root.find('.modal-content label');
          expect(labels[1]).toContainText("Type of plugin");
          var options = $(selector).find('option');
          expect(options[0]).toHaveText('Deb plugin');
          expect(options[1]).toHaveText('Npm plugin');
        });

        it("should change the repository model if name is changed", function () {
          var repository = m.prop(new Repositories.Repository({}));
          mount(repository);
          var input = $root.find(".modal-content input[data-prop-name='name']");
          expect(input).toHaveValue('');
          $(input).val('RepoName').trigger('input');
          m.redraw(true);
          expect(repository().name()).toBe('RepoName');
        });

        it(' should change the selected value in the dropdown if plugin is changed', function () {
          var selector = $root.find(".modal-content select[data-prop-name='plugin']");
          var selectedOption = $(selector).find("option:selected");
          expect(selectedOption).toHaveText('Deb plugin');

          $(selector).val('npm');
          m.redraw(true);
          selectedOption = $(selector).find("option:selected");
          expect(selectedOption).toHaveText('Npm plugin');
        });

        //it('should change the repository model if plugin is changed', function () {
        //  var repository = m.prop(new Repositories.Repository({}));
        //  mount(repository);
        //
        //  expect(repository().pluginMetadata().id()).toBe('deb');
        //  var configurationKeys = repository().configuration().mapConfigurations(function (configuration) {
        //    return configuration.key();
        //  });
        //  expect(configurationKeys).toEqual(["REPO_URL"]);
        //
        //  var selector = $root.find(".modal-content select[data-prop-name='plugin']");
        //  debugger;
        //  $(selector).val('npm');
        //  $(selector).change();
        //  m.redraw(true);
        //
        //  configurationKeys = repository().configuration().mapConfigurations(function (configuration) {
        //    return configuration.key();
        //  });
        //
        //  expect(repository().pluginMetadata().id()).toBe('npm');
        //  expect(configurationKeys).toEqual(["REPO_URL", "USERNAME", "PASSWORD"])
        //});
      });
    });
  });
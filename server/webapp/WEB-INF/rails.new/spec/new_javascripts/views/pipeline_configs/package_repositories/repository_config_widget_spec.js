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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/repository_config_widget", "models/pipeline_configs/materials",
  'models/pipeline_configs/repositories', 'models/pipeline_configs/plugin_infos'],
  function ($, m, RepositoryConfigWidget, Materials, Repositories, PluginInfos) {

  describe("RepositoryConfigWidget", function () {
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


    var allRepositoriesJSON = {
      "_embedded": {
        "package_repositories": [
          {
            "repo_id":         "2e74f4c6-be61-4122-8bf5-9c0641d44258",
            "name":            "first1",
            "plugin_metadata": {
              "id":      "nuget",
              "version": "1"
            },
            "configuration":   [
              {
                "key":   "REPO_URL",
                "value": "http://"
              },
              {
                "key":   "USERNAME",
                "value": "first"
              },
              {
                "key":             "PASSWORD",
                "encrypted_value": "en5p5YgWfxJkOAYqAy5u0g=="
              }
            ],
            "_embedded":       {
              "packages": []
            }
          },
          {
            "repo_id":         "6e74622b-b921-4546-9fc6-b7f9ba1732ba",
            "name":            "hello",
            "plugin_metadata": {
              "id":      "deb",
              "version": "1"
            },
            "configuration":   [
              {
                "key":   "REPO_URL",
                "value": "http://hello"
              }
            ],
            "_embedded":       {
              "packages": []
            }
          }
        ]
      }
    };


    var repositoryJSON = {
      "repo_id": "e9745dc7-aaeb-48a8-a22a-fa206ad0637e",
      "name": "repo",
      "plugin_metadata": {
        "id": "deb",
        "version": "1"
      },
      "configuration": [
        {
          "key": "REPO_URL",
          "value": "http://"
        },
        {
          "key":   "USERNAME",
          "value": "first"
        },
        {
          "key":             "PASSWORD",
          "encrypted_value": "en5p5YgWfxJkOAYqAy5u0g=="
        }
      ],
      "_embedded": {
        "packages": [

        ]
      }
    };

    var removeModal = function () {
      $('.modal-parent').each(function (_i, elem) {
        $(elem).data('modal').destroy();
      });
    };

    var mount = function (material) {
      m.mount(root,
        m.component(RepositoryConfigWidget,
          {
            'material': material
          })
      );
      m.redraw(true);
    };

    var pkgMaterial;

    beforeEach(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest('/go/api/plugin_info/deb', undefined, 'GET').andReturn({
        responseText: JSON.stringify(debPluginInfoJSON),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/repositories', undefined, 'GET').andReturn({
        responseText: JSON.stringify(allRepositoriesJSON),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/repositories/e9745dc7-aaeb-48a8-a22a-fa206ad0637e', undefined, 'GET').andReturn({
        responseText: JSON.stringify(repositoryJSON),
        status:       200
      });

      pkgMaterial = new Materials().createMaterial({
        type: 'package'
      });

      mount(pkgMaterial);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();

      m.mount(root, null);
      m.redraw(true);
    });

    var setMaterialWithDebainRepository = function () {
      var repository = new Repositories.Repository(repositoryJSON);
      var pluginInfo = new PluginInfos.PluginInfo(debPluginInfoJSON);
      pkgMaterial.repository(repository);
      Repositories([repository]);
      PluginInfos([pluginInfo]);
      mount(pkgMaterial);
    };

    describe("list all profiles", function () {
      it("should give button to create repository if no repository exists", function () {
        var noRepositoryInfo    = $root.find('.repo-selector .no-repo label');
        var createNewRepoButton = $root.find('.repo-selector .no-repo .add-button');

        expect(noRepositoryInfo).toHaveText('No repositories available.');
        expect(createNewRepoButton).toHaveText('Create New Repository');
      });

      it("should give button to add new repository", function () {
        setMaterialWithDebainRepository();
        var noRepositoryInfo = $root.find('.repo-selector label');

        expect(noRepositoryInfo).not.toHaveText('No repositories available.');
        expect($root.find('.repo-selector .add-button')).toExist();
      });

      it('should show select repository drop down', function () {
        setMaterialWithDebainRepository();

      });

      it('should show edit repository information', function () {
        setMaterialWithDebainRepository();

        var editRepositoryBox = $root.find('.repository');
        expect($(editRepositoryBox).find('button')).toExist();

        var editRepositoryLabelNames = _.map($(editRepositoryBox).find('label'), function (label) {
          return $(label).text();
        });

        var editRepositoryInformation = _.map($(editRepositoryBox).find('span'), function (span) {
          return $(span).text();
        });

        expect(editRepositoryLabelNames).toEqual(['Name', 'Plugin', 'Repo_url', 'Username', 'Password']);
        expect(editRepositoryInformation).toEqual(['repo', 'Deb plugin', 'http://', 'first', '***********']);

      });

      it('should have selector to change repository', function () {
        setMaterialWithDebainRepository();
        var repositoryInfo = $root.find('.repo-selector');
        var repoSelector = $(repositoryInfo).find("select[data-prop-name='defaultRepoId']");

        expect(repoSelector).toHaveValue('e9745dc7-aaeb-48a8-a22a-fa206ad0637e');
      });

      //it('should change the edit repository ')

    });
  });
});
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

define(["jquery", "mithril", "lodash", "views/pipeline_configs/package_repositories/repository_config_widget", "models/pipeline_configs/materials",
  'models/pipeline_configs/repositories', 'models/pipeline_configs/plugin_infos'
], function ($, m, _, RepositoryConfigWidget, Materials, Repositories, PluginInfos) {

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
      "repo_id":         "e9745dc7-aaeb-48a8-a22a-fa206ad0637e",
      "name":            "repo",
      "plugin_metadata": {
        "id":      "deb",
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
    };

    var repository2JSON = {
      "repo_id":         "repo2",
      "name":            "repo2Name",
      "plugin_metadata": {
        "id":      "deb",
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

      jasmine.Ajax.stubRequest('/go/api/admin/repositories', undefined, 'GET').andReturn({
        responseText: JSON.stringify(allRepositoriesJSON),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/repositories/e9745dc7-aaeb-48a8-a22a-fa206ad0637e', undefined, 'GET').andReturn({
        responseText: JSON.stringify(repositoryJSON),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/repositories/repo2', undefined, 'GET').andReturn({
        responseText: JSON.stringify(repository2JSON),
        status:       200
      });

      pkgMaterial = new Materials().createMaterial({
        type: 'package'
      });

      mount(pkgMaterial);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();
      PluginInfos([]);
      Repositories([]);
      m.mount(root, null);
      m.redraw(true);
    });


    var setMaterialWithDebainRepository = function () {
      var repository = new Repositories.Repository(repositoryJSON);
      var repo2      = new Repositories.Repository(repository2JSON);
      var pluginInfo = new PluginInfos.PluginInfo(debPluginInfoJSON);
      pkgMaterial.repository(repository);
      Repositories([repository, repo2]);
      PluginInfos([pluginInfo]);
      mount(pkgMaterial);
    };

    describe("Repository Widget", function () {
      it("should give button to create repository if no repository exists", function () {
        var noRepositoryInfo    = $root.find('.no-repo label');
        var createNewRepoButton = $root.find('.no-repo .add-button');

        expect(noRepositoryInfo).toHaveText('No repositories available.');
        expect(createNewRepoButton).toHaveText('Create New Repository');
      });

      it("should give button to add new repository", function () {
        setMaterialWithDebainRepository();
        var noRepositoryInfo = $root.find('.no-repo label');

        expect(noRepositoryInfo).not.toHaveText('No repositories available.');
        expect($root.find('.no-repo .add-button')).toExist();
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

      it('should have the first repository selected by default in the repository dropdown', function () {
        setMaterialWithDebainRepository();
        var repositoryInfo   = $root.find('.repo-selector');
        var defaultSelection = $(repositoryInfo).find("select[data-prop-name='defaultRepoId']");

        expect(defaultSelection).toHaveValue('e9745dc7-aaeb-48a8-a22a-fa206ad0637e');
      });


      it('should change the repository on selection in the repository dropdown', function () {
        setMaterialWithDebainRepository();
        var repositoryInfo   = $root.find('.repo-selector');
        var defaultSelection = $(repositoryInfo).find("select[data-prop-name='defaultRepoId']");
        expect(defaultSelection).toHaveValue('e9745dc7-aaeb-48a8-a22a-fa206ad0637e');

        $(defaultSelection).val('repo2');
        m.redraw(true);

        defaultSelection = $(repositoryInfo).find("select[data-prop-name='defaultRepoId']");
        expect($(defaultSelection).find("option:selected")).toHaveText('repo2Name');

      });

      it('should change the edit repository information on change of repository selector', function () {
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

        var defaultSelection = $root.find("select[data-prop-name='defaultRepoId']");
        $(defaultSelection).val('repo2').trigger('change');
        m.redraw(true);

        editRepositoryBox            = $root.find('.repository');
        editRepositoryLabelNames = _.map($(editRepositoryBox).find('label'), function (label) {
          return $(label).text();
        });

        editRepositoryInformation = _.map($(editRepositoryBox).find('span'), function (span) {
          return $(span).text();
        });

        expect(editRepositoryLabelNames).toEqual(['Name', 'Plugin', 'Repo_url', 'Username', 'Password']);
        expect(editRepositoryInformation).toEqual(['repo2Name', 'Deb plugin', 'http://', 'first', '***********']);

      });

      describe("Repository modal actions", function () {
        var deferred, requestArgs;

        beforeEach(function () {
          setMaterialWithDebainRepository();
          deferred = $.Deferred();
          spyOn(m, 'request').and.returnValue(deferred.promise());
        });

        afterEach(function () {
          removeModal();
        });

        it('should reveal the new repository modal on click of the create new repository button', function () {
          var createRepoButton = $root.find(".add-button");
          $(createRepoButton[0]).click();
          m.redraw(true);
          expect($('.reveal:visible')).toBeInDOM();
        });

        it('should reveal the edit repository modal on click of the edit button', function () {
          var editRepoButton = $root.find('.edit');
          $(editRepoButton).click();
          m.redraw(true);
          expect($('.reveal:visible')).toBeInDOM();
        });

        it('should post to repositories url on click of the save button', function () {
          var createRepoButton = $root.find(".add-button");
          $(createRepoButton[0]).click();
          m.redraw(true);
          var saveButton = $.find('.reveal:visible .modal-buttons .save');
          $(saveButton).click();
          m.redraw(true);
          requestArgs = m.request.calls.all()[2].args[0];
          expect(requestArgs.url).toBe('/go/api/admin/repositories');
          expect(requestArgs.method).toBe('POST');
        });

        it('should put to repositories url on click of the save button while editing', function () {
          var editRepoButton = $root.find(".edit");
          $(editRepoButton).click();
          m.redraw(true);
          var saveButton = $.find('.reveal:visible .modal-buttons .save');
          $(saveButton).click();
          m.redraw(true);
          requestArgs = m.request.calls.all()[1].args[0];

          expect(requestArgs.url).toBe('/go/api/admin/repositories/e9745dc7-aaeb-48a8-a22a-fa206ad0637e');
          expect(requestArgs.method).toBe('PUT');
        });
      });

    });
  });
});
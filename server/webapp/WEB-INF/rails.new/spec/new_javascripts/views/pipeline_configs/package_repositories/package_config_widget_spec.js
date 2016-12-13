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

define(["jquery", "mithril", "lodash", "views/pipeline_configs/package_repositories/package_config_widget", "models/pipeline_configs/materials",
  'models/pipeline_configs/packages', 'models/pipeline_configs/repositories', 'models/pipeline_configs/plugin_infos'
], function ($, m, _, PackageConfigWidget, Materials, Packages, Repositories, PluginInfos) {

  describe("Package Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    /* eslint-disable camelcase */
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

    var repositoryWithPackages = {
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
        "packages": [
          {
            "id":   "packageId",
            "name": "packageName"
          },
          {
            "id":   'secondPackageId',
            "name": "secondPackageName"
          }
        ]
      }
    };

    var packageConfig = {
      id:            'packageId',
      name:          'packageName',
      auto_update:   false,
      configuration: [
        {
          key:   'PACKAGE_NAME',
          value: 'plugin'
        },
        {
          key:   'VERSION_SPEC',
          value: 'version'
        },
        {
          key:   'ARCHITECTURE',
          value: 'jar'
        }
      ],
      package_repo:  {
        id:   'repo-id',
        name: 'repoName'
      }
    };

    var secondPackageConfig = {
      id:            'secondPackageId',
      name:          'secondPackageName',
      auto_update:   false,
      configuration: [
        {
          key:   'PACKAGE_NAME',
          value: 'package'
        },
        {
          key:   'VERSION_SPEC',
          value: 'version'
        },
        {
          key:   'ARCHITECTURE',
          value: 'war'
        }
      ],
      package_repo:  {
        id:   'repo-id',
        name: 'repoName'
      }
    };
    /* eslint-enable camelcase */

    var removeModal = function () {
      $('.modal-parent').each(function (_i, elem) {
        $(elem).data('modal').destroy();
      });
    };

    var mount = function (material) {
      m.mount(root,
        m.component(PackageConfigWidget,
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

      jasmine.Ajax.stubRequest('/go/api/admin/packages/packageId', undefined, 'GET').andReturn({
        responseText: JSON.stringify(packageConfig),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/packages/secondPackageId', undefined, 'GET').andReturn({
        responseText: JSON.stringify(secondPackageConfig),
        status:       200
      });

      jasmine.Ajax.stubRequest('/go/api/admin/plugin_info/deb', undefined, 'GET').andReturn({
        responseText: JSON.stringify(debPluginInfoJSON),
        status:       200
      });

      pkgMaterial = new Materials().createMaterial({
        type: 'package'
      });

      var repository = new Repositories.Repository(repositoryJSON);
      pkgMaterial.repository(repository);
      mount(pkgMaterial);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();
      PluginInfos([]);
      Repositories([]);
      m.mount(root, null);
      m.redraw(true);
    });


    var setMaterialWithPackage = function () {
      var repository      = new Repositories.Repository(repositoryWithPackages);
      var packageMaterial = new Packages.Package(packageConfig);
      var pluginInfo      = new PluginInfos.PluginInfo(debPluginInfoJSON);
      pkgMaterial.repository(repository);
      pkgMaterial.ref(packageMaterial.id());
      Repositories([repository]);
      PluginInfos([pluginInfo]);
      mount(pkgMaterial);
    };

    describe("Package Widget", function () {
      it("should give button to create package if no package exists", function () {
        var noPackageInfo       = $root.find('.no-package label');
        var createNewRepoButton = $root.find('.no-package .add-button');

        expect(noPackageInfo).toHaveText('No packages available in this repository.');
        expect(createNewRepoButton).toHaveText('Create New Package');
      });


      it("should give button to add new repository", function () {
        setMaterialWithPackage();
        var noRepositoryInfo = $root.find('.no-package label');

        expect(noRepositoryInfo).not.toHaveText('No packages available in this repository.');
        expect($root.find('.no-package .add-button')).toExist();
      });

      it('should show edit package information', function () {
        setMaterialWithPackage();

        var editRepositoryBox = $root.find('.package');
        expect($(editRepositoryBox).find('button')).toExist();

        var editRepositoryLabelNames = _.map($(editRepositoryBox).find('label'), function (label) {
          return $(label).text();
        });

        var editRepositoryInformation = _.map($(editRepositoryBox).find('span'), function (span) {
          return $(span).text();
        });

        expect(editRepositoryLabelNames).toEqual(['Name', 'AutoUpdate', 'Package_name', 'Version_spec', 'Architecture']);
        expect(editRepositoryInformation).toEqual(['packageName', 'false', 'plugin', 'version', 'jar']);

      });

      it('should have the first package selected by default in the package dropdown', function () {
        setMaterialWithPackage();
        var packageInformation = $root.find('.package-selector');
        var defaultSelection   = $(packageInformation).find("select[data-prop-name='defaultPackageId']");

        expect(defaultSelection).toHaveValue('packageId');
      });


      it('should change the package on selection in the package dropdown', function () {
        setMaterialWithPackage();
        var packageInfo      = $root.find('.package-selector');
        var defaultSelection = $(packageInfo).find("select[data-prop-name='defaultPackageId']");
        expect(defaultSelection).toHaveValue('packageId');


        $(defaultSelection).val('secondPackageId');
        m.redraw(true);

        defaultSelection = $(packageInfo).find("select[data-prop-name='defaultPackageId']");
        expect($(defaultSelection).find("option:selected")).toHaveText('secondPackageName');
      });

      it('should change the edit package information on change of package selector', function () {

        setMaterialWithPackage();
        var packageInfo       = $root.find('.package-selector');
        var editRepositoryBox = $root.find('.package');
        expect($(editRepositoryBox).find('button')).toExist();

        var editRepositoryLabelNames = _.map($(editRepositoryBox).find('label'), function (label) {
          return $(label).text();
        });

        var editRepositoryInformation = _.map($(editRepositoryBox).find('span'), function (span) {
          return $(span).text();
        });

        expect(editRepositoryLabelNames).toEqual(['Name', 'AutoUpdate', 'Package_name', 'Version_spec', 'Architecture']);
        expect(editRepositoryInformation).toEqual(['packageName', 'false', 'plugin', 'version', 'jar']);


        var defaultSelection = $(packageInfo).find("select[data-prop-name='defaultPackageId']");
        $(defaultSelection).val('secondPackageId').trigger('change');
        m.redraw(true);

        editRepositoryBox        = $root.find('.package');
        editRepositoryLabelNames = _.map($(editRepositoryBox).find('label'), function (label) {
          return $(label).text();
        });

        editRepositoryInformation = _.map($(editRepositoryBox).find('span'), function (span) {
          return $(span).text();
        });

        expect(editRepositoryLabelNames).toEqual(['Name', 'AutoUpdate', 'Package_name', 'Version_spec', 'Architecture']);
        expect(editRepositoryInformation).toEqual(['secondPackageName', 'false', 'package', 'version', 'war']);

      });

      describe("Repository modal actions", function () {
        var deferred, requestArgs;

        beforeEach(function () {
          setMaterialWithPackage();
          deferred = $.Deferred();
          spyOn(m, 'request').and.returnValue(deferred.promise());
        });

        afterEach(function () {
          removeModal();
        });

        it('should reveal the new package modal on click of the create new package button', function () {
          var createPackageButton = $root.find(".no-package .add-button");
          $(createPackageButton[0]).click();
          m.redraw(true);
          expect($('.reveal:visible')).toBeInDOM();
        });

        it('should reveal the edit package modal on click of the edit button', function () {
          var editPackageButton = $root.find('.package .edit');
          $(editPackageButton).click();
          m.redraw(true);
          expect($('.reveal:visible')).toBeInDOM();
        });

        it('should post to packages url on click of the save button', function () {
          var createPackageButton = $root.find(".no-package .add-button");
          $(createPackageButton[0]).click();
          m.redraw(true);
          var saveButton = $.find('.reveal:visible .modal-buttons .save');
          $(saveButton).click();
          m.redraw(true);
          requestArgs = m.request.calls.all()[2].args[0];
          expect(requestArgs.url).toBe('/go/api/admin/packages');
          expect(requestArgs.method).toBe('POST');
        });

        it('should put to packages url on click of the save button while editing', function () {
          var editPackageButton = $root.find(".package .edit");
          $(editPackageButton).click();
          m.redraw(true);
          var saveButton = $.find('.reveal:visible .modal-buttons .save');
          $(saveButton).click();
          m.redraw(true);
          requestArgs = m.request.calls.all()[1].args[0];

          expect(requestArgs.url).toBe('/go/api/admin/packages/packageId');
          expect(requestArgs.method).toBe('PUT');
        });
      });

    });
  });
});
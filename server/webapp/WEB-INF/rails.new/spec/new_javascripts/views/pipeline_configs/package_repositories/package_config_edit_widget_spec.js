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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/package_config_edit_widget", "models/pipeline_configs/materials",
    'models/pipeline_configs/packages', 'models/pipeline_configs/repositories'],
  function ($, m, PackageConfigEditWidget, Materials, Packages, Repositories) {

    describe("PackageConfigEditWidget", function () {
      var packageMaterial, repository;
      var $root = $('#mithril-mount-point'), root = $root.get(0);

      var repoConfig = {
        repo_id:         'repo-id',
        name:            'repoName',
        plugin_metadata: {
          id:      'deb',
          version: '1'
        },
        configuration:   [
          {
            key:   'REPO_URL',
            value: 'http://package-repo'
          }
        ],
        _embedded:       {
          packages: [
            {
              id: 'packageId',
              name: 'packageName'
            }
          ]
        }
      };

      var packageConfig = {
        id: 'packageId',
        name: 'packageName',
        auto_update: false,
        configuration: [
          {
            key: 'PACKAGE_NAME',
            value: 'plugin'
          }
        ],
        package_repo: {
          id: 'repo-id',
          name: 'repoName'
        }
      };

      var mount = function (packageForEdit, repository) {
        m.mount(root,
          m.component(PackageConfigEditWidget,
            {
              'packageForEdit': packageForEdit,
              'repository':     repository,
              'vm':             new Packages.vm()
            })
        );
        m.redraw(true);
      };

      beforeEach(function () {
        packageMaterial = m.prop(new Packages.Package(packageConfig));
        repository  = new Repositories.Repository(repoConfig);
        mount(packageMaterial, repository);
      });

      afterEach(function () {
        m.mount(root, null);
        m.redraw(true);
      });

      it('should have prefilled input for package name', function () {
        var inputForName = $root.find(".modal-content input[data-prop-name='name']");
        expect(inputForName).toHaveValue('packageName')
      });

      it('auto update checkbox should not be checked', function () {
        expect($root.find(".modal-content input[data-prop-name='autoUpdate']")).not.toBeChecked();
      });

      it('should change the package model if name is changed', function () {
        var inputForName = $root.find(".modal-content input[data-prop-name='name']");
        $(inputForName).val('newPackageName').trigger('input');
        m.redraw(true);
        expect(packageMaterial().name()).toBe('newPackageName');
      });

      it('should change the package model if auto update value is changed', function () {
        var autoUpdate = $root.find(".modal-content input[data-prop-name='autoUpdate']");
        expect(packageMaterial().autoUpdate()).toBe(false);
        $(autoUpdate).click();
        m.redraw(true);
        expect(packageMaterial().autoUpdate()).toBe(true);
      });
    });
  });
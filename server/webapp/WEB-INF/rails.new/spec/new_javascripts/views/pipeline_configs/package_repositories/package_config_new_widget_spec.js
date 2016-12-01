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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/package_config_new_widget", "models/pipeline_configs/materials",
    'models/pipeline_configs/packages', 'models/pipeline_configs/repositories'],
  function ($, m, PackageConfigNewWidget, Materials, Packages, Repositories) {

    describe("PackageConfigNewWidget", function () {
      var packageMaterial;
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
          packages: []
        }
      };


      var mount = function (packageForEdit, repository) {
        m.mount(root,
          m.component(PackageConfigNewWidget,
            {
              'packageForEdit': packageForEdit,
              'repository':     repository,
              'vm':             new Packages.vm()
            })
        );
        m.redraw(true);
      };

      beforeEach(function () {
        packageMaterial = m.prop(new Packages.Package({}));
        var repository  = new Repositories.Repository(repoConfig);
        mount(packageMaterial, repository);
      });

      afterEach(function () {
        m.mount(root, null);
        m.redraw(true);
      });

      it("should have input for package name", function () {
        var modal = $root.find('.modal-content');
        expect(modal).toContainElement("input[data-prop-name='name']");
        var labels = $(modal).find('label');
        expect(labels[0]).toContainText("Name");
      });

      it('should have a checkbox for auto update', function () {
        var modal = $root.find('.modal-content');
        expect(modal).toContainElement("input[data-prop-name='autoUpdate']");
      });

      it('should default the value of autoupdate to true', function () {
        expect($root.find(".modal-content input[data-prop-name='autoUpdate']")).toBeChecked();
      });

      it('should change the package model if name is changed', function () {
        var inputForName = $root.find(".modal-content input[data-prop-name='name']");
        $(inputForName).val('newPackage').trigger('input');
        m.redraw(true);
        expect(packageMaterial().name()).toBe('newPackage')
      });

      it('should change the package model if auto update value is changed', function () {
        var autoUpdate = $root.find(".modal-content input[data-prop-name='autoUpdate']");
        expect(packageMaterial().autoUpdate()).toBe(true);
        $(autoUpdate).click();
        m.redraw(true);
        expect(packageMaterial().autoUpdate()).toBe(false);
      });
    });
  });
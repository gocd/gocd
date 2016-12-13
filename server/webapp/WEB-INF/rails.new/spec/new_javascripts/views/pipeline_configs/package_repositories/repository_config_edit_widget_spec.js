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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/repository_config_edit_widget", 'models/pipeline_configs/repositories'
], function ($, m, RepositoryConfigEditWidget, Repositories) {

  describe("RepositoryConfigNewWidget", function () {
    var repository;
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var mount = function (repository) {
      m.mount(root,
        m.component(RepositoryConfigEditWidget,
          {
            'repoForEdit': repository,
            'vm':          new Repositories.vm()
          })
      );
      m.redraw(true);
    };


    beforeEach(function () {
      repository = m.prop(new Repositories.Repository({
        /* eslint-disable camelcase */
        repo_id:         'repoId',
        name:            'repoName',
        plugin_metadata: {
          id:      'deb',
          version: '12.0'
        },
        configuration:   [{
          key:   'REPO_URL',
          value: 'http://foobar'
        }]
        /* eslint-enable camelcase */
      }));
      mount(repository);
    });

    afterEach(function () {
      m.mount(root, null);
      m.redraw(true);
    });

    describe("Repository Edit Widget", function () {
      it("should have prefilled input for repository name", function () {
        var modal = $root.find('.modal-content');
        expect(modal).toContainElement("input[data-prop-name='name']");
        expect($(modal).find("input[data-prop-name='name']")).toHaveValue('repoName');
      });

      it("should show the repository plugin information", function () {
        var typeOfPlugin = $root.find(".modal-body .key-value span");
        expect(typeOfPlugin).toHaveText('deb');
      });

      it('should change the repository model on changing the repository name', function () {
        var inputForName = $root.find(".modal-content input[data-prop-name='name']");
        $(inputForName).val("updatedRepoName").trigger('input');
        m.redraw(true);
        expect(repository().name()).toEqual('updatedRepoName');
        expect(inputForName).toHaveValue('updatedRepoName');
      });
    });
  });
});
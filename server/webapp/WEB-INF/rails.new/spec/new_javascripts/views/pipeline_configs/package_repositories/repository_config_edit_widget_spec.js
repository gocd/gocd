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

define(["jquery", "mithril", "views/pipeline_configs/package_repositories/repo_config_modal_widget", 'models/pipeline_configs/repositories'
], function ($, m, RepositoryConfigEditWidget, Repositories) {

  describe("RepositoryConfigEditWidget", function () {
    var repository;
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var repo1                      = {
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
      ]
    };
    var repo2                      = {
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
      ]
    };

    //var allRepositoriesJSON = {
    //  _embedded: {
    //    package_repositories: [
    //      repo1,
    //      repo2
    //    ]
    //  }
    //};
    var allRepositories = Repositories([
      Repositories.Repository.fromJSON(repo1),
      Repositories.Repository.fromJSON(repo2)
    ]);

    var mount = function (repository) {
      m.mount(root,
        m.component(RepositoryConfigEditWidget,
          {
            'repoForEdit': repository,
            'repositories': allRepositories,
            'vm':          new Repositories.vm()
          })
      );
      m.redraw(true);
    };


    beforeEach(function () {
      repository = m.prop(Repositories.Repository.fromJSON({
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
        var typeOfPlugin = $root.find(".modal-body p");
        expect(typeOfPlugin).toContainText('deb');
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
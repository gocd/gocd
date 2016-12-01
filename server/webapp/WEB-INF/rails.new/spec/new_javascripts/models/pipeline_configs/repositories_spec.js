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

define(['jquery', 'mithril', 'lodash', 'models/pipeline_configs/repositories'],
  function ($, m, _, Repositories) {
    describe('Repositories', function () {
      describe('init', function () {
        var requestArgs;

        beforeEach(function () {
          spyOn(m, 'request').and.returnValue($.Deferred());
        });

        it('should fetch all repositories', function () {
          Repositories.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/repositories');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          Repositories.init();

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should unwrap the response data to return list of repositories', function () {
          Repositories.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          var repositories = {
            _embedded: {
              /* eslint-disable camelcase */
              package_repositories: [
                {id: '1', name: 'repo_1'},
                {id: '2', name: 'repo_2'}
              ]
              /* eslint-enable camelcase */
            }
          };

          expect(requestArgs.unwrapSuccess(repositories)).toEqual(repositories._embedded.package_repositories);
        });
      });

      describe('repository', function () {
        describe('constructor', function () {
          var repository;
          beforeAll(function () {
            repository = new Repositories.Repository({
              /* eslint-disable camelcase */
              repo_id:         'repositoryId',
              name:            'repo',
              plugin_metadata: {id: 'deb', version: '1.1'},
              configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
              _embedded: {packages: []}
              /* eslint-enable camelcase */
            });
          });

          it('should initialize model with id', function () {
            expect(repository.id()).toBe('repositoryId');
          });

          it('should initialize model with the name', function () {
            expect(repository.name()).toBe('repo');
          });

          it('should initialize model with plugin_metadata', function () {
            expect(repository.pluginMetadata().id()).toBe('deb');
            expect(repository.pluginMetadata().version()).toBe('1.1');
          });

          it('should initialize model with configuration', function () {
            expect(repository.configuration().collectConfigurationProperty('key')).toEqual(['REPO_URL', 'username']);
            expect(repository.configuration().collectConfigurationProperty('value')).toEqual(['path/to/repo', 'some_name']);
          });


          it('should not default repo_id of not provided', function () {
            var repo = new Repositories.Repository({
              /* eslint-disable camelcase */
              name:            'repo',
              plugin_metadata: {id: 'deb', version: '1.1'},
              configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}],
              _embedded: {packages: []}
              /* eslint-enable camelcase */
            });
            expect(repo.id()).toBe(undefined);
          });

        });

        describe('create', function () {
          var repository, requestArgs, deferred;

          repository = new Repositories.Repository({
            name:            'repo',
            plugin_metadata: {id: 'deb', version: '1.1'}, //eslint-disable-line camelcase
            configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
            _embedded: {packages: []}
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());

            repository.create();
            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            Repositories.repoIdToEtag = {};
          });

          it('should post to repositories endpoint', function () {
            expect(requestArgs.method).toBe('POST');
            expect(requestArgs.url).toBe('/go/api/admin/repositories');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
          });

          it('should post repository json', function () {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              name:            'repo',
              plugin_metadata: {id: 'deb', version: '1.1'},
              configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function () {
            var xhr = {
              status:            200,
              getResponseHeader: m.prop(),
              responseText:      JSON.stringify({repo_id: 'new_id'})
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_for_repo');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(Repositories.repoIdToEtag).toEqual({'new_id': 'etag_for_repo'});
          });
        });

        describe('update', function () {
          var repository, requestArgs, deferred;

          repository = new Repositories.Repository({
            /* eslint-disable camelcase */
            repo_id:         '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
            name:            'repo',
            plugin_metadata: {id: 'deb', version: '1.1'},
            configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
            _embedded: {packages: []}
            /* eslint-enable camelcase */
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());
            Repositories.repoIdToEtag['43c45e0b-1b0c-46f3-a60a-2bbc5cec069c'] = 'etag';

            repository.update();

            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            Repositories.repoIdToEtag = {};
          });

          it('should put to repository endpoint', function () {
            expect(requestArgs.method).toBe('PUT');
            expect(requestArgs.url).toBe('/go/api/admin/repositories/43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("If-Match", "etag");
          });

          it('should post repository json', function () {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              repo_id:         '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              name:            'repo',
              plugin_metadata: {id: 'deb', version: '1.1'},
              configuration:   [{key: 'REPO_URL', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function () {
            Repositories.repoIdToEtag[repository.id()] = 'etag_before_update';
            var xhr                                    = {
              status:            200,
              getResponseHeader: m.prop()
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_after_update');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(Repositories.repoIdToEtag).toEqual({'43c45e0b-1b0c-46f3-a60a-2bbc5cec069c': 'etag_after_update'});
          });
        });

      });

      describe('findById', function () {
        var requestArgs, deferred;

        beforeAll(function () {
          Repositories([
            new Repositories.Repository({
              repo_id:         'repo_id_1',
              name:            'repo_1',
              plugin_metadata: {id: 'deb', version: '1.1'}, //eslint-disable-line camelcase
              _embedded: {packages: []}

            }),
            new Repositories.Repository({
              repo_id:         'repo_id_2',
              name:            'repo_2',
              plugin_metadata: {id: 'npm', version: '1.1'}, //eslint-disable-line camelcase
              _embedded: {packages: []}
            })
          ]);

          deferred = $.Deferred();
          spyOn(m, 'request').and.returnValue(deferred.promise());
        });

        afterAll(function () {
          Repositories([]);
          Repositories.repoIdToEtag = {};
        });

        it('should fetch repo for a given repo_id', function () {
          Repositories.findById('repo_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/repositories/repo_id_2');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          Repositories.findById('repo_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should serialize the returned json to repository', function () {
          Repositories.findById('repo_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.type).toBe(Repositories.Repository);
        });

        it('should return null if no repository found for the given id', function () {
          expect(Repositories.findById('invalid_plugin_id')).toBe(null);
        });

        it('should stop page redraw', function () {
          //expect(requestArgs.background).toBe(false);
        });

        it('should extract and cache etag for the repository', function () {
          var xhr = {
            getResponseHeader: m.prop()
          };

          spyOn(xhr, 'getResponseHeader').and.returnValue('etag2');

          Repositories.findById('repo_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.extract(xhr);

          expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
          expect(Repositories.repoIdToEtag).toEqual({'repo_id_2': 'etag2'});
        });
      });

      describe('filterByPluginId', function () {
        beforeAll(function () {
          Repositories([
            new Repositories.Repository({
              repo_id:              'repo_id_1',
              plugin_metadata: {id: 'deb', version: '1.1'}, //eslint-disable-line camelcase
              _embedded: {packages: []}

            }),
            new Repositories.Repository({
              repo_id:              'repo_id_2',
              plugin_metadata: {id: 'npm', version: '1.1'}, //eslint-disable-line camelcase
              _embedded: {packages: []}
            }),
            new Repositories.Repository({
              repo_id:              'repo_id_3',
              plugin_metadata: {id: 'deb', version: '1.1'}, //eslint-disable-line camelcase
              _embedded: {packages: []}
            })
          ]);
        });

        afterAll(function () {
          Repositories([]);
        });

        it('should find all repositories for a given plugin id', function () {
          var repositories = Repositories.filterByPluginId('deb');

          expect(repositories.length).toBe(2);
          expect(repositories[0].id()).toBe('repo_id_1');
          expect(repositories[1].id()).toBe('repo_id_3');
        });
      });

      describe('Repositories.Repository.Configurations', function () {
        describe('fromJSON', function () {
          it('should generate a list of configurations', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);

            expect(configurations.countConfiguration()).toBe(2);
            expect(configurations.firstConfiguration().key()).toBe('url');
            expect(configurations.firstConfiguration().value()).toBe('path/to/repo');
            expect(configurations.lastConfiguration().key()).toBe('username');
            expect(configurations.lastConfiguration().value()).toBe('some_name');
          });

          it('should handle secure configurations', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase

            expect(configurations.countConfiguration()).toBe(2);
            expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
            expect(configurations.lastConfiguration().isSecureValue()).toBe(true);
          });
        });

        describe('toJSON', function () {
          it('should serialize to JSON', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);

            expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);
          });

          it('should handle secure configurations', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase

            expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
          });
        });

        describe('setConfiguration', function () {
          it('should add a configuration', function () {
            var configurations = new Repositories.Repository.Configurations([]);

            configurations.setConfiguration('key', 'val');

            expect(configurations.countConfiguration()).toBe(1);
            expect(configurations.firstConfiguration().key()).toBe('key');
            expect(configurations.firstConfiguration().value()).toBe('val');
          });

          it('should update a configuration if present', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}]);

            configurations.setConfiguration('url', 'new/path');

            expect(configurations.countConfiguration()).toBe(1);
            expect(configurations.firstConfiguration().key()).toBe('url');
            expect(configurations.firstConfiguration().value()).toBe('new/path');
          });

          it('should change a secure configuration to unsecure on update', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); //eslint-disable-line camelcase

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

            configurations.setConfiguration('password', 'new_password');

            expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
            expect(configurations.firstConfiguration().value()).toBe('new_password');
          });

          it('should not update a configuration if new value is same as old', function () {
            var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); // eslint-disable-line camelcase

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

            configurations.setConfiguration('password', 'jdbfj+=');

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
            expect(configurations.firstConfiguration().value()).toBe('jdbfj+=');
          });
        });
      });
    });
  });
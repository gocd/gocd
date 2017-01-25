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

define(['lodash','models/pipeline_configs/repositories', 'models/shared/plugin_configurations'],
  function (_, Repositories, PluginConfigurations) {
    describe('Repositories', function () {
      afterEach(function () {
        jasmine.Ajax.uninstall();
      });

      describe('all', function () {
        var repositories = {
          "_embedded": {
            /* eslint-disable camelcase */
            "package_repositories": [
              {"id": '1', "name": 'repo_1'},
              {"id": '2', "name": 'repo_2'}
            ]
            /* eslint-enable camelcase */
          }
        };

        it('should fetch all repositories', function () {
          jasmine.Ajax.withMock(function () {
            jasmine.Ajax.stubRequest('/go/api/admin/repositories').andReturn({
              responseText: JSON.stringify(repositories),
              status:       200
            });

            var successCallback = jasmine.createSpy().and.callFake(function (repos) {
              expect(repos.countRepository()).toBe(2);
            });

            Repositories.all().then(successCallback);
            expect(successCallback).toHaveBeenCalled();

            var request = jasmine.Ajax.requests.mostRecent();

            expect(request.method).toBe('GET');
            expect(request.url).toBe('/go/api/admin/repositories');
            expect(request.requestHeaders['Content-Type']).toContain('application/json');
            expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          });
        });

        it('should call error callback for non-200 response', function () {
          jasmine.Ajax.withMock(function () {
            jasmine.Ajax.stubRequest('/go/api/admin/repositories').andReturn({
              responseText: JSON.stringify({message: 'Unauthorized'}),
              status:       403
            });

            var errorCallback = jasmine.createSpy();

            Repositories.all().then(_.noop, errorCallback);

            expect(errorCallback).toHaveBeenCalledWith('Unauthorized');
          });
        });
      });

      describe('repository', function () {
        describe('constructor', function () {
          var repository;
          beforeAll(function () {
            repository = new Repositories.Repository({
              id:             'repositoryId',
              name:           'repo',
              pluginMetadata: Repositories.Repository.PluginMetadata.fromJSON({id: 'deb', version: '1.1'}),
              configuration:  PluginConfigurations.fromJSON([{key: 'REPO_URL', value: 'path/to/repo'}, {
                key:   'username',
                value: 'some_name'
              }]),
              _embedded:      {packages: []}
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

          it('should default repo id to blank if not provided', function () {
            var repo = new Repositories.Repository({
              name:           'repo',
              pluginMetadata: Repositories.Repository.PluginMetadata.fromJSON({id: 'deb', version: '1.1'}),
              configuration:  PluginConfigurations.fromJSON([{key: 'REPO_URL', value: 'path/to/repo'}]),
              _embedded:      {packages: []}
            });
            expect(repo.id()).toBe('');
          });

        });

        describe('create', function () {
          var repository;

          repository = {
            /* eslint-disable camelcase */
            repo_id: 'repoId',
            name: 'repo',
            plugin_metadata: {
              id: 'deb',
              version: '1.1'
            },
            configuration: [
              {
                key: 'REPO_URL',
                value: 'path/to/repo'
              },
              {
                key: 'username',
                value: 'some_user'
              }
            ]
            /* eslint-enable camelcase */
          };

          it('should post to repositories endpoint', function () {
            var repo = Repositories.Repository.fromJSON(repository);

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/repositories').andReturn({
                responseText: JSON.stringify(repository),
                status:       200
              });

              var successCallback = jasmine.createSpy().and.callFake(function (repository) {
                var newRepository = Repositories.Repository.fromJSON(repository);
                expect(newRepository.name()).toBe('repo');
              });

              repo.create().then(successCallback);
              expect(successCallback).toHaveBeenCalled();

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('POST');
              expect(request.url).toBe('/go/api/admin/repositories');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(JSON.parse(request.params)).toEqual(repository);
            });
          });

          it("should not create a repository and call the error callback on non-200 failure code", function () {
            var repo = Repositories.Repository.fromJSON(repository);

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/repositories', undefined, 'POST').andReturn({
                responseText: JSON.stringify({message: 'Unauthorized'}),
                status:       401
              });

              var errorCallback = jasmine.createSpy();

              repo.create().then(_.noop, errorCallback);

              expect(errorCallback).toHaveBeenCalledWith('Unauthorized');

              expect(jasmine.Ajax.requests.count()).toBe(1);

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('POST');
              expect(JSON.parse(request.params)).toEqual(repository);
              expect(request.url).toBe('/go/api/admin/repositories');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(JSON.parse(request.params)).toEqual(repository);
            });
          });
        });

        describe('update', function () {
          var repository, existingRepositoryJSON;

          repository = {
            /* eslint-disable camelcase */
            name: 'repo',
            plugin_metadata: {
              id: 'deb',
              version: '1.1'
            },
            configuration: [
              {
                key: 'REPO_URL',
                value: 'path/to/repo'
              },
              {
                key: 'username',
                value: 'some_user'
              }
            ]
            /* eslint-enable camelcase */
          };

          existingRepositoryJSON = {
            /* eslint-disable camelcase */
            repo_id: 'repoId',
            name: 'repo',
            plugin_metadata: {
              id: 'deb',
              version: '1.1'
            },
            configuration: [
              {
                key: 'REPO_URL',
                value: 'http://old-path'
              },
              {
                key: 'username',
                value: 'some_user'
              }
            ]
            /* eslint-enable camelcase */
          };

          it('should put to repositories endpoint', function () {
            var repo = Repositories.Repository.fromJSON(existingRepositoryJSON);
            repo.etag('old-etag');
            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/repositories/repoId').andReturn({
                responseText: JSON.stringify(repository),
                status:       200
              });

              var successCallback = jasmine.createSpy().and.callFake(function (repository) {
                var newRepository = Repositories.Repository.fromJSON(repository);
                expect(newRepository.name()).toBe('repo');
              });

              repo.update().then(successCallback);
              expect(successCallback).toHaveBeenCalled();

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('PUT');
              expect(request.url).toBe('/go/api/admin/repositories/repoId');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(request.requestHeaders['If-Match']).toContain(repo.etag());
              expect(JSON.parse(request.params)).toEqual(existingRepositoryJSON);
            });
          });

          it('should update a repository and call error callback on error', function () {
            var repo = Repositories.Repository.fromJSON(existingRepositoryJSON);
            repo.etag("some-etag");

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/repositories/repoId', undefined, 'PUT').andReturn({
                responseText: JSON.stringify({message: 'Unauthorized'}),
                status:       401
              });

              var errorCallback = jasmine.createSpy();

              repo.update().then(_.noop, errorCallback);

              expect(errorCallback).toHaveBeenCalledWith('Unauthorized');

              expect(jasmine.Ajax.requests.count()).toBe(1);

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('PUT');
              expect(request.url).toBe('/go/api/admin/repositories/repoId');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(request.requestHeaders['If-Match']).toBe('some-etag');
              expect(JSON.parse(request.params)).toEqual(existingRepositoryJSON);
            });
          });
        });
      });

      describe('get by id', function () {
        var repo1;
        beforeAll(function () {
          /* eslint-disable camelcase */
          repo1 = {
            repo_id:         'repo_id_1',
            name:            'repo_1',
            plugin_metadata: {id: 'deb', version: '1.1'}, //eslint-disable-line camelcase
            configuration:   [{key: 'REPO_URL', value: 'http://repo'}, {key: 'USERNAME', value: 'user'}],
            _embedded:       {packages: []}

          };
          /* eslint-enable camelcase */
        });

        it('should find a repository and call the success callback', function () {
          jasmine.Ajax.withMock(function () {
            jasmine.Ajax.stubRequest('/go/api/admin/repositories/repo_id_1', undefined, 'GET').andReturn({
              responseText:    JSON.stringify(repo1),
              responseHeaders: {
                ETag: 'foo'
              },
              status:          200
            });

            var successCallback = jasmine.createSpy().and.callFake(function (repo) {
              expect(repo.id()).toBe("repo_id_1");
              expect(repo.pluginMetadata().id()).toBe('deb');
              expect(repo.configuration().collectConfigurationProperty('key')).toEqual(['REPO_URL', 'USERNAME']);
              expect(repo.configuration().collectConfigurationProperty('value')).toEqual(['http://repo', 'user']);
              expect(repo.etag()).toBe("foo");
            });

            Repositories.Repository.get('repo_id_1').then(successCallback);

            expect(successCallback).toHaveBeenCalled();

            expect(jasmine.Ajax.requests.count()).toBe(1);
            var request = jasmine.Ajax.requests.mostRecent();
            expect(request.method).toBe('GET');
            expect(request.url).toBe('/go/api/admin/repositories/repo_id_1');
            expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          });
        });

        it("should find a repository and call the error callback on error", function () {
          jasmine.Ajax.withMock(function () {
            jasmine.Ajax.stubRequest('/go/api/admin/repositories/repo_id_2', undefined, 'GET').andReturn({
              responseText: JSON.stringify({message: 'Unauthorized'}),
              status:       401
            });

            var failureCallback = jasmine.createSpy();

            Repositories.Repository.get('repo_id_2').then(_.noop, failureCallback);

            expect(failureCallback).toHaveBeenCalledWith('Unauthorized');

            expect(jasmine.Ajax.requests.count()).toBe(1);
            var request = jasmine.Ajax.requests.mostRecent();
            expect(request.method).toBe('GET');
            expect(request.url).toBe('/go/api/admin/repositories/repo_id_2');
            expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
          });
        });
      });
    });
  });
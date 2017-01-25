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

define(['lodash', 'models/pipeline_configs/packages', 'models/shared/plugin_configurations'],
  function (_, Packages, PluginConfigurations) {
    describe('packages', function () {
      afterEach(function () {
        jasmine.Ajax.uninstall();
      });
      describe('package', function () {
        describe('constructor', function () {
          var packageMaterial;
          beforeAll(function () {
            packageMaterial = new Packages.Package({
              /* eslint-disable camelcase */
              id:            'packageId',
              name:          'packageName',
              packageRepo:  Packages.Package.PackageRepository.fromJSON({id: 'repo-id', name: 'repoName'}),
              configuration: PluginConfigurations.fromJSON([{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}])
              /* eslint-enable camelcase */
            });
          });

          it('should initialize model with id', function () {
            expect(packageMaterial.id()).toBe('packageId');
          });

          it('should initialize model with the name', function () {
            expect(packageMaterial.name()).toBe('packageName');
          });

          it('should initialize model with packageRepoId', function () {
            expect(packageMaterial.packageRepo().id()).toBe('repo-id');
          });

          it('should initialize model with configuration', function () {
            expect(packageMaterial.configuration().collectConfigurationProperty('key')).toEqual(['PACKAGE_SPEC', 'username']);
            expect(packageMaterial.configuration().collectConfigurationProperty('value')).toEqual(['path/to/repo', 'some_name']);
          });

          it('should default autoUpdate to true if not provided', function () {
            expect(packageMaterial.autoUpdate()).toBe(true);
          });

        });

        describe('create', function () {
          var packageJSON;

          packageJSON = {
            /* eslint-disable camelcase */
            id: 'packageId',
            name: 'pkg',
            auto_update: 'false',
            package_repo: {
              id: 'repoId',
              name: 'repoName'
            },
            configuration: [
              {
                key: 'PACKAGE_SPEC',
                value: '4.*'
              },
              {
                key: 'VERSION',
                value: '12'
              }
            ]
            /* eslint-enable camelcase */
          };

          it('should post to packages endpoint', function () {
            var packageMaterial = Packages.Package.fromJSON(packageJSON);

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages').andReturn({
                responseText: JSON.stringify(packageJSON),
                status:       200
              });

              var successCallback = jasmine.createSpy().and.callFake(function (pkg) {
                var newPackage = Packages.Package.fromJSON(pkg);
                expect(newPackage.name()).toBe('pkg');
              });

              packageMaterial.create().then(successCallback);
              expect(successCallback).toHaveBeenCalled();

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('POST');
              expect(request.url).toBe('/go/api/admin/packages');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(JSON.parse(request.params)).toEqual(packageJSON);
            });
          });

          it("should not create a package and call the error callback on non-200 failure code", function () {
            var packageMaterial = Packages.Package.fromJSON(packageJSON);

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages', undefined, 'POST').andReturn({
                responseText: JSON.stringify({message: 'Unauthorized'}),
                status:       401
              });

              var errorCallback = jasmine.createSpy();

              packageMaterial.create().then(_.noop, errorCallback);

              expect(errorCallback).toHaveBeenCalledWith('Unauthorized');

              expect(jasmine.Ajax.requests.count()).toBe(1);

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('POST');
              expect(JSON.parse(request.params)).toEqual(packageJSON);
              expect(request.url).toBe('/go/api/admin/packages');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(JSON.parse(request.params)).toEqual(packageJSON);
            });
          });
        });

        describe('update', function () {
          var packageJSON, existingPackageJSON;

          packageJSON = {
            /* eslint-disable camelcase */
            id: 'packageId',
            name: 'packageName',
            auto_update: true,
            package_repo: {
              id: 'repoId',
              name: 'repoName'
            },
            configuration: [
              {
                key: 'PACKAGE_SPEC',
                value: '44abc'
              },
              {
                key: 'ARCHITECTURE',
                value: 'jar'
              }
            ]
            /* eslint-enable camelcase */
          };

          existingPackageJSON = {
            /* eslint-disable camelcase */
            id: 'packageId',
            name: 'packageName',
            auto_update: false,
            package_repo: {
              id: 'repoId',
              name: 'repoName'
            },
            configuration: [
              {
                key: 'PACKAGE_SPEC',
                value: 'abc'
              }
            ]
            /* eslint-enable camelcase */
          };

          it('should put to package endpoint', function () {
            var existingPackage = Packages.Package.fromJSON(existingPackageJSON);
            existingPackage.etag('old-etag');
            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages/packageId').andReturn({
                responseText: JSON.stringify(packageJSON),
                status:       200
              });

              var successCallback = jasmine.createSpy().and.callFake(function (pkg) {
                var updatedPackage = Packages.Package.fromJSON(pkg);
                expect(updatedPackage.name()).toBe('packageName');
                expect(updatedPackage.configuration().collectConfigurationProperty('key')).toEqual(['PACKAGE_SPEC', 'ARCHITECTURE']);
                expect(updatedPackage.configuration().collectConfigurationProperty('value')).toEqual(['44abc', 'jar']);
              });

              existingPackage.update().then(successCallback);
              expect(successCallback).toHaveBeenCalled();

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('PUT');
              expect(request.url).toBe('/go/api/admin/packages/packageId');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(request.requestHeaders['If-Match']).toContain(existingPackage.etag());
              expect(JSON.parse(request.params)).toEqual(existingPackageJSON);
            });
          });

          it('should not update a package and call error callback on error', function () {
            var pkg = Packages.Package.fromJSON(existingPackageJSON);
            pkg.etag("some-etag");

            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages/packageId', undefined, 'PUT').andReturn({
                responseText: JSON.stringify({message: 'Someone has modified the entity'}),
                status:       412
              });

              var errorCallback = jasmine.createSpy();

              pkg.update().then(_.noop, errorCallback);

              expect(errorCallback).toHaveBeenCalledWith('Someone has modified the entity');

              expect(jasmine.Ajax.requests.count()).toBe(1);

              var request = jasmine.Ajax.requests.mostRecent();

              expect(request.method).toBe('PUT');
              expect(request.url).toBe('/go/api/admin/packages/packageId');
              expect(request.requestHeaders['Content-Type']).toContain('application/json');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
              expect(request.requestHeaders['If-Match']).toBe('some-etag');
              expect(JSON.parse(request.params)).toEqual(existingPackageJSON);
            });
          });
        });

        describe('get by id', function () {
          var package1;
          beforeAll(function () {
            /* eslint-disable camelcase */
            package1 = {
              id:            'package_id_1',
              name:          'package_1',
              package_repo:  {id: 'repoId', name: 'repoName'},
              configuration: [{key: 'VERSION_SPEC', value: '4.*'}, {key: 'ARCHITECTURE', value: 'jar'}]

            };
            /* eslint-enable camelcase */

          });

          it('should find a repository and call the success callback', function () {
            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages/package_id_1', undefined, 'GET').andReturn({
                responseText:    JSON.stringify(package1),
                responseHeaders: {
                  ETag: 'foo'
                },
                status:          200
              });

              var successCallback = jasmine.createSpy().and.callFake(function (pkg) {
                expect(pkg.id()).toBe("package_id_1");
                expect(pkg.packageRepo().id()).toBe('repoId');
                expect(pkg.configuration().collectConfigurationProperty('key')).toEqual(['VERSION_SPEC', 'ARCHITECTURE']);
                expect(pkg.configuration().collectConfigurationProperty('value')).toEqual(['4.*', 'jar']);
                expect(pkg.etag()).toBe("foo");
              });

              Packages.Package.get('package_id_1').then(successCallback);

              expect(successCallback).toHaveBeenCalled();

              expect(jasmine.Ajax.requests.count()).toBe(1);
              var request = jasmine.Ajax.requests.mostRecent();
              expect(request.method).toBe('GET');
              expect(request.url).toBe('/go/api/admin/packages/package_id_1');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
            });
          });

          it("should find a repository and call the error callback on error", function () {
            jasmine.Ajax.withMock(function () {
              jasmine.Ajax.stubRequest('/go/api/admin/packages/package_id_2', undefined, 'GET').andReturn({
                responseText: JSON.stringify({message: 'Unauthorized'}),
                status:       401
              });

              var failureCallback = jasmine.createSpy();

              Packages.Package.get('package_id_2').then(_.noop, failureCallback);

              expect(failureCallback).toHaveBeenCalledWith('Unauthorized');

              expect(jasmine.Ajax.requests.count()).toBe(1);
              var request = jasmine.Ajax.requests.mostRecent();
              expect(request.method).toBe('GET');
              expect(request.url).toBe('/go/api/admin/packages/package_id_2');
              expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
            });
          });
        });
      });
    });
  });
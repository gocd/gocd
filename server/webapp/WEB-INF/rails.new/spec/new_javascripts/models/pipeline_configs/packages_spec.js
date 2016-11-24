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

define(['jquery', 'mithril', 'lodash', 'models/pipeline_configs/packages', 'models/pipeline_configs/repositories'],
  function ($, m, _, Packages, Repositories) {
    describe('packages', function () {
      //describe('init', function () {
      //  var requestArgs;
      //
      //  beforeEach(function () {
      //    jasmine.Ajax.install();
      //    jasmine.Ajax.stubRequest(/\/api\/admin\/repositories\/repo_id/).andReturn({
      //      "responseText": JSON.stringify({_embedded: {packages: [{id: "one", name: "test"}]}}),
      //      "status":       200
      //    });
      //    jasmine.Ajax.stubRequest(/\/api\/admin\/repositories/).andReturn({
      //      "responseText": JSON.stringify({_embedded: {package_repositories: {_embedded: {packages: [{id: "one", name: "test"}]}}}}),
      //      "status":       200
      //    });
      //  });
      //
      //  afterAll(function () {
      //    jasmine.Ajax.uninstall();
      //  });
      //
      //  it("should fetch all packages' partial information for a given repository", function () {
      //    spyOn(m, 'request').and.returnValue($.Deferred());
      //    Packages.init("repo_id");
      //
      //    requestArgs = m.request.calls.mostRecent().args[0];
      //
      //    expect(requestArgs.method).toBe('GET');
      //    expect(requestArgs.url).toBe('/go/api/admin/repositories/repo_id');
      //  });
      //
      //  it('should unwrap the response data to return list of repositories', function () {
      //    Repositories.init();
      //    Packages.init('repo_id');
      //    expect(Packages(), [{id:"one", name:"test"}]);
      //  });
      //});

      describe('pacakge', function () {
        describe('constructor', function () {
          var packageMaterial;
          beforeAll(function () {
            packageMaterial = new Packages.Package({
              /* eslint-disable camelcase */
              id:            'packageId',
              name:          'packageName',
              package_repo:  {id: 'repo-id', name: 'repoName'},
              configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
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

        });

        describe('create', function () {
          var packageMaterial, requestArgs, deferred;

          packageMaterial = new Packages.Package({
            id:            'packageId',
            name:          'packageName',
            package_repo:  {id: 'repo-id', name: 'repoName'},
            configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());

            packageMaterial.create();
            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            Packages.packageIdToEtag = {};
          });

          it('should post to packages endpoint', function () {
            expect(requestArgs.method).toBe('POST');
            expect(requestArgs.url).toBe('/go/api/admin/packages');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
          });

          it('should post package json', function () {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              id:            'packageId',
              name:          'packageName',
              auto_update:    true,
              package_repo:  {id: 'repo-id', name: 'repoName'},
              configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function () {
            var xhr = {
              status:            200,
              getResponseHeader: m.prop(),
              responseText:      JSON.stringify({id: 'new_id'})
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_for_package');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(Packages.packageIdToEtag).toEqual({'new_id': 'etag_for_package'});
          });
        });

        describe('update', function () {
          var packageMaterial, requestArgs, deferred;

          packageMaterial = new Packages.Package({
            /* eslint-disable camelcase */
            id:            'packageId',
            name:          'packageName',
            package_repo:  {id: 'repo-id', name: 'repoName'},
            configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
            /* eslint-enable camelcase */
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());
            Packages.packageIdToEtag['packageId'] = 'etag';

            packageMaterial.update();

            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            Packages.packageIdToEtag = {};
          });

          it('should put to package endpoint', function () {
            expect(requestArgs.method).toBe('PUT');
            expect(requestArgs.url).toBe('/go/api/admin/packages/packageId');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("If-Match", "etag");
          });

          it('should post package json', function () {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              id:            'packageId',
              name:          'packageName',
              auto_update:   true,
              package_repo:  {id: 'repo-id', name: 'repoName'},
              configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}],
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function () {
            Packages.packageIdToEtag[packageMaterial.id()] = 'etag_before_update';
            var xhr                                        = {
              status:            200,
              getResponseHeader: m.prop()
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_after_update');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(Packages.packageIdToEtag).toEqual({'packageId': 'etag_after_update'});
          });
        });

      });

      describe('findById', function () {
        var requestArgs, deferred;

        beforeAll(function () {
          Packages([
            new Packages.Package({
              /* eslint-disable camelcase */
              id:            'packageId0',
              name:          'packageName',
              package_repo:  {id: 'repo-id', name: 'repoName'},
              configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            }),
            new Packages.Package({
              /* eslint-disable camelcase */
              id:            'packageId1',
              name:          'packageName',
              package_repo:  {id: 'repo-id', name: 'repoName'},
              configuration: [{key: 'PACKAGE_SPEC', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            })
          ]);

          deferred = $.Deferred();
          spyOn(m, 'request').and.returnValue(deferred.promise());
        });

        afterAll(function () {
          Packages([]);
          Packages.packagesIdToEtag = {};
        });

        it('should fetch package for a given package_id', function () {
          Packages.findById('packageId1');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/packages/packageId1');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          Packages.findById('packageId1');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should serialize the returned json to package', function () {
          Packages.findById('packageId1');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.type).toBe(Packages.Package);
        });

        it('should return null if no package found for the given id', function () {
          expect(Packages.findById('invalid_package_id')).toBe(null);
        });

        it('should stop page redraw', function () {
          expect(requestArgs.background).toBe(false);
        });

        it('should extract and cache etag for the package', function () {
          var xhr = {
            getResponseHeader: m.prop()
          };

          spyOn(xhr, 'getResponseHeader').and.returnValue('etag2');

          Packages.findById('packageId1');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.extract(xhr);

          expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
          expect(Packages.packageIdToEtag).toEqual({'packageId1': 'etag2'});
        });
      });

      //describe('Repositories.Repository.Configurations', function () {
      //  describe('fromJSON', function () {
      //    it('should generate a list of configurations', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);
      //
      //      expect(configurations.countConfiguration()).toBe(2);
      //      expect(configurations.firstConfiguration().key()).toBe('url');
      //      expect(configurations.firstConfiguration().value()).toBe('path/to/repo');
      //      expect(configurations.lastConfiguration().key()).toBe('username');
      //      expect(configurations.lastConfiguration().value()).toBe('some_name');
      //    });
      //
      //    it('should handle secure configurations', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
      //
      //      expect(configurations.countConfiguration()).toBe(2);
      //      expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
      //      expect(configurations.lastConfiguration().isSecureValue()).toBe(true);
      //    });
      //  });
      //
      //  describe('toJSON', function () {
      //    it('should serialize to JSON', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);
      //
      //      expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);
      //    });
      //
      //    it('should handle secure configurations', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
      //
      //      expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
      //    });
      //  });
      //
      //  describe('setConfiguration', function () {
      //    it('should add a configuration', function () {
      //      var configurations = new Repositories.Repository.Configurations([]);
      //
      //      configurations.setConfiguration('key', 'val');
      //
      //      expect(configurations.countConfiguration()).toBe(1);
      //      expect(configurations.firstConfiguration().key()).toBe('key');
      //      expect(configurations.firstConfiguration().value()).toBe('val');
      //    });
      //
      //    it('should update a configuration if present', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}]);
      //
      //      configurations.setConfiguration('url', 'new/path');
      //
      //      expect(configurations.countConfiguration()).toBe(1);
      //      expect(configurations.firstConfiguration().key()).toBe('url');
      //      expect(configurations.firstConfiguration().value()).toBe('new/path');
      //    });
      //
      //    it('should change a secure configuration to unsecure on update', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); //eslint-disable-line camelcase
      //
      //      expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
      //
      //      configurations.setConfiguration('password', 'new_password');
      //
      //      expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
      //      expect(configurations.firstConfiguration().value()).toBe('new_password');
      //    });
      //
      //    it('should not update a configuration if new value is same as old', function () {
      //      var configurations = Repositories.Repository.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); // eslint-disable-line camelcase
      //
      //      expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
      //
      //      configurations.setConfiguration('password', 'jdbfj+=');
      //
      //      expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
      //      expect(configurations.firstConfiguration().value()).toBe('jdbfj+=');
      //    });
      //  });
      //});
    });
  });
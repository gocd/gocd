/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(['jquery', 'mithril', 'lodash', 'models/pipeline_configs/scms'],
  function ($, m, _, SCMs) {
    describe('SCMs', function () {
      describe('init', function () {
        var requestArgs;

        beforeEach(function () {
          spyOn(m, 'request').and.returnValue($.Deferred());
        });

        it('should fetch all scms', function () {
          SCMs.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/scms');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          SCMs.init();

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should unwrap the response data to return list of scms', function () {
          SCMs.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          var scms = {
            _embedded: {
              scms: [
                {id: '1', name: 'plugin_1'},
                {id: '2', name: 'plugin_2'}]
            }
          };

          expect(requestArgs.unwrapSuccess(scms)).toEqual(scms._embedded.scms);
        });
      });

      describe('findById', function () {
        var requestArgs, deferred;

        beforeAll(function () {
          SCMs([
            new SCMs.SCM({
              id:              'plugin_id_1',
              name:            'material_1',
              plugin_metadata: {id: 'github.pr', version: '1.1'} //eslint-disable-line camelcase

            }),
            new SCMs.SCM({
              id:              'plugin_id_2',
              name:            'material_2',
              plugin_metadata: {id: 'scm_plugin', version: '1.1'}, //eslint-disable-line camelcase
            })
          ]);

          deferred = $.Deferred();
          spyOn(m, 'request').and.returnValue(deferred.promise());
        });

        afterAll(function() {
          SCMs([]);
          SCMs.scmIdToEtag = {};
        });

        it('should fetch scm for a given id', function () {
          SCMs.findById('plugin_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/scms/material_2');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          SCMs.findById('plugin_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should serialize the returned json to SCM', function () {
          SCMs.findById('plugin_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.type).toBe(SCMs.SCM);
        });

        it('should return null if no SCM found for the given id', function() {
          expect(SCMs.findById('invalid_plugin_id')).toBe(null);
        });

        it('should stop page redraw', function() {
          expect(requestArgs.background).toBe(false);
        });

        it('should extract and cache etag for the scm', function() {
          var xhr = {
            getResponseHeader: m.prop()
          };

          spyOn(xhr, 'getResponseHeader').and.returnValue('etag2');

          SCMs.findById('plugin_id_2');

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.extract(xhr);

          expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
          expect(SCMs.scmIdToEtag).toEqual({'plugin_id_2': 'etag2'});
        });
      });

      describe('filterByPluginId', function () {
        beforeAll(function () {
          SCMs([
            new SCMs.SCM({
              id:              'plugin_id_1',
              plugin_metadata: {id: 'github.pr', version: '1.1'} //eslint-disable-line camelcase

            }),
            new SCMs.SCM({
              id:              'plugin_id_2',
              plugin_metadata: {id: 'scm_plugin', version: '1.1'} //eslint-disable-line camelcase
            }),
            new SCMs.SCM({
              id:              'plugin_id_3',
              plugin_metadata: {id: 'github.pr', version: '1.1'} //eslint-disable-line camelcase
            })
          ]);
        });

        afterAll(function () {
          SCMs([]);
        });

        it('should find all SCMs for a given plugin id', function () {
          var scms = SCMs.filterByPluginId('github.pr');

          expect(scms.length).toBe(2);
          expect(scms[0].id()).toBe('plugin_id_1');
          expect(scms[1].id()).toBe('plugin_id_3');
        });
      });

      describe('SCM', function () {
        describe('constructor', function () {
          var scm;
          beforeAll(function () {
            scm = new SCMs.SCM({
              /* eslint-disable camelcase */
              id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              name:            'material_name',
              auto_update:     true,
              plugin_metadata: {id: 'github.pr', version: '1.1'},
              configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            });
          });

          it('should initialize model with id', function () {
            expect(scm.id()).toBe('43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
          });

          it('should initialize model with name', function () {
            expect(scm.name()).toBe('material_name');
          });

          it('should initialize model with auto_update', function () {
            expect(scm.autoUpdate()).toBe(true);
          });

          it('should initialize model with plugin_metadata', function () {
            expect(scm.pluginMetadata().id()).toBe('github.pr');
            expect(scm.pluginMetadata().version()).toBe('1.1');
          });

          it('should initialize model with configuration', function () {
            expect(scm.configuration().collectConfigurationProperty('key')).toEqual(['url', 'username']);
            expect(scm.configuration().collectConfigurationProperty('value')).toEqual(['path/to/repo', 'some_name']);
          });
        });

        describe('update', function() {
          var scm, requestArgs, deferred;

          scm = new SCMs.SCM({
            /* eslint-disable camelcase */
            id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
            name:            'material_name',
            auto_update:     true,
            plugin_metadata: {id: 'github.pr', version: '1.1'},
            configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
            /* eslint-enable camelcase */
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());
            SCMs.scmIdToEtag['43c45e0b-1b0c-46f3-a60a-2bbc5cec069c'] = 'etag';

            scm.update();

            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            SCMs.scmIdToEtag = {};
          });

          it('should patch to scm endpoint', function () {
            expect(requestArgs.method).toBe('PATCH');
            expect(requestArgs.url).toBe('/go/api/admin/scms/material_name');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("If-Match", "etag");
          });

          it('should post SCM json', function() {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              name:            'material_name',
              auto_update:     true,
              plugin_metadata: {id: 'github.pr', version: '1.1'},
              configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function() {
            SCMs.scmIdToEtag[scm.id()] = 'etag_before_update';
            var xhr = {
              status:            200,
              getResponseHeader: m.prop()
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_after_update');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(SCMs.scmIdToEtag).toEqual({'43c45e0b-1b0c-46f3-a60a-2bbc5cec069c': 'etag_after_update'});
          });
        });

        describe('create', function() {
          var scm, requestArgs, deferred;

          scm = new SCMs.SCM({
            name:            'material_name',
            auto_update:     true, //eslint-disable-line camelcase
            plugin_metadata: {id: 'github.pr', version: '1.1'}, //eslint-disable-line camelcase
            configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
          });

          beforeAll(function () {
            deferred = $.Deferred();
            spyOn(m, 'request').and.returnValue(deferred.promise());

            scm.create();
            requestArgs = m.request.calls.mostRecent().args[0];
          });

          afterAll(function () {
            SCMs.scmIdToEtag = {};
          });

          it('should post to scm endpoint', function () {
            expect(requestArgs.method).toBe('POST');
            expect(requestArgs.url).toBe('/go/api/admin/scms');
          });

          it('should post required headers', function () {
            var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
            requestArgs.config(xhr);

            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
            expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
          });

          it('should post SCM json', function() {
            expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify({
              /* eslint-disable camelcase */
              name:            'material_name',
              auto_update:     true,
              plugin_metadata: {id: 'github.pr', version: '1.1'},
              configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
              /* eslint-enable camelcase */
            }));
          });

          it('should update etag cache on success', function() {
            var xhr = {
              status:            200,
              getResponseHeader: m.prop(),
              responseText : JSON.stringify({id: 'new_id'})
            };

            spyOn(xhr, 'getResponseHeader').and.returnValue('etag_for_scm');

            requestArgs = m.request.calls.mostRecent().args[0];
            requestArgs.extract(xhr);

            expect(xhr.getResponseHeader).toHaveBeenCalledWith('ETag');
            expect(SCMs.scmIdToEtag).toEqual({'new_id': 'etag_for_scm'});
          });
        });

        describe('clone', function() {
          it('should return a cloned copy of the object', function () {
            /* eslint-disable camelcase */
            var scm = new SCMs.SCM({
              id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              name:            'material_name',
              auto_update:     true,
              plugin_metadata: {id: 'github.pr', version: '1.1'},
              configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
            });
            /* eslint-enable camelcase */

            expect(JSON.stringify(scm.clone())).toEqual(JSON.stringify(scm));
          });
        });

        describe('reInitialize', function() {
          it('should re-initilaize with provided data', function () {
            /* eslint-disable camelcase */
            var scm = new SCMs.SCM({
              id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              name:            'material_name',
              auto_update:     true,
              plugin_metadata: {id: 'github.pr', version: '1.1'},
              configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
            });

            var sampleJSON = {
              id:              '1ada7306-d028-415c-960b-1242abcd4834',
              name:            'new_name',
              auto_update:     false,
              plugin_metadata: {id: 'new.github.pr', version: 'new.1.1'},
              configuration:   [{key: 'url', value: 'path/to/new/repo'}, {key: 'username', value: 'new_name'}]
            };
            /* eslint-enable camelcase */

            scm.reInitialize(sampleJSON);

            expect(JSON.stringify(sampleJSON)).toEqual(JSON.stringify(scm));
          });
        });
      });

      describe('SCMs.SCM.Configurations', function () {
        describe('fromJSON', function () {
          it('should generate a list of configurations', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);

            expect(configurations.countConfiguration()).toBe(2);
            expect(configurations.firstConfiguration().key()).toBe('url');
            expect(configurations.firstConfiguration().value()).toBe('path/to/repo');
            expect(configurations.lastConfiguration().key()).toBe('username');
            expect(configurations.lastConfiguration().value()).toBe('some_name');
          });

          it('should handle secure configurations', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase

            expect(configurations.countConfiguration()).toBe(2);
            expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
            expect(configurations.lastConfiguration().isSecureValue()).toBe(true);
          });
        });

        describe('toJSON', function () {
          it('should serialize to JSON', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);

            expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]);
          });

          it('should handle secure configurations', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase

            expect(JSON.parse(JSON.stringify(configurations))).toEqual([{key: 'username', value: 'some_name'}, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
          });
        });

        describe('setConfiguration', function () {
          it('should add a configuration', function () {
            var configurations = new SCMs.SCM.Configurations([]);

            configurations.setConfiguration('key', 'val');

            expect(configurations.countConfiguration()).toBe(1);
            expect(configurations.firstConfiguration().key()).toBe('key');
            expect(configurations.firstConfiguration().value()).toBe('val');
          });

          it('should update a configuration if present', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}]);

            configurations.setConfiguration('url', 'new/path');

            expect(configurations.countConfiguration()).toBe(1);
            expect(configurations.firstConfiguration().key()).toBe('url');
            expect(configurations.firstConfiguration().value()).toBe('new/path');
          });

          it('should change a secure configuration to unsecure on update', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); //eslint-disable-line camelcase

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

            configurations.setConfiguration('password', 'new_password');

            expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
            expect(configurations.firstConfiguration().value()).toBe('new_password');
          });

          it('should not update a configuration if new value is same as old', function () {
            var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); // eslint-disable-line camelcase

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

            configurations.setConfiguration('password', 'jdbfj+=');

            expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
            expect(configurations.firstConfiguration().value()).toBe('jdbfj+=');
          });
        });
      });

    });
  });

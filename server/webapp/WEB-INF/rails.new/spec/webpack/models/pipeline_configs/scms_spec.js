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
xdescribe('SCMs', function () {
  var SCMs = require('models/pipeline_configs/scms');

  describe('init', function () {
    afterEach(function () {
      SCMs([]);
    });

    it('should fetch all scms', function () {
      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest('/go/api/admin/scms', undefined, 'GET').andReturn({
          responseText: JSON.stringify({
            _embedded: {
              scms: [
                {id: 1, name: 'plugin_1'},
                {id: 2, name: 'plugin_2'}]
            }
          }),
          status:       200,
          headers:      {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        var successCallback = jasmine.createSpy().and.callFake(function (scms) {
          expect(scms.length).toBe(2);

          expect(scms[0].id()).toBe(1);
          expect(scms[1].id()).toBe(2);
        });

        SCMs.init().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

  });

  describe('findById', function () {
    beforeEach(function () {
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
    });

    afterEach(function () {
      SCMs([]);
      SCMs.scmIdToEtag = {};
    });

    it('should fetch scm for a given id', function () {
      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest('/go/api/admin/scms/material_2', undefined, 'GET').andReturn({
          responseText:    JSON.stringify({
            id:              'plugin_id_2',
            name:            'material_2',
            plugin_metadata: {id: 'scm_plugin', version: '1.1'}, //eslint-disable-line camelcase
          }),
          status:          200,
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        var successCallback = jasmine.createSpy().and.callFake(function (scm) {
          expect(scm.id()).toBe('plugin_id_2');
          expect(SCMs.scmIdToEtag[scm.id()]).toBe('etag');
        });

        SCMs.findById('plugin_id_2').then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should return null if no SCM found for the given id', function () {
      expect(SCMs.findById('invalid_plugin_id')).toBe(null);
    });
  });

  describe('filterByPluginId', function () {
    beforeEach(function () {
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

    afterEach(function () {
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
      beforeEach(function () {
        scm = new SCMs.SCM({
          /* eslint-disable camelcase */
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'material_name',
          auto_update:     false,
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
        expect(scm.autoUpdate()).toBe(false);
      });

      it('should initialize model with plugin_metadata', function () {
        expect(scm.pluginMetadata().id()).toBe('github.pr');
        expect(scm.pluginMetadata().version()).toBe('1.1');
      });

      it('should initialize model with configuration', function () {
        expect(scm.configuration().collectConfigurationProperty('key')).toEqual(['url', 'username']);
        expect(scm.configuration().collectConfigurationProperty('value')).toEqual(['path/to/repo', 'some_name']);
      });

      it('should default auto_update to true if not provided', function () {
        var pluggableScm = new SCMs.SCM({
          /* eslint-disable camelcase */
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'material_name',
          plugin_metadata: {id: 'github.pr', version: '1.1'},
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
          /* eslint-enable camelcase */
        });
        expect(pluggableScm.autoUpdate()).toBe(true);
      });
    });

    describe('update', function () {
      afterEach(function () {
        SCMs([]);
        SCMs.scmIdToEtag = {};
      });

      it('should patch to scm endpoint', function () {
        let json = {
          /* eslint-disable camelcase */
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'material_name',
          auto_update:     true,
          plugin_metadata: {id: 'github.pr', version: '1.1'},
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
          /* eslint-enable camelcase */
        };

        var scm = new SCMs.SCM(json);

        SCMs.scmIdToEtag['43c45e0b-1b0c-46f3-a60a-2bbc5cec069c'] = 'etag';

        jasmine.Ajax.withMock(function () {
          jasmine.Ajax.stubRequest('/go/api/admin/scms/material_name', undefined, 'PATCH').andReturn({
            responseText:    JSON.stringify(json),
            status:          200,
            responseHeaders: {
              ETag:           'some-etag',
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          var successCallback = jasmine.createSpy().and.callFake(function (scm) {
            expect(scm.name()).toBe('material_name');
            expect(SCMs.scmIdToEtag[scm.id()]).toBe('some-etag');
          });

          scm.update().then(successCallback);
          expect(successCallback).toHaveBeenCalled();
        });
      });

    });

    describe('create', function () {
      afterEach(function () {
        SCMs([]);
        SCMs.scmIdToEtag = {};
      });

      it('should post to scm endpoint', function () {
        let json = {
          name:            'material_name',
          auto_update:     true, //eslint-disable-line camelcase
          plugin_metadata: {id: 'github.pr', version: '1.1'}, //eslint-disable-line camelcase
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        };

        var scm = new SCMs.SCM(json);

        jasmine.Ajax.withMock(function () {
          jasmine.Ajax.stubRequest('/go/api/admin/scms', undefined, 'POST').andReturn({
            responseText:    JSON.stringify(json),
            status:          200,
            responseHeaders: {
              ETag:           'some-etag',
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          var successCallback = jasmine.createSpy().and.callFake(function (scm) {
            expect(scm.name()).toBe('material_name');
            expect(SCMs.scmIdToEtag[scm.id()]).toBe('some-etag');
          });

          scm.create().then(successCallback);
          expect(successCallback).toHaveBeenCalled();
        });
      });
    });

    describe('clone', function () {
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

    describe('reInitialize', function () {
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
        var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {
          key:   'username',
          value: 'some_name'
        }]);

        expect(configurations.countConfiguration()).toBe(2);
        expect(configurations.firstConfiguration().key()).toBe('url');
        expect(configurations.firstConfiguration().value()).toBe('path/to/repo');
        expect(configurations.lastConfiguration().key()).toBe('username');
        expect(configurations.lastConfiguration().value()).toBe('some_name');
      });

      it('should handle secure configurations', function () {
        var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {
          key:             'password',
          encrypted_value: 'adkfkk=' // eslint-disable-line camelcase
        }]);

        expect(configurations.countConfiguration()).toBe(2);
        expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
        expect(configurations.lastConfiguration().isSecureValue()).toBe(true);
      });
    });

    describe('toJSON', function () {
      it('should serialize to JSON', function () {
        var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {
          key:   'username',
          value: 'some_name'
        }]);

        expect(JSON.parse(JSON.stringify(configurations))).toEqual([{
          key:   'url',
          value: 'path/to/repo'
        }, {key: 'username', value: 'some_name'}]);
      });

      it('should handle secure configurations', function () {
        var configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {
          key:             'password',
          encrypted_value: 'adkfkk=' // eslint-disable-line camelcase
        }]);

        expect(JSON.parse(JSON.stringify(configurations))).toEqual([{
          key:   'username',
          value: 'some_name'
        }, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
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

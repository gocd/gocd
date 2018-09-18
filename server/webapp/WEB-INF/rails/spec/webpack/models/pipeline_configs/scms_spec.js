/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe('SCMs', () => {
  const SCMs = require('models/pipeline_configs/scms');

  describe('init', () => {
    afterEach(() => {
      SCMs([]);
    });

    it('should fetch all scms', () => {
      jasmine.Ajax.withMock(() => {
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
            'Content-Type': 'application/vnd.go.cd.v2+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake(() => {
          expect(SCMs().length).toBe(2);

          expect(SCMs()[0].id()).toBe(1);
          expect(SCMs()[1].id()).toBe(2);
        });

        expect(SCMs().length).toBe(0);
        SCMs.init().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

  });

  describe('findById', () => {
    beforeEach(() => {
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

    afterEach(() => {
      SCMs([]);
      SCMs.scmIdToEtag = {};
    });

    it('should fetch scm for a given id', () => {
      jasmine.Ajax.withMock(() => {
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

        const successCallback = jasmine.createSpy().and.callFake((scm) => {
          expect(scm.id()).toBe('plugin_id_2');
          expect(SCMs.scmIdToEtag[scm.id()]).toBe('etag');
        });

        SCMs.findById('plugin_id_2').then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should return null if no SCM found for the given id', () => {
      expect(SCMs.findById('invalid_plugin_id')).toBe(null);
    });
  });

  describe('filterByPluginId', () => {
    beforeEach(() => {
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

    afterEach(() => {
      SCMs([]);
    });

    it('should find all SCMs for a given plugin id', () => {
      const scms = SCMs.filterByPluginId('github.pr');

      expect(scms.length).toBe(2);
      expect(scms[0].id()).toBe('plugin_id_1');
      expect(scms[1].id()).toBe('plugin_id_3');
    });
  });

  describe('SCM', () => {
    describe('constructor', () => {
      let scm;
      beforeEach(() => {
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

      it('should initialize model with id', () => {
        expect(scm.id()).toBe('43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
      });

      it('should initialize model with name', () => {
        expect(scm.name()).toBe('material_name');
      });

      it('should initialize model with auto_update', () => {
        expect(scm.autoUpdate()).toBe(false);
      });

      it('should initialize model with plugin_metadata', () => {
        expect(scm.pluginMetadata().id()).toBe('github.pr');
        expect(scm.pluginMetadata().version()).toBe('1.1');
      });

      it('should initialize model with configuration', () => {
        expect(scm.configuration().collectConfigurationProperty('key')).toEqual(['url', 'username']);
        expect(scm.configuration().collectConfigurationProperty('value')).toEqual(['path/to/repo', 'some_name']);
      });

      it('should default auto_update to true if not provided', () => {
        const pluggableScm = new SCMs.SCM({
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

    describe('update', () => {
      afterEach(() => {
        SCMs([]);
        SCMs.scmIdToEtag = {};
      });

      it('should patch to scm endpoint', () => {
        const json = {
          /* eslint-disable camelcase */
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'material_name',
          auto_update:     true,
          plugin_metadata: {id: 'github.pr', version: '1.1'},
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
          /* eslint-enable camelcase */
        };

        const scm = new SCMs.SCM(json);

        SCMs.scmIdToEtag['43c45e0b-1b0c-46f3-a60a-2bbc5cec069c'] = 'etag';

        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest('/go/api/admin/scms/material_name', undefined, 'PATCH').andReturn({
            responseText:    JSON.stringify(json),
            status:          200,
            responseHeaders: {
              ETag:           'some-etag',
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          const successCallback = jasmine.createSpy().and.callFake((scm) => {
            expect(scm.name()).toBe('material_name');
            expect(SCMs.scmIdToEtag[scm.id()]).toBe('some-etag');
          });

          scm.update().then(successCallback);
          expect(successCallback).toHaveBeenCalled();
        });
      });

    });

    describe('create', () => {
      afterEach(() => {
        SCMs([]);
        SCMs.scmIdToEtag = {};
      });

      it('should post to scm endpoint', () => {
        const json = {
          name:            'material_name',
          auto_update:     true, //eslint-disable-line camelcase
          plugin_metadata: {id: 'github.pr', version: '1.1'}, //eslint-disable-line camelcase
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        };

        const scm = new SCMs.SCM(json);

        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest('/go/api/admin/scms', undefined, 'POST').andReturn({
            responseText:    JSON.stringify(json),
            status:          200,
            responseHeaders: {
              ETag:           'some-etag',
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          const successCallback = jasmine.createSpy().and.callFake((scm) => {
            expect(scm.name()).toBe('material_name');
            expect(SCMs.scmIdToEtag[scm.id()]).toBe('some-etag');
          });

          scm.create().then(successCallback);
          expect(successCallback).toHaveBeenCalled();
        });
      });
    });

    describe('clone', () => {
      it('should return a cloned copy of the object', () => {
        /* eslint-disable camelcase */
        const scm = new SCMs.SCM({
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

    describe('reInitialize', () => {
      it('should re-initilaize with provided data', () => {
        /* eslint-disable camelcase */
        const scm = new SCMs.SCM({
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'material_name',
          auto_update:     true,
          plugin_metadata: {id: 'github.pr', version: '1.1'},
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        });

        const sampleJSON = {
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

  describe('SCMs.SCM.Configurations', () => {
    describe('fromJSON', () => {
      it('should generate a list of configurations', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {
          key:   'username',
          value: 'some_name'
        }]);

        expect(configurations.countConfiguration()).toBe(2);
        expect(configurations.firstConfiguration().key()).toBe('url');
        expect(configurations.firstConfiguration().value()).toBe('path/to/repo');
        expect(configurations.lastConfiguration().key()).toBe('username');
        expect(configurations.lastConfiguration().value()).toBe('some_name');
      });

      it('should handle secure configurations', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {
          key:             'password',
          encrypted_value: 'adkfkk=' // eslint-disable-line camelcase
        }]);

        expect(configurations.countConfiguration()).toBe(2);
        expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
        expect(configurations.lastConfiguration().isSecureValue()).toBe(true);
      });
    });

    describe('toJSON', () => {
      it('should serialize to JSON', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}, {
          key:   'username',
          value: 'some_name'
        }]);

        expect(JSON.parse(JSON.stringify(configurations))).toEqual([{
          key:   'url',
          value: 'path/to/repo'
        }, {key: 'username', value: 'some_name'}]);
      });

      it('should handle secure configurations', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'username', value: 'some_name'}, {
          key:             'password',
          encrypted_value: 'adkfkk=' // eslint-disable-line camelcase
        }]);

        expect(JSON.parse(JSON.stringify(configurations))).toEqual([{
          key:   'username',
          value: 'some_name'
        }, {key: 'password', encrypted_value: 'adkfkk='}]); // eslint-disable-line camelcase
      });
    });

    describe('setConfiguration', () => {
      it('should add a configuration', () => {
        const configurations = new SCMs.SCM.Configurations([]);

        configurations.setConfiguration('key', 'val');

        expect(configurations.countConfiguration()).toBe(1);
        expect(configurations.firstConfiguration().key()).toBe('key');
        expect(configurations.firstConfiguration().value()).toBe('val');
      });

      it('should update a configuration if present', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'url', value: 'path/to/repo'}]);

        configurations.setConfiguration('url', 'new/path');

        expect(configurations.countConfiguration()).toBe(1);
        expect(configurations.firstConfiguration().key()).toBe('url');
        expect(configurations.firstConfiguration().value()).toBe('new/path');
      });

      xit('should change a secure configuration to unsecure on update', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); //eslint-disable-line camelcase

        expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

        configurations.setConfiguration('password', 'new_password');

        expect(configurations.firstConfiguration().isSecureValue()).toBe(false);
        expect(configurations.firstConfiguration().value()).toBe('new_password');
      });

      xit('should not update a configuration if new value is same as old', () => {
        const configurations = SCMs.SCM.Configurations.fromJSON([{key: 'password', encrypted_value: 'jdbfj+='}]); // eslint-disable-line camelcase

        expect(configurations.firstConfiguration().isSecureValue()).toBe(true);

        configurations.setConfiguration('password', 'jdbfj+=');

        expect(configurations.firstConfiguration().isSecureValue()).toBe(true);
        expect(configurations.firstConfiguration().value()).toBe('jdbfj+=');
      });
    });
  });

});

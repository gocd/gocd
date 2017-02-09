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
describe('PluginInfos', function () {

  var PluginInfos = require("models/pipeline_configs/plugin_infos");

  afterEach(function () {
    PluginInfos([]);
  });

  describe('all', function () {
    it('should fetch all plugin_infos', function () {

      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest('/go/api/admin/plugin_info', undefined, 'GET').andReturn({
          responseText:    JSON.stringify({
            _embedded: {
              plugin_info: [pluginInfoJSON()] // eslint-disable-line camelcase
            }
          }),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v2+json'
          }
        });

        var successCallback = jasmine.createSpy().and.callFake(function (pluginInfos) {
          expect(pluginInfos.length).toEqual(1);
          expect(pluginInfos[0].id()).toEqual(pluginInfoJSON().id);
          expect(pluginInfos[0].name()).toEqual(pluginInfoJSON().name);
        });

        PluginInfos.all().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
      });
    });
  });

  function pluginInfoJSON() {
    /* eslint-disable camelcase */
    let pluginInfoJSON = {
      id:                          'plugin_id',
      name:                        'plugin_name',
      version:                     'plugin_version',
      type:                        'plugin_type',
      display_name:                'Plugin Display Name',
      pluggable_instance_settings: {
        configurations: [
          {
            key:      'url',
            metadata: {required: true, secure: false}
          },
          {
            key:      'username',
            type:     'package',
            metadata: {required: true, secure: false}
          }],
        view:           {
          template: 'plugin_view_template'
        }
      }
    };
    /* eslint-enable camelcase */
    return pluginInfoJSON;
  }

  describe('PluginInfo', function () {
    var pluginInfo;
    beforeEach(function () {
      pluginInfo = new PluginInfos.PluginInfo(pluginInfoJSON());
    });

    it('should initialize with id', function () {
      expect(pluginInfo.id()).toBe('plugin_id');
    });

    it('should initialize with name', function () {
      expect(pluginInfo.name()).toBe('plugin_name');
    });

    it('should initialize with version', function () {
      expect(pluginInfo.version()).toBe('plugin_version');
    });

    it('should initialize with type', function () {
      expect(pluginInfo.type()).toBe('plugin_type');
    });

    it('should initialize with view template', function () {
      expect(pluginInfo.viewTemplate()).toBe('plugin_view_template');
    });

    it('should initialize with display name', function () {
      expect(pluginInfo.displayName()).toBe('Plugin Display Name');
    });

    it('should default to name in absence of display_name', function () {
      var plugin = new PluginInfos.PluginInfo({name: 'plugin_name'});
      expect(plugin.displayName()).toBe('plugin_name');
    });

    it('should initialize with configurations', function () {
      expect(pluginInfo.configurations()).toEqual([
        {
          key:      'url',
          metadata: {required: true, secure: false}
        },
        {
          key:      'username',
          type:     'package',
          metadata: {required: true, secure: false}
        }]);
    });
  });

  describe('PluginInfo.get', function () {
    it('should fetch the plugin for the given id', function () {

      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_info/${pluginInfoJSON().id}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(pluginInfoJSON()),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v2+json'
          }
        });

        var successCallback = jasmine.createSpy().and.callFake(function (pluginInfo) {
          expect(pluginInfo.id()).toEqual(pluginInfoJSON().id);
          expect(pluginInfo.name()).toEqual(pluginInfoJSON().name);
        });

        PluginInfos.PluginInfo.get(pluginInfoJSON().id).then(successCallback);

        expect(successCallback).toHaveBeenCalled();
      });
    });
  });

  describe('filterByType', function () {
    it('should return plugins for the given type', function () {
      var scm            = new PluginInfos.PluginInfo({id: 'id1', type: 'scm'});
      var task1          = new PluginInfos.PluginInfo({id: 'id2', type: 'task'});
      var task2          = new PluginInfos.PluginInfo({id: 'id3', type: 'task'});
      var authentication = new PluginInfos.PluginInfo({id: 'id4', type: 'authentication'});

      PluginInfos([scm, task1, task2, authentication]);
      var pluginInfos = PluginInfos.filterByType('task');

      expect(pluginInfos.length).toBe(2);
      expect(pluginInfos[0].type()).toBe('task');
      expect(pluginInfos[1].type()).toBe('task');
    });
  });

  describe('findById', function () {
    it('should return plugins for the given id', function () {
      var scm            = new PluginInfos.PluginInfo({id: 'id1', type: 'scm'});
      var task1          = new PluginInfos.PluginInfo({id: 'id2', type: 'task'});
      var task2          = new PluginInfos.PluginInfo({id: 'id3', type: 'task'});
      var authentication = new PluginInfos.PluginInfo({id: 'id4', type: 'authentication'});

      PluginInfos([scm, task1, task2, authentication]);
      var pluginInfo = PluginInfos.findById('id2');

      expect(pluginInfo.id()).toBe('id2');
    });
  });
});

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

define(['jquery', 'mithril', 'lodash', "models/pipeline_configs/plugin_infos"], function ($, m, _, PluginInfos) {
  describe('PluginInfos', function () {
    describe('init', function () {
      var requestArgs, deferred;

      beforeAll(function () {
        deferred = $.Deferred();
        spyOn(m, 'request').and.returnValue(deferred.promise());

        PluginInfos.init();

        requestArgs = m.request.calls.mostRecent().args[0];
      });

      it('should fetch all plugin_infos', function () {
        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/plugin_info');
      });

      it('should post required headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
      });

      it('should not stop page redraw', function() {
        expect(requestArgs.background).toBe(true);
      });

      it('should unwrap the response data to return list of plugin_infos', function () {
        var pluginInfos = {_embedded: {plugin_info: ['plugin']}}; //eslint-disable-line camelcase

        expect(requestArgs.unwrapSuccess(pluginInfos)).toEqual(['plugin']);
      });

      it('should deserialize to PluginInfo object', function() {
        expect(requestArgs.type).toBe(PluginInfos.PluginInfo);
      });

      it('should initialize PluginInfos on success', function() {
        deferred.resolve(['plugin']);

        expect(PluginInfos()).toEqual(['plugin']);
      });
    });

    describe('PluginInfo', function() {
      var pluginInfo;
      beforeAll(function () {
        /* eslint-disable camelcase */
        pluginInfo = new PluginInfos.PluginInfo({
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
        });
        /* eslint-enable camelcase */
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
            key:  'username',
            type: 'package',
            metadata: {required: true, secure: false}
          }]);
      });
    });

    describe('PluginInfo.byId', function () {
      var requestArgs, deferred;

      beforeAll(function () {
        deferred = $.Deferred();
        spyOn(m, 'request').and.returnValue(deferred.promise());

        PluginInfos.PluginInfo.byId("plugin_id");

        requestArgs = m.request.calls.mostRecent().args[0];
      });

      it('should fetch the plugin for the given id', function () {
        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/plugin_info/plugin_id');
      });

      it('should post required headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
      });

      it('should deserialize to Plugin object', function() {
        expect(requestArgs.type).toBe(PluginInfos.PluginInfo);
      });
    });

    describe('filterByType', function () {
      it('should return plugins for the given type', function() {
        var scm = new PluginInfos.PluginInfo({ id: 'id1', type: 'scm'});
        var task1 = new PluginInfos.PluginInfo({id: 'id2', type: 'task'});
        var task2 = new PluginInfos.PluginInfo({id: 'id3', type: 'task'});
        var authentication = new PluginInfos.PluginInfo({id: 'id4', type: 'authentication'});

        PluginInfos([scm, task1, task2,  authentication]);
        var pluginInfos = PluginInfos.filterByType('task');

        expect(pluginInfos.length).toBe(2);
        expect(pluginInfos[0].type()).toBe('task');
        expect(pluginInfos[1].type()).toBe('task');
      });
    });

    describe('findById', function () {
      it('should return plugins for the given id', function() {
        var scm = new PluginInfos.PluginInfo({ id: 'id1', type: 'scm'});
        var task1 = new PluginInfos.PluginInfo({id: 'id2', type: 'task'});
        var task2 = new PluginInfos.PluginInfo({id: 'id3', type: 'task'});
        var authentication = new PluginInfos.PluginInfo({id: 'id4', type: 'authentication'});

        PluginInfos([scm, task1, task2,  authentication]);
        var pluginInfo = PluginInfos.findById('id2');

        expect(pluginInfo.id()).toBe('id2');
      });
    });
  });
});

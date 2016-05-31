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

define(['lodash', "pipeline_configs/models/plugins"], function (_, Plugins) {
  describe('Plugins', function () {
    describe('init', function () {
      var requestArgs, deferred;

      beforeAll(function () {
        deferred = $.Deferred();
        spyOn(m, 'request').and.returnValue(deferred.promise());

        Plugins.init();

        requestArgs = m.request.calls.mostRecent().args[0];
      });

      it('should fetch all plugins', function () {
        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/plugins')
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

      it('should unwrap the response data to return list of plugins', function () {
        var plugins = {_embedded: {plugins: ['plugin']}};

        expect(requestArgs.unwrapSuccess(plugins)).toEqual(['plugin']);
      });

      it('should deserialize to Plugin object', function() {
        expect(requestArgs.type).toBe(Plugins.Plugin);
      });

      it('should initialize Plugins on success', function() {
        deferred.resolve(['plugin']);

        expect(Plugins()).toEqual(['plugin']);
      });
    });

    describe('Plugin', function() {
      var plugin;
      beforeAll(function () {
        plugin = new Plugins.Plugin({
          id:             'plugin_id',
          name:           'plugin_name',
          version:        'plugin_version',
          type:           'plugin_type',
          viewTemplate:   'plugin_view_template',
          configurations: [
            {
              key:      'url',
              metadata: {required: true, secure: false}
            },
            {
              key:      'username',
              type:     'package',
              metadata: {required: true, secure: false}
            }]});
        });

      it('should initialize with id', function () {
        expect(plugin.id()).toBe('plugin_id');
      });

      it('should initialize with name', function () {
        expect(plugin.name()).toBe('plugin_name');
      });

      it('should initialize with version', function () {
        expect(plugin.version()).toBe('plugin_version');
      });

      it('should initialize with type', function () {
        expect(plugin.viewTemplate()).toBe('plugin_view_template');
      });

      it('should initialize with configurations', function () {
        expect(plugin.configurations()).toEqual([
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

    describe('Plugin.byId', function () {
      var requestArgs, deferred;

      beforeAll(function () {
        deferred = $.Deferred();
        spyOn(m, 'request').and.returnValue(deferred.promise());

        Plugins.Plugin.byId("plugin_id");

        requestArgs = m.request.calls.mostRecent().args[0];
      });

      it('should fetch the plugin for the given id', function () {
        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/plugins/plugin_id')
      });

      it('should post required headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
      });

      it('should deserialize to Plugin object', function() {
        expect(requestArgs.type).toBe(Plugins.Plugin);
      });
    });

    describe('filterByType', function () {
      it('should return plugins for the given type', function() {
        var scm = new Plugins.Plugin({ id: 'id1', type: 'scm'});
        var task1 = new Plugins.Plugin({id: 'id2', type: 'task'});
        var task2 = new Plugins.Plugin({id: 'id3', type: 'task'});
        var authentication = new Plugins.Plugin({id: 'id4', type: 'authentication'});

        Plugins([scm, task1, task2,  authentication]);
        var plugins = Plugins.filterByType('task');

        expect(plugins.length).toBe(2);
        expect(plugins[0].type()).toBe('task');
        expect(plugins[1].type()).toBe('task');
      });
    });

    describe('findById', function () {
      it('should return plugins for the given id', function() {
        var scm = new Plugins.Plugin({ id: 'id1', type: 'scm'});
        var task1 = new Plugins.Plugin({id: 'id2', type: 'task'});
        var task2 = new Plugins.Plugin({id: 'id3', type: 'task'});
        var authentication = new Plugins.Plugin({id: 'id4', type: 'authentication'});

        Plugins([scm, task1, task2,  authentication]);
        var plugin = Plugins.findById('id2');

        expect(plugin.id()).toBe('id2');
      });
    });
  });
});
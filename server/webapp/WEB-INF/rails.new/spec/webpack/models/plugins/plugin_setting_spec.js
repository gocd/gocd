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
describe('Plugin Settings', () => {

  const _ = require('lodash');
  const s = require('string-plus');

  const PluginSetting = require('models/plugins/plugin_setting');

  const pluginSettingJSON = {
    "plugin_id": "github.oauth.login",
    "configuration": [
      {
        "key": "server_base_url",
        "value": "https://localhost:8154/go"
      }
    ]
  };

  it('should deserialize plugin setting from JSON', () => {
    const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);
    expect(pluginSetting.pluginId()).toBe("github.oauth.login");
    expect(pluginSetting.configuration().collectConfigurationProperty('key')).toEqual(['server_base_url']);
    expect(pluginSetting.configuration().collectConfigurationProperty('value')).toEqual(['https://localhost:8154/go']);
  });

  it('should serialize plugin setting to JSON', () => {
    const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);
    expect(JSON.parse(JSON.stringify(pluginSetting, s.snakeCaser))).toEqual(pluginSettingJSON);
  });

  describe("get plugin settings", () => {
    it('should get plugin settings for a plugin id and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSettingJSON['plugin_id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(pluginSettingJSON),
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((pluginSetting) => {
          expect(pluginSetting.pluginId()).toBe("github.oauth.login");
          expect(pluginSetting.configuration().collectConfigurationProperty('key')).toEqual(['server_base_url']);
          expect(pluginSetting.configuration().collectConfigurationProperty('value')).toEqual(['https://localhost:8154/go']);
          expect(pluginSetting.etag()).toBe("etag");
        });

        PluginSetting.get(pluginSettingJSON['plugin_id']).then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/admin/plugin_settings/${pluginSettingJSON['plugin_id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should try to get plugin settings and call the error callback on error", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSettingJSON['plugin_id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const failureCallback = jasmine.createSpy();

        PluginSetting.get(pluginSettingJSON['plugin_id']).then(_.noop, failureCallback);

        expect(failureCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/admin/plugin_settings/${pluginSettingJSON['plugin_id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });

  describe("create plugin settings", () => {
    it("should create plugin settings and call the success callback", () => {
      const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/plugin_settings', undefined, 'POST').andReturn({
          responseText:    JSON.stringify(pluginSettingJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        pluginSetting.create().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual(pluginSettingJSON['plugin_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
        expect(request.url).toBe('/go/api/admin/plugin_settings');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
      });

    });

    it("should not create plugin settings entity and call the error callback on non-422 failure code", () => {
      const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/plugin_settings', undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        pluginSetting.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
        expect(request.url).toBe('/go/api/admin/plugin_settings');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
      });
    });

    it("should not create plugin settings and call the error callback on 422 failure code with the profile object", () => {
      const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/plugin_settings', undefined, 'POST').andReturn({
          responseText:    JSON.stringify({data: pluginSettingJSON}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((pluginSettingWithErrors) => {
          expect(pluginSettingWithErrors.pluginId()).toBe(pluginSetting.pluginId());
        });

        pluginSetting.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
        expect(request.url).toBe('/go/api/admin/plugin_settings');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
      });
    });
  });

  describe("update plugin settings", () => {
    it('should update plugin_settings and call success callback', () => {
      const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);
      pluginSetting.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSetting.pluginId()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify(pluginSettingJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const successCallback = jasmine.createSpy();

        pluginSetting.update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual('github.oauth.login');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`/go/api/admin/plugin_settings/${pluginSetting.pluginId()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
      });
    });

    it('should try to update plugin settings and call error callback on error', () => {
      const pluginSetting = PluginSetting.fromJSON(pluginSettingJSON);
      pluginSetting.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/admin/plugin_settings/${pluginSetting.pluginId()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        pluginSetting.update().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`/go/api/admin/plugin_settings/${pluginSetting.pluginId()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(pluginSettingJSON);
      });
    });
  });
});

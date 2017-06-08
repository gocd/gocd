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

describe('Authorization Configuration', () => {

  const _ = require('lodash');
  const s = require('string-plus');

  const AuthConfigs        = require('models/auth_configs/auth_configs');
  const authConfigIndexUrl = '/go/api/admin/security/auth_configs';

  const authConfigJSON = () => ({
    "id":         "ldap",
    "plugin_id":  "cd.go.authorization.ldap",
    "properties": [
      {
        "key":   "Url",
        "value": "ldap://ldap.server.url"
      },
      {
        "key":   "ManagerDN",
        "value": "uid=admin,ou=system"
      },
      {
        "key":             "Password",
        "encrypted_value": "gGx7G+4+BAQ="
      }
    ]
  });

  const allAuthConfigJSON = {
    "_embedded": {
      "auth_configs": [authConfigJSON()]
    }
  };

  it('should deserialize a auth config from JSON', () => {
    const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
    expect(authConfig.id()).toBe("ldap");
    expect(authConfig.pluginId()).toBe("cd.go.authorization.ldap");
    expect(authConfig.properties().collectConfigurationProperty('key')).toEqual(['Url', 'ManagerDN', 'Password']);
    expect(authConfig.properties().collectConfigurationProperty('value')).toEqual(['ldap://ldap.server.url', 'uid=admin,ou=system', "gGx7G+4+BAQ="]);
  });

  it('should serialize a auth config to JSON', () => {
    const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
    expect(JSON.parse(JSON.stringify(authConfig, s.snakeCaser))).toEqual(authConfigJSON());
  });

  describe("list all auth configs", () => {
    it('should get all  auth configs and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(authConfigIndexUrl).andReturn({
          responseText:    JSON.stringify(allAuthConfigJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake((authConfigs) => {
          expect(authConfigs.countAuthConfig()).toBe(1);
          expect(authConfigs.firstAuthConfig().id()).toBe("ldap");
          expect(authConfigs.firstAuthConfig().pluginId()).toBe("cd.go.authorization.ldap");
        });

        AuthConfigs.all().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should get all  auth configs and call the error callback on error', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(authConfigIndexUrl).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        AuthConfigs.all().then(_.noop, errorCallback);
        expect(errorCallback).toHaveBeenCalledWith('Boom!');
      });
    });
  });

  describe("update a auth config", () => {
    it('should update a auth config and call success callback', () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      authConfig.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfig.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({id: 'ldap', 'plugin_id': 'cd.go.authorization.passwordfile'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const successCallback = jasmine.createSpy();

        authConfig.update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(successCallback.calls.mostRecent().args[0].id()).toEqual('ldap');
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual('cd.go.authorization.passwordfile');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfig.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
      });
    });

    it('should update a auth config and call error callback on error', () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      authConfig.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfig.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        authConfig.update().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfig.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
      });
    });
  });

  describe("create a auth config", () => {

    it('should serialize and deserialize auth config with secure field', () => {
      const authConfig       = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      const sortedProperties = authConfig.properties().sortByConfigurations();

      expect(sortedProperties[0].toJSON()).toEqual({key: "Url", value: "ldap://ldap.server.url"});
      expect(sortedProperties[1].toJSON()).toEqual({key: "ManagerDN", value: "uid=admin,ou=system"});
      expect(sortedProperties[2].toJSON()).toEqual({key: "Password", "encrypted_value": "gGx7G+4+BAQ="});
    });

    it("should create a auth  config and call the success callback", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify(authConfigJSON()),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        authConfig.create().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].id()).toEqual(authConfigJSON()['id']);
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual(authConfigJSON()['plugin_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
        expect(request.url).toBe(authConfigIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
      });

    });

    it("should not create a auth config and call the error callback on non-422 failure code", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        authConfig.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
        expect(request.url).toBe(authConfigIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
      });
    });

    it("should not create a auth config and call the error callback on 422 failure code with the auth config object", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(authConfigIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({data: authConfigJSON()}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((authConfigWithErrors) => {
          expect(authConfigWithErrors.id()).toBe(authConfig.id());
          expect(authConfigWithErrors.pluginId()).toBe(authConfig.pluginId());
        });

        authConfig.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
        expect(request.url).toBe(authConfigIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(authConfigJSON());
      });
    });
  });

  describe("find a auth config", () => {
    it('should find a auth config and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfigJSON()['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(authConfigJSON()),
          responseHeaders: {
            ETag:           'foo',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((authConfig) => {
          expect(authConfig.id()).toBe("ldap");
          expect(authConfig.pluginId()).toBe("cd.go.authorization.ldap");
          expect(authConfig.properties().collectConfigurationProperty('key')).toEqual(['Url', 'ManagerDN', 'Password']);
          expect(authConfig.properties().collectConfigurationProperty('value')).toEqual(['ldap://ldap.server.url', 'uid=admin,ou=system', 'gGx7G+4+BAQ=']);
          expect(authConfig.etag()).toBe("foo");
        });

        AuthConfigs.AuthConfig.get(authConfigJSON()['id']).then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfigJSON()['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should find a auth config and call the error callback on error", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfigJSON()['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const failureCallback = jasmine.createSpy();

        AuthConfigs.AuthConfig.get(authConfigJSON()['id']).then(_.noop, failureCallback);

        expect(failureCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfigJSON()['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });

  describe('delete a auth config', () => {
    it("should call the success callback with the message", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfig.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Success!'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        authConfig.delete().then(successCallback);

        expect(successCallback).toHaveBeenCalledWith('Success!');
        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfig.id()}`);
      });
    });

    it("should call the error callback with the message", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${authConfigIndexUrl}/${authConfig.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();
        authConfig.delete().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${authConfigIndexUrl}/${authConfig.id()}`);
      });
    });
  });

  describe('verify connection', () => {
    it("should call the success callback with message", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());

      const responseJSON = {
        "status":      "success",
        "message":     "Connection ok",
        "auth_config": authConfigJSON()
      };

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
          responseText:    JSON.stringify(responseJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        authConfig.verifyConnection().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].id()).toEqual(authConfigJSON()['id']);
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual(authConfigJSON()['plugin_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('POST');
        expect(request.url).toBe('/go/api/admin/internal/security/auth_configs/verify_connection');
      });
    });

    it("should call the error callback with the message", () => {
      const authConfig = AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/internal/security/auth_configs/verify_connection', undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          412,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();
        authConfig.verifyConnection().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();
        expect(errorCallback).toHaveBeenCalledWith({errorMessage: 'Boom!'});

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('POST');
        expect(request.url).toBe('/go/api/admin/internal/security/auth_configs/verify_connection');
      });
    });
  });

  describe('Validate auth config id', () => {
    let authConfig;
    beforeEach(() => {
      authConfig =  AuthConfigs.AuthConfig.fromJSON(authConfigJSON());
    });

    it('should return empty object on valid auth config id', () => {
      authConfig.id("foo.bar-baz_bar");

      const result = authConfig.validate();

      expect(result._isEmpty()).toEqual(true);
    });

    it('should return error if auth config id contains `!`', () => {
      authConfig.id("foo!bar");

      const result = authConfig.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if auth config id contains `*`', () => {
      authConfig.id("foo*bar");

      const result = authConfig.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if auth config id start with `.`', () => {
      authConfig.id(".foo");

      const result = authConfig.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });
  });

});

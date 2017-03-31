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

describe('Roles Configuration', () => {

  const _ = require('lodash');
  const s = require('string-plus');

  const Roles        = require('models/roles/roles');
  const roleIndexUrl = '/go/api/admin/security/roles';

  const roleJSON     = {
    "name":           "blackbird",
    "auth_config_id": "ldap",
    "properties":     [
      {
        "key":   "AttributeName",
        "value": "memberOf"
      },
      {
        "key":   "AttributeValue",
        "value": "ou=group-name,ou=system,dc=example,dc=com"
      }
    ]
  };
  const allRolesJSON = {
    "_embedded": {
      "roles": [roleJSON]
    }
  };

  it('should deserialize a role from JSON', () => {
    const role = Roles.Role.fromJSON(roleJSON);
    expect(role.name()).toBe("blackbird");
    expect(role.authConfigId()).toBe("ldap");
    expect(role.properties().collectConfigurationProperty('key')).toEqual(['AttributeName', 'AttributeValue']);
    expect(role.properties().collectConfigurationProperty('value')).toEqual(['memberOf', 'ou=group-name,ou=system,dc=example,dc=com']);
  });

  it('should serialize a role to JSON', () => {
    const role = Roles.Role.fromJSON(roleJSON);
    expect(JSON.parse(JSON.stringify(role, s.snakeCaser))).toEqual(roleJSON);
  });

  describe("list all roles", () => {
    it('should get all  roles and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(roleIndexUrl).andReturn({
          responseText:    JSON.stringify(allRolesJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake((roles) => {
          expect(roles.countRole()).toBe(1);
          expect(roles.firstRole().name()).toBe("blackbird");
          expect(roles.firstRole().authConfigId()).toBe("ldap");
        });

        Roles.all().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should get all  roles and call the error callback on error', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(roleIndexUrl).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        Roles.all().then(_.noop, errorCallback);
        expect(errorCallback).toHaveBeenCalledWith('Boom!');
      });
    });
  });

  describe("update a role", () => {
    it('should update a role and call success callback', () => {
      const role = Roles.Role.fromJSON(roleJSON);
      role.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${role.name()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({name: 'spacetiger', 'auth_config_id': 'passwordfile'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const successCallback = jasmine.createSpy();

        role.update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(successCallback.calls.mostRecent().args[0].name()).toEqual('spacetiger');
        expect(successCallback.calls.mostRecent().args[0].authConfigId()).toEqual('passwordfile');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${roleIndexUrl}/${role.name()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
      });
    });

    it('should update a role and call error callback on error', () => {
      const role = Roles.Role.fromJSON(roleJSON);
      role.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${role.name()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        role.update().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${roleIndexUrl}/${role.name()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
      });
    });
  });

  describe("create a role", () => {
    it("should create a role and call the success callback", () => {
      const role = Roles.Role.fromJSON(roleJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify(roleJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        role.create().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].name()).toEqual(roleJSON['name']);
        expect(successCallback.calls.mostRecent().args[0].authConfigId()).toEqual(roleJSON['auth_config_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
        expect(request.url).toBe(roleIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
      });

    });

    it("should not create a role and call the error callback on non-422 failure code", () => {
      const role = Roles.Role.fromJSON(roleJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        role.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
        expect(request.url).toBe(roleIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
      });
    });

    it("should not create a role and call the error callback on 422 failure code with the role object", () => {
      const role = Roles.Role.fromJSON(roleJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(roleIndexUrl, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({data: roleJSON}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((roleWithErrors) => {
          expect(roleWithErrors.name()).toBe(role.name());
          expect(roleWithErrors.authConfigId()).toBe(role.authConfigId());
        });

        role.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
        expect(request.url).toBe(roleIndexUrl);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(roleJSON);
      });
    });
  });

  describe("find a role", () => {
    it('should find a role and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${roleJSON['name']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(roleJSON),
          responseHeaders: {
            ETag:           'foo',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((role) => {
          expect(role.name()).toBe("blackbird");
          expect(role.authConfigId()).toBe("ldap");
          expect(role.properties().collectConfigurationProperty('key')).toEqual(['AttributeName', 'AttributeValue']);
          expect(role.properties().collectConfigurationProperty('value')).toEqual(['memberOf', 'ou=group-name,ou=system,dc=example,dc=com']);
          expect(role.etag()).toBe("foo");
        });

        Roles.Role.get(roleJSON['name']).then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${roleIndexUrl}/${roleJSON['name']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should find a role and call the error callback on error", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${roleJSON['name']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const failureCallback = jasmine.createSpy();

        Roles.Role.get(roleJSON['name']).then(_.noop, failureCallback);

        expect(failureCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${roleIndexUrl}/${roleJSON['name']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });

  describe('delete a role', () => {
    it("should call the success callback with the message", () => {
      const role = Roles.Role.fromJSON(roleJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${role.name()}`).andReturn({
          responseText:    JSON.stringify({message: 'Success!'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        role.delete().then(successCallback);

        expect(successCallback).toHaveBeenCalledWith('Success!');
        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${roleIndexUrl}/${role.name()}`);
      });
    });

    it("should call the error callback with the message", () => {
      const role = Roles.Role.fromJSON(roleJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${roleIndexUrl}/${role.name()}`).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();
        role.delete().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${roleIndexUrl}/${role.name()}`);
      });
    });
  });
});

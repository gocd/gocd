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
describe('Elastic Agent Profile', () => {

  const _ = require('lodash');
  const s = require('string-plus');

  const ElasticProfiles = require('models/elastic_profiles/elastic_profiles');

  const profileJSON     = {
    "id":         "unit-tests",
    "plugin_id":  "cd.go.contrib.elastic-agent.docker",
    "properties": [
      {
        "key":   "Image",
        "value": "gocdcontrib/gocd-dev-build"
      },
      {
        "key":   "Environment",
        "value": "JAVA_HOME=/opt/java\nMAKE_OPTS=-j8"
      }
    ]
  };
  const allProfilesJSON = {
    "_embedded": {
      "profiles": [profileJSON]
    }
  };

  it('should deserialize a profile from JSON', () => {
    const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
    expect(profile.id()).toBe("unit-tests");
    expect(profile.pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
    expect(profile.properties().collectConfigurationProperty('key')).toEqual(['Image', 'Environment']);
    expect(profile.properties().collectConfigurationProperty('value')).toEqual(['gocdcontrib/gocd-dev-build', 'JAVA_HOME=/opt/java\nMAKE_OPTS=-j8']);
  });

  it('should serialize a profile to JSON', () => {
    const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
    expect(JSON.parse(JSON.stringify(profile, s.snakeCaser))).toEqual(profileJSON);
  });

  describe("list all profiles", () => {
    it('should get all elastic profiles and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
          responseText:    JSON.stringify(allProfilesJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake((profiles) => {
          expect(profiles.countProfile()).toBe(1);
          expect(profiles.firstProfile().id()).toBe("unit-tests");
          expect(profiles.firstProfile().pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
        });

        ElasticProfiles.all().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should get all elastic profiles and call the error callback on error', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        ElasticProfiles.all().then(_.noop, errorCallback);
        expect(errorCallback).toHaveBeenCalledWith('Boom!');
      });
    });
  });

  describe("update a profile", () => {
    it('should update a profile and call success callback', () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
      profile.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profile.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({id: 'gocd', 'plugin_id': 'docker'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const successCallback = jasmine.createSpy();

        profile.update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(successCallback.calls.mostRecent().args[0].id()).toEqual('gocd');
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual('docker');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profile.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
      });
    });

    it('should update a profile and call error callback on error', () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
      profile.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profile.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        profile.update().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profile.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
      });
    });
  });

  describe("create a profile", () => {
    it("should create a profile and call the success callback", () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
          responseText:    JSON.stringify(profileJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        profile.create().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].id()).toEqual(profileJSON['id']);
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual(profileJSON['plugin_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
        expect(request.url).toBe('/go/api/elastic/profiles');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
      });

    });

    it("should not create a profile and call the error callback on non-422 failure code", () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        profile.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
        expect(request.url).toBe('/go/api/elastic/profiles');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
      });
    });

    it("should not create a profile and call the error callback on 422 failure code with the profile object", () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles', undefined, 'POST').andReturn({
          responseText:    JSON.stringify({data: profileJSON}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((profileWithErrors) => {
          expect(profileWithErrors.id()).toBe(profile.id());
          expect(profileWithErrors.pluginId()).toBe(profile.pluginId());
        });

        profile.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
        expect(request.url).toBe('/go/api/elastic/profiles');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(profileJSON);
      });
    });
  });

  describe("find a profile", () => {
    it('should find a profile and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profileJSON['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(profileJSON),
          responseHeaders: {
            ETag:           'foo',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((profile) => {
          expect(profile.id()).toBe("unit-tests");
          expect(profile.pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
          expect(profile.properties().collectConfigurationProperty('key')).toEqual(['Image', 'Environment']);
          expect(profile.properties().collectConfigurationProperty('value')).toEqual(['gocdcontrib/gocd-dev-build', 'JAVA_HOME=/opt/java\nMAKE_OPTS=-j8']);
          expect(profile.etag()).toBe("foo");
        });

        ElasticProfiles.Profile.get(profileJSON['id']).then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profileJSON['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should find a profile and call the error callback on error", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profileJSON['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          401,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const failureCallback = jasmine.createSpy();

        ElasticProfiles.Profile.get(profileJSON['id']).then(_.noop, failureCallback);

        expect(failureCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profileJSON['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });

  describe('delete a profile', () => {
    it("should call the success callback with the message", () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profile.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Success!'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        profile.delete().then(successCallback);

        expect(successCallback).toHaveBeenCalledWith('Success!');
        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profile.id()}`);
      });
    });

    it("should call the error callback with the message", () => {
      const profile = ElasticProfiles.Profile.fromJSON(profileJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`/go/api/elastic/profiles/${profile.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();
        profile.delete().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`/go/api/elastic/profiles/${profile.id()}`);
      });
    });
  });

  describe('Validate elastic profile id', () => {
    let elasticProfile;
    beforeEach(() => {
      elasticProfile =  ElasticProfiles.Profile.fromJSON(profileJSON);
    });

    it('should return empty object on valid elastic profile id', () => {
      elasticProfile.id("foo.bar-baz_bar");

      const result = elasticProfile.validate();

      expect(result._isEmpty()).toEqual(true);
    });

    it('should return error if elastic profile id contains `!`', () => {
      elasticProfile.id("foo!bar");

      const result = elasticProfile.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if elastic profile id contains `*`', () => {
      elasticProfile.id("foo*bar");

      const result = elasticProfile.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if elastic profile id starts with `.`', () => {
      elasticProfile.id(".foo");

      const result = elasticProfile.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });
  });
});

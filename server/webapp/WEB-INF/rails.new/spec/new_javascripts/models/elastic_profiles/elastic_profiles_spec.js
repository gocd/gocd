/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  'mithril', 'lodash', 'string-plus',
  'models/model_mixins',
  'models/elastic_profiles/elastic_profiles',
  'js-routes'
], function (m, _, s, Mixin, ElasticProfiles) {
  describe('Elastic Agent Profile', function () {

    var agentJSON = {
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

    it('should deserialize a profile from JSON', function () {
      var profile = ElasticProfiles.Profile.fromJSON(agentJSON);
      expect(profile.id()).toBe("unit-tests");
      expect(profile.pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
      expect(profile.properties().collectConfigurationProperty('key')).toEqual(['Image', 'Environment']);
      expect(profile.properties().collectConfigurationProperty('value')).toEqual(['gocdcontrib/gocd-dev-build', 'JAVA_HOME=/opt/java\nMAKE_OPTS=-j8']);
    });

    it('should serialize a profile to JSON', function () {
      var profile = ElasticProfiles.Profile.fromJSON(agentJSON);
      expect(JSON.parse(JSON.stringify(profile, s.snakeCaser))).toEqual(agentJSON);
    });

    it('should get all elastic profiles', function () {
      jasmine.Ajax.withMock(function () {

        jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
          responseText: JSON.stringify({
            "_embedded": {
              "profiles": [agentJSON]
            }
          }),
          status:       200
        });

        var profiles = ElasticProfiles.all();
        expect(profiles().countProfile()).toBe(1);
        expect(profiles().firstProfile().id()).toBe("unit-tests");
        expect(profiles().firstProfile().pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
      });
    });

    it('should update a profile', function () {
      var profile = ElasticProfiles.Profile.fromJSON(agentJSON);
      profile.etag("some-etag");

      jasmine.Ajax.withMock(function () {
        profile.update();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        var request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe('/go/api/elastic/profiles/' + profile.id());
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(agentJSON);
      });
    });

    it('should create a profile', function () {
      var profile = ElasticProfiles.Profile.fromJSON(agentJSON);

      jasmine.Ajax.withMock(function () {
        profile.create();

        expect(jasmine.Ajax.requests.count()).toBe(1);

        var request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(request.url).toBe('/go/api/elastic/profiles');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(agentJSON);
      });
    });

    it('should find a profile', function () {
      jasmine.Ajax.withMock(function () {
        jasmine.Ajax.stubRequest('/go/api/elastic/profiles/' + agentJSON['id']).andReturn({
          responseText:    JSON.stringify(agentJSON),
          responseHeaders: {
            ETag: 'foo'
          },
          status:          200
        });

        var profile = ElasticProfiles.Profile.get(agentJSON['id'])();

        expect(profile.id()).toBe("unit-tests");
        expect(profile.pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
        expect(profile.properties().collectConfigurationProperty('key')).toEqual(['Image', 'Environment']);
        expect(profile.properties().collectConfigurationProperty('value')).toEqual(['gocdcontrib/gocd-dev-build', 'JAVA_HOME=/opt/java\nMAKE_OPTS=-j8']);
        expect(profile.etag()).toBe("foo");

        expect(jasmine.Ajax.requests.count()).toBe(1);

        var request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/elastic/profiles/' + profile.id());
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });
});
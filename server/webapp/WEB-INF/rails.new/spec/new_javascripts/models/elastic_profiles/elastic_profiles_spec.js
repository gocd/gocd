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
  'models/elastic_profiles/elastic_profiles'
], function (m, _, s, Mixin, ElasticProfiles) {
  describe('Elastic Agent Profile', function () {

    function ajaxCall(data) {
      jasmine.Ajax.stubRequest(/\/api\/elastic\/profiles/).andReturn({
        "responseText": JSON.stringify({
          "_embedded": {
            "profiles": data
          }
        }),
        "status":       200
      });
    }


    beforeAll(function () {
      jasmine.Ajax.install();
      ajaxCall([json]);
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
    });

    var json = {
      "id": "unit-tests",
      "plugin_id": "cd.go.contrib.elastic-agent.docker",
      "properties": [
        {
          "key": "Image",
          "value": "gocdcontrib/gocd-dev-build"
        },
        {
          "key": "Environment",
          "value": "JAVA_HOME=/opt/java\nMAKE_OPTS=-j8"
        }
      ]
    };

    it('should deserialize a profile from JSON', function () {

      var profile = ElasticProfiles.Profile.fromJSON(json);
      expect(profile.id()).toBe("unit-tests");
      expect(profile.pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
      expect(profile.properties().collectConfigurationProperty('key')).toEqual(['Image', 'Environment']);
      expect(profile.properties().collectConfigurationProperty('value')).toEqual(['gocdcontrib/gocd-dev-build', 'JAVA_HOME=/opt/java\nMAKE_OPTS=-j8']);
    });

    it('should serialize a profile to JSON', function () {
      var profile = ElasticProfiles.Profile.fromJSON(json);
      expect(JSON.parse(JSON.stringify(profile, s.snakeCaser))).toEqual(json);
    });

    it('should get all elastic profiles', function(){
      var profiles = ElasticProfiles.all();
      expect(profiles().countProfile()).toBe(1);
      expect(profiles().firstProfile().id()).toBe("unit-tests");
      expect(profiles().firstProfile().pluginId()).toBe("cd.go.contrib.elastic-agent.docker");
    });

    it('should update a profile', function () {
      var profile = ElasticProfiles.Profile.fromJSON(json);
      profile.update();

    });
  });
});
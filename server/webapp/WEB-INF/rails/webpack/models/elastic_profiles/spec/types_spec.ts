/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {ElasticProfile} from "models/elastic_profiles/types";
import {EncryptedValue, PlainTextValue} from "models/shared/config_value";
import {Configuration, Configurations} from "models/shared/configuration";

describe("Elastic Profile Types", () => {

  describe("Validation", () => {
    it("should validate elastic profile", () => {
      const elasticProfile = new ElasticProfile("", "", new Configurations([]));
      expect(elasticProfile.isValid()).toBe(false);
      expect(elasticProfile.errors().count()).toBe(2);
      expect(elasticProfile.errors().keys().sort()).toEqual(["id", "pluginId"]);
    });

    it("should validate elastic profile id format", () => {
      const elasticProfile = new ElasticProfile("invalid id", "pluginId", new Configurations([]));
      expect(elasticProfile.isValid()).toBe(false);
      expect(elasticProfile.errors().count()).toBe(1);
      expect(elasticProfile.errors().keys()).toEqual(["id"]);
      expect(elasticProfile.errors().errors("id"))
        .toEqual(["Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."]);
    });
  });

  describe("Serialization and Deserialization", () => {
    it("should serialize elastic profile", () => {
      const elasticProfile = new ElasticProfile(
        "docker1",
        "cd.go.docker",
        new Configurations([
                             new Configuration("image", new PlainTextValue("gocd/server")),
                             new Configuration("secret", new EncryptedValue("alskdad"))
                           ]));

      expect(JSON.parse(JSON.stringify(elasticProfile.toJSON()))).toEqual({
                                                id: "docker1",
                                                plugin_id: "cd.go.docker",
                                                properties: [{
                                                  key: "image",
                                                  value: "gocd/server"
                                                },
                                                  {
                                                    key: "secret",
                                                    encrypted_value: "alskdad"
                                                  }
                                                ]
                                              });
    });

    it("should deserialize elastic profile", () => {
      const elasticProfile = ElasticProfile.fromJSON({
                                                       id: "docker1",
                                                       plugin_id: "cd.go.docker",
                                                       properties: [{
                                                         key: "image",
                                                         value: "gocd/server",
                                                         encrypted_value: null
                                                       },
                                                         {
                                                           key: "memory",
                                                           value: "10M",
                                                           encrypted_value: null
                                                         }
                                                       ]
                                                     });

      expect(elasticProfile.id()).toEqual("docker1");
      expect(elasticProfile.pluginId()).toEqual("cd.go.docker");
      expect(elasticProfile.properties().count()).toBe(2);
      expect(elasticProfile.properties().valueFor("image")).toEqual("gocd/server");
      expect(elasticProfile.properties().valueFor("memory")).toEqual("10M");
    });

    it("should serialize encrypted value as value when updated", () => {
      const elasticProfile = new ElasticProfile(
        "docker1",
        "cd.go.docker",
        new Configurations([
                             new Configuration("image", new PlainTextValue("gocd/server")),
                             new Configuration("secret", new EncryptedValue("alskdad"))
                           ]));

      elasticProfile.properties().setConfiguration("secret", "foo");

      expect(JSON.parse(JSON.stringify(elasticProfile.toJSON()))).toEqual({
                                                id: "docker1",
                                                plugin_id: "cd.go.docker",
                                                properties: [{
                                                  key: "image",
                                                  value: "gocd/server"
                                                },
                                                  {
                                                    key: "secret",
                                                    value: "foo"
                                                  }
                                                ]
                                              });

    });
  });
});

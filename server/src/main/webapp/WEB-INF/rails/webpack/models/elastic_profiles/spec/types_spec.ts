/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {ClusterProfile, ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {EncryptedValue, PlainTextValue} from "models/shared/config_value";
import {Configuration, Configurations} from "models/shared/configuration";

describe("Types", () => {
  describe("Elastic Agent Profiles", () => {
    describe("Validation", () => {
      it("should validate elastic agent profile", () => {
        const elasticProfile = new ElasticAgentProfile("", "", "", true, new Configurations([]));
        expect(elasticProfile.isValid()).toBe(false);
        expect(elasticProfile.errors().count()).toBe(3);
        expect(elasticProfile.errors().keys().sort()).toEqual(["clusterProfileId", "id", "pluginId"]);
      });

      it("should validate elastic agent profile id format", () => {
        const elasticProfile = new ElasticAgentProfile("invalid id", "pluginId", "foo", true, new Configurations([]));
        expect(elasticProfile.isValid()).toBe(false);
        expect(elasticProfile.errors().count()).toBe(1);
        expect(elasticProfile.errors().keys()).toEqual(["id"]);
        expect(elasticProfile.errors().errors("id"))
          .toEqual(["Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."]);
      });

      it("should validate existence of cluster profile id", () => {
        const elasticProfile = new ElasticAgentProfile("id", "pluginId", undefined, true, new Configurations([]));
        expect(elasticProfile.isValid()).toBe(false);
        expect(elasticProfile.errors().count()).toBe(1);
        expect(elasticProfile.errors().keys()).toEqual(["clusterProfileId"]);
        expect(elasticProfile.errors().errors("clusterProfileId"))
          .toEqual(["Cluster profile id must be present"]);
      });
    });

    describe("Serialization and Deserialization", () => {
      it("should serialize elastic agent profile", () => {
        const elasticProfile = new ElasticAgentProfile(
          "docker1",
          "cd.go.docker",
          "prod-cluster",
          true,
          new Configurations([
                               new Configuration("image", new PlainTextValue("gocd/server")),
                               new Configuration("secret", new EncryptedValue("alskdad"))
                             ]));

        expect(JSON.parse(JSON.stringify(elasticProfile.toJSON()))).toEqual({
                                                                              id: "docker1",
                                                                              plugin_id: "cd.go.docker",
                                                                              cluster_profile_id: "prod-cluster",
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

      it("should deserialize elastic agent profile", () => {
        const elasticProfile = ElasticAgentProfile.fromJSON({
                                                              id: "docker1",
                                                              plugin_id: "cd.go.docker",
                                                              cluster_profile_id: "prod-cluster",
                                                              can_administer: true,
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
        expect(elasticProfile.clusterProfileId()).toEqual("prod-cluster");
        expect(elasticProfile.properties()!.count()).toBe(2);
        expect(elasticProfile.properties()!.valueFor("image")).toEqual("gocd/server");
        expect(elasticProfile.properties()!.valueFor("memory")).toEqual("10M");
      });

      it("should serialize encrypted value as value when updated", () => {
        const elasticProfile = new ElasticAgentProfile(
          "docker1",
          "cd.go.docker",
          "prod-cluster",
          true,
          new Configurations([
                               new Configuration("image", new PlainTextValue("gocd/server")),
                               new Configuration("secret", new EncryptedValue("alskdad"))
                             ]));

        elasticProfile.properties()!.setConfiguration("secret", "foo");

        expect(JSON.parse(JSON.stringify(elasticProfile.toJSON()))).toEqual({
                                                                              id: "docker1",
                                                                              plugin_id: "cd.go.docker",
                                                                              cluster_profile_id: "prod-cluster",
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

      it("should filter the elastic agent profiles by cluster profile", () => {
        const elasticAgentProfiles = new ElasticAgentProfiles([new ElasticAgentProfile("profile_1",
                                                                                       "plugin_id",
                                                                                       "cluster1")]);
        expect(elasticAgentProfiles.filterByClusterProfile("cluster1").length).toEqual(1);
        expect(elasticAgentProfiles.filterByClusterProfile("cluster2").length).toEqual(0);
      });
    });
  });

  describe("Cluster Profiles", () => {
    describe("Validation", () => {
      it("should validate cluster profile", () => {
        const clusterProfile = new ClusterProfile("", "", true, new Configurations([]));
        expect(clusterProfile.isValid()).toBe(false);
        expect(clusterProfile.errors().count()).toBe(2);
        expect(clusterProfile.errors().keys().sort()).toEqual(["id", "pluginId"]);
      });

      it("should validate cluster profile id format", () => {
        const clusterProfile = new ClusterProfile("invalid id", "pluginId", true, new Configurations([]));
        expect(clusterProfile.isValid()).toBe(false);
        expect(clusterProfile.errors().count()).toBe(1);
        expect(clusterProfile.errors().keys()).toEqual(["id"]);
        expect(clusterProfile.errors().errors("id"))
          .toEqual(["Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."]);
      });
    });

    describe("Serialization and Deserialization", () => {
      it("should serialize cluster profile", () => {
        const clusterProfile = new ClusterProfile(
          "docker1",
          "cd.go.docker",
          true,
          new Configurations([
                               new Configuration("image", new PlainTextValue("gocd/server")),
                               new Configuration("secret", new EncryptedValue("alskdad"))
                             ]));

        expect(JSON.parse(JSON.stringify(clusterProfile.toJSON()))).toEqual({
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

      it("should deserialize cluster profile", () => {
        const clusterProfile = ClusterProfile.fromJSON({
                                                         id: "docker1",
                                                         plugin_id: "cd.go.docker",
                                                         can_administer: true,
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

        expect(clusterProfile.id()).toEqual("docker1");
        expect(clusterProfile.pluginId()).toEqual("cd.go.docker");
        expect(clusterProfile.properties()!.count()).toBe(2);
        expect(clusterProfile.properties()!.valueFor("image")).toEqual("gocd/server");
        expect(clusterProfile.properties()!.valueFor("memory")).toEqual("10M");
      });

      it("should serialize encrypted value as value when updated", () => {
        const clusterProfile = new ClusterProfile(
          "docker1",
          "cd.go.docker",
          true,
          new Configurations([
                               new Configuration("image", new PlainTextValue("gocd/server")),
                               new Configuration("secret", new EncryptedValue("alskdad"))
                             ]));

        clusterProfile.properties()!.setConfiguration("secret", "foo");

        expect(JSON.parse(JSON.stringify(clusterProfile.toJSON()))).toEqual({
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
});

/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {ClusterProfile, ClusterProfiles} from "models/cluster_profiles/cluster_profiles";
import {clusterProfilesTestData, clusterProfileTestData} from "models/cluster_profiles/spec/test_data";

describe("ClusterProfile", () => {
  describe("Serialization and Deserialization", () => {

    it("should deserialize cluster profile", () => {
      const clusterProfileJSON = clusterProfileTestData("cluster_1", "plugin_1");
      const clusterProfile     = ClusterProfile.fromJSON(clusterProfileJSON);

      expect(clusterProfile.id()).toEqual("cluster_1");
      expect(clusterProfile.pluginId()).toEqual("plugin_1");
      expect(clusterProfile.properties().count()).toBe(2);
      expect(clusterProfile.properties().valueFor("key_1")).toEqual("value_1");
      expect(clusterProfile.properties().valueFor("key_2")).toEqual("value_2");
    });

  });
});

describe("ClusterProfiles", () => {
  describe("Serialization and Deserialization", () => {

    it("should deserialize cluster profiles", () => {
      const clusterProfilesJSON = clusterProfilesTestData();
      const clusterProfiles     = ClusterProfiles.fromJSON(clusterProfilesJSON);

      expect(clusterProfiles.length).toBe(2);
      expect(clusterProfiles[0].id()).toEqual("cluster_1");
      expect(clusterProfiles[0].pluginId()).toEqual("plugin_1");
      expect(clusterProfiles[0].properties().valueFor("key_1")).toEqual("value_1");
      expect(clusterProfiles[0].properties().valueFor("key_2")).toEqual("value_2");

      expect(clusterProfiles[1].id()).toEqual("cluster_2");
      expect(clusterProfiles[1].pluginId()).toEqual("plugin_2");
      expect(clusterProfiles[1].properties().valueFor("key_1")).toEqual("value_1");
      expect(clusterProfiles[1].properties().valueFor("key_2")).toEqual("value_2");
    });
  });

  it("should group cluster profiles by plugin id", () => {
    const clusterProfilesJSON = clusterProfilesTestData();
    clusterProfilesJSON._embedded.cluster_profiles.push(clusterProfileTestData("cluster_3", "plugin_1"));
    const clusterProfiles = ClusterProfiles.fromJSON(clusterProfilesJSON);

    const groupedClusterProfiles = clusterProfiles.groupByPlugin();

    expect(groupedClusterProfiles.plugin_1).toHaveLength(2);
    expect(groupedClusterProfiles.plugin_1[0].id()).toEqual("cluster_1");
    expect(groupedClusterProfiles.plugin_1[1].id()).toEqual("cluster_3");

    expect(groupedClusterProfiles.plugin_2).toHaveLength(1);
    expect(groupedClusterProfiles.plugin_2[0].id()).toEqual("cluster_2");
  });
});

describe("Cluster Profile Validations", () => {
  it("should validate presence of id", () => {
    const clusterProfileJson = clusterProfileTestData("cluster_1", "plugin_1");
    delete clusterProfileJson.id;
    const clusterProfile = ClusterProfile.fromJSON(clusterProfileJson);
    clusterProfile.isValid();

    const errors = clusterProfile.errors();
    expect(errors.hasErrors()).toBeTruthy();
    expect(errors.errorsForDisplay("id")).toEqual("Id must be present.");
  });

  it("should validate format of id", () => {
    const clusterProfile = ClusterProfile.fromJSON(clusterProfileTestData("cluster id with spaces not allowed", "plugin_1"));

    clusterProfile.isValid();

    const errors = clusterProfile.errors();
    expect(errors.hasErrors()).toBeTruthy();
    expect(errors.errorsForDisplay("id"))
      .toEqual(
        "Invalid id. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
  });

  it("should validate presence of plugin id", () => {
    const clusterProfileJson = clusterProfileTestData("cluster_1", "plugin_1");
    delete clusterProfileJson.plugin_id;
    const clusterProfile = ClusterProfile.fromJSON(clusterProfileJson);
    clusterProfile.isValid();

    const errors = clusterProfile.errors();
    expect(errors.hasErrors()).toBeTruthy();
    expect(errors.errorsForDisplay("pluginId")).toEqual("Plugin id must be present.");
  });
});

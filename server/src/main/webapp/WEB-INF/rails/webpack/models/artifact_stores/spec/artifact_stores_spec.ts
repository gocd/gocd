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

import {ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactStoreTestData} from "models/artifact_stores/spec/test_data";

describe("ArtifactStoreModal", () => {
  it("should deserialize json to ArtifactStores", () => {
    const artifactStoresJSON = ArtifactStoreTestData.artifactStoreList(
      ArtifactStoreTestData.dockerArtifactStore(),
      ArtifactStoreTestData.mavenArtifactStore()
    );

    const artifactStores = ArtifactStores.fromJSON(artifactStoresJSON);

    expect(artifactStores.length).toEqual(2);

    expect(artifactStores[0].id()).toEqual("hub.docker");
    expect(artifactStores[0].pluginId()).toEqual("cd.go.artifact.docker.registry");
    expect(artifactStores[0].properties().count()).toEqual(3);
    expect(artifactStores[0].properties().valueFor("RegistryURL")).toEqual("https://your_docker_registry_url");
    expect(artifactStores[0].properties().valueFor("Username")).toEqual("admin");
    expect(artifactStores[0].properties().valueFor("Password"))
      .toEqual("AES:tdfTtYtIUSAF2JXJP/3YwA==:43Kjidjuh42NHKisCAs/BQ==");

    expect(artifactStores[1].id()).toEqual("maven.central");
    expect(artifactStores[1].pluginId()).toEqual("cd.go.artifact.maven.registry");
    expect(artifactStores[1].properties().count()).toEqual(3);
    expect(artifactStores[1].properties().valueFor("RegistryURL")).toEqual("https://your.maven.registry");
    expect(artifactStores[1].properties().valueFor("Username")).toEqual("bob");
    expect(artifactStores[1].properties().valueFor("Password"))
      .toEqual("AES:tdfTtYtIUSAF2JXJP/3YwA==:43Kjidjuh42NHKisCAs/BQ==");
  });

  it("should validate presence of plugin id", () => {
    const dockerJSON = ArtifactStoreTestData.dockerArtifactStore();
    delete dockerJSON.plugin_id;
    const artifactStores = ArtifactStores.fromJSON(ArtifactStoreTestData.artifactStoreList(dockerJSON));

    const isValid = artifactStores[0].isValid();

    expect(isValid).toBe(false);
    expect(artifactStores[0].errors().count()).toEqual(1);
    expect(artifactStores[0].errors().keys()).toEqual(["pluginId"]);
  });

  it("should validate presence of id", () => {
    const dockerJSON = ArtifactStoreTestData.dockerArtifactStore();
    delete dockerJSON.id;
    const artifactStores = ArtifactStores.fromJSON(ArtifactStoreTestData.artifactStoreList(dockerJSON));

    const isValid = artifactStores[0].isValid();

    expect(isValid).toBe(false);
    expect(artifactStores[0].errors().count()).toEqual(1);
    expect(artifactStores[0].errors().keys()).toEqual(["id"]);
  });

  it("should validate pattern for id", () => {
    const dockerJSON     = ArtifactStoreTestData.dockerArtifactStore();
    dockerJSON.id        = "&%$Not-allowed";
    const artifactStores = ArtifactStores.fromJSON(ArtifactStoreTestData.artifactStoreList(dockerJSON));

    const isValid = artifactStores[0].isValid();

    expect(isValid).toBe(false);
    expect(artifactStores[0].errors().count()).toEqual(1);
    expect(artifactStores[0].errors().keys()).toEqual(["id"]);
  });

  it("should validate length for id", () => {
    const dockerJSON     = ArtifactStoreTestData.dockerArtifactStore();
    dockerJSON.id        = "This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters";
    const artifactStores = ArtifactStores.fromJSON(ArtifactStoreTestData.artifactStoreList(dockerJSON));

    const isValid = artifactStores[0].isValid();

    expect(isValid).toBe(false);
    expect(artifactStores[0].errors().count()).toEqual(1);
    expect(artifactStores[0].errors().keys()).toEqual(["id"]);
  });
});

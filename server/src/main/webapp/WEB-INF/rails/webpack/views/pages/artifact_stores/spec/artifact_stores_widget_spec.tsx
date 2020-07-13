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

import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactStoreTestData} from "models/artifact_stores/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ArtifactPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {ArtifactStoresWidget} from "views/pages/artifact_stores/artifact_stores_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ArtifactStoresModal", () => {
  const helper              = new TestHelper();
  const onEdit              = jasmine.createSpy("onEdit");
  const onClone             = jasmine.createSpy("onClone");
  const onDelete            = jasmine.createSpy("onDelete");
  const dockerArtifactStore = ArtifactStore.fromJSON(ArtifactStoreTestData.dockerArtifactStore());

  afterEach((done) => helper.unmount(done));

  it("should show flash message when no artifact plugin installed", () => {
    mount(new ArtifactStores(), new PluginInfos());

    expect(helper.byTestId("flash-message-warning")).toBeInDOM();
    expect(helper.textByTestId("flash-message-warning")).toBe("To use this page, you must ensure that there are one or more artifact plugins installed. Please see this page for a list of supported plugins.");
  });

  it("should render action buttons", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.byTestId("artifact-stores-group");

    expect(helper.byTestId("artifact-store-edit", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-clone", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-delete", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-edit", groups)).not.toBeDisabled();
    expect(helper.byTestId("artifact-store-clone", groups)).not.toBeDisabled();
    expect(helper.byTestId("artifact-store-delete", groups)).not.toBeDisabled();
  });

  it("should disable edit and clone button when plugin is not installed", () => {
    mount(new ArtifactStores(dockerArtifactStore), new PluginInfos());

    const groups = helper.byTestId("artifact-stores-group");

    expect(helper.byTestId("artifact-store-edit", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-clone", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-delete", groups)).toBeInDOM();
    expect(helper.byTestId("artifact-store-edit", groups)).toBeDisabled();
    expect(helper.byTestId("artifact-store-clone", groups)).toBeDisabled();
    expect(helper.byTestId("artifact-store-delete", groups)).not.toBeDisabled();
  });

  it("should render artifact store properties", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.byTestId("artifact-stores-group");

    expect(helper.byTestId("key-value-key-registryurl", groups)).toContainText("RegistryURL");
    expect(helper.byTestId("key-value-key-username", groups)).toContainText("Username");
    expect(helper.byTestId("key-value-key-password", groups)).toContainText("Password");
    expect(helper.byTestId("key-value-value-registryurl", groups)).toContainText("https://your_docker_registry_url");
    expect(helper.byTestId("key-value-value-username", groups)).toContainText("admin");
    expect(helper.byTestId("key-value-value-password", groups)).toContainText("************");
  });

  it("should callback the edit function when edit button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    helper.clickByTestId("artifact-store-edit");

    expect(onEdit).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    helper.clickByTestId("artifact-store-clone");

    expect(onClone).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    helper.clickByTestId("artifact-store-delete");

    expect(onDelete).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should list artifact stores", () => {
    const mavenArtifactStore = ArtifactStore.fromJSON(ArtifactStoreTestData.mavenArtifactStore());
    const artifactStores     = new ArtifactStores(dockerArtifactStore, mavenArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.allByTestId("artifact-stores-group");

    expect(groups.length).toEqual(2);

    expect(helper.textByTestId("plugin-name", groups.item(0))).toBe("Artifact plugin for docker");
    expect(helper.textByTestId("key-value-value-plugin-id", groups.item(0))).toBe("cd.go.artifact.docker.registry");
    expect(helper.textByTestId("key-value-value-id", groups.item(0))).toBe("hub.docker");

    expect(helper.textByTestId("plugin-name", groups.item(1))).toBe("Plugin is not installed");
    expect(helper.textByTestId("key-value-value-plugin-id", groups.item(1))).toBe("cd.go.artifact.maven.registry");
    expect(helper.textByTestId("key-value-value-id", groups.item(1))).toBe("maven.central");
  });

  function mount(artifactStores: ArtifactStores, pluginInfos: PluginInfos) {
    helper.mount(() => <ArtifactStoresWidget pluginInfos={Stream(pluginInfos)}
                                             artifactStores={artifactStores}
                                             onEdit={onEdit}
                                             onClone={onClone}
                                             onDelete={onDelete}/>);
  }
});

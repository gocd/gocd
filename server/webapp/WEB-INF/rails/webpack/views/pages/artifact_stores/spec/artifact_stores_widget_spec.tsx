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

import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactStoreTestData} from "models/artifact_stores/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ArtifactPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
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

    expect(helper.findByDataTestId("flash-message-info")).toBeInDOM();
    expect(helper.findByDataTestId("flash-message-info").text()).toEqual("No artifact plugin installed.");
  });

  it("should render action buttons", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.findByDataTestId("artifact-stores-group");

    expect(helper.findIn(groups.eq(0), "artifact-store-edit")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-clone")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-delete")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-edit")).not.toBeDisabled();
    expect(helper.findIn(groups.eq(0), "artifact-store-clone")).not.toBeDisabled();
    expect(helper.findIn(groups.eq(0), "artifact-store-delete")).not.toBeDisabled();
  });

  it("should disable edit and clone button when plugin is not installed", () => {
    mount(new ArtifactStores(dockerArtifactStore), new PluginInfos());

    const groups = helper.findByDataTestId("artifact-stores-group");

    expect(helper.findIn(groups.eq(0), "artifact-store-edit")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-clone")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-delete")).toBeInDOM();
    expect(helper.findIn(groups.eq(0), "artifact-store-edit")).toBeDisabled();
    expect(helper.findIn(groups.eq(0), "artifact-store-clone")).toBeDisabled();
    expect(helper.findIn(groups.eq(0), "artifact-store-delete")).not.toBeDisabled();
  });

  it("should render artifact store properties", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.findByDataTestId("artifact-stores-group");

    expect(helper.findIn(groups, "key-value-key-registryurl")).toContainText("RegistryURL");
    expect(helper.findIn(groups, "key-value-key-username")).toContainText("Username");
    expect(helper.findIn(groups, "key-value-key-password")).toContainText("Password");
    expect(helper.findIn(groups, "key-value-value-registryurl")).toContainText("https://your_docker_registry_url");
    expect(helper.findIn(groups, "key-value-value-username")).toContainText("admin");
    expect(helper.findIn(groups, "key-value-value-password")).toContainText("************");
  });

  it("should callback the edit function when edit button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    simulateEvent.simulate(helper.findByDataTestId("artifact-store-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    simulateEvent.simulate(helper.findByDataTestId("artifact-store-clone").get(0), "click");

    expect(onClone).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    const artifactStores = new ArtifactStores(dockerArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    simulateEvent.simulate(helper.findByDataTestId("artifact-store-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(dockerArtifactStore, jasmine.any(Event));
  });

  it("should list artifact stores", () => {
    const mavenArtifactStore = ArtifactStore.fromJSON(ArtifactStoreTestData.mavenArtifactStore());
    const artifactStores     = new ArtifactStores(dockerArtifactStore, mavenArtifactStore);
    mount(artifactStores, new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())));

    const groups = helper.findByDataTestId("artifact-stores-group");

    expect(groups.length).toEqual(2);

    expect(helper.findIn(groups.eq(0), "plugin-name").text())
      .toEqual("Artifact plugin for docker");
    expect(helper.findIn(groups.eq(0), "key-value-value-plugin-id").text())
      .toEqual("cd.go.artifact.docker.registry");
    expect(helper.findIn(groups.eq(0), "key-value-value-id").text())
      .toEqual("hub.docker");

    expect(helper.findIn(groups.eq(1), "plugin-name").text())
      .toEqual("Plugin is not installed");
    expect(helper.findIn(groups.eq(1), "key-value-value-plugin-id").text())
      .toEqual("cd.go.artifact.maven.registry");
    expect(helper.findIn(groups.eq(1), "key-value-value-id").text())
      .toEqual("maven.central");
  });

  function mount(artifactStores: ArtifactStores, pluginInfos: PluginInfos) {
    helper.mount(() => <ArtifactStoresWidget pluginInfos={Stream(pluginInfos)}
                                             artifactStores={artifactStores}
                                             onEdit={onEdit}
                                             onClone={onClone}
                                             onDelete={onDelete}/>);
  }
});

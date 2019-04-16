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

import * as m from "mithril";
import {ArtifactStore} from "models/artifact_stores/artifact_stores";
import {Configurations} from "models/shared/configuration";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {ArtifactPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {ArtifactStoreModalBody} from "views/pages/artifact_stores/artifact_store_modal_body";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ArtifactModalBodyWidget", () => {
  const helper        = new TestHelper();
  const pluginInfos   = [PluginInfo.fromJSON(ArtifactPluginInfo.docker())];
  const pluginIdProxy = jasmine.createSpy("pluginIdProxy");

  afterEach((done) => helper.unmount(done));

  it("should render artifact store view", () => {
    const artifactStore = new ArtifactStore("", "cd.go.artifact.docker.registry", new Configurations([]));
    mount(artifactStore);

    expect(helper.findByDataTestId("form-field-label-id")).toContainText("Id");
    expect(helper.findByDataTestId("form-field-input-id")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-id").val()).toEqual("");

    expect(helper.findByDataTestId("form-field-label-plugin-id")).toContainText("Plugin Id");
    expect(helper.findByDataTestId("form-field-input-plugin-id").val()).toEqual("cd.go.artifact.docker.registry");

    expect(helper.find(".plugin-view")).toContainText("This is store config view.");
  });

  function mount(artifactStore: ArtifactStore, disableId = false) {
    helper.mount(() => <ArtifactStoreModalBody pluginInfos={pluginInfos}
                                         artifactStore={artifactStore}
                                         pluginIdProxy={pluginIdProxy}
                                         disableId={disableId}/>);
  }
});

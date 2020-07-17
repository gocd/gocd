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
import {Scms} from "models/materials/pluggable_scm";
import {Material} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {Materials} from "models/pipeline_configs/pipeline_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ModalManager} from "views/components/modal/modal_manager";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/material_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MaterialModalSpec", () => {
  let material: Material;
  let materials: Materials;
  let scms: Scms;
  let pkgRepos: PackageRepositories;
  let pluginInfos: PluginInfos;

  beforeEach(() => {
    material    = new Material();
    materials   = new Materials();
    scms        = new Scms();
    pkgRepos    = new PackageRepositories();
    pluginInfos = new PluginInfos();
  });
  afterEach(() => ModalManager.closeAll());

  it('should render material editor', () => {
    const modal = new MaterialModal("Some modal title", Stream(material), "test", "test", Stream(materials), Stream(scms), Stream(pkgRepos), Stream(pluginInfos), jasmine.createSpy("pipelineConfigSave"), true, false);
    modal.render();
    m.redraw.sync();
    const helper = new TestHelper().forModal();

    expect(modal).toContainTitle("Some modal title");
    expect(modal).toContainButtons(["Cancel", "Save"]);
    expect(helper.byTestId('form-field-input-material-type')).toBeInDOM();
    expect(helper.byTestId('materials-destination-warning-message')).not.toBeInDOM();
  });

  it("should show save and reset buttons", () => {
    const modal = new MaterialModal("Some modal title", Stream(material), "test", "test", Stream(materials), Stream(scms), Stream(pkgRepos), Stream(pluginInfos), jasmine.createSpy("pipelineConfigSave"), true, false);
    modal.render();
    m.redraw.sync();
    const helper = new TestHelper().forModal();

    expect(helper.byTestId('button-save', document.body)).toBeInDOM();
    expect(helper.byTestId('button-cancel', document.body)).toBeInDOM();
  });

  it("should not show save and reset buttons for read only materials", () => {
    const modal = new MaterialModal("Some modal title", Stream(material), "test", "test", Stream(materials), Stream(scms), Stream(pkgRepos), Stream(pluginInfos), jasmine.createSpy("pipelineConfigSave"), true, true);
    modal.render();
    m.redraw.sync();
    const helper = new TestHelper().forModal();

    expect(helper.byTestId('button-save', document.body)).not.toBeInDOM();
    expect(helper.byTestId('button-cancel', document.body)).not.toBeInDOM();
  });
});

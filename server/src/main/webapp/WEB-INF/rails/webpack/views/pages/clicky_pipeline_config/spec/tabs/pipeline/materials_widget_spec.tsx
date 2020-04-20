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
import {Filter} from "models/maintenance_mode/material";
import {Scm, Scms} from "models/materials/pluggable_scm";
import {Material, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {Package, PackageRepositories, PackageRepository, Packages} from "models/package_repositories/package_repositories";
import {getPackage, getPackageRepository, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {Materials, PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {MaterialsWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_widget";
import {getPluggableScm, getScmPlugin} from "views/pages/pluggable_scms/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MaterialsWidgetSpec", () => {
  const helper = new TestHelper();
  let pipelineConfig: PipelineConfig;
  let pluginInfos: PluginInfos;
  let pkgRepos: PackageRepositories;
  let pkgs: Packages;
  let scms: Scms;

  beforeEach(() => {
    pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
    pkgRepos       = new PackageRepositories();
    pkgs           = new Packages();
    scms           = new Scms();
    pluginInfos    = new PluginInfos();
  });
  afterEach((done) => helper.unmount(done));

  function mount(materials: Stream<Materials> = pipelineConfig.materials) {
    helper.mount(() => <MaterialsWidget materials={materials} pluginInfos={Stream(pluginInfos)}
                                        packageRepositories={Stream(pkgRepos)}
                                        packages={Stream(pkgs)} scmMaterials={Stream(scms)}
                                        pipelineConfigSave={jasmine.createSpy("pipelineConfigSave")}
                                        flashMessage={new FlashMessageModelWithTimeout()}/>);
  }

  describe("Add Material", () => {
    it("should render button", () => {
      mount();

      expect(helper.byTestId("add-material-button")).toBeInDOM();
    });
  });

  it("should render preconfigured materials", () => {
    mount();

    expect(helper.byTestId('flash-message-alert')).not.toBeInDOM();
    const headerRow = helper.byTestId("table-header-row");
    expect(helper.qa("th", headerRow)[0].textContent).toBe("Material Name");
    expect(helper.qa("th", headerRow)[1].textContent).toBe("Type");
    expect(helper.qa("th", headerRow)[2].textContent).toBe("Url/Description");

    const dataRow = helper.byTestId("table-row");
    expect(helper.qa("td", dataRow)).toHaveLength(4);
    expect(helper.qa("td", dataRow)[0].textContent).toBe("GM");
    expect(helper.qa("td", dataRow)[1].textContent).toBe("Git");
    expect(helper.qa("td", dataRow)[2].textContent).toBe("test-repo");
  });

  it('should disable delete button if only one material is present', () => {
    mount();

    const deleteMaterialButton = helper.byTestId('delete-material-button');
    expect(deleteMaterialButton).toBeInDOM();
    expect(deleteMaterialButton).toBeDisabled();
    expect(deleteMaterialButton).toHaveAttr('title', 'Cannot delete this material as pipeline should have at least one material');
  });

  describe('PackageMaterials', () => {
    it('should render package materials', () => {
      const pkgRepo     = PackageRepository.fromJSON(getPackageRepository());
      const pkg         = Package.fromJSON(getPackage());
      const pluginInfo  = PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension());
      const pkgMaterial = new Material("package", new PackageMaterialAttributes("", false, pkg.id()));

      pluginInfos.push(pluginInfo);
      pkgRepos.push(pkgRepo);
      pkgs.push(pkg);
      pipelineConfig.materials().push(pkgMaterial);

      mount();

      const dataRow = helper.q("tr[data-id='1']");
      expect(helper.qa("td", dataRow)).toHaveLength(4);
      expect(helper.qa("td", dataRow)[0].textContent).toBe("pkg-repo-name_pkg-name");
      expect(helper.qa("td", dataRow)[1].textContent).toBe("Package");
      expect(helper.qa("td", dataRow)[2].textContent).toBe("Plugin: NPM plugin for package repoRepository: pkg-repo-name [REPO_URL=https://npm.com]Package: pkg-name [PACKAGE_ID=pkg]");
    });

    it('should render with missing plugin', () => {
      const pkgRepo     = PackageRepository.fromJSON(getPackageRepository());
      const pkg         = Package.fromJSON(getPackage());
      const pkgMaterial = new Material("package", new PackageMaterialAttributes("", false, pkg.id()));

      pkgRepos.push(pkgRepo);
      pkgs.push(pkg);
      pipelineConfig.materials().push(pkgMaterial);

      mount();

      const dataRow = helper.q("tr[data-id='1']");
      expect(helper.qa("td", dataRow)).toHaveLength(4);
      expect(helper.qa("td", dataRow)[0].textContent).toBe("pkg-repo-name_pkg-name");
      expect(helper.qa("td", dataRow)[1].textContent).toBe("Package");
      expect(helper.qa("td", dataRow)[2].textContent).toBe("Plugin: Plugin 'npm' Missing!!!Repository: pkg-repo-name [REPO_URL=https://npm.com]Package: pkg-name [PACKAGE_ID=pkg]");
    });
  });

  describe('PluginMaterials', () => {
    it('should render plugin materials', () => {
      const scm            = Scm.fromJSON(getPluggableScm());
      const pluginInfo     = PluginInfo.fromJSON(getScmPlugin());
      const pluginMaterial = new Material("plugin", new PluggableScmMaterialAttributes("", false, scm.id(), "", new Filter([])));

      scms.push(scm);
      pluginInfos.push(pluginInfo);
      pipelineConfig.materials().push(pluginMaterial);

      mount();

      const dataRow = helper.q("tr[data-id='1']");
      expect(helper.qa("td", dataRow)).toHaveLength(4);
      expect(helper.qa("td", dataRow)[0].textContent).toBe(scm.name());
      expect(helper.qa("td", dataRow)[1].textContent).toBe("Github");
      expect(helper.qa("td", dataRow)[2].textContent).toBe("[url=https://github.com/sample/example.git]");
    });

    it('should render with missing plugin', () => {
      const scm            = Scm.fromJSON(getPluggableScm());
      const pluginMaterial = new Material("plugin", new PluggableScmMaterialAttributes("", false, scm.id(), "", new Filter([])));

      scms.push(scm);
      pipelineConfig.materials().push(pluginMaterial);

      mount();

      const dataRow = helper.q("tr[data-id='1']");
      expect(helper.qa("td", dataRow)).toHaveLength(4);
      expect(helper.qa("td", dataRow)[0].textContent).toBe(scm.name());
      expect(helper.qa("td", dataRow)[1].textContent).toBe("Plugin");
      expect(helper.qa("td", dataRow)[2].textContent).toBe("Plugin 'scm-plugin-id' Missing!!![url=https://github.com/sample/example.git]");
    });
  });
});

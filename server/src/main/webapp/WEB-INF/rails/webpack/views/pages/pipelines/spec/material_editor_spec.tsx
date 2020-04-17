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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {
  GitMaterialAttributes,
  Material,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes
} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {getScmPlugin} from "views/pages/pluggable_scms/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialEditor} from "../material_editor";

describe("AddPipeline: Material Editor", () => {
  const helper = new TestHelper();
  let material: Material;

  beforeEach(() => {
    material = new Material();
  });

  afterEach(() => helper.unmount());

  it("Generates structure", () => {
    helper.mount(() => <MaterialEditor material={material}/>);

    expect(helper.q("select")).toBeTruthy();
    expect(helper.q("label")).toBeTruthy();
    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option"))
      .toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server", "Another Pipeline"]);
  });

  it("`disabled` attribute makes all child fields disabled", () => {
    helper.mount(() => <MaterialEditor material={material} disabled={true}/>);

    const els = helper.qa("input,select");
    expect(els.length).not.toBe(0);
    for (const el of Array.from(els)) {
      expect(el.matches("[disabled]")).toBe(true);
    }
  });

  it("Generates scm only structure", () => {
    helper.mount(() => <MaterialEditor material={material} scmOnly={true}/>);

    expect(helper.q("select")).toBeTruthy();
    expect(helper.q("label")).toBeTruthy();
    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option"))
      .toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server"]);
  });

  it("Selecting a material updates the model type", () => {
    helper.mount(() => <MaterialEditor material={material}/>);

    expect(material.type()).toBeUndefined();
    expect(material.attributes()).toBeUndefined();

    helper.onchange("select", "p4");
    expect(material.type()).toBe("p4");
    expect(material.attributes() instanceof P4MaterialAttributes).toBe(true);
    expect(material.attributes()!.autoUpdate()).toBeTrue();
    expect(helper.byTestId("form-field-label-repository-url")).toBeFalsy();
    expect(helper.byTestId("form-field-label-p4-view")).toBeTruthy();
    expect(helper.byTestId("form-field-label-p4-view").textContent).toBe("P4 View*");

    helper.onchange("select", "git");
    expect(material.type()).toBe("git");
    expect(material.attributes() instanceof GitMaterialAttributes).toBe(true);
    expect(material.attributes()!.autoUpdate()).toBeTrue();
    expect(helper.byTestId("form-field-label-p4-view")).toBeFalsy();
    expect(helper.byTestId("form-field-label-repository-url")).toBeTruthy();
    expect(helper.byTestId("form-field-label-repository-url").textContent).toBe("Repository URL*");
  });

  it('should render the package options with message', () => {
    const pluginInfos = new PluginInfos(PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension()));
    helper.mount(() => <MaterialEditor material={material} showExtraMaterials={true} pluginInfos={pluginInfos}
                                       packageRepositories={new PackageRepositories()}/>);

    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option")).toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server", "Another Pipeline", "Package Materials", "Plugin Materials"]);

    expect(helper.q('span[data-test-id="package-msg"]')).not.toBeInDOM();
    helper.onchange("select", "package");

    expect(material.type()).toBe("package");
    expect(material.attributes() instanceof PackageMaterialAttributes).toBe(true);
    expect(material.attributes()!.autoUpdate()).toBeTrue();
    expect((material.attributes() as PackageMaterialAttributes)!.ref()).toBe("");

    const msgElement = helper.q('span[data-test-id="package-msg"]');
    expect(msgElement).toBeInDOM();
    expect(msgElement.textContent).toBe('Create New or select existing packages below.');
    expect(helper.q('a', msgElement)).toHaveAttr('href', SparkRoutes.packageRepositoriesSPA());
  });

  it('should render the scm options with message', () => {
    const pluginInfos = new PluginInfos(PluginInfo.fromJSON(getScmPlugin()));
    helper.mount(() => <MaterialEditor material={material} showExtraMaterials={true} pluginInfos={pluginInfos}
                                       packageRepositories={new PackageRepositories()}/>);

    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option")).toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server", "Another Pipeline", "Package Materials", "Plugin Materials"]);

    expect(helper.q('span[data-test-id="plugin-msg"]')).not.toBeInDOM();
    helper.onchange("select", "plugin");

    expect(material.type()).toBe("plugin");
    expect(material.attributes() instanceof PluggableScmMaterialAttributes).toBe(true);
    expect(material.attributes()!.autoUpdate()).toBeTrue();

    const attributes = (material.attributes() as PluggableScmMaterialAttributes)!;
    expect(attributes.ref()).toBe("");
    expect(attributes.destination()).toBe("");
    expect(attributes.filter().ignore()).toEqual([]);

    const msgElement = helper.q('span[data-test-id="plugin-msg"]');
    expect(msgElement).toBeInDOM();
    expect(msgElement.textContent).toBe('Create New or select existing scms below.');
    expect(helper.q('a', msgElement)).toHaveAttr('href', SparkRoutes.pluggableScmSPA());
  });

  it('should disable only material type selection when `disabledMaterialTypeSelection` is set to true', () => {
    material.type("git");
    helper.mount(() => <MaterialEditor material={material} disabledMaterialTypeSelection={true}/>);

    expect(helper.byTestId('form-field-input-material-type')).toBeDisabled();
    expect(helper.byTestId('form-field-input-repository-url')).not.toBeDisabled();
  });

  it('should disable only scm material type options when `disableScmMaterials` is set to true', () => {
    helper.mount(() => <MaterialEditor material={material} showExtraMaterials={true} disableScmMaterials={true}/>);

    const materialTypeSelection = helper.byTestId('form-field-input-material-type');
    expect(materialTypeSelection).not.toBeDisabled();

    ["dependency", "package"]
      .forEach((type) => {
        expect(helper.q(`option[value='${type}']`, materialTypeSelection)).not.toHaveAttr('disabled');
      });
    ["git", "hg", "svn", "p4", "tfs", "plugin"]
      .forEach((type) => {
        expect(helper.q(`option[value='${type}']`, materialTypeSelection)).toHaveAttr('disabled');
      });
  });
});

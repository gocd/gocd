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
import {GitMaterialAttributes, Material, P4MaterialAttributes, PackageMaterialAttributes} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
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

  it('should render the package options', () => {
    helper.mount(() => <MaterialEditor material={material} showExtraMaterials={true}
                                       packageRepositories={new PackageRepositories()}/>);

    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option")).toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server", "Another Pipeline", "Package"]);

    helper.onchange("select", "package");

    expect(material.type()).toBe("package");
    expect(material.attributes() instanceof PackageMaterialAttributes).toBe(true);
    expect(material.attributes()!.autoUpdate()).toBeTrue();
    expect((material.attributes() as PackageMaterialAttributes)!.ref()).toBe("");
  });

  it('should disable only material type selection when `disabledMaterialTypeSelection` is set to true', () => {
    material.type("git");
    helper.mount(() => <MaterialEditor material={material} disabledMaterialTypeSelection={true}/>);

    expect(helper.byTestId('form-field-input-material-type')).toBeDisabled();
    expect(helper.byTestId('form-field-input-repository-url')).not.toBeDisabled();
  });
});

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
import {GitMaterialAttributes, Material, P4MaterialAttributes} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialEditor} from "../material_editor";

describe("AddPipeline: Material Editor", () => {
  const helper = new TestHelper();
  let material: Material;

  beforeEach(() => {
    material = new Material();

    helper.mount(() => {
      return <MaterialEditor material={material} group={"default"}/>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure", () => {
    expect(helper.q("select")).toBeTruthy();
    expect(helper.q("label")).toBeTruthy();
    expect(helper.q("label").textContent).toBe("Material Type*");
    expect(helper.textAll("option"))
      .toEqual(["Git", "Mercurial", "Subversion", "Perforce", "Team Foundation Server", "Another Pipeline"]);
  });

  it("Selecting a material updates the model type", () => {
    expect(material.type()).toBeUndefined();
    expect(material.attributes()).toBeUndefined();

    helper.onchange("select", "p4");
    expect(material.type()).toBe("p4");
    expect(material.attributes() instanceof P4MaterialAttributes).toBe(true);
    expect(helper.byTestId("form-field-label-repository-url")).toBeFalsy();
    expect(helper.byTestId("form-field-label-p4-view")).toBeTruthy();
    expect(helper.byTestId("form-field-label-p4-view").textContent).toBe("P4 View*");

    helper.onchange("select", "git");
    expect(material.type()).toBe("git");
    expect(material.attributes() instanceof GitMaterialAttributes).toBe(true);
    expect(helper.byTestId("form-field-label-p4-view")).toBeFalsy();
    expect(helper.byTestId("form-field-label-repository-url")).toBeTruthy();
    expect(helper.byTestId("form-field-label-repository-url").textContent).toBe("Repository URL*");
  });
});

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
import {
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes,
} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "../scm_material_fields";

describe("AddPipeline: SCM Material Fields", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("GitFields structure", () => {
    const material = new Material("git", new GitMaterialAttributes());
    helper.mount(() => <GitFields material={material} group={"default"}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "repository-branch":       "Repository Branch",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("HgFields structure", () => {
    const material = new Material("hg", new HgMaterialAttributes());
    helper.mount(() => <HgFields material={material} group={"default"}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("SvnFields structure", () => {
    const material = new Material("svn", new SvnMaterialAttributes());
    helper.mount(() => <SvnFields material={material} group={"default"}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "check-externals":         "Check Externals",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("P4Fields structure", () => {
    const material = new Material("p4", new P4MaterialAttributes());
    helper.mount(() => <P4Fields material={material} group={"default"}/>);

    assertLabelledInputsPresent({
      "p4-protocol-host-port":     "P4 [Protocol:][Host:]Port*",
      "p4-view":                   "P4 View*",
      "use-ticket-authentication": "Use Ticket Authentication",
      "username":                  "Username",
      "password":                  "Password",
      "alternate-checkout-path":   "Alternate Checkout Path",
      "material-name":             "Material Name",
    });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("TfsFields structure", () => {
    const material = new Material("tfs", new TfsMaterialAttributes());
    helper.mount(() => <TfsFields material={material} group={"default"}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "project-path":            "Project Path*",
      "domain":                  "Domain",
      "username":                "Username*",
      "password":                "Password*",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  function assertLabelledInputsPresent(idsToLabels: {[key: string]: string}) {
    const keys = Object.keys(idsToLabels);
    expect(keys.length > 0).toBe(true);

    for (const id of keys) {
      expect(helper.byTestId(`form-field-label-${id}`)).toBeTruthy();
      expect(helper.byTestId(`form-field-label-${id}`).textContent!.startsWith(idsToLabels[id])).toBe(true);
      expect(helper.byTestId(`form-field-input-${id}`)).toBeTruthy();
    }
  }
});

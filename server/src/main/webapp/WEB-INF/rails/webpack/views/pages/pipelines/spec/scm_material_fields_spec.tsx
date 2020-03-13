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
import {GitMaterialAttributes, HgMaterialAttributes, Material, P4MaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes,} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "../scm_material_fields";

describe("AddPipeline: SCM Material Fields", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("GitFields structure", () => {
    const material = new Material("git", new GitMaterialAttributes());
    helper.mount(() => <GitFields material={material} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "repository-branch":       "Repository Branch",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("HgFields structure", () => {
    const material = new Material("hg", new HgMaterialAttributes());
    helper.mount(() => <HgFields material={material} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "repository-branch":       "Repository Branch",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("SvnFields structure", () => {
    const material = new Material("svn", new SvnMaterialAttributes());
    helper.mount(() => <SvnFields material={material} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "check-externals":         "Check Externals",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("P4Fields structure", () => {
    const material = new Material("p4", new P4MaterialAttributes());
    helper.mount(() => <P4Fields material={material} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "p4-protocol-host-port":     "P4 [Protocol:][Host:]Port*",
      "p4-view":                   "P4 View*",
      "use-ticket-authentication": "Use Ticket Authentication",
      "username":                  "Username",
      "password":                  "Password",
      "alternate-checkout-path":   "Alternate Checkout Path",
      "material-name":             "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("TfsFields structure", () => {
    const material = new Material("tfs", new TfsMaterialAttributes());
    helper.mount(() => <TfsFields material={material} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "project-path":            "Project Path*",
      "domain":                  "Domain",
      "username":                "Username*",
      "password":                "Password*",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("hides test connection button when `hideTestConnection` === true", () => {
    const material = new Material("git", new GitMaterialAttributes());
    helper.mount(() => <GitFields material={material} hideTestConnection={true} showLocalWorkingCopyOptions={true}/>);

    assertLabelledInputsPresent({
      "repository-url":          "Repository URL*",
      "repository-branch":       "Repository Branch",
      "username":                "Username",
      "password":                "Password",
      "alternate-checkout-path": "Alternate Checkout Path",
      "material-name":           "Material Name",
    });
    assertAutoUpdateSwitchPresent();

    expect(helper.byTestId("test-connection-button")).toBe(null!);
  });

  it("displays test connection button by default when `hideTestConnection` is falsey (e.g., unspecified)", () => {
    const material = new Material("git", new GitMaterialAttributes());
    helper.mount(() => <GitFields material={material} showLocalWorkingCopyOptions={true}/>);

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  it("does not display certain advanced settings when `showLocalWorkingCopyOptions` === false", () => {
    const material = new Material("git", new GitMaterialAttributes());
    helper.mount(() => <GitFields material={material} showLocalWorkingCopyOptions={false}/>);

    assertLabelledInputsPresent({
      "repository-url":    "Repository URL*",
      "repository-branch": "Repository Branch",
      "username":          "Username",
      "password":          "Password",
    });

    assertLabelledInputsAbsent(
      "alternate-checkout-path",
      "material-name",
    );
    assertAutoUpdateSwitchAbsent();

    expect(helper.byTestId("test-connection-button")).toBeInDOM();
  });

  function assertLabelledInputsPresent(idsToLabels: {[key: string]: string}) {
    const keys = Object.keys(idsToLabels);
    expect(keys.length > 0).toBe(true);

    for (const id of keys) {
      expect(helper.byTestId(`form-field-label-${id}`)).toBeInDOM();
      expect(helper.byTestId(`form-field-label-${id}`).textContent!.startsWith(idsToLabels[id])).toBe(true);
      expect(helper.byTestId(`form-field-input-${id}`)).toBeInDOM();
    }
  }

  function assertLabelledInputsAbsent(...keys: string[]) {
    expect(keys.length > 0).toBe(true);

    for (const id of keys) {
      expect(helper.byTestId(`form-field-label-${id}`)).toBe(null!);
      expect(helper.byTestId(`form-field-input-${id}`)).toBe(null!);
    }
  }

  function assertAutoUpdateSwitchPresent() {
    const helpTextElement = helper.q('#switch-btn-help-text');

    expect(helper.byTestId('auto-update-material')).toBeInDOM();
    expect(helper.textByTestId('switch-label')).toBe('Poll for new changes');
    expect(helpTextElement.textContent).toEqual("By default GoCD polls the repository for changes automatically. If set to false, then GoCD will not poll the repository for changes");
  }

  function assertAutoUpdateSwitchAbsent() {
    expect(helper.byTestId('auto-update-material')).not.toBeInDOM();
  }
});

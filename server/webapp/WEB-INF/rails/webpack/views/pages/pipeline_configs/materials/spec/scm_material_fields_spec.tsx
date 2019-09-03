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
import {Material} from "models/new_pipeline_configs/materials";
import {TestHelper} from "views/pages/spec/test_helper";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "../scm_material_fields";

describe("AddPipeline: SCM Material Fields", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("GitFields structure", () => {
    const material = new Material("git");
    helper.mount(() => <GitFields material={material}/>);

    assertLabelledInputsPresent({
                                  "repository-url": "Repository URL",
                                  "repository-branch": "Repository Branch",
                                  "username": "Username",
                                  "password": "Password",
                                  "alternate-checkout-path": "Alternate Checkout Path",
                                  "material-name": "Material Name",
                                });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("HgFields structure", () => {
    const material = new Material("hg");
    helper.mount(() => <HgFields material={material}/>);

    assertLabelledInputsPresent({
                                  "repository-url": "Repository URL",
                                  "repository-branch": "Repository Branch",
                                  "username": "Username",
                                  "password": "Password",
                                  "alternate-checkout-path": "Alternate Checkout Path",
                                  "material-name": "Material Name",
                                });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("SvnFields structure", () => {
    const material = new Material("svn");
    helper.mount(() => <SvnFields material={material}/>);

    assertLabelledInputsPresent({
                                  "repository-url": "Repository URL",
                                  "check-externals": "Check Externals",
                                  "username": "Username",
                                  "password": "Password",
                                  "alternate-checkout-path": "Alternate Checkout Path",
                                  "material-name": "Material Name",
                                });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("P4Fields structure", () => {
    const material = new Material("p4");
    helper.mount(() => <P4Fields material={material}/>);

    assertLabelledInputsPresent({
                                  "p4-protocol-host-port": "P4 [Protocol:][Host:]Port",
                                  "p4-view": "P4 View",
                                  "use-ticket-authentication": "Use Ticket Authentication",
                                  "username": "Username",
                                  "password": "Password",
                                  "alternate-checkout-path": "Alternate Checkout Path",
                                  "material-name": "Material Name",
                                });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  it("TfsFields structure", () => {
    const material = new Material("tfs");
    helper.mount(() => <TfsFields material={material}/>);

    assertLabelledInputsPresent({
                                  "repository-url": "Repository URL",
                                  "project-path": "Project Path",
                                  "domain": "Domain",
                                  "username": "Username",
                                  "password": "Password",
                                  "alternate-checkout-path": "Alternate Checkout Path",
                                  "material-name": "Material Name",
                                });

    expect(helper.byTestId("test-connection-button")).toBeTruthy();
  });

  function assertLabelledInputsPresent(idsToLabels: { [key: string]: string }) {
    const keys = Object.keys(idsToLabels);
    expect(keys.length > 0).toBe(true);

    for (const id of keys) {
      expect(helper.byTestId(`form-field-label-${id}`)).toBeTruthy();
      expect(helper.findByDataTestId(`form-field-label-${id}`).text().startsWith(idsToLabels[id])).toBe(true);
      expect(helper.byTestId(`form-field-input-${id}`)).toBeTruthy();
    }
  }
});

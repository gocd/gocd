/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import _ from "lodash";
import m from "mithril";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {ModalState} from "views/components/modal";
import {EditEnvironmentVariablesModal} from "views/pages/new-environments/edit_environment_variables_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Edit environment variables modal", () => {
  const helper = new TestHelper();
  let modal: EditEnvironmentVariablesModal;
  let environment: EnvironmentWithOrigin;

  beforeEach(() => {
    environment = EnvironmentWithOrigin.fromJSON(data.xml_environment_json());
    modal       = new EditEnvironmentVariablesModal(environment, _.noop);
    helper.mount(() => modal.view());
  });

  afterEach(helper.unmount.bind(helper));

  it("should have a title", () => {
    expect(modal.title()).toBe("Edit Environment Variables");
  });

  it("should add environment variables", () => {
    expect(modal.environmentVariablesToAdd().length).toBe(0);

    helper.clickByTestId("add-plain-text-variables-btn");
    // Clicked on add multiple times to check if blank variables not get added
    helper.clickByTestId("add-plain-text-variables-btn");
    helper.clickByTestId("add-plain-text-variables-btn");
    helper.clickByTestId("add-secure-variables-btn");

    const newVarName = helper.allByTestId("env-var-name")[1] as HTMLInputElement;
    helper.oninput(newVarName, "new-var");
    const newVarValue = helper.allByTestId("env-var-value")[1] as HTMLInputElement;
    helper.oninput(newVarValue, "new-value");

    const existingVarName = helper.allByTestId("env-var-name")[0] as HTMLInputElement;
    helper.oninput(existingVarName, "new-name-for-existing");

    const variablesToAdd = modal.environmentVariablesToAdd();

    expect(variablesToAdd.length).toBe(2);
    expect(variablesToAdd[0].name()).toBe("new-name-for-existing");
    expect(variablesToAdd[1].name()).toBe("new-var");
  });

  it("should remove environment variables", () => {
    const oldNameForEnvVar1 = environment.environmentVariables()[0].name();
    const oldNameForEnvVar2 = environment.environmentVariables()[1].name();
    expect(modal.environmentVariablesToRemove().length).toBe(0);

    helper.click(helper.allByTestId("remove-env-var-btn")[1]);
    const existingVarName = helper.allByTestId("env-var-name")[0] as HTMLInputElement;
    helper.oninput(existingVarName, "new-name-for-existing");

    const variablesToRemove = modal.environmentVariablesToRemove();
    expect(variablesToRemove.length).toBe(2);
    expect(variablesToRemove[0].name()).toBe(oldNameForEnvVar1);
    expect(variablesToRemove[1].name()).toBe(oldNameForEnvVar2);
  });

  it('should render buttons', () => {
    expect(helper.byTestId("cancel-button")).toBeInDOM();
    expect(helper.byTestId("cancel-button")).toHaveText("Cancel");
    expect(helper.byTestId("save-button")).toBeInDOM();
    expect(helper.byTestId("save-button")).toHaveText("Save");
  });

  it('should disable save and cancel button if modal state is loading', () => {
    modal.modalState = ModalState.LOADING;
    m.redraw.sync();
    expect(helper.byTestId("save-button")).toBeDisabled();
    expect(helper.byTestId("cancel-button")).toBeDisabled();
    expect(helper.byTestId("spinner")).toBeInDOM();
  });
});

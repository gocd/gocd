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
import Stream from "mithril/stream";
import {Template} from "models/admin_templates/templates";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ModalState} from "views/components/modal";
import {ModalManager} from "views/components/modal/modal_manager";
import {ShowTemplateModal} from "views/pages/admin_templates/modals";
import {massiveTemplate} from "views/pages/admin_templates/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ShowTemplateModal", () => {
  let modal: ShowTemplateModal;

  beforeEach(() => {
    modal            = new ShowTemplateModal("build-gradle-linux",
                                             Stream<Template>(massiveTemplate()),
                                             new PluginInfos());
    modal.modalState = ModalState.OK;
    modal.render();
    m.redraw.sync();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render stage/job tree details", () => {
    const testHelper = new TestHelper().forModal();
    expect(modal).toContainTitle("Showing template build-gradle-linux");

    // renders job/stage tree
    expect(testHelper.textByTestId("stage-job-tree")).toContain("build-non-server");
    expect(testHelper.textByTestId("stage-job-tree")).toContain("FastTests");
    expect(testHelper.textByTestId("stage-job-tree")).toContain("Jasmine");
  });

  it("shouold render stage details", () => {
    const testHelper = new TestHelper().forModal();

    // stage details
    expect(testHelper.textByTestId("selected-stage-build-non-server")).toContain("Showing stage build-non-server");

    expect(testHelper.textByTestId("key-value-value-stage-type")).toEqual("On success");
    expect(testHelper.textByTestId("key-value-value-fetch-materials")).toEqual("Yes");
    expect(testHelper.textByTestId("key-value-value-never-cleanup-artifacts")).toEqual("No");
    expect(testHelper.textByTestId("key-value-value-clean-working-directory")).toEqual("No");

    // env vars in stage
    expect(testHelper.textByTestId("key-value-key-pipeline-env-var")).toEqual("pipeline-env-var");
    expect(testHelper.textByTestId("key-value-value-pipeline-env-var")).toEqual("blah");
    expect(testHelper.textByTestId("key-value-key-secure-pipeline-env-var")).toEqual("secure-pipeline-env-var");
    expect(testHelper.textByTestId("key-value-value-secure-pipeline-env-var")).toEqual("******");
  });

  it("shouold render job details", () => {
    const testHelper = new TestHelper().forModal();

    // select job
    testHelper.click(testHelper.byTestId("tree-node-fasttests").querySelector("a")!);

    // assert it shows up
    expect(testHelper.textByTestId("selected-job-build-non-server-FastTests"))
      .toContain("Showing job build-non-server > FastTests");
  });
});

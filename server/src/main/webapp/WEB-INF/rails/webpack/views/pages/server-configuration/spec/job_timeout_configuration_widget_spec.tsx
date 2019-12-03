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
import {ArtifactConfig, DefaultJobTimeout} from "models/server-configuration/server_configuration";
import {TestHelper} from "views/pages/spec/test_helper";
import {DefaultJobTimeoutVM} from "../../../../models/server-configuration/server_configuration_vm";
import {JobTimeoutConfigurationWidget} from "../job_timeout_configuration_widget";

describe("defaultJobTimeoutWidget", () => {
  const helper      = new TestHelper();
  let jobTimeoutVM: DefaultJobTimeoutVM;
  const onSaveSpy   = jasmine.createSpy("onSave");
  const onCancelSpy = jasmine.createSpy("onCancel");

  function mount(defaultJobTimeout: DefaultJobTimeout) {
    jobTimeoutVM = new DefaultJobTimeoutVM();
    jobTimeoutVM.sync(defaultJobTimeout);

    const savePromise: Promise<ArtifactConfig> = new Promise((resolve) => {
      onSaveSpy();
      resolve();
    });

    helper.mount(() =>
                   <JobTimeoutConfigurationWidget defaultJobTimeoutVM={Stream(jobTimeoutVM)}
                                                  onDefaultJobTimeoutSave={() => savePromise}
                                                  onCancel={onCancelSpy}/>);
  }

  afterEach((done) => helper.unmount(done));

  it("should render form", () => {
    mount(new DefaultJobTimeout(0));

    expect(helper.byTestId("form-field-input-default-job-timeout")).toBeInDOM();
    expect(helper.byTestId("checkbox-for-job-timeout")).toBeInDOM();
    expect(helper.byTestId("form-field-label-never-job-timeout")).toBeInDOM();
    expect(helper.byTestId("form-field-label-default-job-timeout")).toBeInDOM();
    expect(helper.byTestId("form-field-input-default-job-timeout").nextSibling)
      .toHaveText("the job will get cancel after the given minutes of inactivity");
  });

  describe("Save", () => {
    it("should have save button", () => {
      mount(new DefaultJobTimeout(0));
      expect(helper.byTestId("save")).toBeInDOM();
    });

    it("should call onSave", () => {
      mount(new DefaultJobTimeout(0));
      helper.click(helper.byTestId("save"));
      expect(onSaveSpy).toHaveBeenCalled();
    });
  });

  describe("Cancel", () => {
    it("should render cancel button", () => {
      mount(new DefaultJobTimeout(0));
      expect(helper.byTestId("cancel")).toHaveText("Cancel");
    });

    it("should call onCancel", () => {
      mount(new DefaultJobTimeout(0));
      helper.clickByTestId("cancel");
      expect(onCancelSpy).toHaveBeenCalledWith(jobTimeoutVM);
    });
  });

  it("should disable the input field and never job timeout should be checked", () => {
    mount(new DefaultJobTimeout(0));

    expect(helper.byTestId("form-field-input-default-job-timeout")).toBeDisabled();
    expect(helper.byTestId("checkbox-for-job-timeout")).toBeChecked();
  });

  it("should enable the input field when never-job-timeout checkbox is not checked ", () => {
    mount(new DefaultJobTimeout(0));

    helper.clickByTestId("checkbox-for-job-timeout");
    expect(helper.byTestId("form-field-input-default-job-timeout")).not.toBeDisabled();
  });

  it("should show error text", () => {
    const defaultJobTimeout = new DefaultJobTimeout(0);
    mount(defaultJobTimeout);

    jobTimeoutVM.jobTimeout().errors().add("defaultJobTimeout", "some-error");
    m.redraw.sync();

    const inputId = helper.byTestId("form-field-input-default-job-timeout").getAttribute("id");
    expect(helper.q(`#${inputId}-error-text`)).toHaveText("some-error.");
  });

});

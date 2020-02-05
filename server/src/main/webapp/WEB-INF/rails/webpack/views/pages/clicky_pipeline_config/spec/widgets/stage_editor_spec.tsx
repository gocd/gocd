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
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {StageEditor} from "views/pages/clicky_pipeline_config/widgets/stage_editor";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineConfig: StageEditor", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render stage name", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(helper.byTestId("stage-name-input")).toBeInDOM();
    expect(helper.byTestId("stage-name-input")).toHaveValue("Test");

    helper.oninput(helper.byTestId("stage-name-input"), "Junit");

    expect(helper.byTestId("stage-name-input")).toHaveValue("Junit");
    expect(stage.name()).toEqual("Junit");
  });

  it("should allow to change approval type", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(stage.approval().typeAsString()).toEqual("manual");

    helper.clickByTestId("switch-checkbox");

    expect(stage.approval().typeAsString()).toEqual("success");
  });

  it("should render checkbox for allow only in success", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);
    expect(stage.approval().allowOnlyOnSuccess()).toBeFalse();

    helper.clickByTestId("allow-only-on-success-checkbox");

    expect(stage.approval().allowOnlyOnSuccess()).toBeTrue();
  });

  it("should render job name text field", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);
    expect(stage.firstJob().name()).toEqual("Job1");

    helper.oninput(helper.byTestId("job-name-input"), "Junit");

    expect(stage.firstJob().name()).toEqual("Junit");
  });

  function mount(stage: Stage) {
    helper.mount(() => <StageEditor stage={Stream(stage)}/>);
  }
});

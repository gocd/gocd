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
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityData} from "models/pipeline_activity/spec/test_data";
import {TestHelper} from "../../spec/test_helper";
import {PipelineActivityWidget} from "../pipeline_activity_widget";

describe("PipelineActivityWidget", () => {
  const helper              = new TestHelper();
  const showBuildCaseFor    = Stream<string>();
  const cancelStageInstance = jasmine.createSpy("cancelStageInstance");
  const runPipeline         = jasmine.createSpy("runPipeline");
  const runStage            = jasmine.createSpy("runStage");
  const addOrUpdateComment  = jasmine.createSpy("addOrUpdateComment");

  afterEach(helper.unmount.bind(helper));

  it("should render header", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
    mount(activity);

    expect(helper.byTestId("instance-header")).toBeInDOM();
    expect(helper.byTestId("instance-header")).toHaveText("Instance");
  });

  it("should render stage name", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
    mount(activity);

    expect(helper.byTestId("stage-foo")).toBeInDOM();
    expect(helper.byTestId("stage-foo")).toHaveText("foo");
  });

  function mount(activity: PipelineActivity) {
    helper.mount(() => <PipelineActivityWidget pipelineActivity={Stream(activity)}
                                               showBuildCaseFor={showBuildCaseFor}
                                               runStage={runStage}
                                               cancelStageInstance={cancelStageInstance}
                                               addOrUpdateComment={addOrUpdateComment}
                                               runPipeline={runPipeline}/>);
  }
});

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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineActivity, StageConfig} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityData} from "models/pipeline_activity/spec/test_data";
// @ts-ignore
import state from "views/dashboard/models/stage_overview_state";
import {TestHelper} from "../../spec/test_helper";
import styles from "../index.scss";
import {PipelineActivityWidget} from "../pipeline_activity_widget";

describe("PipelineActivityWidget", () => {
  const helper              = new TestHelper();
  const showBuildCauseFor   = Stream<string>();
  const showCommentFor      = Stream<string>();
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

  it('should render header for the first stage with no margin class', () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
    mount(activity);

    const dataTestId = `stage-${activity.groups()[0].config().stages()[0].name()}`;
    expect(helper.byTestId(dataTestId)).toBeInDOM();
    expect(helper.byTestId(dataTestId)).toHaveClass(styles.noMargin);
  });

  it('should render header for the non-first stage with margin23 class', () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.underConstruction());
    activity.groups()[0].config().stages().push(new StageConfig("second", true));
    mount(activity);

    const dataTestId = `stage-${activity.groups()[0].config().stages()[1].name()}`;
    expect(helper.byTestId(dataTestId)).toBeInDOM();
    expect(helper.byTestId(dataTestId)).toHaveClass(styles.margin23);
  });

  function mount(activity: PipelineActivity) {
    helper.mount(() => <PipelineActivityWidget pipelineActivity={Stream(activity)}
                                               showBuildCaseFor={showBuildCauseFor}
                                               showCommentFor={showCommentFor}
                                               runStage={runStage}
                                               addOrUpdateComment={addOrUpdateComment}
                                               canOperatePipeline={false}
                                               canAdministerPipeline={false}
                                               stageOverviewState={state.StageOverviewState}
                                               showStageOverview={() => null}
                                               runPipeline={runPipeline}/>);
  }
});

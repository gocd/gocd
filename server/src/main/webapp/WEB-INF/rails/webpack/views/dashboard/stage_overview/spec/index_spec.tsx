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
import {Agents} from "../../../../models/agents/agents";
import {TestHelper} from "../../../pages/spec/test_helper";
import {StageOverview} from "../index";
import {StageInstance} from "../models/stage_instance";
import {StageOverviewViewModel} from "../models/stage_overview_view_model";
import {StageState} from "../models/types";
import {TestData} from "./test_data";

describe("Stage Overview Widget", () => {
  const helper = new TestHelper();
  const pipelineName = "build-linux";
  const pipelineCounter = 123456789;
  const stageName = "build-and-run-jasmine-specs";
  const stageCounter = 97654321;

  afterEach(() => {
    helper.unmount();
  });

  it("should render spinner when there is no stage overview", () => {
    mount(pipelineName, pipelineCounter, stageName, stageCounter, undefined);

    expect(helper.byTestId('stage-overview-container')).not.toBeInDOM();
    expect(helper.byTestId('stage-overview-container-spinner')).toBeInDOM();
  });

  it("should render stage overview", () => {
    const stageOverviewViewModel = new StageOverviewViewModel(pipelineName, pipelineCounter, stageName, stageCounter, StageInstance.fromJSON(TestData.stageInstanceJSON()), new Agents());
    mount(pipelineName, pipelineCounter, stageName, stageCounter, stageOverviewViewModel);

    expect(helper.byTestId('stage-overview-container')).toBeInDOM();
    expect(helper.byTestId('stage-overview-container-spinner')).not.toBeInDOM();
  });

  function mount(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: number | string, stageOverviewViewModel: StageOverviewViewModel | undefined) {

    helper.mount(() => {
      const stageInstanceFromDashboard = {
        canOperate: true,
        status:     'passed'
      };

      return <StageOverview pipelineName={pipelineName}
                            pipelineCounter={pipelineCounter}
                            canAdminister={false}
                            stages={[]}
                            stageName={stageName}
                            stageCounter={stageCounter}
                            stageInstanceFromDashboard={stageInstanceFromDashboard}
                            stageOverviewVM={Stream(stageOverviewViewModel)}
                            stageStatus={StageState[StageState.Passed]}/>;
    });
  }

});

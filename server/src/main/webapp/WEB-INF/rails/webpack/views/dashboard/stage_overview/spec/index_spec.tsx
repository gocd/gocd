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
import {SparkRoutes} from "../../../../helpers/spark_routes";
import {TestHelper} from "../../../pages/spec/test_helper";
import {StageOverview} from "../index";
import {StageState} from "../models/types";
import {TestData} from "./test_data";

describe("Stage Overview Widget", () => {
  const helper = new TestHelper();
  const pipelineName = "build-linux";
  const pipelineCounter = 123456789;
  const stageName = "build-and-run-jasmine-specs";
  const stageCounter = 97654321;

  beforeEach(() => {
    mount(pipelineName, pipelineCounter, stageName, stageCounter);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render stage overview", () => {
    expect(helper.byTestId('stage-overview-container')).toBeInDOM();
  });

  function mount(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: number | string) {
    jasmine.Ajax.withMock(() => {
      const STAGE_INSTANCE_URL = SparkRoutes.getStageInstance(pipelineName, pipelineCounter, stageName, stageCounter);
      const response = TestData.stageInstanceJSON();

      jasmine.Ajax.stubRequest(STAGE_INSTANCE_URL, undefined, "GET").andReturn({
        responseText:    JSON.stringify(response),
        status:          200,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json",
          "ETag":         "ETag"
        }
      });

      helper.mount(() => {
        return <StageOverview pipelineName={pipelineName}
                              pipelineCounter={pipelineCounter}
                              stageName={stageName}
                              stageCounter={stageCounter}
                              stageStatus={StageState[StageState.Passed]}/>;
      });
    });
  }

});

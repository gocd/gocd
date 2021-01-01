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

import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import {CompareHeaderWidget} from "../compare_header_widget";

describe('CompareHeaderWidget', () => {
  const testHelper = new TestHelper();

  it('should display pipeline name with a link to history', () => {
    const pipelineName = "up42";
    testHelper.mount(() => <CompareHeaderWidget pipelineName={pipelineName}/>);

    expect(testHelper.byTestId("page-header-pipeline-label")).toBeInDOM();
    expect(testHelper.byTestId("page-header-pipeline-name")).toBeInDOM();
    expect(testHelper.textByTestId("page-header-pipeline-name")).toBe(pipelineName);

    expect(testHelper.q("a", testHelper.byTestId("page-header-pipeline-name")).getAttribute("href")).toBe(SparkRoutes.pipelineHistoryPath(pipelineName));

    testHelper.unmount();
  });
});

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
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";;
import {StepsWidget} from "views/pages/clicky_pipeline_config/widgets/steps_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("StepsWidget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render pipeline name when selected", () => {
    const params = {pipeline_name: "up42"} as PipelineConfigRouteParams;

    mount(params);

    expect(helper.byTestId("step-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("step-pipeline-name")).toHaveText("up42");
  });

  it("should render pipeline link and stage name when stage is selected", () => {
    const params = {pipeline_name: "up42", stage_name: "Junit"} as PipelineConfigRouteParams;

    mount(params);

    expect(helper.byTestId("step-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("step-pipeline-name")).toHaveText("up42");

    expect(helper.byTestId("step-stage-name")).toBeInDOM();
    expect(helper.byTestId("step-stage-name")).toHaveText("Junit");
  });

  it("should render pipeline link, stage link  and job name when job is selected", () => {
    const params = {pipeline_name: "up42", stage_name: "Junit", job_name: "Test"} as PipelineConfigRouteParams;

    mount(params);

    expect(helper.byTestId("step-pipeline-name")).toBeInDOM();
    expect(helper.byTestId("step-pipeline-name")).toHaveText("up42");

    expect(helper.byTestId("step-stage-name")).toBeInDOM();
    expect(helper.byTestId("step-stage-name")).toHaveText("Junit");

    expect(helper.byTestId("step-job-name")).toBeInDOM();
    expect(helper.byTestId("step-job-name")).toHaveText("Test");
  });

  function mount(params: PipelineConfigRouteParams) {
    const routeInfo = {route: "", params};
    helper.mount(() => <StepsWidget routeInfo={routeInfo}/>);
  }
});

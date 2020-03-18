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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {PipelineConfigRouteParams, RouteInfo} from "views/pages/clicky_pipeline_config/pipeline_config";
import {Attrs, NavigationWidget} from "views/pages/clicky_pipeline_config/widgets/navigation_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineNavigation", () => {
  const helper      = new TestHelper();
  const changeRoute = jasmine.createSpy("changeRoute");

  afterEach(helper.unmount.bind(helper));

  it("should render collapsed pipeline name", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig);

    expect(helper.byTestId("tree-node-test")).toBeInDOM();
    expect(helper.byTestId("tree-node-test")).toHaveText("Test");
  });

  it("should render stages on click of pipeline name", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig);

    helper.clickByTestId("nav-test-icon");

    expect(helper.byTestId("tree-node-stageone")).toBeInDOM();
    expect(helper.byTestId("tree-node-stageone")).toHaveText("StageOne");

    expect(helper.byTestId("tree-node-stagetwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-stagetwo")).toHaveText("StageTwo");
  });

  it("should render jobs on click of stage name", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);
    helper.clickByTestId("nav-test-icon");

    helper.clickByTestId("nav-stagetwo-icon");

    expect(helper.byTestId("tree-node-stagetwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobone")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobtwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobthree")).toBeInDOM();
    expect(helper.byTestId("tree-node-stagetwo")).toHaveText("StageTwo");
    expect(helper.byTestId("tree-node-jobone")).toHaveText("JobOne");
    expect(helper.byTestId("tree-node-jobtwo")).toHaveText("JobTwo");
    expect(helper.byTestId("tree-node-jobthree")).toHaveText("JobThree");
  });

  it("should answer whether the current route is a pipeline route", () => {
    mount(PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages()));

    const routeInfo = {
      params: {
        pipeline_name: "pipeline"
      }
    };

    // @ts-ignore
    const vnode = {attrs: {routeInfo}} as m.Vnode<Attrs>;

    expect(NavigationWidget.isPipelineRoute(vnode)).toBeTrue();
    expect(NavigationWidget.isStageRoute(vnode, "stage")).toBeFalse();
    expect(NavigationWidget.isJobRoute(vnode, "stage", "job")).toBeFalse();
  });

  it("should answer whether the current route is a stage route", () => {
    mount(PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages()));

    const routeInfo = {
      params: {
        pipeline_name: "pipeline",
        stage_name: "stage"
      }
    };

    // @ts-ignore
    const vnode = {attrs: {routeInfo}} as m.Vnode<Attrs>;

    expect(NavigationWidget.isPipelineRoute(vnode)).toBeFalse();
    expect(NavigationWidget.isStageRoute(vnode, "stage")).toBeTrue();
    expect(NavigationWidget.isJobRoute(vnode, "stage", "job")).toBeFalse();
  });

  it("should answer whether the current route is a job route", () => {
    mount(PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages()));

    const routeInfo = {
      params: {
        pipeline_name: "pipeline",
        stage_name: "stage",
        job_name: "job"
      }
    };

    // @ts-ignore
    const vnode = {attrs: {routeInfo}} as m.Vnode<Attrs>;

    expect(NavigationWidget.isPipelineRoute(vnode)).toBeFalse();
    expect(NavigationWidget.isStageRoute(vnode, "stage")).toBeFalse();
    expect(NavigationWidget.isJobRoute(vnode, "stage", "job")).toBeTrue();
  });

  function mount(pipelineConfig: PipelineConfig, routeInfo?: RouteInfo<PipelineConfigRouteParams>) {
    if (!routeInfo) {
      routeInfo = {
        route: "up42/general",
        params: {pipeline_name: pipelineConfig.name(), tab_name: "general"}
      };
    }

    helper.mount(() => <NavigationWidget pipelineConfig={pipelineConfig}
                                         routeInfo={routeInfo!}
                                         changeRoute={changeRoute}/>);
  }
});

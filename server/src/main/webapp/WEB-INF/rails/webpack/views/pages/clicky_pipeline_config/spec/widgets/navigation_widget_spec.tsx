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
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams, RouteInfo} from "views/pages/clicky_pipeline_config/tab_handler";
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

    helper.clickByTestId("tree-node-test");

    expect(helper.byTestId("tree-node-stageone")).toBeInDOM();
    expect(helper.byTestId("tree-node-stageone")).toHaveText("StageOne");

    expect(helper.byTestId("tree-node-stagetwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-stagetwo")).toHaveText("StageTwo");
  });

  it("should render jobs on click of stage name", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);
    helper.clickByTestId("tree-node-test");

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

  it("should render template", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTemplate());
    const template       = new TemplateConfig("template1", []);

    mount(pipelineConfig, template);

    expect(helper.byTestId("tree-node-pipeline-from-template")).toBeInDOM();
    expect(helper.byTestId("tree-node-pipeline-from-template")).toHaveText("pipeline-from-template");

    expect(helper.byTestId("tree-node-template1")).toBeInDOM();
    expect(helper.byTestId("tree-node-template1")).toHaveText("template1");
  });

  it("should render template name as a link with view template option", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTemplate());
    const template       = new TemplateConfig("template1", []);

    mount(pipelineConfig, template);

    expect(helper.q("a", helper.byTestId("tree-node-template1"))).toHaveText("template1");
    expect(helper.byTestId("Search-icon")).toBeInDOM();
  });

  function mount(pipelineConfig: PipelineConfig,
                 templateConfig?: TemplateConfig,
                 routeInfo?: RouteInfo<PipelineConfigRouteParams>) {
    if (!routeInfo) {
      routeInfo = {
        route: `${pipelineConfig.name()}/general`,
        params: {pipeline_name: pipelineConfig.name(), tab_name: "general"}
      };
    }

    helper.mount(() => <NavigationWidget config={pipelineConfig}
                                         isTemplateConfig={false}
                                         templateConfig={templateConfig}
                                         routeInfo={routeInfo!}
                                         changeRoute={changeRoute}/>);
  }
});

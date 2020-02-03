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
import {NavigationWidget} from "views/pages/clicky_pipeline_config/widgets/navigation_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineNavigation", () => {
  const helper      = new TestHelper();
  const changeRoute = jasmine.createSpy("changeRoute");

  afterEach(helper.unmount.bind(helper));

  it("should render pipeline, stages and jobs", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig);

    expect(helper.byTestId("tree-node-test")).toBeInDOM();
    expect(helper.byTestId("tree-node-test")).toHaveText("Test");

    expect(helper.byTestId("tree-node-stageone")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobone")).toBeInDOM();
    expect(helper.byTestId("tree-node-stageone")).toHaveText("StageOne");
    expect(helper.byTestId("tree-node-jobone")).toHaveText("JobOne");

    expect(helper.byTestId("tree-node-stagetwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobone")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobtwo")).toBeInDOM();
    expect(helper.byTestId("tree-node-jobthree")).toBeInDOM();
    expect(helper.byTestId("tree-node-stagetwo")).toHaveText("StageTwo");
    expect(helper.byTestId("tree-node-jobone")).toHaveText("JobOne");
    expect(helper.byTestId("tree-node-jobtwo")).toHaveText("JobTwo");
    expect(helper.byTestId("tree-node-jobthree")).toHaveText("JobThree");
  });

  function mount(pipelineConfig: PipelineConfig) {
    helper.mount(() => <NavigationWidget pipelineConfig={pipelineConfig}
                                         changeRoute={changeRoute}/>);
  }
});

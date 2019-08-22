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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import * as simulateEvent from "simulate-event";
import {PipelineConfigCreateWidget} from "views/pages/pipeline_configs/pipeline_config_create_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineCreateWidgetSpec", () => {
  const helper         = new TestHelper();
  const pipelineConfig = new PipelineConfig("", [], []);

  beforeEach(() => {
    helper.mount(() => <PipelineConfigCreateWidget pipelineConfig={pipelineConfig}/>);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render pipeline name field", () => {
    const pipelineNameHelpText = "No spaces. Only letters, numbers, hyphens, underscores and period. Max 255 chars";

    expect(helper.findByDataTestId("form-field-label-pipeline-name")).toContainText("Pipeline name");
    expect(helper.findByDataTestId("pipeline-name-input")).toHaveLength(1);
    expect(helper.findByDataTestId("pipeline-name-input")).toHaveProp("required");
    expect(helper.findByDataTestId("pipeline-details-container")).toContainText(pipelineNameHelpText);
  });

  it("should bind pipeline name field", () => {
    const pipelineName = "Test-pipeline";

    helper.findByDataTestId("pipeline-name-input").val(pipelineName);
    simulateEvent.simulate(helper.findByDataTestId("pipeline-name-input")[0], "input");
    m.redraw.sync();

    expect(pipelineConfig.name()).toBe(pipelineName);

    const updatedPipelineName = "Foo-bar";
    pipelineConfig.name(updatedPipelineName);
    m.redraw.sync();

    expect(helper.findByDataTestId("pipeline-name-input").val()).toBe(updatedPipelineName);
  });

  it("should display error on name field for invalid input format", () => {
    const pipelineName = "Test pipeline";
    const errorText    = "Only letters, numbers, hyphens, underscores, and periods are allowed.";

    helper.findByDataTestId("pipeline-name-input").val(pipelineName);
    simulateEvent.simulate(helper.findByDataTestId("pipeline-name-input")[0], "input");
    m.redraw.sync();

    expect(helper.findByDataTestId("pipeline-details-container")).toContainText(errorText);
  });
});

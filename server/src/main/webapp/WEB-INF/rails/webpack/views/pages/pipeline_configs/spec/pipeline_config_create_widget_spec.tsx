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

import _ from "lodash";
import m from "mithril";
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import {PipelineConfigCreateWidget} from "views/pages/pipeline_configs/pipeline_config_create_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("PipelineCreateWidgetSpec", () => {
  const helper         = new TestHelper();
  const pipelineConfig = new PipelineConfig("", [], []);
  const noop           = _.noop;
  const operations     = {
    onDelete: noop,
    onAdd: noop,
    onUpdate: noop
  };
  const pipelineSettingsCallback = jasmine.createSpy();

  beforeEach(() => {
    helper.mount(() => <PipelineConfigCreateWidget pipelineConfig={pipelineConfig}
                                                   materialOperations={operations}
                                                   onPipelineSettingsEdit={pipelineSettingsCallback}/>);
  });

  afterEach(() => helper.unmount());

  it("should render pipeline name field", () => {
    const pipelineNameHelpText = "No spaces. Only letters, numbers, hyphens, underscores and period. Max 255 chars";

    expect(helper.textByTestId("form-field-label-pipeline-name")).toContain("Pipeline name");
    expect(helper.allByTestId("pipeline-name-input")).toHaveLength(1);
    expect(helper.byTestId("pipeline-name-input")).toHaveProp("required");
    expect(helper.textByTestId("pipeline-details-container")).toContain(pipelineNameHelpText);
  });

  it("should bind pipeline name field", () => {
    const pipelineName = "Test-pipeline";

    helper.oninput(helper.byTestId("pipeline-name-input"), pipelineName);

    expect(pipelineConfig.name()).toBe(pipelineName);

    const updatedPipelineName = "Foo-bar";
    pipelineConfig.name(updatedPipelineName);
    m.redraw.sync();

    expect(helper.byTestId("pipeline-name-input")).toHaveValue(updatedPipelineName);
  });

  it("should display error on name field for invalid input format", () => {
    const pipelineName = "Test pipeline";
    const errorText    = "Only letters, numbers, hyphens, underscores, and periods are allowed.";

    helper.oninput(helper.byTestId("pipeline-name-input"), pipelineName);

    expect(helper.textByTestId("pipeline-details-container")).toContain(errorText);
  });

  it("should open pipeline settings modal", () => {
    helper.clickByTestId("pipeline-settings-button");
    expect(pipelineSettingsCallback).toHaveBeenCalled();
  });
});

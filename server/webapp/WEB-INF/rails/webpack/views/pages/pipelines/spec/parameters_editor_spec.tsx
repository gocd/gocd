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
import {PipelineParameters} from "models/pipeline_configs/parameters";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineParametersEditor} from "../parameters_editor";

describe("ParametersEditor", () => {
  const helper = new TestHelper();
  let config: PipelineConfig;

  beforeEach(() => {
    config = new PipelineConfig("", [], []);
    helper.mount(() => <PipelineParametersEditor parameters={config.parameters} />);
  });

  afterEach(helper.unmount.bind(helper));

  xit("should update the model when a parameter is updated", () => {
    expect(config.parameters()).toEqual([]);

    helper.oninput(helper.byTestId("form-field-input-"), "my-param");
    helper.oninput(helper.byTestId("form-field-input-pipeline-name"), "lalala");

    expect(config.parameters()).toEqual([new PipelineParameters("my-param", "lalala")]);
  });
});

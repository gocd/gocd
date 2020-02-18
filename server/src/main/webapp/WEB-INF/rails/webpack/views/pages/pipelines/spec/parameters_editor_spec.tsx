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
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineParametersEditor} from "../parameters_editor";

describe("ParametersEditor", () => {
  const helper = new TestHelper();
  let config: PipelineConfig;
  const paramList = Stream([] as PipelineParameter[]);

  beforeEach(() => {
    config = new PipelineConfig("", [], []);
    paramList([]);
  });

  afterEach(helper.unmount.bind(helper));

  it("should update the model when a parameter is updated", () => {
    helper.mount(() => <PipelineParametersEditor parameters={config.parameters} paramList={paramList} />);
    expect(config.parameters()).toEqual([]);

    helper.oninput(helper.byTestId("form-field-input-param-name-0"), "my-param");
    helper.oninput(helper.byTestId("form-field-input-param-value-0"), "lalala");

    expect(asJson(config.parameters())).toEqual(asJson([new PipelineParameter("my-param", "lalala")]));
  });

  it("should update model when parameter removed", () => {
    config.parameters([
      new PipelineParameter("my-param", "lalala"),
      new PipelineParameter("my-fav-param", "lalala"),
      new PipelineParameter("my-other-param", "lalala")
    ]);

    const [param1, , param3] = config.parameters();
    helper.mount(() => <PipelineParametersEditor parameters={config.parameters} paramList={paramList}/>);

    helper.click(helper.qa("table button").item(1));

    expect(asJson(config.parameters())).toEqual(asJson([param1, param3]));
  });

  it("should at least have an empty param", () => {
    //empty inputs on load
    helper.mount(() => <PipelineParametersEditor parameters={config.parameters} paramList={paramList} />);
    expect(helper.qa("table input[type=\"text\"]").length).toBe(2);
    expect((helper.byTestId("form-field-input-param-name-0") as HTMLInputElement).value).toBe("");
    expect((helper.byTestId("form-field-input-param-value-0") as HTMLInputElement).value).toBe("");

    helper.oninput(helper.byTestId("form-field-input-param-name-0"), "my-param");
    helper.oninput(helper.byTestId("form-field-input-param-value-0"), "lalala");
    expect((helper.byTestId("form-field-input-param-name-0") as HTMLInputElement).value).toBe("my-param");
    expect((helper.byTestId("form-field-input-param-value-0") as HTMLInputElement).value).toBe("lalala");
    helper.click(helper.qa("table button").item(0));

    //empty inputs when remove
    expect(helper.qa("table input[type=\"text\"]").length).toBe(2);
    expect((helper.byTestId("form-field-input-param-name-0") as HTMLInputElement).value).toBe("");
    expect((helper.byTestId("form-field-input-param-value-0") as HTMLInputElement).value).toBe("");
  });
});

function asJson(params: PipelineParameter[]) {
  return params.map((p) => p.toApiPayload());
}

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

import asSelector from "helpers/selector_proxy";
import * as m from "mithril";
import {Stage} from "models/pipeline_configs/stage";
import {TestHelper} from "views/pages/spec/test_helper";
import * as css from "../components.scss";
import {StageEditor} from "../stage_editor";

describe("AddPipeline: StageEditor", () => {
  const sel = asSelector<typeof css>(css);
  const helper = new TestHelper();
  let stage: Stage;

  beforeEach(() => {
    stage = new Stage("", []);
    helper.mount(() => <StageEditor stage={stage}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure", () => {
    expect(helper.byTestId("form-field-label-stage-name")).toBeTruthy();
    expect(helper.byTestId("form-field-label-stage-name").textContent).toBe("Stage Name*");

    expect(helper.byTestId("form-field-input-stage-name")).toBeTruthy();

    expect(helper.q(sel.switchLabelText)).toBeTruthy();
    expect(helper.byTestId("switch-label")).toBeTruthy();
    expect(helper.byTestId("switch-label").textContent!.startsWith("Automatically run this stage on upstream changes")).toBe(true);
    expect(helper.byTestId("switch-paddle")).toBeTruthy();
    expect(helper.byTestId("switch-checkbox")).toBeTruthy();
  });

  it("Binds to model", () => {
    expect(stage.name()).toBe("");

    helper.oninput(helper.byTestId("form-field-input-stage-name"), "my-stage");
    expect(stage.name()).toBe("my-stage");

    expect(stage.approval().type()).toBe("success");

    helper.click(helper.byTestId("switch-paddle"));
    expect(stage.approval().type()).toBe("manual");
  });
});

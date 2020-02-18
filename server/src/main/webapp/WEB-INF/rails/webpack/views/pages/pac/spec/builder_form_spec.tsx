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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import Stream from "mithril/stream";
import {ExecTask} from "models/pipeline_configs/task";
import * as events from "simulate-event";
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";
import {TestHelper} from "views/pages/spec/test_helper";
import * as taskStyles from "../../pipelines/task_terminal.scss";
import {BuilderForm} from "../builder_form";

describe("AddPaC: BuilderForm", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof taskStyles>(taskStyles);

  afterEach(() => helper.unmount());

  it("fires content change handler when the form changes", (done) => {
    const vm = new PipelineConfigVM();
    helper.mount(() => <BuilderForm pluginId={Stream()} vm={vm} onContentChange={(changed: boolean) => {
      expect(changed).toBe(true);
      done();
    }}/>);

    helper.onchange(`input[type="text"]`, "foo");
  });

  it("fires content change handler when the form changes", (done) => {
    const vm = new PipelineConfigVM();
    helper.mount(() => <BuilderForm pluginId={Stream()} vm={vm} onContentChange={(changed: boolean) => {
      expect(changed).toBe(true);
      done();
    }}/>);

    helper.onchange(`input[type="text"]`, "foo");
  });

  it("detects changes to task terminal (non-input)", (done) => {
    const vm = new PipelineConfigVM();
    helper.mount(() => <BuilderForm pluginId={Stream()} vm={vm} onContentChange={(changed: boolean) => {
      expect(changed).toBe(true);
      done();
    }}/>);

    hitEnter(editText("id"));
    expect(vm.job.tasks().map((t) => (t as ExecTask).toJSON())).toEqual([{
      type: "exec",
      attributes: {
        command: "id",
        arguments: [],
        run_if: []
      }
    }]);
  });

  it("content change handler receives a flag as to whether the content is different", (done) => {
    const vm = new PipelineConfigVM();
    let times = 0;
    helper.mount(() => <BuilderForm pluginId={Stream()} vm={vm} onContentChange={(changed: boolean) => {
      times++;
      if (times === 1) {
        expect(changed).toBe(true);
      }

      if (times === 2) {
        expect(changed).toBe(false);
        done();
      }

      if (times > 2) {
        done.fail("Should only have been called twice");
      }
    }}/>);

    helper.onchange(`input[type="text"]`, "foo");
    helper.onchange(`input[type="text"]`, "foo");
  });

  function editText(text: string): Element {
    const el = helper.q(sel.currentEditor);
    el.textContent = text;
    return el;
  }

  function hitEnter(input: Element) {
    events.simulate(input, "keydown", {which: 13});
  }
});

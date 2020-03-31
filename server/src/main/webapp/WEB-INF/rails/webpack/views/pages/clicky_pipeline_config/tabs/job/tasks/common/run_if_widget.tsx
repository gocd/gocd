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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {RunIfCondition} from "models/pipeline_configs/task";
import {CheckboxField} from "views/components/forms/input_fields";
import styles from "./run_if.scss";

interface Attrs {
  runIf: Stream<RunIfCondition[]>;
}

interface State {
  passed: Stream<boolean>;
  failed: Stream<boolean>;
  any: Stream<boolean>;

  onchange: () => void;
  clearPassedAndFailed: () => void;
}

export class RunIfConditionWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.runIf().length === 0) {
      vnode.attrs.runIf(["passed"]);
    }

    vnode.state.passed = Stream(vnode.attrs.runIf().indexOf("passed") !== -1);
    vnode.state.failed = Stream(vnode.attrs.runIf().indexOf("failed") !== -1);
    vnode.state.any    = Stream(vnode.attrs.runIf().indexOf("any") !== -1);

    vnode.state.clearPassedAndFailed = () => {
      vnode.state.passed(false);
      vnode.state.failed(false);
    };

    vnode.state.onchange = () => {
      const updatedRunIf: RunIfCondition[] = [];

      if (vnode.state.passed()) {
        updatedRunIf.push("passed");
      }

      if (vnode.state.failed()) {
        updatedRunIf.push("failed");
      }

      if (vnode.state.any()) {
        updatedRunIf.push("any");
      }

      vnode.attrs.runIf(updatedRunIf);
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    return (
      <div data-test-id="run-if-condition">
        <h4>Run If Conditions:</h4>
        <div class={styles.runIfCheckboxWrapper}>
          <CheckboxField label="Passed"
                         readonly={vnode.state.any()}
                         onchange={vnode.state.onchange}
                         property={vnode.state.passed}/>
          <CheckboxField label="Failed"
                         readonly={vnode.state.any()}
                         onchange={vnode.state.onchange}
                         property={vnode.state.failed}/>
          <CheckboxField label="Any"
                         onchange={() => {
                           vnode.state.clearPassedAndFailed();
                           vnode.state.onchange();
                         }}
                         property={vnode.state.any}/>
        </div>
      </div>
    );
  }
}

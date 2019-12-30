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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {PipelineInstance} from "models/compare/pipeline_instance";
import styles from "./index.scss";
import {InstanceSelectionWidget} from "./instance_selection_widget";

interface Attrs {
  pipelineName: string;
  fromInstance: PipelineInstance;
  toInstance: PipelineInstance;

  reloadWithNewCounters: (fromCounter: number, toCounter: number) => void;
}

export class ComparisonSelectionWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const onFromInstanceChange = (counter: number) => vnode.attrs.reloadWithNewCounters(counter, vnode.attrs.toInstance.counter());
    const onToInstanceChange   = (counter: number) => vnode.attrs.reloadWithNewCounters(vnode.attrs.fromInstance.counter(), counter);

    return <div data-test-id="comparison-selection-widget">
      <table>
        <tbody>
        <tr>
          <td class={styles.pipelineInstanceSelection}>
            <InstanceSelectionWidget instance={vnode.attrs.fromInstance} onInstanceChange={onFromInstanceChange}/>
          </td>
          <td class={styles.pipelineComparisonText}>compared to</td>
          <td class={styles.pipelineInstanceSelection}>
            <InstanceSelectionWidget instance={vnode.attrs.toInstance} onInstanceChange={onToInstanceChange}/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>;
  }
}

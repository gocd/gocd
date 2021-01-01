/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";
import {Stage} from "models/pipeline_configs/stage";
import {RadioField} from "views/components/forms/input_fields";
import style from "../index.scss";

interface Attrs {
  stage: Stage;
}

export class StagePermissionWidget extends MithrilViewComponent<Attrs> {
  readonly isInheritingFromPipelineGroup = Stream<"Yes" | "No">();

  oninit(vnode: m.Vnode<Attrs>) {
    this.isInheritingFromPipelineGroup(vnode.attrs.stage.approval().authorization().isEmpty() ? "Yes" : "No");
  }

  view(vnode: m.Vnode<Attrs>) {
    return <div>
      <h3>Permissions</h3>
      <span class={style.help}>All system administrators and pipeline group administrators can operate on this stage (this cannot be overridden).</span>
      <RadioField label={"For this stage:"}
                  property={this.isInheritingFromPipelineGroup}
                  inline={true}
                  possibleValues={[
                    {label: "Inherit from the pipeline group", value: "Yes"},
                    {label: "Specify locally", value: "No"}
                  ]}/>
    </div>;
  }
}

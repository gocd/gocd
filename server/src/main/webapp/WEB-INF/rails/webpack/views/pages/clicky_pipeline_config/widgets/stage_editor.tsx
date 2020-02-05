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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Stage} from "models/pipeline_configs/stage";
import {Form} from "views/components/forms/form";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import {Help} from "views/components/tooltip";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";

interface Attrs {
  stage: Stream<Stage>;
}

export class StageEditor extends MithrilViewComponent<Attrs> {
  private readonly APPROVAL_TYPE_HELP = "If 'on' then stage will automatically schedule once the preceding stage completes successfully. " +
    "Otherwise, user have to manually trigger the stage. For the first stage in a pipeline, setting this " +
    "to 'on' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.";

  private readonly ALLOW_ONLY_ON_SUCCESS_HELP = "Only allow stage to be scheduled if the previous stage run is successful.";

  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stage();
    return <Form compactForm={true}>
      <TextField label={"Stage name"}
                 required={true}
                 dataTestId={"stage-name-input"}
                 errorText={stage.errors().errorsForDisplay("name")}
                 property={stage.name}/>

      <SwitchBtn label={["Trigger completion of previous stage:", <Help content={this.APPROVAL_TYPE_HELP}/>]}
                 field={stage.approval().typeAsStream()}
                 small={true}
                 onclick={StageEditor.approvalChange.bind(this, stage)}/>

      <CheckboxField label={["Allow Only On Success", <Help content={this.ALLOW_ONLY_ON_SUCCESS_HELP}/>]}
                     dataTestId={"allow-only-on-success-checkbox"}
                     property={stage.approval().allowOnlyOnSuccess}/>

      <h3>Initial Job and Task</h3>
      <span>You can add more jobs and tasks to this stage once the stage has been created.</span>

      <TextField label={"Job name"}
                 required={true}
                 dataTestId={"job-name-input"}
                 errorText={stage.firstJob().errors().errorsForDisplay("name")}
                 property={stage.firstJob().name}/>

      <TaskTerminalField label="Type your tasks below at the prompt" property={stage.firstJob().tasks}
                         errorText={stage.firstJob().errors().errorsForDisplay("tasks")} required={true}/>
    </Form>;
  }

  private static approvalChange(stage: Stage, e: MouseEvent) {
    const checkbox = e.currentTarget as HTMLInputElement;
    stage.approval().typeAsStream()(checkbox.checked);
  }
}

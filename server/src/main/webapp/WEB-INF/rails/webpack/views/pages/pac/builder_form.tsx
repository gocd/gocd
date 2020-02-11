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

// utils
import classnames from "classnames";
import {override} from "helpers/css_proxies";
import {sha256} from "helpers/digest";
// components
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
// CSS
import formCss from "views/components/forms/forms.scss";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import * as defaultStyles from "views/pages/pac/styles.scss";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {JobEditor} from "views/pages/pipelines/job_editor";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {PipelineConfigVMAware} from "views/pages/pipelines/pipeline_config_view_model";
import {PipelineInfoEditor} from "views/pages/pipelines/pipeline_info_editor";
import {StageEditor} from "views/pages/pipelines/stage_editor";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";
import {UserInputPane} from "views/pages/pipelines/user_input_pane";
import * as uipStyles from "views/pages/pipelines/user_input_pane.scss";

const uipCss: typeof uipStyles = override(uipStyles, {
  userInput: [uipStyles.userInput, defaultStyles.logicalSection].join(" ")
});

const altFormStyles = override(formCss, {
  formGroup: classnames(formCss.formGroup, defaultStyles.singleFormEl),
  formControl: classnames(formCss.formControl, defaultStyles.choices),
});

interface Attrs extends PipelineConfigVMAware {
  onContentChange?: (changed: boolean) => void;
  pluginId: Stream<string>;
  onMaterialChange?: (e: Event) => void;
}

export class BuilderForm extends MithrilComponent<Attrs> {
  private hash = Stream("");

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    vnode.dom.addEventListener("change", (e) => {
      const vm = vnode.attrs.vm;
      sha256(JSON.stringify(vm.pipeline.toApiPayload())).
        then((hash) => {
          if ("function" === typeof vnode.attrs.onContentChange) {
            vnode.attrs.onContentChange(hash !== this.hash());
            this.hash(hash);
          }
        }).catch((err) => console.warn(err)); // tslint:disable-line no-console
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    const { pipeline, material, isUsingTemplate } = vnode.attrs.vm;

    return <div class={defaultStyles.builderForm}>
      <UserInputPane css={uipCss}>
        <h3 class={defaultStyles.builderSectionHeading}>Select Configuration Language</h3>
        <SelectField property={vnode.attrs.pluginId} css={altFormStyles} onchange={vnode.attrs.onContentChange}>
          <SelectFieldOptions selected={vnode.attrs.pluginId()} items={vnode.attrs.vm.exportPlugins()}/>
        </SelectField>
      </UserInputPane>

      <UserInputPane css={uipCss} onchange={vnode.attrs.onMaterialChange}>
        <h3 class={defaultStyles.builderSectionHeading}>Material</h3>
        <MaterialEditor material={material}/>
      </UserInputPane>

      <UserInputPane css={uipCss}>
        <h3 class={defaultStyles.builderSectionHeading}>Pipeline Name</h3>
        <PipelineInfoEditor pipelineConfig={pipeline} isUsingTemplate={isUsingTemplate}/>
      </UserInputPane>

      <PipelineBodyEditor {...vnode.attrs}/>
    </div>;
  }
}

class PipelineBodyEditor extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const { stage, job } = vnode.attrs.vm;

    return vnode.attrs.vm.whenTemplateAbsent(() => [
      <UserInputPane css={uipCss}>
        <h3 class={defaultStyles.builderSectionHeading}>Stage Details</h3>
        <StageEditor stage={stage} />
      </UserInputPane>,

      <UserInputPane css={uipCss}>
        <h3 class={defaultStyles.builderSectionHeading}>Job and Tasks</h3>
        <JobEditor job={job}/>
        <TaskTerminalField label="Type your tasks below at the prompt" property={job.tasks} errorText={job.errors().errorsForDisplay("tasks")} required={true}/>
        <AdvancedSettings forceOpen={_.some(job.environmentVariables(), (env) => env.errors().hasErrors())}>
          <EnvironmentVariablesWidget environmentVariables={job.environmentVariables()}/>
        </AdvancedSettings>
      </UserInputPane>
    ]);
  }
}

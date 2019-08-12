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
import {override} from "helpers/css_proxies";
import _ from "lodash";
import m from "mithril";
import stream from "mithril/stream";

// components
import {MithrilComponent} from "jsx/mithril-component";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {EnvironmentVariablesEditor} from "views/pages/pipelines/environment_variables_editor";
import {JobEditor} from "views/pages/pipelines/job_editor";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {PipelineConfigVMAware} from "views/pages/pipelines/pipeline_config_view_model";
import {PipelineInfoEditor} from "views/pages/pipelines/pipeline_info_editor";
import {StageEditor} from "views/pages/pipelines/stage_editor";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";
import {UserInputPane} from "views/pages/pipelines/user_input_pane";

// CSS
import * as defaultStyles from "views/pages/pac/styles.scss";
import * as uipStyles from "views/pages/pipelines/user_input_pane.scss";

const uipCss: typeof uipStyles = override(uipStyles, {
  userInput: [uipStyles.userInput, defaultStyles.logicalSection].join(" ")
});

interface Attrs extends PipelineConfigVMAware {
  onContentChange?: (changed: boolean) => void;
}

export class BuilderForm extends MithrilComponent<Attrs> {
  private hash = stream("");

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    vnode.dom.addEventListener("change", (e) => {
      const vm = vnode.attrs.vm;
      sha256(vm.pluginId() + JSON.stringify(vm.pipeline.toApiPayload())).
        then((hash) => {
          if ("function" === typeof vnode.attrs.onContentChange) {
            vnode.attrs.onContentChange(hash !== this.hash());
            this.hash(hash);
          }
        }).catch((err) => console.warn(err)); // tslint:disable-line no-console
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    const { pipeline, material, isUsingTemplate, pluginId } = vnode.attrs.vm;

    return <div class={defaultStyles.builderForm}>
      <UserInputPane css={uipCss} heading="Part 0: Select Config Language">
        <SelectField property={pluginId} label="Choose a Pipelines as Code plugin">
          <SelectFieldOptions selected={pluginId()} items={vnode.attrs.vm.exportPlugins()}/>
        </SelectField>
      </UserInputPane>

      <UserInputPane css={uipCss} heading="Part 1: Material">
        <MaterialEditor material={material}/>
      </UserInputPane>

      <UserInputPane css={uipCss} heading="Part 2: Pipeline Name">
        <PipelineInfoEditor pipelineConfig={pipeline} isUsingTemplate={isUsingTemplate}/>
      </UserInputPane>

      <PipelineStructure {...vnode.attrs}/>
    </div>;
  }
}

class PipelineStructure extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const { stage, job } = vnode.attrs.vm;

    return vnode.attrs.vm.whenTemplateAbsent(() => [
      <UserInputPane css={uipCss} heading="Part 3: Stage Details">
        <StageEditor stage={stage} />
      </UserInputPane>,

      <UserInputPane css={uipCss} heading="Part 4: Job and Tasks">
        <JobEditor job={job}/>
        <TaskTerminalField label="Type your tasks below at the prompt" property={job.tasks} errorText={job.errors().errorsForDisplay("tasks")} required={true}/>
        <AdvancedSettings forceOpen={_.some(job.environmentVariables(), (env) => env.errors().hasErrors())}>
          <EnvironmentVariablesEditor variables={job.environmentVariables}/>
        </AdvancedSettings>
      </UserInputPane>
    ]);
  }
}

/** produces a Promise<string>, resolving a sha-256 hex digest of specified string content */
function sha256(subj: string): Promise<string> {
  return new Promise<string>((res, rej) => {
    crypto.subtle.digest("SHA-256", encode(subj)).then((buf) => {
      const ba = new Uint8Array(buf);
      const hexes = new Array(ba.length);

      for (let i = ba.length - 1; i >= 0; i--) {
        hexes[i] = ba[i].toString(16).padStart(2, "0");
      }

      res(hexes.join(""));
    }, (err) => rej(err));
  });
}

function encode(subj: string): Uint8Array {
  if ("function" === typeof TextEncoder) {
    return new TextEncoder().encode(subj);
  }

  // Pre-Chromium MS Edge browsers do not support TextEncoder
  return Uint8Array.from(subj.split("").map((s) => s.charCodeAt(0)));
}

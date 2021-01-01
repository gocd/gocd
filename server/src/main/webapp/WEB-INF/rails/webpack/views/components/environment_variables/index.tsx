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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
import s from "underscore.string";
import {ButtonIcon, Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {EnvironmentVariableWidget} from "./environment_variable_widget";
import styles from "./index.scss";

export interface GroupedEnvironmentVariablesAttrs {
  title: string;
  environmentVariables: EnvironmentVariables;
  onAdd: () => void;
  onRemove: (environmentVariable: EnvironmentVariable) => void;
}

export class GroupedEnvironmentVariables extends MithrilComponent<GroupedEnvironmentVariablesAttrs> {
  renderEnvironmentVariableWidget(vnode: m.Vnode<GroupedEnvironmentVariablesAttrs>,
                                  environmentVariable: EnvironmentVariable) {
    return <EnvironmentVariableWidget environmentVariable={environmentVariable} onRemove={vnode.attrs.onRemove}/>;
  }

  view(vnode: m.Vnode<GroupedEnvironmentVariablesAttrs>): m.Children {
    let noEnvironmentVariablesConfiguredMessage: m.Children;
    if (vnode.attrs.environmentVariables.length === 0) {
      noEnvironmentVariablesConfiguredMessage = `No ${vnode.attrs.title} are configured.`;
    }

    return <div class={styles.groupContainer}>
      <h4 data-test-id={`${s.slugify(vnode.attrs.title)}-title`}>{vnode.attrs.title}</h4>
      <FlashMessage dataTestId={`${s.slugify(vnode.attrs.title)}-msg`} type={MessageType.info} message={noEnvironmentVariablesConfiguredMessage}/>
      {vnode.attrs.environmentVariables.map((envVar) => this.renderEnvironmentVariableWidget(vnode, envVar))}
      <Secondary small={true} icon={ButtonIcon.ADD} data-test-id={`add-${s.slugify(vnode.attrs.title)}-btn`}
                 onclick={vnode.attrs.onAdd.bind(this)}>
        Add
      </Secondary>
    </div>;
  }
}

interface EnvironmentVariablesWidgetAttrs {
  environmentVariables: EnvironmentVariables;
}

export class EnvironmentVariablesWidget extends MithrilComponent<EnvironmentVariablesWidgetAttrs, {}> {

  static onAdd(isSecure: boolean, vnode: m.Vnode<EnvironmentVariablesWidgetAttrs, {}>) {
    vnode.attrs.environmentVariables.push(new EnvironmentVariable("", undefined, isSecure, ""));
  }

  static onRemove(envVar: EnvironmentVariable, vnode: m.Vnode<EnvironmentVariablesWidgetAttrs, {}>) {
    vnode.attrs.environmentVariables.remove(envVar);
  }

  view(vnode: m.Vnode<EnvironmentVariablesWidgetAttrs, {}>): m.Children {
    return <div>
      <GroupedEnvironmentVariables environmentVariables={vnode.attrs.environmentVariables.plainTextVariables()}
                                   title="Plain Text Variables"
                                   onAdd={EnvironmentVariablesWidget.onAdd.bind(this, false, vnode)}
                                   onRemove={(envVar: EnvironmentVariable) => EnvironmentVariablesWidget.onRemove(envVar,
                                                                                                                  vnode)}/>
      <GroupedEnvironmentVariables environmentVariables={vnode.attrs.environmentVariables.secureVariables()}
                                   title="Secure Variables"
                                   onAdd={EnvironmentVariablesWidget.onAdd.bind(this, true, vnode)}
                                   onRemove={(envVar: EnvironmentVariable) => EnvironmentVariablesWidget.onRemove(envVar,
                                                                                                                  vnode)}/>
    </div>;
  }
}

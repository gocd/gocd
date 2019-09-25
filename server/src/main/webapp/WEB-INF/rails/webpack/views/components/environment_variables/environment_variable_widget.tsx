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
import {EnvironmentVariable} from "models/environment_variables/types";
import {SimplePasswordField, TextField} from "views/components/forms/input_fields";
import {Close} from "views/components/icons";
import styles from "./index.scss";

interface EnvironmentVariableWidgetAttrs {
  environmentVariable: EnvironmentVariable;
  onRemove: (environmentVariable: EnvironmentVariable) => void;
}

export class EnvironmentVariableWidget extends MithrilViewComponent<EnvironmentVariableWidgetAttrs> {
  view(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>): m.Children {
    return <div class={styles.environmentVariableWrapper}>
      <div class={styles.name}>
        <TextField property={vnode.attrs.environmentVariable.name}
                   readonly={!vnode.attrs.environmentVariable.editable()}
                   dataTestId="env-var-name"/>
      </div>
      {this.getValueField(vnode)}
      <div class={styles.actions}>
        <Close title={"remove"} iconOnly={true}
               onclick={vnode.attrs.onRemove.bind(this, vnode.attrs.environmentVariable)}
               data-test-id="remove-env-var-btn"/>
      </div>
    </div>;
  }

  getValueField(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>) {
    if (vnode.attrs.environmentVariable.secure()) {
      return <SimplePasswordField property={vnode.attrs.environmentVariable.encryptedValue}
                                  readonly={!vnode.attrs.environmentVariable.editable()}
                                  dataTestId={"env-var-value"}/>;
    }
    return <TextField property={vnode.attrs.environmentVariable.value}
                      readonly={!vnode.attrs.environmentVariable.editable()}
                      dataTestId={"env-var-value"}/>;
  }
}

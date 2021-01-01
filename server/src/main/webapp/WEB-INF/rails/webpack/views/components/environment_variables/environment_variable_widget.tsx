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
import {EnvironmentVariable} from "models/environment_variables/types";
import {PasswordField, TextField} from "views/components/forms/input_fields";
import {Close} from "views/components/icons";
import styles from "./index.scss";

export interface EnvironmentVariableWidgetAttrs {
  environmentVariable: EnvironmentVariable;
  onRemove: (environmentVariable: EnvironmentVariable) => void;
}

export class EnvironmentVariableWidget extends MithrilViewComponent<EnvironmentVariableWidgetAttrs> {
  view(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>): m.Children {

    let removeHtml;

    if (vnode.attrs.environmentVariable.editable()) {
      removeHtml = <Close title={"remove"} iconOnly={true}
                          onclick={vnode.attrs.onRemove.bind(this, vnode.attrs.environmentVariable)}
                          data-test-id="remove-env-var-btn"/>;
    } else if(vnode.attrs.environmentVariable.reasonForNonEditable()) {
      removeHtml = <div data-test-id="info-tooltip-wrapper" className={styles.infoTooltipWrapper}>
        <i data-test-id={"info-icon"} className={styles.infoIcon}/>
        <div data-test-id="info-tooltip-content" className={styles.infoTooltipContent}>
          {this.reasonForNonEditableEnvVar(vnode)}
        </div>
      </div>;
    }

    const dataTestId = vnode.attrs.environmentVariable.secure() ? 'secure-env-var-name' : 'env-var-name';
    return <div class={styles.environmentVariableWrapper} data-test-id="environment-variable-wrapper">
      <div class={styles.name}>
        <TextField property={vnode.attrs.environmentVariable.name}
                   readonly={!vnode.attrs.environmentVariable.editable()}
                   dataTestId={dataTestId}
                   errorText={vnode.attrs.environmentVariable.errors().errorsForDisplay("name")}/>
      </div>
      {this.getValueField(vnode)}
      <div class={styles.actions}>
        {removeHtml}
      </div>
    </div>;
  }

  getValueField(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>) {
    const environmentVariable = vnode.attrs.environmentVariable;
    if (environmentVariable.secure()) {
      return <PasswordField property={environmentVariable.encryptedValue}
                            readonly={!environmentVariable.editable()}
                            dataTestId={"secure-env-var-value"}
                            errorText={environmentVariable.errors().errorsForDisplay("value")}/>;
    }

    return <TextField property={environmentVariable.value}
                      readonly={!environmentVariable.editable()}
                      dataTestId={"env-var-value"}
                      errorText={environmentVariable.errors().errorsForDisplay("value")}/>;
  }

  reasonForNonEditableEnvVar(vnode: m.Vnode<EnvironmentVariableWidgetAttrs>) {
    return <p>{vnode.attrs.environmentVariable.reasonForNonEditable()}</p>;
  }
}

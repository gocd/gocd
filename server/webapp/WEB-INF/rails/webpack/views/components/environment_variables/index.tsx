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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
import s from "underscore.string";
import {ButtonIcon, Secondary} from "views/components/buttons";
import {EnvironmentVariableWidget} from "views/components/environment_variables/environment_variable_widget";

export enum EnvironmentVariableType {
  Secure    = "Secure",
  PlainText = "Plain Text"
}

interface Callbacks {
  onAdd: (environmentVariable: EnvironmentVariable) => void;
  onRemove: (environmentVariable: EnvironmentVariable) => void;
}

interface Attrs extends Callbacks {
  type: EnvironmentVariableType;
  environmentVariables: EnvironmentVariables;
}

class EnvironmentVariablesGroupedByType extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
    const typeTestId = s.slugify(vnode.attrs.type);
    return <div>
      <h4 data-test-id={`${typeTestId}-env-var-title`}>{vnode.attrs.type} Variables</h4>
      {
        vnode.attrs.environmentVariables.map((envVar) => {
          return <EnvironmentVariableWidget environmentVariable={envVar} onRemove={vnode.attrs.onRemove}
                                            type={vnode.attrs.type}/>;
        })
      }
      <Secondary small={true} icon={ButtonIcon.ADD} data-test-id={`add-${typeTestId}-env-var-btn`}
                 onclick={() => {
                   const secure              = vnode.attrs.type === EnvironmentVariableType.Secure;
                   const environmentVariable = new EnvironmentVariable("", "", secure);

                   vnode.attrs.onAdd(environmentVariable);
                 }}>
        Add
      </Secondary>
    </div>;
  }
}

interface EnvironmentVariablesWidgetAttrs extends Callbacks {
  environmentVariables: EnvironmentVariables;
}

export class EnvironmentVariablesWidget extends MithrilComponent<EnvironmentVariablesWidgetAttrs, {}> {
  view(vnode: m.Vnode<EnvironmentVariablesWidgetAttrs, {}>): m.Children {
    return <div>
      <EnvironmentVariablesGroupedByType environmentVariables={vnode.attrs.environmentVariables.plainTextVariables()}
                                         type={EnvironmentVariableType.PlainText} onRemove={vnode.attrs.onRemove}
                                         onAdd={vnode.attrs.onAdd}/>
      <EnvironmentVariablesGroupedByType environmentVariables={vnode.attrs.environmentVariables.secureVariables()}
                                         type={EnvironmentVariableType.Secure} onRemove={vnode.attrs.onRemove}
                                         onAdd={vnode.attrs.onAdd}/>
    </div>;
  }
}

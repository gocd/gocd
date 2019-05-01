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
import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {EnvironmentVariableConfig} from "models/pipeline_configs/environment_variable_config";
import * as Buttons from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {PasswordField, TextField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import * as css from "./components.scss";

interface Attrs {
  variables: Stream<EnvironmentVariableConfig[]>;
}

export class EnvironmentVariablesEditor extends MithrilViewComponent<Attrs> {
  variableList: Stream<EnvironmentVariableConfig[]> = stream();

  oninit(vnode: m.Vnode<Attrs, this>) {
    this.variableList([
      new EnvironmentVariableConfig(false, "", ""),
      new EnvironmentVariableConfig(true, "", "")
    ]);
  }

  view(vnode: m.Vnode<Attrs, this>) {
    const plainVars = _.filter(this.variableList(), (variable) => {
      return !variable.secure();
    });

    const secureVars = _.filter(this.variableList(), (variable) => {
      return variable.secure();
    });

    return (
      <FormBody>
        <Form last={true} compactForm={true}>
          <div class={css.envVars}>
            <label>
              Environment Variables
              <Buttons.Cancel onclick={this.addVar.bind(this, false)} small={true} icon={Buttons.ButtonIcon.ADD} />
            </label>
            <Table headers={["key", "value", ""]} data={
              _.map(plainVars, (variable) => {
                return [
                    <TextField property={variable.name} errorText={variable.errors().errorsForDisplay("name")} onchange={this.update.bind(this, vnode.attrs.variables, variable)}/>,
                    <TextField property={variable.value} errorText={variable.errors().errorsForDisplay("value")} onchange={this.update.bind(this, vnode.attrs.variables, variable)}/>,
                    <Buttons.Cancel onclick={this.removeVar.bind(this, vnode.attrs.variables, variable)} small={true} icon={Buttons.ButtonIcon.REMOVE} />
                ];
              })
            }/>
          </div>
          <div class={css.envVars}>
            <label>
              Secure Variables
              <Buttons.Cancel onclick={this.addVar.bind(this, true)} small={true} icon={Buttons.ButtonIcon.ADD} />
            </label>
            <Table headers={["key", "value", ""]} data={
              _.map(secureVars, (variable) => {
                return [
                    <TextField property={variable.name} errorText={variable.errors().errorsForDisplay("name")} onchange={this.update.bind(this, vnode.attrs.variables, variable)}/>,
                    <PasswordField property={variable.encryptedValue} errorText={variable.errors().errorsForDisplay("encryptedValue")} onchange={this.update.bind(this, vnode.attrs.variables, variable)}/>,
                    <Buttons.Cancel onclick={this.removeVar.bind(this, vnode.attrs.variables, variable)} small={true} icon={Buttons.ButtonIcon.REMOVE} />
                ];
              })
            }/>
          </div>
        </Form>
      </FormBody>
    );
  }

  update(variables: Stream<EnvironmentVariableConfig[]>, variable: EnvironmentVariableConfig, event: Event) {
    event.stopPropagation();
    if (!_.includes(variables(), variable)) {
      variables().push(variable);
    }
  }

  addVar(secure: boolean, event: Event) {
    event.stopPropagation();
    this.variableList().push(new EnvironmentVariableConfig(secure, "", ""));
  }

  removeVar(variables: Stream<EnvironmentVariableConfig[]>, variable: EnvironmentVariableConfig, event: Event) {
    event.stopPropagation();
    variables(_.reject(variables(), (v) => v === variable));
    this.variableList(_.reject(this.variableList(), (v) => v === variable));
  }
}

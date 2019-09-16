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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Parameter} from "models/new_pipeline_configs/parameter";
import * as Buttons from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {HelpText, TextField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import css from "views/pages/pipeline_configs/environment_variables_editor.scss";

//Note: Using the same CSS from environment_variables_editor
//Feel free to change in the future

interface Attrs {
  parameters: Stream<Parameter[]>;
}

export class ParametersEditor extends MithrilViewComponent<Attrs> {
  parameterList: Stream<Parameter[]> = Stream();

  oninit(vnode: m.Vnode<Attrs, this>) {
    this.parameterList([new Parameter("", "")]);
  }

  view(vnode: m.Vnode<Attrs, this>) {
    return (
      <FormBody>
        <Form last={true} compactForm={true}>
          <div class={css.envVars}>
            <label>
              Parameters
              <Buttons.Cancel onclick={this.addVar.bind(this)} small={true} icon={Buttons.ButtonIcon.ADD}/>
            </label>
            <HelpText
              helpText="Parameters help reduce repetition within your configurations and combined with templates allow you to setup complex configurations."
              docLink="configuration/admin_use_parameters_in_configuration.html"
              helpTextId="help-params"/>
            <Table headers={["name", "value", ""]} data={
              _.map(this.parameterList(), (parameter) => {
                return [
                  <TextField property={parameter.name} errorText={parameter.errors().errorsForDisplay("name")}
                             onchange={this.update.bind(this, vnode.attrs.parameters, parameter)}/>,
                  <TextField property={parameter.value} errorText={parameter.errors().errorsForDisplay("value")}
                             onchange={this.update.bind(this, vnode.attrs.parameters, parameter)}/>,
                  <Buttons.Cancel onclick={this.removeVar.bind(this, vnode.attrs.parameters, parameter)} small={true}
                                  icon={Buttons.ButtonIcon.REMOVE}/>
                ];
              })
            }/>
          </div>
        </Form>
      </FormBody>
    );
  }

  update(variables: Stream<Parameter[]>, variable: Parameter, event: Event) {
    event.stopPropagation();
    if (!_.includes(variables(), variable)) {
      variables().push(variable);
    }
  }

  addVar(event: Event) {
    event.stopPropagation();
    this.parameterList().push(new Parameter("", ""));
  }

  removeVar(variables: Stream<Parameter[]>, variable: Parameter, event: Event) {
    event.stopPropagation();
    variables(_.reject(variables(), (v) => v === variable));
    this.parameterList(_.reject(this.parameterList(), (v) => v === variable));
  }
}

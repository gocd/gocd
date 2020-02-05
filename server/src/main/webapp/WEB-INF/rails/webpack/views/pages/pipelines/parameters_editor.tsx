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

import {makeEvent} from "helpers/compat";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import * as Buttons from "views/components/buttons";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {Form, FormBody} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import css from "./parameters_editor.scss";

interface Attrs {
  parameters: Stream<PipelineParameter[]>;
  paramList: Stream<PipelineParameter[]>;
}

export class PipelineParametersEditor extends MithrilViewComponent<Attrs> {
  notifyChange: () => void = _.noop;

  oninit(vnode: m.Vnode<Attrs>) {
    vnode.attrs.paramList(vnode.attrs.parameters().filter((p) => !p.isEmpty()).concat(new PipelineParameter("", "")));
  }

  oncreate(vnode: m.VnodeDOM<Attrs>) {
    this.notifyChange = () => vnode.dom.dispatchEvent(makeEvent("change"));
  }

  view(vnode: m.Vnode<Attrs>) {
    return (
      <FormBody>
        <Form last={true} compactForm={true}>
          <div class={css.envVars}>
            <label>
              Parameters
              <Tooltip.Help size={TooltipSize.medium}
                            content={<span>Parameters help reduce repetition within your configurations and combined with templates allow you to setup complex configurations. <a
                              href="https://docs.gocd.org/current/configuration/admin_use_parameters_in_configuration.html">Read more</a></span>}/>
            </label>
            <Buttons.Cancel onclick={this.add.bind(this, vnode.attrs.paramList)} small={true}
                            icon={Buttons.ButtonIcon.ADD}/>
            <Table headers={["name", "value", ""]} data={
              _.map(vnode.attrs.paramList(), (param, i) => {
                return [
                  <IdentifierInputField dataTestId={`form-field-input-param-name-${i}`} property={param.name}
                                        errorText={param.errors().errorsForDisplay("name")}
                                        onchange={this.update.bind(this, vnode.attrs.parameters, vnode.attrs.paramList)}/>,
                  <TextField dataTestId={`form-field-input-param-value-${i}`} property={param.value}
                             onchange={this.update.bind(this, vnode.attrs.parameters, vnode.attrs.paramList)}/>,
                  <Buttons.Cancel onclick={this.remove.bind(this, vnode.attrs.parameters, vnode.attrs.paramList, param)}
                                  small={true} icon={Buttons.ButtonIcon.REMOVE}/>
                ];
              })
            }/>
          </div>
        </Form>
      </FormBody>
    );
  }

  update(params: Stream<PipelineParameter[]>, paramsList: Stream<PipelineParameter[]>, event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    params(paramsList().filter((p) => !p.isEmpty()));
    this.notifyChange();
  }

  add(paramsList: Stream<PipelineParameter[]>, event: Event) {
    event.stopPropagation();
    paramsList().push(new PipelineParameter("", ""));
  }

  remove(params: Stream<PipelineParameter[]>, paramsList: Stream<PipelineParameter[]>, paramToDelete: PipelineParameter, event?: Event) {
    if (event) {
      event.stopPropagation();
    }

    paramsList(paramsList().filter((p) => p !== paramToDelete));

    if (!paramsList().length) {
      paramsList([new PipelineParameter("", "")]);
    }

    this.update(params, paramsList);
  }
}

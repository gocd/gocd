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
import {PipelineParameters} from "models/pipeline_configs/parameters";
import * as Buttons from "views/components/buttons";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {Form, FormBody} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import css from "./environment_variables_editor.scss";

interface Attrs {
  parameters: Stream<PipelineParameters[]>;
}

export class PipelineParametersEditor extends MithrilViewComponent<Attrs> {
  paramList = Stream([] as PipelineParameters[]);

  oninit(vnode: m.Vnode<Attrs>) {
    this.paramList(vnode.attrs.parameters().filter((p) => !p.isEmpty()).concat(new PipelineParameters("", "")));
  }

  view(vnode: m.Vnode<Attrs>) {
    return (
      <FormBody>
        <Form last={true} compactForm={true}>
          <div class={css.envVars}>
            <label>
              Parameters
              <Tooltip.Help size={TooltipSize.medium} content={`Parameters help reduce repetition within your configurations and combined with templates allow you to setup complex configurations.`} />
            </label>
            <Buttons.Cancel onclick={this.add.bind(this)} small={true} icon={Buttons.ButtonIcon.ADD} />
            <Table headers={["name", "optional default value", ""]} data={
              _.map(this.paramList(), (param, i) => {
                return [
                  <IdentifierInputField dataTestId={`form-field-input-param-name-${i}`} property={param.name} errorText={param.errors().errorsForDisplay("name")} onchange={this.update.bind(this, vnode.attrs.parameters)} />,
                  <TextField dataTestId={`form-field-input-param-value-${i}`} property={param.value} onchange={this.update.bind(this, vnode.attrs.parameters)} />,
                  <Buttons.Cancel onclick={this.remove.bind(this, vnode.attrs.parameters, param)} small={true} icon={Buttons.ButtonIcon.REMOVE} />
                ];
              })
            } />
          </div>
        </Form>
      </FormBody>
    );
  }

  update(params: Stream<PipelineParameters[]>, event?: Event) {
    if (event) {event.stopPropagation(); }
    params(this.paramList().filter((p) => !p.isEmpty()));
  }

  add(event: Event) {
    event.stopPropagation();
    this.paramList().push(new PipelineParameters("", ""));
  }

  remove(params: Stream<PipelineParameters[]>, paramToDelete: PipelineParameters, event?: Event) {
    if (event) {event.stopPropagation(); }

    this.paramList(this.paramList().filter((p) => p !== paramToDelete));

    if (!this.paramList().length) {
      this.paramList([new PipelineParameters("", "")]);
    }

    this.update(params);
  }
}

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

import * as Routes from "gen/ts-routes";
import {ErrorResponse} from "helpers/api_request_builder";
import asSelector from "helpers/selector_proxy";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Material} from "models/materials/types";
import {Validatable} from "models/mixins/new_validatable_mixin";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import * as Buttons from "views/components/buttons";
import * as css from "./components.scss";

const sel = asSelector<typeof css>(css);

interface Attrs {
  pipelineConfig: PipelineConfig;
  loc?: LocationHandler;
}

export interface LocationHandler {
  go: (url: string) => void;
}

class WindowLocation implements LocationHandler {
  go(url: string) {
    window.location.href = url;
  }
}

export class PipelineActions extends MithrilViewComponent<Attrs> {
  private location: LocationHandler = new WindowLocation();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.loc) {
      this.location = vnode.attrs.loc;
    }
  }

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return (
      <footer class={css.actions}>
        <Buttons.Cancel css={css} onclick={this.onCancel.bind(this)} small={false}>Cancel</Buttons.Cancel>
        <div class={css.saveBtns}>
          <span class={css.errorResponse}></span>
          <Buttons.Secondary css={css} onclick={this.onSave.bind(this, true, vnode.attrs.pipelineConfig)} small={false}>Save + Edit Full Config</Buttons.Secondary>
          <Buttons.Primary css={css} onclick={this.onSave.bind(this, false, vnode.attrs.pipelineConfig)} small={false}>Save + Run This Pipeline</Buttons.Primary>
        </div>
      </footer>
    );
  }

  onCancel(event: Event): void {
    event.stopPropagation();
    this.location.go("/go/pipelines");
  }

  onSave(shouldPause: boolean, pipelineConfig: PipelineConfig, event: Event): void {
    event.stopPropagation();
    this.clearError();

    if (pipelineConfig.isValid()) {
      pipelineConfig.create().then((response) => {
        response.do(() => {
          if (shouldPause) {
            pipelineConfig.pause().then(() => {
              this.location.go(Routes.pipelineEditPath("pipelines", pipelineConfig.name(), "general"));
            });
          } else  {
            pipelineConfig.run().then(() => {
              this.location.go(`/go/pipelines?new_pipeline_name=${pipelineConfig.name()}`);
            });
          }
        }, (error: ErrorResponse) => {
          if (error.body) {
            this.markFieldsWithErrors(JSON.parse(error.body).data, pipelineConfig);
          }
          this.setError(error.message);
        });
      }).catch((reason) => {
        this.setError(reason);
      });
    } else {
      this.setError("Please fix the validation errors above before proceeding.");
    }
  }

  private markFieldsWithErrors(data: any, config: PipelineConfig) {
    if (!data) { return; }

    addErrorsToModel(config, data);

    // materials
    addErrorsToAssociations(Array.from(config.materials()), data.materials as Result[]);

    // stages
    addErrorsToAssociations(Array.from(config.stages()), data.stages as Result[]);

    // TODO: add the rest of the deeply nested hierarchy.
    m.redraw();
  }

  private clearError(): Node {
    return empty(document.querySelector(sel.errorResponse)!);
  }

  private setError(text: string) {
    this.clearError().textContent = text;
  }
}

interface ErrorMap {
  [key: string]: string[];
}

interface Result { errors?: ErrorMap; }

function addErrorsToAssociations(assoc: Validatable[], results: Result[]) {
  for (let i = results.length - 1; i >= 0; i--) {
    addErrorsToModel(assoc[i], results[i]);
  }
}

function addErrorsToModel(model: Validatable, result: Result) {
  if (result.errors) {
    for (const key of Object.keys(result.errors)) {
      ((model instanceof Material) ? model.attributes() : model).errors().add(key, result.errors[key][0]);
    }
  }
}

function empty(el: Node) {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}

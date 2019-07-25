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
import {asSelector} from "helpers/css_proxies";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import * as Buttons from "views/components/buttons";
import * as css from "./actions.scss";

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
      pipelineConfig.create(shouldPause).then((response) => {
        response.do(() => {
          if (shouldPause) {
            this.location.go(Routes.pipelineEditPath("pipelines", pipelineConfig.name(), "general"));
          } else  {
            pipelineConfig.run().then(() => {
              this.location.go(`/go/pipelines?new_pipeline_name=${pipelineConfig.name()}`);
            });
          }
        }, (error: ErrorResponse) => {
          const errFrag = document.createDocumentFragment();
          errFrag.appendChild(document.createTextNode(error.message));
          let unmatched;

          if (error.body) {
            unmatched = pipelineConfig.consumeErrorsResponse(JSON.parse(error.body).data);
            m.redraw();
          }

          if (unmatched && unmatched.keys().length) {
            const list = document.createElement("ol");

            for (const key of unmatched.keys()) {
              const item = document.createElement("li");
              item.appendChild(document.createTextNode(`${key}: ${unmatched.errorsForDisplay(key)}`));
              list.appendChild(item);
            }

            errFrag.appendChild(document.createTextNode(": "));
            errFrag.appendChild(list);
          }

          this.setError(errFrag);
        });
      }).catch((reason) => {
        this.setError(reason);
      });
    } else {
      this.setError(document.createTextNode("Please fix the validation errors above before proceeding."));
    }
  }

  private clearError(): Node {
    return empty(document.querySelector(sel.errorResponse)!);
  }

  private setError(contents: Node) {
    this.clearError().appendChild(contents);
  }
}

function empty(el: Node) {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}

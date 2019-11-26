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
import {LocationHandler, WindowLocation} from "helpers/location_handler";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import * as Buttons from "views/components/buttons";
import {OperationState} from "views/pages/page_operations";
import {ServerErrors} from "views/pages/pipelines/server_errors";
import css from "./actions.scss";

interface Attrs {
  pipelineConfig: PipelineConfig;
  loc?: LocationHandler;
}

export class PipelineActions extends MithrilViewComponent<Attrs> {
  private location: LocationHandler                    = new WindowLocation();
  private globalError: Stream<string>                  = Stream();
  private globalErrorDetail: Stream<Errors>            = Stream();
  private ajaxOperationMonitor: Stream<OperationState> = Stream<OperationState>(OperationState.UNKNOWN);

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.loc) {
      this.location = vnode.attrs.loc;
    }
  }

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return (
      <footer class={css.actions}>
        <Buttons.Cancel css={css} onclick={this.onCancel.bind(this)} small={false}
                        ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Buttons.Cancel>
        <div class={css.saveBtns}>
          <ServerErrors message={this.globalError} details={this.globalErrorDetail}/>

          <Buttons.Secondary css={css} ajaxOperation={this.onSave.bind(this, true, vnode.attrs.pipelineConfig)}
                             ajaxOperationMonitor={this.ajaxOperationMonitor} small={false}>Save + Edit Full
            Config</Buttons.Secondary>
          <Buttons.Primary css={css} ajaxOperation={this.onSave.bind(this, false, vnode.attrs.pipelineConfig)}
                           ajaxOperationMonitor={this.ajaxOperationMonitor} small={false}>Save + Run This
            Pipeline</Buttons.Primary>
        </div>
      </footer>
    );
  }

  onCancel(event: Event): void {
    event.stopPropagation();
    this.location.go("/go/pipelines");
  }

  onSave(shouldPause: boolean, pipelineConfig: PipelineConfig, event: Event): Promise<any> {
    event.stopPropagation();
    event.preventDefault();

    this.clearErrors();

    if (pipelineConfig.isValid()) {
      return pipelineConfig.create(shouldPause).then((response) => {
        response.do(() => {
          if (shouldPause) {
            this.location.go(Routes.pipelineEditPath("pipelines", pipelineConfig.name(), "general"));
          } else {
            pipelineConfig.run().then(() => {
              this.location.go(`/go/pipelines?new_pipeline_name=${pipelineConfig.name()}`);
            });
          }
        }, (err: ErrorResponse) => {
          this.globalError(JSON.parse(err.body!).message);
          if (err.body) {
            this.globalErrorDetail(pipelineConfig.consumeErrorsResponse(JSON.parse(err.body).data));
          }
        });
      }).catch((reason) => {
        this.globalError(reason);
      }).finally(m.redraw);
    } else {
      this.globalError("Please fix the validation errors above before proceeding.");
      return Promise.resolve();
    }
  }

  clearErrors() {
    this.globalError       = Stream();
    this.globalErrorDetail = Stream();
  }
}

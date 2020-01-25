/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import _ from 'lodash';
import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {NavigationWidget} from "views/pages/clicky_pipeline_config/navigation_widget";
import {PipelineConfigWidget} from "views/pages/clicky_pipeline_config/pipeline_config_widget";
import {Page, PageState} from "views/pages/page";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";
import styles from "./index.scss";

interface PageMeta {
  pipelineName: string;
}

export interface ChangeRouteEvent {
  newRoute: string;
}

export class PipelineConfigPage<T> extends Page<null, T> {
  private pipelineConfig?: PipelineConfig;
  private originalJSON: any;

  oninit(vnode: m.Vnode<null, T>) {
    super.oninit(vnode);
    window.addEventListener("beforeunload", (e) => {
      if (this.isPipelineConfigChanged()) {
        e.returnValue = "";
        e.preventDefault();
        return false;
      }
      return true;
    });
  }

  changeRoute(event: ChangeRouteEvent, success: () => void): void {
    if (this.isPipelineConfigChanged()) {
      new ConfirmationDialog("Unsaved changes",
        "There are unsaved changes on your form. 'Proceed' will discard these changes",
        () => {
          this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);
          return Promise.resolve(success());
        }
      ).render();
    } else {
      success();
    }
  }

  isPipelineConfigChanged() {
    const newPayload = this.pipelineConfig?.toApiPayload();
    return !_.isEqual(newPayload, PipelineConfig.fromJSON(this.originalJSON).toApiPayload());
  }

  componentToDisplay(vnode: m.Vnode<null, T>): m.Children {
    return (
      <div class={styles.mainContainer}>
        <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/>
        <div class={styles.navigation}>
          <NavigationWidget pipelineConfig={this.pipelineConfig!}
                            changeRoute={this.changeRoute.bind(this)}/>
        </div>

        <div class={styles.entityConfigContainer}>
          <PipelineConfigWidget pipelineConfig={this.pipelineConfig!}
                                changeRoute={this.changeRoute.bind(this)}/>
        </div>
      </div>
    );
  }

  pageName(): string {
    return "Pipelines";
  }

  fetchData(vnode: m.Vnode<null, T>): Promise<any> {
    return PipelineConfig.get(this.getMeta().pipelineName).then((result) => result.do(this.onSuccess.bind(this), this.onFailure.bind(this)));
  }

  protected getMeta(): PageMeta {
    return super.getMeta() as PageMeta;
  }

  private onSuccess(successResponse: SuccessResponse<string>) {
    this.originalJSON   = JSON.parse(successResponse.body);
    this.pipelineConfig = PipelineConfig.fromJSON(this.originalJSON);
    this.pageState      = PageState.OK;
  }

  private onFailure(errorResponse: ErrorResponse) {
    this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
    this.pageState = PageState.FAILED;
  }
}

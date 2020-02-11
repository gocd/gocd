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
import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {MessageType} from "views/components/flash_message";
import {Page, PageState} from "views/pages/page";

interface PageMeta {
  pipelineName: string;
}

export interface RouteInfo<T> {
  route: string;
  params: T;
}

export interface PipelineConfigRouteParams {
  pipeline_name: string;
  job_name?: string;
  stage_name?: string;
  tab_name: string;
}

export class PipelineConfigPage<T> extends Page<null, T> {
  private pipelineConfig?: PipelineConfig;

  componentToDisplay(vnode: m.Vnode<null, T>): m.Children {
    return <div>This is pipeline config: {this.pipelineConfig?.name()}</div>;
  }

  pageName(): string {
    return "Pipelines";
  }

  fetchData(vnode: m.Vnode<null, T>): Promise<any> {
    return PipelineConfig.get(this.getMeta().pipelineName)
                         .then((result) => result.do(this.onSuccess.bind(this), this.onFailure.bind(this)));
  }

  protected getMeta(): PageMeta {
    return super.getMeta() as PageMeta;
  }

  private onSuccess(successResponse: SuccessResponse<string>) {
    const json          = JSON.parse(successResponse.body);
    this.pipelineConfig = PipelineConfig.fromJSON(json);
    this.pageState      = PageState.OK;
  }

  private onFailure(errorResponse: ErrorResponse) {
    this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
    this.pageState = PageState.FAILED;
  }
}

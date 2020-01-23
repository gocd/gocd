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

import m from "mithril";
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import {Page, PageState} from "views/pages/page";
import {PipelineConfigWidget} from "views/pages/clicky_pipeline_config/pipeline_config_widget";
import {ApiResult, ErrorResponse, SuccessResponse} from "../../helpers/api_request_builder";
import {MessageType} from "../components/flash_message";

interface State {
  pipelineConfig: PipelineConfig;
  meta: PageMeta;
}

interface PageMeta {
  pipelineName: string;
}

export class PipelineConfigPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.meta = this.getMeta() as PageMeta;
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PipelineConfigWidget/>;
  }

  pageName(): string {
    return "Pipelines";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return PipelineConfig.get(vnode.state.meta.pipelineName).then(this.handleActionApiResponse.bind(this));
  }

  private handleActionApiResponse(result: ApiResult<string>) {
    result.do(this.onSuccess.bind(this), this.onFailure.bind(this));
  }

  private onFailure(errorResponse: ErrorResponse) {
    this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
    this.pageState = PageState.OK;
  }

  private onSuccess(successResponse: SuccessResponse<string>) {
    console.log(successResponse);
    this.pageState = PageState.OK;
  }
}

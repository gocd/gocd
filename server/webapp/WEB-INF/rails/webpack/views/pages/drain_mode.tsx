/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiRequestBuilder, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import * as m from "mithril";
import {DrainModeAPIs} from "models/drain_mode/drain_mode_apis";
import {DrainModeInfo, StageLocator} from "models/drain_mode/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {Page} from "views/pages/page";

interface SaveOperation<T> {
  onSave: (obj: T, e: Event) => void;
  onSuccessfulSave: (successResponse: SuccessResponse<T>) => void;
  onError: (errorResponse: ErrorResponse) => void;
}

interface State extends SaveOperation<DrainModeInfo> {
  drainModeInfo: DrainModeInfo;
  message: Message;
  toggleDrainMode: (e: Event) => void;
  onCancelStage: (stageLocator: StageLocator, e: Event) => void;
}

export class Message {
  type: MessageType;
  message: string;

  constructor(type: MessageType, message: string) {
    this.type    = type;
    this.message = message;
  }
}

export class DrainModePage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.toggleDrainMode = (e: Event) => {
      e.stopPropagation();
      const updateOperation = vnode.state.drainModeInfo.drainModeState() ? DrainModeAPIs.disable : DrainModeAPIs.enable;
      updateOperation().then(() => this.fetchData(vnode)).finally(m.redraw);
    };

    vnode.state.onCancelStage = (stageLocator: StageLocator, e: Event) => {
      e.stopPropagation();
      ApiRequestBuilder.POST(SparkRoutes.cancelStage(stageLocator.pipelineName, stageLocator.stageName),
                             undefined,
                             {headers: {Confirm: "true"}})
                       .then(() => {
                         vnode.state.message = new Message(MessageType.success, `Stage ${stageLocator.stageName} successfully cancelled.`);
                         this.fetchData(vnode);
                       }, vnode.state.onError);
    };

    vnode.state.onError = (errorResponse: ErrorResponse) => {
      vnode.state.message = new Message(MessageType.alert, errorResponse.message);
    };

    new AjaxPoller(() => this.fetchData(vnode)).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    const mayBeMessage = vnode.state.message ?
      <FlashMessage type={vnode.state.message.type} message={vnode.state.message.message}/> : null;
    return (
      <div>
        {mayBeMessage}
        <DrainModeWidget drainModeInfo={vnode.state.drainModeInfo}
                         toggleDrainMode={vnode.state.toggleDrainMode}
                         onCancelStage={vnode.state.onCancelStage}/>
      </div>
    );
  }

  headerPanel() {
    return <HeaderPanel title="Server Drain Mode"/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return DrainModeAPIs.info().then((info) => {
      info.do((successResponse) => {
        vnode.state.drainModeInfo = successResponse.body;
        m.redraw();
      }, vnode.state.onError);
    });
  }

  pageName(): string {
    return "Server Drain Mode";
  }
}

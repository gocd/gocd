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
import {AjaxPoller} from "helpers/ajax_poller";
import {ApiRequestBuilder, ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {MaintenanceModeAPIs} from "models/maintenance_mode/maintenance_mode_apis";
import {MaintenanceModeInfo, StageLocator} from "models/maintenance_mode/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {ToggleConfirmModal} from "views/pages/maintenance_mode/confirm_modal";
import {MaintenanceModeWidget} from "views/pages/maintenance_mode/maintenance_mode_widget";
import {Page} from "views/pages/page";

const CLEAR_MESSAGE_AFTER_INTERVAL_IN_SECONDS = 10;

interface SaveOperation<T> {
  onSave: (obj: T, e: Event) => void;
  onSuccessfulSave: (successResponse: SuccessResponse<T>) => void;
  onError: (errorResponse: ErrorResponse) => void;
}

interface State extends SaveOperation<MaintenanceModeInfo> {
  maintenanceModeInfo: MaintenanceModeInfo;
  message: Message;
  toggleMaintenanceMode: (e: Event) => void;
  onCancelStage: (stageLocator: StageLocator) => void;
}

export class Message {
  type: MessageType;
  message: string | null;

  constructor(type: MessageType, message: string) {
    this.type    = type;
    this.message = message;
    setTimeout(() => {
      this.message = null;
      m.redraw();
    }, CLEAR_MESSAGE_AFTER_INTERVAL_IN_SECONDS * 1000);
  }
}

export class MaintenanceModePage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.toggleMaintenanceMode = (e: Event) => {
      e.stopPropagation();

      const enableOrDisableText = vnode.state.maintenanceModeInfo.maintenanceModeState() ? "Disable" : "Enable";
      const message             = <span>Are you sure you want to <strong>{enableOrDisableText}</strong> GoCD Server maintenance mode?</span>;

      const modal: ToggleConfirmModal = new ToggleConfirmModal(message, () => {
        const updateOperation = vnode.state.maintenanceModeInfo.maintenanceModeState() ? MaintenanceModeAPIs.disable : MaintenanceModeAPIs.enable;
        return updateOperation().then(() => this.fetchData(vnode)).then(modal.close.bind(modal)).finally(m.redraw);
      });

      modal.render();
    };

    vnode.state.onCancelStage = (stageLocator: StageLocator) => {
      return ApiRequestBuilder.POST(SparkRoutes.cancelStage(stageLocator.pipelineName, stageLocator.stageName),
                                    undefined,
                                    {headers: {Confirm: "true"}})
                              .then(() => {
                                vnode.state.message = new Message(MessageType.success,
                                                                  `Stage ${stageLocator.stageName} successfully cancelled.`);
                                this.fetchData(vnode);
                              }, vnode.state.onError);
    };

    vnode.state.onError = (errorResponse: ErrorResponse) => {
      vnode.state.message = new Message(MessageType.alert, JSON.parse(errorResponse.body!).message);
    };

    const options = {
      repeaterFn: () => this.fetchData(vnode),
      intervalSeconds: 30,
      initialIntervalSeconds: 30
    };

    new AjaxPoller(options).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const mayBeMessage = (vnode.state.message && vnode.state.message.message !== null) ?
      <FlashMessage type={vnode.state.message.type} message={vnode.state.message.message}/> : null;
    return (
      <div>
        {mayBeMessage}
        <MaintenanceModeWidget maintenanceModeInfo={vnode.state.maintenanceModeInfo}
                               toggleMaintenanceMode={vnode.state.toggleMaintenanceMode}
                               onCancelStage={vnode.state.onCancelStage}/>
      </div>
    );
  }

  headerPanel() {
    return <HeaderPanel title="Server Maintenance Mode"/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return MaintenanceModeAPIs.info().then((info) => {
      info.do((successResponse) => {
        vnode.state.maintenanceModeInfo = successResponse.body;
        m.redraw();
      }, vnode.state.onError);
    });
  }

  pageName(): string {
    return "Server Maintenance Mode";
  }
}

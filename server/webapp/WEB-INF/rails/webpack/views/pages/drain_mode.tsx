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

import {ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import {MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {Page} from "views/pages/page";

interface SaveOperation<T> {
  onSave: (obj: T) => void;
  onSuccessfulSave: (successResponse: SuccessResponse<T>) => void;
  onError: (errorResponse: ErrorResponse) => void;
}

interface State extends SaveOperation<DrainModeSettings> {
  drainModeSettings: DrainModeSettings;
  message: Message;
  onReset: (drainModeSettings: DrainModeSettings) => void;
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
    vnode.state.onSave = (drainModeSettings: DrainModeSettings) => {
      DrainModeSettings.update(drainModeSettings)
                       .then((result) => result.do(vnode.state.onSuccessfulSave, vnode.state.onError))
                       .finally(m.redraw);
    };

    vnode.state.onReset = (drainModeSettings: DrainModeSettings) => {
      drainModeSettings.reset();
    };

    vnode.state.onSuccessfulSave = (successResponse: SuccessResponse<DrainModeSettings>) => {
      vnode.state.drainModeSettings = successResponse.body;
      const state                   = vnode.state.drainModeSettings.drain() ? "on" : "off";
      vnode.state.message           = new Message(MessageType.success, `Drain mode turned ${state}.`);
    };

    vnode.state.onError = (errorResponse: ErrorResponse) => {
      vnode.state.message = new Message(MessageType.alert, errorResponse.message);
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <DrainModeWidget settings={vnode.state.drainModeSettings}
                            onSave={vnode.state.onSave.bind(this, vnode.state.drainModeSettings)}
                            onReset={vnode.state.onReset.bind(this, vnode.state.drainModeSettings)}
                            message={vnode.state.message}/>;
  }

  headerPanel() {
    return <HeaderPanel title="Server Drain Mode"/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return DrainModeSettings.get().then((settings) => {
      settings.do((successResponse) => vnode.state.drainModeSettings = successResponse.body,
                  () => this.setErrorState());
    });
  }

  pageName(): string {
    return "Server Drain Mode";
  }
}

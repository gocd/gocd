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
import {MithrilComponent} from "jsx/mithril-component";
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Switch} from "views/components/forms/input_fields";

import * as m from "mithril";
import * as Buttons from "views/components/buttons";
import * as styles from "./index.scss";

interface Attrs {
  settings: DrainModeSettings;
}

class Message {
  type: MessageType;
  message: string;

  constructor(type: MessageType, message: string) {
    this.type    = type;
    this.message = message;
  }
}

interface State {
  settings: DrainModeSettings;
  message: Message;

  onSuccess: (successResponse: SuccessResponse<DrainModeSettings>) => void;
  onError: (errorResponse: ErrorResponse) => void;
  save: (drainModeSettings: DrainModeSettings) => void;
  reset: (drainModeSettings: DrainModeSettings) => void;
}

export class DrainModeWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.save = (drainModeSettings: DrainModeSettings) => {
      DrainModeSettings.update(drainModeSettings)
                       .then((result) => result.do(vnode.state.onSuccess, vnode.state.onError))
                       .finally(m.redraw);
    };

    vnode.state.reset = (drainModeSettings: DrainModeSettings) => {
      drainModeSettings.reset();
    };

    vnode.state.onSuccess = (successResponse: SuccessResponse<DrainModeSettings>) => {
      vnode.state.settings = successResponse.body;
      const state            = vnode.state.settings.isDrainMode() ? "on" : "off";
      vnode.state.message  = new Message(MessageType.success, `Drain mode turned ${state}.`);
    };

    vnode.state.onError = (errorResponse: ErrorResponse) => {
      vnode.state.message = new Message(MessageType.alert, errorResponse.message);
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const settings = vnode.attrs.settings;
    let maybeMessage;

    if (vnode.state.message) {
      maybeMessage = <FlashMessage type={vnode.state.message.type}>{vnode.state.message.message}</FlashMessage>;
    }

    return [
      maybeMessage,
      <div className={styles.drainModeWidget}>
        <div className={styles.drainModeDescription}>
          <p>
            Some description about what is drain mode.
          </p>
        </div>

        <div className={styles.drainModeInfo}>
          <p>Is server in drain mode: {`${settings.isDrainMode()}`}</p>
          <p>Drain mode updated by: {settings.updatedBy()}</p>
          <p>Drain mode updated on: {settings.updatedOn()}</p>

          <div>
            <Switch label={"Toggle Drain Mode"} property={settings.isDrainMode}/>
          </div>

          <div className="button-wrapper">
            <Buttons.Primary onclick={vnode.state.save.bind(this, settings)}>Save</Buttons.Primary>
            <Buttons.Reset onclick={vnode.state.reset.bind(this, settings)}>Reset</Buttons.Reset>
          </div>
        </div>
      </div>
    ];
  }
}

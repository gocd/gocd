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

import {MithrilViewComponent} from "jsx/mithril-component";
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import {FlashMessage} from "views/components/flash_message";
import {Switch} from "views/components/forms/input_fields";
import {Message} from "views/pages/drain_mode";

import * as m from "mithril";
import * as Buttons from "views/components/buttons";
import * as styles from "./index.scss";

interface Attrs {
  settings: DrainModeSettings;
  message: Message;
  onSave: (drainModeSettings: DrainModeSettings) => void;
  onReset: (drainModeSettings: DrainModeSettings) => void;
}

export class DrainModeWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const settings = vnode.attrs.settings;
    let maybeMessage;

    if (vnode.attrs.message) {
      maybeMessage = <FlashMessage type={vnode.attrs.message.type}>{vnode.attrs.message.message}</FlashMessage>;
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
          <p>Is server in drain mode: {`${settings.drain()}`}</p>
          <p>Drain mode updated by: {settings.updatedBy}</p>
          <p>Drain mode updated on: {settings.updatedOn}</p>

          <div>
            <Switch label={"Toggle Drain Mode"} property={settings.drain}/>
          </div>

          <div className="button-wrapper">
            <Buttons.Primary onclick={vnode.attrs.onSave.bind(this, settings)}>Save</Buttons.Primary>
            <Buttons.Reset onclick={vnode.attrs.onReset.bind(this, settings)}>Reset</Buttons.Reset>
          </div>
        </div>
      </div>
    ];
  }
}

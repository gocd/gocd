/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import _ from "lodash";
import m from "mithril";

import {MithrilComponent} from "jsx/mithril-component";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Spinner} from "views/components/spinner";
import {PluginSettingsModal} from "./plugin_settings_modal";
import {PluginWidget} from "./plugin_widget";

export interface Attrs {
  isUserAnAdmin: boolean;
  pluginInfos: PluginInfos;
}

interface HasSuccessMessage {
  successMessage: m.Children;
  clearMessage: () => void;
}

interface State extends HasSuccessMessage {
  edit: (pluginInfo: any, event: MouseEvent) => void;
  onSuccessfulSave: (msg: m.Children) => void;
}

export class PluginsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    let timeoutID: number;
    vnode.state.edit = (pluginInfo: any, event: MouseEvent) => {
      event.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      new PluginSettingsModal(pluginInfo, vnode.state.onSuccessfulSave).render();
    };

    vnode.state.clearMessage = () => {
      vnode.state.successMessage = null;
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      vnode.state.successMessage = msg;
      timeoutID                  = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (!vnode.attrs.pluginInfos) {
      return <Spinner/>;
    }
    return (
      <div data-test-id="plugins-list">
        <FlashMessage type={MessageType.success} message={vnode.state.successMessage}/>
        {_.sortBy(vnode.attrs.pluginInfos, (pluginInfo) => pluginInfo.id).map((pluginInfo: PluginInfo) => {
          return (
            <PluginWidget key={pluginInfo.id}
                          pluginInfo={pluginInfo}
                          onEdit={vnode.state.edit.bind(vnode.state, pluginInfo)}
                          isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
          );
        })}
      </div>
    );
  }
}

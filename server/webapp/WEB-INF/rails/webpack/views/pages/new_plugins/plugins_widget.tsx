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


import * as m from "mithril";

import {PluginWidget} from "./plugin_widget";
import {Spinner} from "../../components/spinner";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {PluginSettingsModal} from "./plugin_settings_modal";
import {SuccessFlashMessage} from "../../components/flash_message";

//todo: change this to pluginInfos:PluginInfos
export interface Attrs {
  isUserAnAdmin: boolean;
  pluginInfos: any;
}

interface State {
  edit: Function;
  successMessage: string | null;
  onSuccessfulSave: Function,
  clearMessage: Function
}

export class PluginsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    let timeoutID: number;
    vnode.state.edit = function (pluginInfo: any, event: MouseEvent) {
      event.stopPropagation();
      if (timeoutID) {
        clearTimeout(timeoutID)
      }

      new PluginSettingsModal(pluginInfo, vnode.state.onSuccessfulSave).render();
    };

    vnode.state.clearMessage = function () {
      vnode.state.successMessage = null;
    };

    vnode.state.onSuccessfulSave = function (msg: string) {
      vnode.state.successMessage = msg;
      timeoutID                  = window.setTimeout(vnode.state.clearMessage.bind(vnode.state), 10000);
    }
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (!vnode.attrs.pluginInfos()) {
      return <Spinner/>
    }

    return (
      <div class="plugins-settings">
        <SuccessFlashMessage message={vnode.state.successMessage}/>
        {vnode.attrs.pluginInfos().sortByPluginInfos((pi: any) => pi.id()).map((pluginInfo: any) => {
          return (
            <PluginWidget key={pluginInfo.id()}
                          pluginInfo={pluginInfo}
                          onEdit={vnode.state.edit.bind(vnode.state, pluginInfo)}
                          isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
          );
        })}
      </div>
    );
  }
}

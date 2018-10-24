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
import {MithrilViewComponent} from "../../../jsx/mithril-component";

//todo: change this to pluginInfos:PluginInfos
export interface Attrs {
  isUserAnAdmin: boolean;
  pluginInfos: any;
}

export class PluginsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    if (!vnode.attrs.pluginInfos()) {
      return <Spinner/>
    }

    //todo: NOTE, GoCD is shipped with bundled plugins, no need to check if any plugin exists

    return (
      <div class="plugins-settings">
        {vnode.attrs.pluginInfos().sortByPluginInfos((pi: any) => pi.id()).map((pluginInfo: any) => {
          return (
            <PluginWidget pluginInfo={pluginInfo}
                          isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
          );
        })}
      </div>
    );
  }
}

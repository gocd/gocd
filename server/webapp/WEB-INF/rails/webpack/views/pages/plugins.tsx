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

import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_infos";
import {PluginsWidget} from "views/pages/new_plugins/plugins_widget";

const HeaderPanel = require('views/components/header_panel');

let pluginInfos: Array<PluginInfo<any>>;

const onSuccess = (res: Array<PluginInfo<any>>) => {
  pluginInfos = res;
  m.redraw();
};

export class PluginsPage extends MithrilComponent {
  oninit() {
    //todo: onfailure render generic error_onload_page
    PluginInfos.all().then(onSuccess); //todo: include_bad
  }

  view() {
    const isUserAnAdmin = document.body.getAttribute('data-is-user-admin');

    return <main class="main-container">
      <HeaderPanel title="Plugins"/>
      <PluginsWidget pluginInfos={pluginInfos} isUserAnAdmin={isUserAnAdmin === 'true'}/>
    </main>;
  }
}

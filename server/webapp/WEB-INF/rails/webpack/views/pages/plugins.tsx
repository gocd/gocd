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
import {PluginsWidget} from "./new_plugins/plugins_widget";

//todo: Change these requires to import
const Stream      = require('mithril/stream');
const PluginInfos = require('models/shared/plugin_infos');
const HeaderPanel = require('views/components/header_panel');

const pluginInfos = Stream();

//todo: change pluginInfos to typescript and fix res:PluginInfos
const onSuccess = (res: any) => {
  pluginInfos(res);
  m.redraw();
};

export class PluginsPage {
  oninit() {
    //todo: onfailure render generic error_onload_page
    PluginInfos.all(null, {include_bad: true}).then(onSuccess);
  }

  view() {
    const isUserAnAdmin = document.body.getAttribute('data-is-user-admin');

    return <main class="main-container">
      <HeaderPanel title="Plugins"/>
      <PluginsWidget pluginInfos={pluginInfos} isUserAnAdmin={isUserAnAdmin === 'true'}/>
    </main>;
  }
}

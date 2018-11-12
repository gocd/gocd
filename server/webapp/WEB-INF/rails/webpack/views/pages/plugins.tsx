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
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {PluginsWidget} from "views/pages/new_plugins/plugins_widget";
import {Page} from "./page";

export class PluginsPage extends Page {

  private pluginInfos?: Array<PluginInfo<Extension>>;

  pageName() {
    return "Plugins";
  }

  componentToDisplay() {
    if (this.pluginInfos) {
      const isUserAnAdmin = document.body.getAttribute("data-is-user-admin");
      return <PluginsWidget pluginInfos={this.pluginInfos} isUserAnAdmin={isUserAnAdmin === "true"}/>;
    }
  }

  fetchData() {
    return PluginInfoCRUD.all({include_bad: true}).then(this.onSuccess.bind(this));
  }

  private onSuccess(pluginInfos: Array<PluginInfo<Extension>>) {
    this.pluginInfos = pluginInfos;
  }
}

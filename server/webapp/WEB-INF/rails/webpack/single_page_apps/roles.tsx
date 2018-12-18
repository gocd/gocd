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

import Page from "helpers/spa_base";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {PageLoadError} from "views/components/page_load_error";
import {RolesPage} from "views/pages/roles";

const RolesWidget = require("views/roles/roles_widget");

const PluginInfos = require("models/shared/plugin_infos");
const AuthConfigs = require("models/auth_configs/auth_configs");

export class RolesSPA extends Page {
  constructor() {
    super(RolesPage);
  }
}

$(() => {
  const rolesContainer = $("#roles");

  if (rolesContainer.get().length === 0) {
    return new RolesSPA();
  }

  const onSuccess = (args: any) => {
    let authorizationPluginInfos = args[0];
    authorizationPluginInfos     = new PluginInfos(authorizationPluginInfos
                                                     .filterPluginInfo((pi: any) => pi.extensions()
                                                                                      .authorization
                                                                                      .capabilities()
                                                                                      .canAuthorize()));

    const authConfigsOfInstalledPlugin = new AuthConfigs(args[1].filterAuthConfig((ac: any) => authorizationPluginInfos.findById(
      ac.pluginId()) !== undefined));

    m.mount($("#roles").get(0), RolesWidget({
                                              pluginInfos: stream(authorizationPluginInfos),
                                              allAuthConfigs: stream(args[1]),
                                              authConfigsOfInstalledPlugin: stream(authConfigsOfInstalledPlugin)
                                            }));
  };

  const onFailure = () => {
    const component = {
      view() {
        return (<PageLoadError message="There was a problem fetching the roles"/>);
      }
    };
    m.mount($("#roles").get(0), component);
  };

  Promise.all([PluginInfos.all(null, {type: "authorization"}), AuthConfigs.all()]).then(onSuccess, onFailure);
});

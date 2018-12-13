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
import {AuthConfigsPage} from "views/pages/auth_configs";

//tslint:disable
const m                 = require("mithril");
const stream            = require("mithril/stream");
const AuthConfigsWidget = require("views/auth_configs/auth_configs_widget");
const PluginInfos       = require("models/shared/plugin_infos");
const PageLoadError     = require("views/shared/page_load_error");

//tslint:enable

export class AuthConfigsSPA extends Page {
  constructor() {
    super(AuthConfigsPage);
  }
}

$(() => {
  const authConfigContainer = $("#auth-configs");
  if (authConfigContainer.get().length === 0) {
    return new AuthConfigsSPA();
  }

  const onSuccess = (pluginInfos: any) => {
    const component = {
      view() {
        return (<AuthConfigsWidget pluginInfos={stream(pluginInfos)}/>);
      }
    };

    m.mount($("#auth-configs").get(0), component);
  };

  const onFailure = () => {
    const component = {
      view() {
        return (<PageLoadError message="There was a problem fetching the authorization configurations"/>);
      }
    };
    m.mount($("#auth-configs").get(0), component);
  };

  PluginInfos.all(null, {type: "authorization"}).then(onSuccess, onFailure);
});

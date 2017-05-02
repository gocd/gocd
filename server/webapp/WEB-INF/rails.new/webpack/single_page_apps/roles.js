/*
 * Copyright 2017 ThoughtWorks, Inc.
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
const $              = require('jquery');
const m              = require('mithril');
const Stream         = require('mithril/stream');
const RolesWidget    = require('views/roles/roles_widget');
const PluginInfos    = require('models/shared/plugin_infos');
const AuthConfigs    = require('models/auth_configs/auth_configs');
const VersionUpdater = require('models/shared/version_updater');
require('foundation-sites');

$(() => {
  new VersionUpdater().update();

  Promise.all([PluginInfos.all(null, {type: 'authorization'}), AuthConfigs.all()]).then((args) => {

    let authorizationPluginInfos = args[0];
    authorizationPluginInfos     = new PluginInfos(authorizationPluginInfos.filterPluginInfo((pi) => pi.capabilities().canAuthorize()));

    const authConfigsOfInstalledPlugin = new AuthConfigs(args[1].filterAuthConfig((ac) => authorizationPluginInfos.findById(ac.pluginId()) !== undefined));

    m.mount($("#roles").get(0), RolesWidget({
      pluginInfos:                  Stream(authorizationPluginInfos),
      allAuthConfigs:               Stream(args[1]),
      authConfigsOfInstalledPlugin: Stream(authConfigsOfInstalledPlugin)
    }));

    $(document).foundation();
  });

});



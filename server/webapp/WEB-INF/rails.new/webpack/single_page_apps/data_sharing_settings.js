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

const $ = require('jquery');
const m = require('mithril');

const VersionUpdater      = require('models/shared/version_updater');
const DataSharingSettings = require('models/data_sharing_settings/data_sharing_settings');

const PageLoadError             = require('views/shared/page_load_error');
const DataSharingSettingsWidget = require('views/data_sharing_settings/data_sharing_settings_widget');

require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  new VersionUpdater().update();
  const container = $("#data-sharing-settings-container").get(0);

  const onSuccess = (settings) => {
    m.mount(container, {
      view() {
        return (<DataSharingSettingsWidget settings={settings}/>);
      }
    });

    $(document).foundation();
  };

  const onFailure = () => {
    m.mount(container, {
      view() {
        return (<PageLoadError message="There was a problem fetching data sharing settings information"/>);
      }
    });
  };

  DataSharingSettings.get().then(onSuccess, onFailure);
});



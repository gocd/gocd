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

import {MithrilComponent} from "../../jsx/mithril-component";

const $             = require('jquery');
const m             = require('mithril');
const Stream        = require('mithril/stream');
const PluginsWidget = require('views/plugins/plugins_widget_new');
const PluginInfos   = require('models/shared/plugin_infos');
const PageLoadError = require('views/shared/page_load_error');
const Spinner       = require('views/shared/spinner');
const HeaderPanel   = require('views/components/header_panel');

let thingToDisplay = <Spinner/>;

const onSuccess = (pluginInfos: any) => {
  const isUserAnAdmin = $('body').attr('data-is-user-admin');
  thingToDisplay      =
    <PluginsWidget pluginInfos={Stream(pluginInfos)} isUserAnAdmin={Stream(isUserAnAdmin === 'true')}/>;
  m.redraw();
};

const onFailure = () => {
  thingToDisplay = <PageLoadError message="There was a problem fetching plugins"/>;
  m.redraw();
};


export class PluginsPage extends MithrilComponent<null, null> {
  oninit() {
    PluginInfos.all(null, {'include_bad': true}).then(onSuccess, onFailure);
  }

  view() {
    return <div>
      <HeaderPanel title="Plugins"/>
      {thingToDisplay}
    </div>;
  }
}

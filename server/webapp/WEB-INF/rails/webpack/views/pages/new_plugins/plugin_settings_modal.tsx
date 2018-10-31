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

import * as m from 'mithril';
import * as Buttons from "views/components/buttons";
import {AlertFlashMessage} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import * as foundationStyles from './foundation_hax.scss';

const PluginSetting = require('models/plugins/plugin_setting');
const AngularPlugin = require('views/shared/angular_plugin');

export class PluginSettingsModal extends Modal {
  private readonly pluginInfo: any;
  private pluginSettings: any;
  private errorMessage: string | null = null;
  private successCallback: (msg: string) => void;

  constructor(pluginInfo: any, successCallback: (msg: string) => void) {
    super(Size.large);
    this.pluginInfo      = pluginInfo;
    this.successCallback = successCallback;

    PluginSetting.get(this.pluginInfo.id()).then(this.onFulfilled.bind(this), this.onFailure.bind(this)).always(m.redraw);
  }

  title() {
    return "Edit plugin settings";
  }

  body() {
    if (this.errorMessage) {
      return (<AlertFlashMessage message={this.errorMessage}/>);
    }

    if (!this.pluginSettings) {
      return <Spinner/>;
    }

    return (
      <div class={foundationStyles.foundationHax}>
        <div class="row collapse">
          <AngularPlugin pluginInfoSettings={this.pluginInfo.pluginSettings}
                         configuration={this.pluginSettings.configuration}
                         key={this.pluginInfo.id()}/>
        </div>
      </div>
    );
  }

  buttons(): JSX.Element[] {
    return [
      <Buttons.Primary onclick={this.performSave.bind(this)}>OK</Buttons.Primary>,
      <Buttons.Cancel onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }

  private onFulfilled(pluginSettings: any) {
    this.pluginSettings = pluginSettings;
    this.errorMessage   = null;
  }

  private onFailure(error: string, jqXHR: XMLHttpRequest) {
    if (jqXHR.status === 404) {
      this.pluginSettings = new PluginSetting({pluginId: this.pluginInfo.id()});
    } else {
      this.errorMessage = error;
    }
  }

  private performSave() {
    const self = this;
    if (this.pluginSettings.etag()) {
      this.pluginSettings.update()
        .then(() => {
          self.successCallback(`The plugin settings for ${this.pluginSettings.pluginId()} were updated successfully.`);
          self.close();
        }, self.showErrors.bind(self))
        .always(m.redraw);
    } else {
      this.pluginSettings.create()
        .then(() => {
          this.successCallback(`The plugin settings for ${this.pluginSettings.pluginId()} were created successfully.`);
          self.close();
        }, self.showErrors.bind(self))
        .always(m.redraw);
    }
  }

  private showErrors(pluginSettingsWithError: any) {
    this.pluginSettings = pluginSettingsWithError;
  }
}

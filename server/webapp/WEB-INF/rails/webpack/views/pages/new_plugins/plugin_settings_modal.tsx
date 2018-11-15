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

import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import * as m from "mithril";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginSettings} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";
import {PluginSettingsCRUD} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
// import * as foundationStyles from "./foundation_hax.scss";

const AngularPluginNew = require("views/shared/angular_plugin_new");
const Stream           = require("mithril/stream");

export class PluginSettingsModal extends Modal {
  private readonly pluginInfo: PluginInfo<any>;
  private pluginSettings?: PluginSettings;
  private etag?: string;
  private errorMessage: string | null = null;
  private successCallback: (msg: string) => void;

  constructor(pluginInfo: PluginInfo<any>, successCallback: (msg: string) => void) {
    super(Size.large);
    this.pluginInfo      = pluginInfo;
    this.successCallback = successCallback;

    PluginSettingsCRUD.get(this.pluginInfo.id).then(this.onFulfilled.bind(this));
  }

  title() {
    return "Edit plugin settings";
  }

  body() {
    if (this.errorMessage) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage}/>);
    }

    if (!this.pluginSettings) {
      return <Spinner/>;
    }

    return (
        <div class="row collapse">
          <AngularPluginNew
            pluginInfoSettings={Stream(this.pluginInfo.firstExtensionWithPluginSettings().pluginSettings)}
            configuration={this.pluginSettings}
            key={this.pluginInfo.id}/>
        </div>
    );
  }

  buttons(): JSX.Element[] {
    return [
      <Buttons.Primary onclick={this.performSave.bind(this)}>OK</Buttons.Primary>,
      <Buttons.Cancel onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }

  private onFulfilled(result: ApiResult<ObjectWithEtag<PluginSettings>>) {
    result.do((successResponse) => {
      this.pluginSettings = successResponse.body.object;
      this.etag           = successResponse.body.etag;
      this.errorMessage   = null;
    }, (errorResponse) => {
      if (result.getStatusCode() === 404) {
        this.pluginSettings = new PluginSettings(this.pluginInfo.id);
        this.errorMessage   = null;
      } else {
        this.errorMessage = errorResponse.message;
      }
    });
  }

  private performSave() {
    const self = this;
    if (!self.pluginSettings) {
      throw Error("Cannot perform save, pluginSettings not present");
    }
    if (self.etag) {
      PluginSettingsCRUD.update(self.pluginSettings, self.etag)
        .then((apiResult) => {
          apiResult.do(() => {
              if (self.pluginSettings) {
                self.successCallback(`The plugin settings for ${self.pluginSettings.plugin_id} were updated successfully.`);
                self.close();
              }
            },
            (errorResponse) => self.showErrors(apiResult, errorResponse));
        });
    } else {
      PluginSettingsCRUD.create(self.pluginSettings)
        .then((apiResult) => {
          apiResult.do(() => {
            if (self.pluginSettings) {
              this.successCallback(`The plugin settings for ${self.pluginSettings.plugin_id} were created successfully.`);
              self.close();
            }
          }, (errorResponse) => self.showErrors(apiResult, errorResponse));
        });
    }
  }

  private showErrors(apiResult: ApiResult<ObjectWithEtag<PluginSettings>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      this.pluginSettings = PluginSettings.fromJSON(JSON.parse(errorResponse.body).data);
    }
  }
}

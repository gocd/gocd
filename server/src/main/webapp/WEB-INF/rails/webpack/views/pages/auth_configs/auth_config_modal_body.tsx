/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import { MithrilViewComponent } from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import { AuthConfig } from "models/auth_configs/auth_configs";
import { AuthorizationExtension } from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import { PluginInfo, PluginInfos } from "models/shared/plugin_infos_new/plugin_info";
import { FlashMessage } from "views/components/flash_message";
import { Form, FormHeader } from "views/components/forms/form";
import {CheckboxField, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import { Message } from "views/pages/maintenance_mode";

const AngularPluginNew = require('views/shared/angular_plugin_new').AngularPluginNew;

interface Attrs {
  pluginInfos: PluginInfos;
  authConfig: AuthConfig;
  disableId: boolean;
  pluginIdProxy: (newPluginId?: string) => any;
  message?: Message;
}

export class AuthConfigModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    const pluginList = _.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return { id: pluginInfo.id, text: pluginInfo.about.name };
    });

    const pluginInfo = this.findPluginInfo(vnode.attrs.pluginInfos, vnode.attrs.authConfig.pluginId()!);
    const pluginSettings = pluginInfo.extensionOfType<AuthorizationExtension>(ExtensionTypeString.AUTHORIZATION)!.authConfigSettings;
    let mayBeMessage: any;
    if (vnode.attrs.message) {
      mayBeMessage = <FlashMessage type={vnode.attrs.message.type} message={vnode.attrs.message.message} />;
    }
    return (
      <div>
        <FormHeader>
          {mayBeMessage}
          <Form>
            <TextField label="Id"
              readonly={vnode.attrs.disableId}
              property={vnode.attrs.authConfig.id}
              errorText={vnode.attrs.authConfig.errors().errorsForDisplay("id")}
              required={true} />

            <SelectField label="Plugin"
              property={vnode.attrs.pluginIdProxy.bind(this)}
              required={true}
              errorText={vnode.attrs.authConfig.errors().errorsForDisplay("pluginId")}>
              <SelectFieldOptions selected={vnode.attrs.authConfig.pluginId()}
                items={pluginList} />
            </SelectField>
            <CheckboxField required={false}
                           helpText="Allow only those users to login who have explicitly been added by an administrator."
                           label="Allow only known users to login"
                           property={vnode.attrs.authConfig.allowOnlyKnownUsersToLogin}/>
          </Form>
        </FormHeader>

        <div>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={Stream(pluginSettings)}
              configuration={vnode.attrs.authConfig.properties()}
              key={pluginInfo.id} />
          </div>
        </div>
      </div>
    );
  }

  private findPluginInfo(pluginInfos: PluginInfos, pluginId: string): PluginInfo {
    return pluginInfos.find((pluginInfo) => pluginInfo.id === pluginId) as PluginInfo;
  }
}

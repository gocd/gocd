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
import { AuthConfig, AuthConfigs } from "models/auth_configs/auth_configs";
import { GoCDAttributes, GoCDRole, PluginAttributes, PluginRole } from "models/roles/roles";
import { ExtensionTypeString } from "models/shared/plugin_infos_new/extension_type";
import { AuthorizationExtension} from "models/shared/plugin_infos_new/extensions";
import { PluginInfos } from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import { Form } from "views/components/forms/form";
import { Option, SelectField, SelectFieldOptions, TextField } from "views/components/forms/input_fields";
import { UsersWidget } from "views/pages/roles/users_widget";
import styles from "./index.scss";

const AngularPluginNew = require('views/shared/angular_plugin_new').AngularPluginNew;

interface ModalAttrs {
  role: GoCDRole | PluginRole;
  isNameDisabled: boolean;
}

interface PluginModalAttrs extends ModalAttrs {
  readonly pluginInfos: PluginInfos;
  readonly authConfigs: AuthConfigs;
}

export class GoCDRoleModalBodyWidget extends MithrilViewComponent<ModalAttrs> {
  private newUser: Stream<string> = Stream();
  private lastUserAdded?: string;

  view(vnode: m.Vnode<ModalAttrs>): m.Children | void | null {
    const role = vnode.attrs.role as GoCDRole;
    return (<Form>
      <TextField label="Role name"
        readonly={vnode.attrs.isNameDisabled}
        property={role.name}
        errorText={role.errors().errorsForDisplay("name")}
        required={true} />

      <li class={styles.usersInRole}>
        <UsersWidget roleAttributes={role.attributes}
          selectedUser={this.lastUserAdded}
          readOnly={false} />
      </li>

      <TextField label="Role users" required={false} property={this.newUser} />

      <li class={styles.addUserToRole}>
        <Buttons.Primary
          data-test-id="role-add-user-button"
          onclick={this.addNewUserToRole.bind(this, vnode)}>Add</Buttons.Primary>
      </li>
    </Form>);
  }

  private addNewUserToRole(vnode: m.Vnode<ModalAttrs>, e: MouseEvent) {
    if (this.newUser && this.newUser() && this.newUser().length > 0) {
      (vnode.attrs.role.attributes() as GoCDAttributes).addUser(this.newUser());
      this.lastUserAdded = this.newUser();
      this.newUser = Stream("");
    }
  }
}

export class PluginRoleModalBodyWidget extends MithrilViewComponent<PluginModalAttrs> {

  view(vnode: m.Vnode<PluginModalAttrs>): m.Children | void | null {
    const pluginAttributes = (vnode.attrs.role.attributes() as PluginAttributes);
    let authID = pluginAttributes.authConfigId;
    if (!authID) {
      const authConfigOfInstalledPlugin = _.find(vnode.attrs.authConfigs, (authConfig) => {
        const authConfigWithPlugin = _.find(vnode.attrs.pluginInfos,
          (pluginInfo) => {
            return authConfig.pluginId() === pluginInfo.id;
          });
        if (authConfigWithPlugin) {
          return authConfig;
        }
      }) as AuthConfig;
      if (!authConfigOfInstalledPlugin) {
        return;
      }
      authID = authConfigOfInstalledPlugin.id()!;
    }

    const authConfig = vnode.attrs.authConfigs.findById(authID);
    pluginAttributes.authConfigId = authConfig.id()!;
    const pluginInfo = _.find(vnode.attrs.pluginInfos, (pl) => pl.id === authConfig.pluginId());

    if (!pluginInfo) {
      return;
    }
    const pluginSettings = (pluginInfo
      .extensionOfType(ExtensionTypeString.AUTHORIZATION)! as AuthorizationExtension).roleSettings;

    let authConfigs = _.map(vnode.attrs.authConfigs, (authConfig: AuthConfig) => {
      const authConfigWithPlugin = _.find(vnode.attrs.pluginInfos,
        (pluginInfo) => {
          return authConfig.pluginId() === pluginInfo.id;
        });
      if (authConfigWithPlugin) {
        return {
          id: authConfig.id(),
          text: `${authConfig.id()} (${authConfigWithPlugin.about.name ? authConfigWithPlugin.about.name : authConfig.pluginId})`
        };
      }
    }) as Option[];
    authConfigs = _.filter(authConfigs, (value) => {
      return !!value;
    }) as Option[];

    return (
      <div>
        <div>
          <Form>
            <TextField label="Role name"
              readonly={vnode.attrs.isNameDisabled}
              property={vnode.attrs.role.name}
              errorText={vnode.attrs.role.errors().errorsForDisplay("name")}
              required={true} />

            <SelectField label="Auth Config Id"
              property={this.authConfigIdProxy.bind(this, vnode)}
              required={true}
              errorText={vnode.attrs.role.errors().errorsForDisplay("auth_config_id")}>
              <SelectFieldOptions selected={authConfig.pluginId()}
                items={authConfigs} />
            </SelectField>
          </Form>
        </div>
        <div>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={Stream(pluginSettings)}
              configuration={pluginAttributes.properties()}
              key={pluginInfo.id} />
          </div>
        </div>
      </div>);
  }

  private authConfigIdProxy(vnode: m.Vnode<PluginModalAttrs>, newValue?: string) {
    const role = vnode.attrs.role as PluginRole;
    const attributes = role.attributes() as PluginAttributes;
    if (newValue && attributes.authConfigId !== newValue) {
      const authConfig = vnode.attrs.authConfigs.findById(newValue);
      if (authConfig) {
        attributes.authConfigId = authConfig.id()!;
      }
    }
    return attributes.authConfigId;
  }

}

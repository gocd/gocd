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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs_new";
import {GoCDAttributes, GoCDRole, PluginAttributes, PluginRole} from "models/roles/roles_new";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {AuthorizationSettings, Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {Form} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";

const AngularPluginNew = require("views/shared/angular_plugin_new");

interface ModalAttrs {
  role: GoCDRole | PluginRole;
  isNameDisabled: boolean;
}

interface PluginModalAttrs extends ModalAttrs {
  readonly pluginInfos: Array<PluginInfo<Extension>>;
  readonly authConfigs: AuthConfigs;
}

export class GoCDRoleModalBodyWidget extends MithrilViewComponent<ModalAttrs> {
  private newUsername: Stream<string> = stream("");
  private lastUsernameAdded!: string;
  private role!: GoCDRole | PluginRole;

  view(vnode: m.Vnode<ModalAttrs, this>): m.Children | void | null {
    this.role = vnode.attrs.role as GoCDRole;
    return (<div>
      <div>
        <div>
          <TextField label="Role Name"
                     disabled={vnode.attrs.isNameDisabled}
                     property={this.role.name}
                     errorText={this.role.errors().errorsForDisplay("name")}
                     required={true}/>
        </div>
        <div data-test-id="tags" class={"role-edit-only row show"}>
          {this.role.attributes().users.map((user) => {
            return (
              <div data-alert
                   class={this.getClass(user)}>
                {user}
                {this.getDeleteButton(user)}
              </div>
            );
          })}
        </div>
        <div>
          <TextField label="Role users" required={false} property={this.newUsername}/>
          <Buttons.Primary
            data-test-id="role-add-user-button"
            onclick={this.addNewUserToRole.bind(this)}>Add</Buttons.Primary>
        </div>
      </div>
    </div>);
  }

  private getDeleteButton(user: string) {
    return (<span aria-hidden="true" class="role-user-delete-icon"
                  onclick={this.deleteUserFromRole.bind(this, user)}>&times;</span>);
  }

  private getClass(user: string) {
    return this.lastUsernameAdded === user ? "tag current-user-tag" : "tag";
  }

  private addNewUserToRole(e: MouseEvent) {
    if (this.newUsername) {
      (this.role.attributes() as GoCDAttributes).addUser(this.newUsername());
      this.lastUsernameAdded = this.newUsername();
      this.newUsername       = stream("");
    }
  }

  private deleteUserFromRole(username: string) {
    if (username) {
      (this.role.attributes() as GoCDAttributes).deleteUser(username);
    }
  }
}

export class PluginRoleModalBodyWidget extends MithrilViewComponent<PluginModalAttrs> {
  private pluginInfo!: Stream<PluginInfo<Extension>>;
  private pluginInfos!: Array<PluginInfo<Extension>>;
  private authConfig!: Stream<AuthConfig>;
  private role!: GoCDRole | PluginRole;

  view(vnode: m.Vnode<PluginModalAttrs, this>): m.Children | void | null {
    this.pluginInfos = vnode.attrs.pluginInfos;
    this.role        = vnode.attrs.role;

    const pluginAttributes = (vnode.attrs.role.attributes() as PluginAttributes);
    let authID             = pluginAttributes.authConfigId;
    if (!authID) {
      const authConfigOfInstalledPlugin = _.find(vnode.attrs.authConfigs, (authConfig) => {
        const authConfigWithPlugin = _.find(vnode.state.pluginInfos,
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
      authID                        = authConfigOfInstalledPlugin.id();
      pluginAttributes.authConfigId = authID;
    }

    const authConfig = _.find(vnode.attrs.authConfigs, (ac) => ac.id() === authID);
    if (!authConfig) {
      return;
    }
    const pluginInfo = _.find(vnode.attrs.pluginInfos, (pl) => pl.id === authConfig.pluginId());

    if (!pluginInfo) {
      return;
    }
    this.pluginInfo      = stream(pluginInfo);
    const pluginSettings = (pluginInfo
      .extensionOfType(ExtensionType.AUTHORIZATION)! as AuthorizationSettings).authConfigSettings;

    const pluginList = _.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });
    return (
      <div>
        <div>
          {/*<FormHeader>*/}
          <Form>
            <TextField label="Id"
                       disabled={vnode.attrs.isNameDisabled}
                       property={this.authId.bind(this)}
                       errorText={authConfig.errors().errorsForDisplay("id")}
                       required={true}/>

            <SelectField label="Plugin ID"
                         property={this.pluginIdProxy.bind(this)}
                         required={true}
                         errorText={authConfig.errors().errorsForDisplay("pluginId")}>
              <SelectFieldOptions selected={authConfig.pluginId()}
                                  items={pluginList}/>
            </SelectField>
          </Form>
          {/*</FormHeader>*/}
        </div>
        <div>
          <div className="row collapse">
            <AngularPluginNew
              pluginInfoSettings={stream(pluginSettings)}
              configuration={authConfig.properties()}
              key={this.pluginInfo().id}/>
          </div>
        </div>
      </div>);
  }

  private pluginIdProxy(newValue ?: string) {
    if (newValue) {
      if (this.pluginInfo().id !== newValue) {
        const pluginInfo = _.find(this.pluginInfos, (p) => p.id === newValue);
        this.pluginInfo(pluginInfo!);
        this.authConfig(new AuthConfig(this.authConfig().id(), pluginInfo!.id, new Configurations([])));
      }
    }
    return this.pluginInfo().id;
  }

  private authId(newValue?: string): string {
    if (newValue) {
      (this.role.attributes() as PluginAttributes).authConfigId = newValue;
    }
    return (this.role.attributes() as PluginAttributes).authConfigId;
  }

}

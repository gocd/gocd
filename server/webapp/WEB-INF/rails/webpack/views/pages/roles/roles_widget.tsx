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
import {AuthConfigs} from "models/auth_configs/auth_configs_new";
import {GoCDRole, PluginAttributes, PluginRole, Roles} from "models/roles/roles_new";
import {Configuration} from "models/shared/configuration";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as s from "underscore.string";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";
import {UsersWidget} from "views/pages/roles/users_widget";
import * as styles from "./index.scss";

const gocdIcon = require("../../../../app/assets/images/gocd.svg");

interface Attrs extends EditOperation<GoCDRole | PluginRole>, CloneOperation<GoCDRole | PluginRole>, DeleteOperation<GoCDRole | PluginRole> {
  pluginInfos: Array<PluginInfo<Extension>>;
  authConfigs: AuthConfigs;
  roles: Roles;
}

interface RoleAttrs extends EditOperation<GoCDRole | PluginRole>, CloneOperation<GoCDRole | PluginRole>, DeleteOperation<GoCDRole | PluginRole> {
  role: GoCDRole | PluginRole;
}

interface PluginRoleAttrs extends RoleAttrs {
  pluginInfos: Array<PluginInfo<Extension>>;
  authConfigs: AuthConfigs;
}

export class RolesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return <div data-test-id="role-widget">
      {vnode.attrs.roles.map((role) => {
        if (role.isPluginRole()) {
          return (
            <PluginRoleWidget role={role}
                              authConfigs={vnode.attrs.authConfigs}
                              pluginInfos={vnode.attrs.pluginInfos} onEdit={vnode.attrs.onEdit}
                              onClone={vnode.attrs.onClone} onDelete={vnode.attrs.onDelete}/>);
        } else {
          return (<GoCDRoleWidget role={role} onEdit={vnode.attrs.onEdit} onClone={vnode.attrs.onClone}
                                  onDelete={vnode.attrs.onDelete}/>);
        }
      })}
    </div>;
  }
}

abstract class RoleWidget extends MithrilViewComponent<RoleAttrs | PluginRoleAttrs> {
  static headerIcon(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>) {
    if (!vnode.attrs.role.isPluginRole()) {
      return <HeaderIcon name="GoCD role" imageUrl={gocdIcon}/>;
    }

    const role             = vnode.attrs.role as PluginRole;
    const pluginAttributes = vnode.attrs as PluginRoleAttrs;
    const authConfig       = pluginAttributes.authConfigs.findById(role.attributes().authConfigId);
    if (authConfig) {
      const pluginInfo = _.find(pluginAttributes.pluginInfos, {id: authConfig.pluginId()});
      if (pluginInfo && pluginInfo.imageUrl) {
        return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
      } else {
        return <HeaderIcon/>;
      }
    }
  }

  static headerMap(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>) {
    const map = new Map();
    map.set("Name", vnode.attrs.role.name());
    if (!vnode.attrs.role.isPluginRole()) {
      return map;
    }

    const role             = vnode.attrs.role as PluginRole;
    const pluginAttributes = vnode.attrs as PluginRoleAttrs;
    const authConfig       = pluginAttributes.authConfigs.findById(role.attributes().authConfigId);

    if (authConfig) {
      map.set("Auth Config Id", authConfig.id());
      const pluginInfo = _.find(pluginAttributes.pluginInfos, {id: authConfig.pluginId()});
      if (pluginInfo) {
        map.set("Plugin", pluginInfo.about.name);
      } else {
        map.set("Plugin", `Plugin '${authConfig.pluginId()}' not found`);
      }
    }
    return map;
  }

  static pluginIsNotInstalled(pluginRoleAttrs: PluginRoleAttrs) {
    if (!pluginRoleAttrs.pluginInfos || pluginRoleAttrs.pluginInfos.length === 0) {
      return true;
    }

    const authConfigId = (pluginRoleAttrs.role.attributes() as PluginAttributes).authConfigId;
    const pluginId     = pluginRoleAttrs.authConfigs.findById(authConfigId)!.pluginId();

    const pluginInfo = _.find(pluginRoleAttrs.pluginInfos, (pluginInfo) => {
      return pluginId === pluginInfo.id;
    });

    return !pluginInfo;
  }

  view(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>): m.Children | void | null {
    const isDisabled    = vnode.attrs.role.isPluginRole() ? RoleWidget.pluginIsNotInstalled(vnode.attrs as PluginRoleAttrs) : false;
    const header        = [RoleWidget.headerIcon(vnode),
      <KeyValuePair inline={true} data={RoleWidget.headerMap(vnode)}/>];
    const actionButtons = [
      <IconGroup>
        <Edit data-test-id="role-edit" disabled={isDisabled}
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, vnode.attrs.role)}/>
        <Clone data-test-id="role-clone" disabled={isDisabled}
               onclick={vnode.attrs.onClone.bind(vnode.attrs, vnode.attrs.role)}/>
        <Delete data-test-id="role-delete"
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.role)}/>
      </IconGroup>];

    return (<CollapsiblePanel header={header} actions={actionButtons}
                              dataTestId={`role-${s.slugify(vnode.attrs.role.name())}`}>
      {this.viewForRole(vnode)}
    </CollapsiblePanel>);
  }

  abstract viewForRole(vnode: m.Vnode<RoleAttrs>): m.Children;
}

export class GoCDRoleWidget extends RoleWidget {
  viewForRole(vnode: m.Vnode<RoleAttrs>): m.Children {
    const gocdRole = vnode.attrs.role as GoCDRole;
    let body;
    if (gocdRole.attributes().users.length === 0) {
      body = (<span data-test-id="no-users-message" className={styles.noUsersMessage}>No users in this role.</span>);
    } else {
      body = (<UsersWidget roleAttributes={gocdRole.attributes()}/>);
    }
    return body;
  }
}

export class PluginRoleWidget extends RoleWidget {
  viewForRole(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>): m.Children {
    const pluginRole = vnode.attrs.role as PluginRole;
    return (<KeyValuePair data={this.asMap(pluginRole.attributes().properties())}/>);
  }

  asMap(configurations: Configuration[]): Map<string, string> {
    return new Map<string, string>(configurations
                                     .map((prop) => [prop.key, prop.displayValue()] as [string, string]));
  }
}

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
import {GoCDRole, PluginRole, Roles} from "models/roles/roles_new";
import {Configuration} from "models/shared/configuration";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

interface Attrs extends EditOperation<GoCDRole | PluginRole>, CloneOperation<GoCDRole | PluginRole>, DeleteOperation<GoCDRole | PluginRole> {
  pluginInfos: Array<PluginInfo<Extension>>;
  authConfigs: AuthConfigs;
  roles: Roles;
}

interface RoleAttrs extends EditOperation<GoCDRole | PluginRole>, CloneOperation<GoCDRole | PluginRole>, DeleteOperation<GoCDRole | PluginRole> {
  role: GoCDRole | PluginRole;
}

interface PluginRoleAttrs extends RoleAttrs {
  pluginInfo: Array<PluginInfo<Extension>>;
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
                              pluginInfo={vnode.attrs.pluginInfos} onEdit={vnode.attrs.onEdit}
                              onClone={vnode.attrs.onClone} onDelete={vnode.attrs.onDelete}/>);
        } else {
          return (<GoCDRoleWidget role={role} onEdit={vnode.attrs.onEdit} onClone={vnode.attrs.onEdit}
                                  onDelete={vnode.attrs.onDelete}/>);
        }
      })}
    </div>;
  }
}

abstract class RoleWidget extends MithrilViewComponent<RoleAttrs | PluginRoleAttrs> {
  static headerIcon(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>) {
    if (!vnode.attrs.role.isPluginRole()) {
      return <HeaderIcon name="No Plugin" imageUrl="/go/assets/gocd.svg"/>;
    }

    const role             = vnode.attrs.role as PluginRole;
    const pluginAttributes = vnode.attrs as PluginRoleAttrs;
    const authConfig       = pluginAttributes.authConfigs.findById(role.attributes().authConfigId);
    if (authConfig) {
      const pluginInfo = _.find(pluginAttributes.pluginInfo, {id: authConfig.pluginId()});
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
      const pluginInfo = _.find(pluginAttributes.pluginInfo, {id: authConfig.pluginId()});
      if (pluginInfo) {
        map.set("Plugin", pluginInfo.about.name);
      } else {
        map.set("Plugin", `Plugin '${authConfig.pluginId()}' not found`);
      }
    }
    return map;
  }

  view(vnode: m.Vnode<RoleAttrs | PluginRoleAttrs>): m.Children | void | null {
    const header        = [RoleWidget.headerIcon(vnode),
      <KeyValuePair inline={true} data={RoleWidget.headerMap(vnode)}/>];
    const actionButtons = [
      <IconGroup>
        <Edit data-test-id="role-edit"
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, vnode.attrs.role)}/>
        <Clone data-test-id="role-clone"
               onclick={vnode.attrs.onClone.bind(vnode.attrs, vnode.attrs.role)}/>
        <Delete data-test-id="role-delete"
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.role)}/>
      </IconGroup>];

    return (<CollapsiblePanel header={header} actions={actionButtons}>
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
      body = (<span data-test-id="no-users-message" class="no-users-message">No users in this role.</span>);
    } else {
      //todo need to update this to tag component
      body = (gocdRole.attributes().users.map((user) => {
        return (<div data-alert className="tag">
          {user}
        </div>);
      }));
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
    //TODO: _.toPairs
    return new Map<string, string>(configurations
                                     .map((prop) => [prop.key, prop.displayValue()] as [string, string]));
  }
}

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

import m from "mithril";
import Stream from "mithril/stream";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {ExtensionTypeString, PackageRepoExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {NoPluginsOfTypeInstalled} from "views/components/no_plugins_installed";
import {PackageRepositoriesWidget} from "views/pages/package_repositories/package_repositories_widget";
import {Page, PageState} from "views/pages/page";
import {RequiresPluginInfos} from "views/pages/page_operations";

interface State extends RequiresPluginInfos {
  packageRepositories: Stream<PackageRepositories>;
}

export class PackageRepositoriesPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.packageRepositories = Stream();
    vnode.state.pluginInfos         = Stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (vnode.state.pluginInfos().length === 0) {
      return (<NoPluginsOfTypeInstalled extensionType={new PackageRepoExtensionType()}/>);
    }

    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <PackageRepositoriesWidget packageRepositories={vnode.state.packageRepositories}
                                 pluginInfos={vnode.state.pluginInfos}/>
    </div>;
  }

  pageName(): string {
    return "Package Repositories";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PackageRepositoriesCRUD.all(), PluginInfoCRUD.all({type: ExtensionTypeString.PACKAGE_REPO})])
                  .then((result) => {
                    result[0].do((successResponse) => {
                      vnode.state.packageRepositories(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    });

                    result[1].do((successResponse) => {
                      vnode.state.pluginInfos(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    })
                  });

  }
}

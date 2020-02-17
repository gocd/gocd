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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {PackageRepository} from "models/package_repositories/package_repositories";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {KeyValuePair} from "views/components/key_value_pair";
import {ConfigurationDetailsWidget} from "./configuration_details_widget";
import {PackagesWidget} from "./packages_widget";

interface Attrs {
  packageRepository: PackageRepository;
  pluginInfo: PluginInfo;
}

export class PackageRepositoryWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const header = <KeyValuePair inline={true}
                                 data={PackageRepositoryWidget.headerMap(vnode.attrs.packageRepository, vnode.attrs.pluginInfo)}/>;

    const packageRepository = vnode.attrs.packageRepository;
    const pkgRepoProperties = packageRepository.configuration() ? packageRepository.configuration()!.asMap() : [];
    const pkgRepoDetails    = new Map([
                                        ["Name", packageRepository.name()],
                                        ["Plugin Id", packageRepository.pluginMetadata().id()],
                                        ...Array.from(pkgRepoProperties)
                                      ]);

    return <CollapsiblePanel key={vnode.attrs.packageRepository.repoId()}
                             nonExpandable={true}
                             header={header}
                             dataTestId={"package-repository-panel"}>
      <ConfigurationDetailsWidget header={"Package Repository configuration"} data={pkgRepoDetails}/>
      <PackagesWidget packages={vnode.attrs.packageRepository.packages}/>
    </CollapsiblePanel>;
  }

  private static headerMap(packageRepository: PackageRepository, pluginInfo?: PluginInfo) {
    const map = new Map();
    map.set("Name", packageRepository.name());
    if (pluginInfo) {
      map.set("Plugin Id", pluginInfo.id);
    }
    return map;
  }
}

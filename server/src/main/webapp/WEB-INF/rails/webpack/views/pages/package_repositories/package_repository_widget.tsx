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
import {ButtonIcon, Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {PackageOperations} from "views/pages/package_repositories";
import {CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";
import {ConfigurationDetailsWidget} from "./configuration_details_widget";
import {PackagesWidget} from "./packages_widget";
import styles from "./index.scss";

interface Attrs extends EditOperation<PackageRepository>, CloneOperation<PackageRepository>, DeleteOperation<PackageRepository> {
  packageRepository: PackageRepository;
  pluginInfo: PluginInfo;
  packageOperations: PackageOperations;
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
    const actionButtons     = [
      <Secondary onclick={vnode.attrs.packageOperations.onAdd.bind(vnode.attrs, packageRepository)}
                 data-test-id={"package-create"}
                 disabled={!vnode.attrs.pluginInfo}
                 icon={ButtonIcon.ADD}>
        Create Package
      </Secondary>,
      <div className={styles.packageRepositoryCrudActions}>
        <IconGroup>
          <Edit data-test-id="package-repo-edit"
                disabled={!vnode.attrs.pluginInfo}
                onclick={vnode.attrs.onEdit.bind(vnode.attrs, packageRepository)}/>
          <Clone data-test-id="package-repo-clone"
                 disabled={!vnode.attrs.pluginInfo}
                 onclick={vnode.attrs.onClone.bind(vnode.attrs, packageRepository)}/>
          <Delete data-test-id="package-repo-delete"
                  onclick={vnode.attrs.onDelete.bind(vnode.attrs, packageRepository)}/>
        </IconGroup>
      </div>];

    return <CollapsiblePanel key={vnode.attrs.packageRepository.repoId()}
                             nonExpandable={true}
                             header={header}
                             actions={actionButtons}
                             dataTestId={"package-repository-panel"}>
      <ConfigurationDetailsWidget header={"Package Repository configuration"} data={pkgRepoDetails}/>
      <PackagesWidget packages={vnode.attrs.packageRepository.packages}
                      pluginInfo={vnode.attrs.pluginInfo}
                      packageOperations={vnode.attrs.packageOperations}/>
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

/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import _ from "lodash";
import m from "mithril";
import {Package, PackageRepositories, PackageRepository} from "models/package_repositories/package_repositories";
import {PackageRepoExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {PluginView} from "views/pages/package_repositories/package_repo_plugin_view";
import {MaterialAutoUpdateToggle} from "views/pages/pipelines/material_auto_update_toggle";

interface Attrs {
  pluginInfos: PluginInfos;
  packageRepositories: PackageRepositories;
  package: Package;
  disableId: boolean;
  disablePackageRepo: boolean;
  onPackageRepoChange: (newPackageRepo?: PackageRepository) => any;
}

export class PackageModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const msgForNewPkg = vnode.attrs.disableId ? "" : "The new package will be available to be used as material in all pipelines. Other admins might be able to edit this package.";

    const selectedPackageRepository = vnode.attrs.packageRepositories.find((repo) => {
      return repo.repoId() === vnode.attrs.package.packageRepo().id();
    })!;

    const pluginInfo = _.find(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return pluginInfo.id === selectedPackageRepository.pluginMetadata().id();
    })!;

    const packageRepos = vnode.attrs.packageRepositories
                              .filter((pkgRepo) => {
                                return pkgRepo.pluginMetadata().id() === selectedPackageRepository.pluginMetadata().id();
                              })
                              .map((pkgRepo: PackageRepository) => {
                                return {id: pkgRepo.repoId(), text: pkgRepo.name()};
                              });

    return <div>
      <FormHeader>
        <FlashMessage type={MessageType.info} message={msgForNewPkg}/>
        <Form>
          <TextField label="Name"
                     readonly={vnode.attrs.disableId}
                     property={vnode.attrs.package.name}
                     placeholder={"Enter the package name"}
                     errorText={vnode.attrs.package.errors().errorsForDisplay("name") || vnode.attrs.package.errors().errorsForDisplay("id")}
                     required={true}/>

          <SelectField label="Package Repository"
                       property={this.packageRepoIdProxy.bind(this, vnode)}
                       required={true}
                       readonly={vnode.attrs.disablePackageRepo}
                       errorText={vnode.attrs.package.packageRepo().errors().errorsForDisplay("id")}>
            <SelectFieldOptions selected={vnode.attrs.package.packageRepo().id()}
                                items={packageRepos}/>
          </SelectField>
        </Form>
      </FormHeader>

      <MaterialAutoUpdateToggle noun="package" toggle={vnode.attrs.package.autoUpdate} errors={vnode.attrs.package.errors()}/>

      <PluginView pluginSettings={(pluginInfo.extensions[0] as PackageRepoExtension).packageSettings}
                  configurations={vnode.attrs.package.configuration()}/>
    </div>;
  }

  private packageRepoIdProxy(vnode: m.Vnode<Attrs, this>, newPackageRepoId?: string) {
    if (!newPackageRepoId) {
      return vnode.attrs.package.packageRepo().id();
    }
    const packageRepo = _.find(vnode.attrs.packageRepositories, (pkgRepo: PackageRepository) => pkgRepo.repoId() === newPackageRepoId);
    return vnode.attrs.onPackageRepoChange(packageRepo);
  }
}

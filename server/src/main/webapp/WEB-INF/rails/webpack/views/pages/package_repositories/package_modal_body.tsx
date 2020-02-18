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
import _ from "lodash";
import m from "mithril";
import {Package, PackageRepositories, PackageRepository} from "models/package_repositories/package_repositories";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";

interface Attrs {
  pluginInfos: PluginInfos;
  packageRepositories: PackageRepositories;
  package: Package;
  disableId: boolean;
  onPackageRepoChange: (newPackageRepo?: PackageRepository) => any;
}

export class PackageModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const packageRepos = _.map(vnode.attrs.packageRepositories, (pkgRepo: PackageRepository) => {
      return {id: pkgRepo.repoId(), text: pkgRepo.name()};
    });
    const msgForNewPkg = vnode.attrs.disableId ? ""
      : <FlashMessage type={MessageType.info}
                      message={"The new package will be available to be used as material in all pipelines. Other admins might be able to edit this package."}/>;
    return <div>
      <FormHeader>
        {msgForNewPkg}
        <Form>
          <TextField label="Name"
                     readonly={vnode.attrs.disableId}
                     property={vnode.attrs.package.name}
                     placeholder={"Enter the package name"}
                     errorText={vnode.attrs.package.errors().errorsForDisplay("name")}
                     required={true}/>

          <SelectField label="Package Repository"
                       property={this.packageRepoIdProxy.bind(this, vnode)}
                       required={true}
                       errorText={vnode.attrs.package.packageRepo().errors().errorsForDisplay("id")}>
            <SelectFieldOptions selected={vnode.attrs.package.packageRepo().id()}
                                items={packageRepos}/>
          </SelectField>
        </Form>
      </FormHeader>

      <div>
        The plugin view will come here.
      </div>
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

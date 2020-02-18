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
import Stream from "mithril/stream";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {RequiresPluginInfos} from "views/pages/page_operations";
import {PackageOperations, PackageRepoOperations} from "views/pages/package_repositories";
import {PackageRepositoryWidget} from "./package_repository_widget";

interface Attrs extends RequiresPluginInfos {
  packageRepositories: Stream<PackageRepositories>;
  packageRepoOperations: PackageRepoOperations;
  packageOperations: PackageOperations;
}

export class PackageRepositoriesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div>
      {vnode.attrs.packageRepositories().map(packageRepo => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: packageRepo.pluginMetadata().id()});
        return <PackageRepositoryWidget packageRepository={packageRepo}
                                        packageOperations={vnode.attrs.packageOperations}
                                        disableActions={pluginInfo === undefined}
                                        onEdit={vnode.attrs.packageRepoOperations.onEdit}
                                        onClone={vnode.attrs.packageRepoOperations.onClone}
                                        onDelete={vnode.attrs.packageRepoOperations.onDelete}/>
      })}
    </div>;
  }
}

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
import {Packages} from "models/package_repositories/package_repositories";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {PackageOperations} from "views/pages/package_repositories";
import {PackageRepoScrollOptions} from "./package_repositories_widget";
import {PackageWidget} from "./package_widget";

interface Attrs {
  packageRepoName: string;
  packages: Stream<Packages>;
  packageOperations: PackageOperations;
  disableActions: boolean;
  scrollOptions: PackageRepoScrollOptions;
}

export class PackagesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const pkgRepoManager = vnode.attrs.scrollOptions.package_repo_sm.sm;
    const pkgManager     = vnode.attrs.scrollOptions.package_sm.sm;
    if (pkgRepoManager.getTarget() === vnode.attrs.packageRepoName && pkgManager.hasTarget()) {
      const target        = pkgManager.getTarget();
      const anchorPkgName = target.split('_')[1];
      if (!_.isEmpty(anchorPkgName)) {
        const hasAnchorElement = vnode.attrs.packages().some((pkg) => pkg.key() === target);
        if (!hasAnchorElement) {
          const msg = `'${anchorPkgName}' package has not been set up.`;
          return <FlashMessage dataTestId="anchor-package-not-present" type={MessageType.alert} message={msg}/>;
        }
      }
    }
    if (_.isEmpty(vnode.attrs.packages())) {
      return <FlashMessage type={MessageType.info}
                           message={"There are no packages defined in this package repository."}/>;
    }
    return <div data-test-id="packages-widget">
      <h4>Packages</h4>
      {vnode.attrs.packages().map((pkg) => {
        return <PackageWidget package={pkg}
                              scrollOptions={vnode.attrs.scrollOptions}
                              disableActions={vnode.attrs.disableActions}
                              onEdit={vnode.attrs.packageOperations.onEdit}
                              onClone={vnode.attrs.packageOperations.onClone}
                              onDelete={vnode.attrs.packageOperations.onDelete}
                              showUsages={vnode.attrs.packageOperations.showUsages}
        />;
      })}
    </div>;
  }
}

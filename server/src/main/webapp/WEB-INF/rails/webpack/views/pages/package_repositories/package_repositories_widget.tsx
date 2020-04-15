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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "views/components/link";
import {PackageOperations, PackageRepoOperations} from "views/pages/package_repositories";
import {RequiresPluginInfos} from "views/pages/page_operations";
import styles from "./index.scss";
import {PackageRepositoryScrollOptions, PackageRepositoryWidget} from "./package_repository_widget";
import {PackageScrollOptions} from "./package_widget";

interface Attrs extends RequiresPluginInfos {
  packageRepositories: Stream<PackageRepositories>;
  packageRepoOperations: PackageRepoOperations;
  packageOperations: PackageOperations;
  scrollOptions: PackageRepoScrollOptions;
}

export interface PackageRepoScrollOptions {
  package_repo_sm: PackageRepositoryScrollOptions;
  package_sm: PackageScrollOptions;
}

export class PackageRepositoriesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pkgRepoScrollOptions = vnode.attrs.scrollOptions.package_repo_sm;
    if (pkgRepoScrollOptions.sm.hasTarget()) {
      const target           = pkgRepoScrollOptions.sm.getTarget();
      const hasAnchorElement = vnode.attrs.packageRepositories().some((pkgRepo) => pkgRepo.name() === target);
      if (!hasAnchorElement) {
        const msg = `'${target}' package repository has not been set up.`;
        return <FlashMessage dataTestId="anchor-package-repo-not-present" type={MessageType.alert} message={msg}/>;
      }
    }
    if (_.isEmpty(vnode.attrs.packageRepositories())) {
      return <div className={styles.tips} data-test-id="package-repo-info">
        <ul>
          <li>Click on "Create Package Repository" to add new package repository.</li>
          <li>A package repository can be set up to use packages as a material in the pipelines. You can read more
            from <Link target="_blank" href={docsUrl("extension_points/package_repository_extension.html")}>here</Link>.
          </li>
        </ul>
      </div>;
    }
    return <div data-test-id="package-repositories-widget">
      {vnode.attrs.packageRepositories().map(packageRepo => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: packageRepo.pluginMetadata().id()});
        return <PackageRepositoryWidget packageRepository={packageRepo}
                                        packageOperations={vnode.attrs.packageOperations}
                                        disableActions={pluginInfo === undefined}
                                        onEdit={vnode.attrs.packageRepoOperations.onEdit}
                                        onClone={vnode.attrs.packageRepoOperations.onClone}
                                        onDelete={vnode.attrs.packageRepoOperations.onDelete}
                                        scrollOptions={vnode.attrs.scrollOptions}/>;
      })}
    </div>;
  }
}

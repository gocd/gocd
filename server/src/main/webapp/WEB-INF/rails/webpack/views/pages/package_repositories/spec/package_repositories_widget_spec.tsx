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
import m from "mithril";
import Stream from "mithril/stream";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {getPackageRepository, pluginInfoWithPackageRepositoryExtension} from "models/package_repositories/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PackageOperations, PackageRepoOperations} from "views/pages/package_repositories";
import {TestHelper} from "views/pages/spec/test_helper";
import {PackageRepositoriesWidget} from "../package_repositories_widget";

describe('PackageRepositoriesWidgetSpec', () => {
  const helper = new TestHelper();
  let pkgRepos: Stream<PackageRepositories>;
  let pluginInfos: Stream<PluginInfos>;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    pkgRepos    = Stream(PackageRepositories.fromJSON([getPackageRepository()]));
    pluginInfos = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension())));
  });

  function mount() {
    const pkgRepoOps = new PackageRepoOperations();
    const pkgOps     = new PackageOperations();
    helper.mount(() => <PackageRepositoriesWidget packageRepositories={pkgRepos}
                                                  pluginInfos={pluginInfos}
                                                  packageOperations={pkgOps}
                                                  packageRepoOperations={pkgRepoOps}/>);
  }

  it('should render info div if repos is empty', () => {
    pkgRepos(new PackageRepositories());
    mount();

    expect(helper.byTestId('package-repositories-widget')).not.toBeInDOM();
    const helpInfo = helper.byTestId('package-repo-info');
    expect(helpInfo).toBeInDOM();
    expect(helper.qa('li', helpInfo)[0].textContent).toBe('Click on "Create Package Repository" to add new package repository.');
    expect(helper.qa('li', helpInfo)[1].textContent).toBe('A package repository can be set up to use packages as a material in the pipelines. You can read more from here.');

    expect(helper.q('a', helpInfo)).toHaveAttr('href', docsUrl("extension_points/package_repository_extension.html"));

  });
});

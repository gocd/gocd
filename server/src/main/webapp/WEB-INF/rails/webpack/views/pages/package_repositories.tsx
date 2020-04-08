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

import {ErrorResponse} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PackagesCRUD} from "models/package_repositories/packages_crud";
import {Package, PackageRepositories, PackageRepository, PackageRepositorySummary, Packages} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {ExtensionTypeString, PackageRepoExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {ButtonIcon, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {NoPluginsOfTypeInstalled} from "views/components/no_plugins_installed";
import configRepoStyles from "views/pages/config_repos/index.scss";
import {PackageRepositoriesWidget} from "views/pages/package_repositories/package_repositories_widget";
import {
  ClonePackageRepositoryModal,
  CreatePackageRepositoryModal,
  DeletePackageRepositoryModal,
  EditPackageRepositoryModal
} from "views/pages/package_repositories/package_repository_modals";
import {Page, PageState} from "views/pages/page";
import {RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";
import {ClonePackageModal, CreatePackageModal, DeletePackageModal, EditPackageModal, UsagePackageModal} from "./package_repositories/package_modals";

export class PackageRepoOperations {
  onAdd: (e: MouseEvent) => void                            = () => _.noop;
  onEdit: (obj: PackageRepository, e: MouseEvent) => void   = () => _.noop;
  onClone: (obj: PackageRepository, e: MouseEvent) => void  = () => _.noop;
  onDelete: (obj: PackageRepository, e: MouseEvent) => void = () => _.noop;
}

export class PackageOperations {
  onAdd: (packageRepo: PackageRepository, e: MouseEvent) => void = () => _.noop;
  onEdit: (obj: Package, e: MouseEvent) => void                  = () => _.noop;
  onClone: (obj: Package, e: MouseEvent) => void                 = () => _.noop;
  onDelete: (obj: Package, e: MouseEvent) => void                = () => _.noop;
  showUsages: (obj: Package, e: MouseEvent) => void              = () => _.noop;
}

interface State extends RequiresPluginInfos, SaveOperation {
  packageRepositories: Stream<PackageRepositories>;
  packages: Stream<Packages>;
  packageRepoOperations: PackageRepoOperations;
  packageOperations: PackageOperations;
  searchText: Stream<string>;
}

export class PackageRepositoriesPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.searchText          = Stream();
    vnode.state.packageRepositories = Stream();
    vnode.state.packages            = Stream();
    vnode.state.pluginInfos         = Stream();

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    this.setPackageRepoOperations(vnode);
    this.setPackageOperations(vnode);
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    let noPluginMsg;
    if (!this.isPluginInstalled(vnode)) {
      noPluginMsg = <NoPluginsOfTypeInstalled extensionType={new PackageRepoExtensionType()}/>;
    }

    const mergedPackageRepos: Stream<PackageRepositories>   = Stream(this.getMergedList(vnode));
    const filteredPackageRepos: Stream<PackageRepositories> = Stream();
    if (vnode.state.searchText()) {
      const results = _.filter(mergedPackageRepos(), (vm: PackageRepository) => vm.matches(vnode.state.searchText()));

      if (_.isEmpty(results)) {
        return <div>
          {noPluginMsg}
          <FlashMessage type={MessageType.info}>No Results for the search
            string: <em>{vnode.state.searchText()}</em></FlashMessage>
        </div>;
      }
      filteredPackageRepos(results);
    } else {
      filteredPackageRepos(mergedPackageRepos());
    }

    return <div>
      {noPluginMsg}
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <PackageRepositoriesWidget packageRepositories={filteredPackageRepos}
                                 pluginInfos={vnode.state.pluginInfos}
                                 packageRepoOperations={vnode.state.packageRepoOperations}
                                 packageOperations={vnode.state.packageOperations}/>
    </div>;
  }

  pageName(): string {
    return "Package Repositories";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PackageRepositoriesCRUD.all(), PackagesCRUD.all(), PluginInfoCRUD.all({type: ExtensionTypeString.PACKAGE_REPO})])
                  .then((result) => {
                    result[0].do((successResponse) => {
                      vnode.state.packageRepositories(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    });

                    result[1].do((successResponse) => {
                      vnode.state.packages(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    });

                    result[2].do((successResponse) => {
                      vnode.state.pluginInfos(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    });
                  });

  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const buttons = [
      <Primary icon={ButtonIcon.ADD} disabled={!this.isPluginInstalled(vnode)}
               onclick={vnode.state.packageRepoOperations.onAdd}>
        Create Package Repository
      </Primary>
    ];
    if (!_.isEmpty(vnode.state.packageRepositories())) {
      const searchBox = <div className={configRepoStyles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} dataTestId={"search-box"}
                     placeholder="Search for a package repository or a package"/>
      </div>;
      buttons.splice(0, 0, searchBox);
    }
    return <HeaderPanel title={this.pageName()} buttons={buttons}/>;
  }

  private getMergedList(vnode: m.Vnode<null, State>): PackageRepositories {
    const pkgRepos = vnode.state.packageRepositories();

    if (!vnode.state.packages || _.isEmpty(vnode.state.packages())) {
      return pkgRepos;
    }

    pkgRepos.forEach((pkgRepo) => {
      const pkgsForRepo = vnode.state.packages()
                               .filter((pkg) => pkg.packageRepo().id() === pkgRepo.repoId());

      pkgRepo.packages().forEach((pkg) => {
        const completePkg = pkgsForRepo.find((p) => p.id() === pkg.id());
        if (completePkg !== undefined) {
          pkg.autoUpdate(completePkg.autoUpdate());
          pkg.configuration(completePkg.configuration());
          pkg.packageRepo(new PackageRepositorySummary(pkgRepo.repoId(), pkgRepo.name()));
        }
      });
    });

    return pkgRepos;
  }

  private isPluginInstalled(vnode: m.Vnode<null, State>) {
    return vnode.state.pluginInfos().length !== 0;
  }

  private setPackageRepoOperations(vnode: m.Vnode<null, State>) {
    vnode.state.packageRepoOperations = new PackageRepoOperations();

    vnode.state.packageRepoOperations.onAdd = (e: MouseEvent) => {
      e.stopPropagation();

      const pluginId          = vnode.state.pluginInfos()[0].id;
      const packageRepository = PackageRepository.default();
      packageRepository.pluginMetadata().id(pluginId);
      new CreatePackageRepositoryModal(packageRepository, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageRepoOperations.onEdit = (pkgRepo: PackageRepository, e: MouseEvent) => {
      e.stopPropagation();
      const copy = new PackageRepository(pkgRepo.repoId(), pkgRepo.name(), pkgRepo.pluginMetadata(), pkgRepo.configuration(), []);
      new EditPackageRepositoryModal(copy, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageRepoOperations.onClone = (pkgRepo: PackageRepository, e: MouseEvent) => {
      e.stopPropagation();
      new ClonePackageRepositoryModal(pkgRepo, vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageRepoOperations.onDelete = (pkgRepo: PackageRepository, e: MouseEvent) => {
      e.stopPropagation();

      new DeletePackageRepositoryModal(pkgRepo, vnode.state.onSuccessfulSave)
        .render();
    };
  }

  private setPackageOperations(vnode: m.Vnode<null, State>) {
    vnode.state.packageOperations = new PackageOperations();

    vnode.state.packageOperations.onAdd = (packageRepo: PackageRepository, e: MouseEvent) => {
      e.stopPropagation();

      const pkg = Package.default();
      pkg.packageRepo().id(packageRepo.repoId());
      new CreatePackageModal(pkg, vnode.state.packageRepositories(), vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageOperations.onEdit = (pkg: Package, e: MouseEvent) => {
      e.stopPropagation();
      const copy = new Package(pkg.id(), pkg.name(), pkg.autoUpdate(), pkg.configuration(), pkg.packageRepo());
      new EditPackageModal(copy, vnode.state.packageRepositories(), vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageOperations.onClone = (pkg: Package, e: MouseEvent) => {
      e.stopPropagation();

      new ClonePackageModal(pkg, vnode.state.packageRepositories(), vnode.state.pluginInfos(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageOperations.onDelete = (pkg: Package, e: MouseEvent) => {
      e.stopPropagation();

      new DeletePackageModal(pkg, vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.packageOperations.showUsages = (pkg: Package, e: MouseEvent) => {
      e.stopPropagation();

      PackagesCRUD.usages(pkg.id())
                  .then((result) => {
                    result.do(
                      (successResponse) => {
                        new UsagePackageModal(pkg.name(), successResponse.body).render();
                      },
                      this.onOperationError(vnode)
                    );
                  });
    };
  }

  private onOperationError(vnode: m.Vnode<null, State>): (errorResponse: ErrorResponse) => void {
    return (errorResponse: ErrorResponse) => {
      vnode.state.onError(JSON.parse(errorResponse.body!).message);
    };
  }

}

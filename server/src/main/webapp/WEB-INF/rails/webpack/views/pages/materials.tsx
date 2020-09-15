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

import {AjaxPoller} from "helpers/ajax_poller";
import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialAPIs, Materials, MaterialWithFingerprint, MaterialWithModification, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {MaterialsWidget} from "views/pages/materials/materials_widget";
import {Page, PageState} from "views/pages/page";
import configRepoStyles from "./config_repos/index.scss";
import {ShowModificationsModal, ShowUsagesModal} from "./materials/modal";

export interface AdditionalInfoAttrs {
  triggerUpdate: (material: MaterialWithModification, e: MouseEvent) => void;
  onEdit: (material: MaterialWithFingerprint, e: MouseEvent) => void;
  showUsages: (material: MaterialWithFingerprint, e: MouseEvent) => void;
  showModifications: (material: MaterialWithFingerprint, e: MouseEvent) => void;
  shouldShowPackageOrScmLink: boolean;
}

export interface MaterialsAttrs extends AdditionalInfoAttrs {
  materials: Stream<Materials>;
}

interface State extends MaterialsAttrs {
  searchText: Stream<string>;
}

export class MaterialsPage extends Page<null, State> {
  etag: Stream<string> = Stream();

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.materials  = Stream();
    vnode.state.searchText = Stream();

    vnode.state.triggerUpdate = (materialWithMod: MaterialWithModification, e: MouseEvent) => {
      e.stopPropagation();
      materialWithMod.materialUpdateInProgress = true;
      const material                           = materialWithMod.config;
      MaterialAPIs.triggerUpdate(material.fingerprint())
                  .then((result) => {
                    result.do(() => {
                      this.flashMessage.success(`An update was scheduled for '${material.displayName()}' material.`);
                    }, (err) => {
                      this.flashMessage.alert(`Unable to schedule an update for '${material.displayName()}' material. ${err.message}`);
                      materialWithMod.materialUpdateInProgress = false;
                    });
                  })
                  .finally(() => {
                    this.etag = Stream();  // flush etag so that the next API call re-renders the page
                  });
    };

    vnode.state.onEdit = (material: MaterialWithFingerprint, e: MouseEvent) => {
      e.stopPropagation();
      const materialType = material.type();
      switch (materialType) {
        case "package":
          const pkgAttrs = material.attributes() as PackageMaterialAttributes;
          window.open(`${SparkRoutes.packageRepositoriesSPA(pkgAttrs.packageRepoName(), pkgAttrs.packageName())}/edit`);
          break;
        case "plugin":
          const pluginAttrs = material.attributes() as PluggableScmMaterialAttributes;
          window.open(`${SparkRoutes.pluggableScmSPA(pluginAttrs.scmName())}/edit`);
          break;
      }
    };

    vnode.state.showUsages = (material: MaterialWithFingerprint, e: MouseEvent) => {
      e.stopPropagation();
      new ShowUsagesModal(material).render();
    };

    vnode.state.showModifications = (material: MaterialWithFingerprint, e: MouseEvent) => {
      e.stopPropagation();
      new ShowModificationsModal(material).render();
    };

    new AjaxPoller({repeaterFn: this.refreshMaterials.bind(this, vnode), initialIntervalSeconds: 10}).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const filteredMaterials: Stream<Materials> = Stream(vnode.state.materials());
    if (vnode.state.searchText()) {
      const results = _.filter(filteredMaterials(), (vm: MaterialWithModification) => vm.matches(vnode.state.searchText()));

      if (_.isEmpty(results)) {
        return <div>
          <FlashMessage type={MessageType.info}>No Results for the search
            string: <em>{vnode.state.searchText()}</em></FlashMessage>
        </div>;
      }
      filteredMaterials(new Materials(...results));
    }
    return [
      <FlashMessage key={this.flashMessage.type} type={this.flashMessage.type} message={this.flashMessage.message}/>
      , <MaterialsWidget key={filteredMaterials().length} materials={filteredMaterials}
                         shouldShowPackageOrScmLink={Page.isUserAnAdmin() || Page.isUserAGroupAdmin()}
                         triggerUpdate={vnode.state.triggerUpdate} onEdit={vnode.state.onEdit}
                         showModifications={vnode.state.showModifications} showUsages={vnode.state.showUsages}/>
    ];
  }

  pageName(): string {
    return "Materials";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.resolve(MaterialAPIs.all(this.etag()))
                  .then((args) => this.onMaterialsAPIResponse(args, vnode));
  }

  helpText(): m.Children {
    return MaterialsWidget.helpText();
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const buttons = [];
    if (!_.isEmpty(vnode.state.materials())) {
      const searchBox = <div className={configRepoStyles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} dataTestId={"search-box"}
                     placeholder="Search for a material name or url"/>
      </div>;
      buttons.push(searchBox);
    }
    return <HeaderPanel title={this.pageName()} buttons={buttons} help={this.helpText()}/>;
  }

  private refreshMaterials(vnode: m.Vnode<null, State>) {
    return Promise.resolve(MaterialAPIs.all(this.etag()))
                  .then((args) => this.onMaterialsAPIResponse(args, vnode));
  }

  private onMaterialsAPIResponse(apiResult: ApiResult<Materials>, vnode: m.Vnode<null, State>) {
    if (apiResult.getStatusCode() === 304) {
      return;
    }
    if (apiResult.getEtag()) {
      this.etag(apiResult.getEtag()!);
    }

    apiResult.do((successResponse) => {
      this.pageState = PageState.OK;
      vnode.state.materials(successResponse.body);
      vnode.state.materials().sortOnType();
    }, (errorResponse: ErrorResponse) => {
      this.flashMessage.alert(errorResponse.message);
      if (errorResponse.body) {
        this.flashMessage.alert(JSON.parse(errorResponse.body).message);
      }
      this.pageState = PageState.OK;
    });
  }
}

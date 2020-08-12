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

import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialAPIs, MaterialWithFingerprint, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {MaterialsWidget} from "views/pages/materials/materials_widget";
import {MaterialVM, MaterialVMs} from "views/pages/materials/models/material_view_model";
import {Page, PageState} from "views/pages/page";
import configRepoStyles from "./config_repos/index.scss";
import {ShowModificationsModal} from "./materials/modal";

export interface AdditionalInfoAttrs {
  onEdit: (material: MaterialWithFingerprint, e: MouseEvent) => void;
  showModifications: (material: MaterialWithFingerprint, e: MouseEvent) => void;
  shouldShowPackageOrScmLink: boolean;
}

export interface MaterialsAttrs extends AdditionalInfoAttrs {
  materialVMs: Stream<MaterialVMs>;
}

interface State extends MaterialsAttrs {
  searchText: Stream<string>;
}

export class MaterialsPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.materialVMs = Stream();
    vnode.state.searchText  = Stream();

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

    vnode.state.showModifications = (material: MaterialWithFingerprint, e: MouseEvent) => {
      e.stopPropagation();
      new ShowModificationsModal(material).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const filteredMaterials: Stream<MaterialVMs> = Stream(vnode.state.materialVMs());
    if (vnode.state.searchText()) {
      const results = _.filter(filteredMaterials(), (vm: MaterialVM) => vm.matches(vnode.state.searchText()));

      if (_.isEmpty(results)) {
        return <div>
          <FlashMessage type={MessageType.info}>No Results for the search
            string: <em>{vnode.state.searchText()}</em></FlashMessage>
        </div>;
      }
      filteredMaterials(new MaterialVMs(...results));
    }
    return [<MaterialsWidget key={filteredMaterials().length} materialVMs={filteredMaterials}
                             shouldShowPackageOrScmLink={Page.isUserAnAdmin() || Page.isUserAGroupAdmin()}
                             onEdit={vnode.state.onEdit} showModifications={vnode.state.showModifications}/>];
  }

  pageName(): string {
    return "Materials";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.all([MaterialAPIs.all()]).then((result) => {
      result[0].do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.materialVMs(MaterialVMs.fromMaterials(successResponse.body));
        vnode.state.materialVMs().sortOnType();
      }, this.setErrorState);
    });
  }

  helpText(): m.Children {
    return MaterialsWidget.helpText();
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const buttons = [];
    if (!_.isEmpty(vnode.state.materialVMs())) {
      const searchBox = <div className={configRepoStyles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} dataTestId={"search-box"}
                     placeholder="Search for a material name or url"/>
      </div>;
      buttons.push(searchBox);
    }
    return <HeaderPanel title={this.pageName()} buttons={buttons} help={this.helpText()}/>;
  }
}

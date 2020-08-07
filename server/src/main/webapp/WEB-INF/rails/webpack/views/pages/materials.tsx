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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialAPIs} from "models/materials/materials";
import {Scms} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {PackagesCRUD} from "models/package_repositories/packages_crud";
import {Packages} from "models/package_repositories/package_repositories";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {MaterialsWidget} from "views/pages/materials/materials_widget";
import {MaterialVM, MaterialVMs} from "views/pages/materials/models/material_view_model";
import {Page, PageState} from "views/pages/page";
import configRepoStyles from "./config_repos/index.scss";

export interface AdditionalInfoAttrs {
  scms: Stream<Scms>;
  packages: Stream<Packages>;
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
    vnode.state.scms        = Stream();
    vnode.state.packages    = Stream();
    vnode.state.searchText  = Stream();
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
    return [<MaterialsWidget key={filteredMaterials().length} materialVMs={filteredMaterials} scms={vnode.state.scms} packages={vnode.state.packages}/>];
  }

  pageName(): string {
    return "Materials";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.all([MaterialAPIs.all(), PluggableScmCRUD.all(), PackagesCRUD.all()]).then((result) => {
      result[0].do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.materialVMs(MaterialVMs.fromMaterials(successResponse.body));
        vnode.state.materialVMs().sortOnType();
      }, this.setErrorState);

      result[1].do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.scms(successResponse.body);
      }, this.setErrorState);

      result[2].do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.packages(successResponse.body);
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

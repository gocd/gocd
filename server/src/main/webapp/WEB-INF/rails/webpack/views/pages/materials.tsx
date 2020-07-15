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
import {MaterialAPIs, MaterialWithFingerprint, MaterialWithFingerprints} from "models/materials/materials";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {MaterialsWidget} from "views/pages/materials/materials_widget";
import {Page, PageState} from "views/pages/page";
import configRepoStyles from "./config_repos/index.scss";

export interface MaterialsAttrs {
  materials: Stream<MaterialWithFingerprints>;
}

interface State extends MaterialsAttrs {
  searchText: Stream<string>;
}

export class MaterialsPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.materials  = Stream();
    vnode.state.searchText = Stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const filteredMaterials: Stream<MaterialWithFingerprints> = Stream(vnode.state.materials());
    if (vnode.state.searchText()) {
      const results = _.filter(filteredMaterials(), (vm: MaterialWithFingerprint) => vm.matches(vnode.state.searchText()));

      if (_.isEmpty(results)) {
        return <div>
          <FlashMessage type={MessageType.info}>No Results for the search
            string: <em>{vnode.state.searchText()}</em></FlashMessage>
        </div>;
      }
      filteredMaterials(new MaterialWithFingerprints(...results));
    }
    return <MaterialsWidget materials={filteredMaterials}/>;
  }

  pageName(): string {
    return "Materials";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.resolve(MaterialAPIs.all()).then((result) => {
      result.do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.materials(successResponse.body);
        vnode.state.materials().sortOnType();
      }, this.setErrorState);
    });
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
}

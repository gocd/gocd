/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import m from "mithril";
import {Compare} from "models/compare/compare";
import {FlashMessage} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {CompareWidget} from "views/pages/compare/compare_widget";
import {Page, PageState} from "views/pages/page";
import {CompareHeaderWidget} from "./compare/compare_header_widget";

interface State {
  dummy?: Compare;
}

export class ComparePage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (this.pageState === PageState.FAILED) {
      return <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>;
    }
    return <CompareWidget/>;
  }

  pageName(): string {
    return "Compare";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    // to be implemented
    return Promise.resolve();
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const sectionName = <CompareHeaderWidget pipelineName={this.getMeta().pipelineName}/>;

    return <HeaderPanel title={sectionName} sectionName={this.pageName()}/>;
  }
}

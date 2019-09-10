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
import Stream from "mithril/stream";
import {Environments} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {Page, PageState} from "views/pages/page";

export class NewEnvironmentsPage extends Page<null, {}> {
  private readonly environments: Stream<Environments> = Stream(new Environments());

  componentToDisplay(vnode: m.Vnode<null, {}>): m.Children {
    return <EnvironmentsWidget environments={this.environments}/>;
  }

  pageName(): string {
    return "Environments";
  }

  fetchData(vnode: m.Vnode<null, {}>): Promise<any> {
    return EnvironmentsAPIs.all().then((result) =>
                                         result.do((successResponse) => {
                                           this.pageState = PageState.OK;
                                           this.environments(successResponse.body);
                                         }, this.setErrorState));
  }
}

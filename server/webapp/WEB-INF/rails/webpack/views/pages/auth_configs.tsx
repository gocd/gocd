/*
* Copyright 2018 ThoughtWorks, Inc.
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

import * as m from "mithril";
import {AuthConfigs} from "models/auth_configs/auth_configs";
import * as Buttons from "views/components/buttons";
import {HeaderPanel} from "views/components/header_panel";
import {AuthConfigsWidget} from "views/pages/auth_configs/auth_configs_widget";
import {Page, PageState} from "views/pages/page";
import {AddOperation} from "views/pages/page_operations";

interface State extends AddOperation<AuthConfigs> {
  authConfigs: AuthConfigs;
}

export class AuthConfigsPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.onAdd = (e: Event) => {
      alert("This alert");
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <AuthConfigsWidget/>;
  }

  pageName(): string {
    return "Authorization Configurations";
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const headerButtons = [
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}
                       data-test-id="add-auth-config-button">Add</Buttons.Primary>
    ];
    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.OK;
    // to be implemented
    return Promise.resolve();
  }
}

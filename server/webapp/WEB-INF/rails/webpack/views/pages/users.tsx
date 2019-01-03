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
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {UserJSON, Users} from "models/users/users";
import {UsersCRUD} from "models/users/users_crud";
import * as Buttons from "views/components/buttons";
import {HeaderPanel} from "views/components/header_panel";
import {Page, PageState} from "views/pages/page";
import {AddOperation} from "views/pages/page_operations";
import {UserSearchModal} from "views/pages/users/add_user_modal";
import {UsersWidget} from "views/pages/users/users_widget";

interface State extends AddOperation<UserJSON> {
  users: Stream<Users>;
}

export class UsersPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.users = stream(new Users([]));
    vnode.state.onAdd = (e) => {
      e.stopPropagation();
      new UserSearchModal(this.flashMessage, this.fetchData.bind(this, vnode)).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <UsersWidget users={vnode.state.users} message={this.flashMessage}/>;
  }

  pageName(): string {
    return "User summary";
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [];
    headerButtons.push(<Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add User</Buttons.Primary>);

    return <HeaderPanel title="Users Management" buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([UsersCRUD.all()]).then((args) => {
      const apiResult = args[0];
      apiResult.do((successResponse) => {
                     vnode.state.users(successResponse.body);
                     this.pageState = PageState.OK;
                   }, (errorResponse) => {
                     // vnode.state.onError(errorResponse.message);
                     this.pageState = PageState.FAILED;
                   }
      );
    });
  }
}

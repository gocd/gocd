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
import {Users} from "models/users/users";
import {UsersWidget} from "views/pages/users/users_widget";
import {Page} from "views/pages/page";

interface State {
  dummy?: Users;
}

export class UsersPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <UsersWidget/>;
  }

  pageName(): string {
    return "SPA Name goes here!";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    // to be implemented
    return Promise.resolve();
  }
}

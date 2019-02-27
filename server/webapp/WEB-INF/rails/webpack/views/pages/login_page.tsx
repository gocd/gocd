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

import * as m from "mithril";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import {Credentials, LoginPageWidget} from "views/pages/login_page/login_page_widget";
import {Page} from "views/pages/page";

export class LoginPage extends Page<null, { credentials: Credentials }> {
  oninit(vnode: m.Vnode<null, { credentials: Credentials }>) {
    super.oninit(vnode);
    vnode.state.credentials = new Credentials();
  }

  view(vnode: m.Vnode<null, { credentials: Credentials }>) {
    return (
      <div style="margin: auto; max-width: 50%;">
        <LoginPageWidget {...this.getMeta()}
                         credentials={vnode.state.credentials}
                         submitCallback={this.submitCredentials}/>
      </div>
    );
  }

  fetchData(vnode: m.Vnode<null, { credentials: Credentials }>): Promise<any> {
    return Promise.resolve();
  }

  pageName(): string {
    throw new Error("Unsupported!");
  }

  componentToDisplay(vnode: m.Vnode<null, { credentials: Credentials }>): m.Children {
    throw new Error("Unsupported!");
  }

  protected getMeta(): AuthPluginInfo {
    return super.getMeta() as AuthPluginInfo;
  }

  private submitCredentials(credentials: Credentials) {
    if (!credentials.isValid()) {
      return;
    }

    // create a "dummy" form and `submit()` it, since the endpoint does not understand JSON, yet :)
    const form         = document.createElement("form");
    form.style.display = "none";
    form.setAttribute("action", "/go/auth/security_check");
    form.setAttribute("method", "post");

    const usernameField = document.createElement("input");
    usernameField.setAttribute("type", "text");
    usernameField.setAttribute("name", "j_username");
    usernameField.value = credentials.username();

    const passwordField = document.createElement("input");
    passwordField.setAttribute("type", "password");
    passwordField.setAttribute("name", "j_password");
    passwordField.value = credentials.password();

    form.append(usernameField, passwordField);
    document.body.appendChild(form);
    form.submit();
    form.remove();
  }
}

/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import {LogoutPageWidget} from "views/pages/logout_page/logout_page_widget";
import {Page} from "views/pages/page";

export class LogoutPagePage extends Page {
  view(vnode: m.Vnode): m.Children {
    return <LogoutPageWidget {...this.getMeta()}/>;
  }

  pageName(): string {
    throw new Error("Unsupported!");
  }

  componentToDisplay(vnode: m.Vnode): m.Children {
    throw new Error("Unsupported!");
  }

  fetchData(vnode: m.Vnode): Promise<any> {
    return Promise.resolve();
  }

  protected getMeta(): AuthPluginInfo {
    return super.getMeta() as AuthPluginInfo;
  }

}

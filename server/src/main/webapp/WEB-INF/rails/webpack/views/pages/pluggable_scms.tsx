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

import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {MessageType} from "views/components/flash_message";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Page, PageState} from "views/pages/page";
import {PluggableScmsWidget} from "views/pages/pluggable_scms/pluggable_scms_widget";
import {RequiresPluginInfos} from "./page_operations";

interface State extends RequiresPluginInfos {
  scms: Stream<Scms>;
}

export class PluggableScmsPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.scms        = Stream();
    vnode.state.pluginInfos = Stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PluggableScmsWidget scms={vnode.state.scms} pluginInfos={vnode.state.pluginInfos}/>;
  }

  pageName(): string {
    return "Pluggable SCMs";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([PluggableScmCRUD.all(), PluginInfoCRUD.all({type: ExtensionTypeString.SCM})])
                  .then((result) => {
                    result[0].do((successResponse) => {
                      vnode.state.scms(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    });

                    result[1].do((successResponse) => {
                      vnode.state.pluginInfos(successResponse.body);
                      this.pageState = PageState.OK;
                    }, (errorResponse) => {
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      this.pageState = PageState.FAILED;
                    })
                  });
  }
}

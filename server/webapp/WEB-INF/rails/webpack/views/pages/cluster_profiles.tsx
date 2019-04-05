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
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ClusterProfiles} from "models/cluster_profiles/cluster_profiles";
import {ClusterProfilesCRUD} from "models/cluster_profiles/cluster_profiles_crud";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {ClusterProfilesWidget} from "views/pages/cluster_profiles/cluster_profiles_widget";
import {Page, PageState} from "views/pages/page";

interface State {
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  clusterProfiles: Stream<ClusterProfiles>;
}

export class ClusterProfilesPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.clusterProfiles = stream(new ClusterProfiles());
    vnode.state.pluginInfos     = stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <ClusterProfilesWidget clusterProfiles={vnode.state.clusterProfiles()}
                                  pluginInfos={vnode.state.pluginInfos}/>;
  }

  pageName(): string {
    return "Cluster Profiles";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([
                         ClusterProfilesCRUD.all(),
                         PluginInfoCRUD.all({type: ExtensionType.ELASTIC_AGENTS})
                       ])
                  .then((results) => {
                    results[0].do(
                      (successResponse) => {
                        this.pageState = PageState.OK;
                        vnode.state.clusterProfiles(successResponse.body);
                      },
                      () => this.setErrorState()
                    );
                    results[1].do(
                      (successResponse) => {
                        this.pageState = PageState.OK;
                        vnode.state.pluginInfos(successResponse.body);
                      },
                      () => this.setErrorState()
                    );
                  });
  }
}

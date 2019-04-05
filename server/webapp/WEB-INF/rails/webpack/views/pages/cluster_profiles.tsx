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

import {ErrorResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {ClusterProfile, ClusterProfiles} from "models/cluster_profiles/cluster_profiles";
import {ClusterProfilesCRUD} from "models/cluster_profiles/cluster_profiles_crud";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ClusterProfilesWidget} from "views/pages/cluster_profiles/cluster_profiles_widget";
import {EditClusterProfileModal, NewClusterProfileModal} from "views/pages/cluster_profiles/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, DeleteOperation, EditOperation, SaveOperation} from "views/pages/page_operations";

interface State extends SaveOperation, AddOperation<void>, EditOperation<ClusterProfile>, DeleteOperation<string> {
  onSuccessfulSave: (msg: m.Children) => void;
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  clusterProfiles: Stream<ClusterProfiles>;
}

export class ClusterProfilesPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.clusterProfiles = stream(new ClusterProfiles());
    vnode.state.pluginInfos     = stream();

    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(errorResponse.message);
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new NewClusterProfileModal(vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onEdit = (clusterProfile: ClusterProfile, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new EditClusterProfileModal(clusterProfile.id(),
                                  vnode.state.pluginInfos(),
                                  vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onDelete = (id: string, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      const deleteConfirmMsg = (
        <span>
          Are you sure you want to delete the cluster profile <strong>{id}</strong>?
        </span>
      );

      const modal = new DeleteConfirmModal(
        deleteConfirmMsg,
        () => {
          ClusterProfilesCRUD.delete(id)
                             .then((result) => {
                               result.do(
                                 () => vnode.state.onSuccessfulSave(
                                   <span>The cluster profile <em>{id}</em> was deleted successfully!</span>
                                 ),
                                 onOperationError
                               );
                             })
                             .finally(modal.close.bind(modal));
        });
      modal.render();
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return [
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>,
      <ClusterProfilesWidget clusterProfiles={vnode.state.clusterProfiles()}
                             pluginInfos={vnode.state.pluginInfos}
                             onEdit={vnode.state.onEdit.bind(vnode.state)}
                             onDelete={vnode.state.onDelete.bind(vnode.state)}/>];
  }

  pageName(): string {
    return "Cluster Profiles";
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [];
    const hasPlugins    = vnode.state.pluginInfos() && vnode.state.pluginInfos().length > 0;
    headerButtons.push(<Buttons.Primary disabled={!hasPlugins} onclick={vnode.state.onAdd.bind(vnode.state)}>
      <span title={!hasPlugins ? "Install some elastic agent plugins to add an cluster profile." : undefined}>Add</span>
    </Buttons.Primary>);

    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
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

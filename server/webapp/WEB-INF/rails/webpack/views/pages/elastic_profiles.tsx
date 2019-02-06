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

import {ErrorResponse} from "helpers/api_request_builder";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ElasticProfilesCRUD} from "models/elastic_profiles/elastic_profiles_crud";
import {ElasticProfile, ElasticProfiles} from "models/elastic_profiles/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ElasticProfilesWidget} from "views/pages/elastic_profiles/elastic_profiles_widget";
import {
  CloneElasticProfileModal,
  EditElasticProfileModal,
  NewElasticProfileModal, UsageElasticProfileModal
} from "views/pages/elastic_profiles/modals";
import {
  AddOperation,
  CloneOperation,
  DeleteOperation,
  EditOperation,
  SaveOperation
} from "views/pages/page_operations";
import {Page, PageState} from "./page";

export interface RequiresPluginInfos {
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
}

export interface State extends RequiresPluginInfos, SaveOperation, EditOperation<ElasticProfile>, CloneOperation<ElasticProfile>, DeleteOperation<string>, AddOperation<void> {
  onShowUsages: (profileId: string, event: MouseEvent) => void;
  elasticProfiles: ElasticProfiles;
}

export class ElasticProfilesPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.pluginInfos     = stream();
    vnode.state.elasticProfiles = new ElasticProfiles([]);

    this.fetchData(vnode);

    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(errorResponse.message);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      new NewElasticProfileModal(vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onClone = (elasticProfile: ElasticProfile, event: MouseEvent) => {
      event.stopPropagation();
      this.flashMessage.clear();

      new CloneElasticProfileModal(elasticProfile.id(), vnode.state.pluginInfos(),
                                   vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onEdit = (elasticProfile: ElasticProfile, event: MouseEvent) => {
      event.stopPropagation();
      this.flashMessage.clear();

      new EditElasticProfileModal(elasticProfile.id(),
                                  vnode.state.pluginInfos(),
                                  vnode.state.onSuccessfulSave).render();
    };

    vnode.state.onDelete = (id: string, event: MouseEvent) => {
      event.stopPropagation();
      this.flashMessage.clear();

      const deleteConfirmMsg = (
        <span>
          Are you sure you want to delete the elastic profile <strong>{id}</strong>?
        </span>
      );

      const modal = new DeleteConfirmModal(
        deleteConfirmMsg,
        () => {
          ElasticProfilesCRUD.delete(id)
                             .then((result) => {
                               result.do(
                                 () => vnode.state.onSuccessfulSave(
                                   <span>The elastic profile <em>{id}</em> was deleted successfully!</span>
                                 ),
                                 onOperationError
                               );
                             })
                             .finally(modal.close.bind(modal));
        });
      modal.render();
    };

    vnode.state.onShowUsages = (id: string, event: MouseEvent) => {
      event.stopPropagation();
      this.flashMessage.clear();

      ElasticProfilesCRUD.usage(id).then((result) => {
        result.do(
          (successResponse) => {
            new UsageElasticProfileModal(id, successResponse.body).render();
          },
          onOperationError
        );
      });
    };
  }

  pageName() {
    return "Elastic Profiles";
  }

  componentToDisplay(vnode: m.Vnode<null, State>) {
    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <ElasticProfilesWidget
        elasticProfiles={vnode.state.elasticProfiles}
        pluginInfos={vnode.state.pluginInfos}
        onEdit={vnode.state.onEdit.bind(vnode.state)}
        onClone={vnode.state.onClone.bind(vnode.state)}
        onDelete={vnode.state.onDelete.bind(vnode.state)}
        onShowUsages={vnode.state.onShowUsages.bind(vnode.state)}
        isUserAnAdmin={ElasticProfilesPage.isUserAnAdmin()}/>
    </div>;
  }

  headerPanel(vnode: m.Vnode<null, State>) {
    const headerButtons = [];
    const hasPlugins    = vnode.state.pluginInfos() && vnode.state.pluginInfos().length > 0;
    headerButtons.push(<Buttons.Primary disabled={!hasPlugins} onclick={vnode.state.onAdd.bind(vnode.state)}>
      <span title={!hasPlugins ? "Install some elastic agent plugins to add an elastic profile." : undefined}>Add</span>
    </Buttons.Primary>);

    return <HeaderPanel title="Elastic Profiles" buttons={headerButtons}/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return Promise.all([
                         PluginInfoCRUD.all({type: ExtensionType.ELASTIC_AGENTS}),
                         ElasticProfilesCRUD.all()
                       ]).then((results) => {
      results[0].do(
        (successResponse) => {
          this.pageState = PageState.OK;
          vnode.state.pluginInfos(successResponse.body);
        },
        () => this.setErrorState()
      );
      results[1].do(
        (successReponse) => {
          this.pageState              = PageState.OK;
          vnode.state.elasticProfiles = successReponse.body;
        },
        () => this.setErrorState()
      );
    });
  }

  private static isUserAnAdmin() {
    const attribute = document.body.getAttribute("data-is-user-admin");
    return attribute ? attribute === "true" : false;
  }
}

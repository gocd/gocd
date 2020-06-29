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

import {JsonUtils} from "helpers/json_utils";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroups} from "models/internal_pipeline_structure/pipeline_structure";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {CurrentUser} from "models/new_preferences/current_user";
import {CurrentUserCRUD} from "models/new_preferences/current_user_crud";
import {NotificationFilter} from "models/new_preferences/notification_filters";
import {NotificationFiltersCRUD} from "models/new_preferences/notification_filters_crud";
import {CurrentUserVM, NotificationFilterVMs, PreferenceVM} from "models/new_preferences/preferences";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "views/components/link";
import {Page, PageState} from "views/pages/page";
import {CreateNotificationFilterModal, EditNotificationFilterModal} from "views/pages/preferences/modals";
import {PreferencesWidget, Sections} from "views/pages/preferences/preferences_widget";
import {ConfirmModal} from "views/pages/server-configuration/confirm_modal";

export interface NotificationsAttrs {
  notificationVMs: Stream<NotificationFilterVMs>;
  pipelineGroups: Stream<PipelineGroups>;

  onAddFilter: (e: MouseEvent) => void;
  onEditFilter: (filter: NotificationFilter, e: MouseEvent) => void;
  onDeleteFilter: (filter: NotificationFilter, e: MouseEvent) => void;
}

export interface EmailSettingsAttrs {
  currentUserVM: Stream<CurrentUserVM>;
  onCancel: (entity: CurrentUserVM, e: MouseEvent) => void;
  onSaveEmailSettings: (entity: CurrentUserVM, e: MouseEvent) => Promise<void | CurrentUser>;
}

export interface Routing {
  activeConfiguration: Sections;
  route: (activeConfiguration: Sections, preferenceVM: PreferenceVM) => void;
}

export interface PreferencesState extends Routing, NotificationsAttrs, EmailSettingsAttrs {
  onFilterSave: (msg: m.Children) => void;
}

export class NewPreferencesPage extends Page<null, PreferencesState> {
  oninit(vnode: m.Vnode<null, PreferencesState>) {
    vnode.state.activeConfiguration = m.route.param().configuration || Sections.MY_NOTIFICATIONS;
    super.oninit(vnode);

    vnode.state.route = (activeConfiguration: Sections, preferenceVM: PreferenceVM) => {
      if (preferenceVM.isModified()) {
        const modal: ConfirmModal = new ConfirmModal("There are unsaved changes. Do you wish to continue?", () => {
          vnode.state.activeConfiguration = activeConfiguration;
          m.route.set(activeConfiguration);
          preferenceVM.reset();
          modal.close();
        });
        modal.render();
      } else {
        vnode.state.activeConfiguration = activeConfiguration;
        m.route.set(activeConfiguration);
      }
    };

    vnode.state.notificationVMs = Stream(new NotificationFilterVMs());
    vnode.state.pipelineGroups  = Stream();
    vnode.state.currentUserVM   = Stream(new CurrentUserVM());

    vnode.state.onCancel            = (entity: PreferenceVM, e: MouseEvent) => {
      e.stopPropagation();
      const modal: ConfirmModal = new ConfirmModal("Do you want to discard the changes?", () => {
        entity.reset();
        modal.close();
      });
      modal.render();
    };
    vnode.state.onSaveEmailSettings = ((entity: CurrentUserVM, e: MouseEvent) => {
      e.stopPropagation();
      return CurrentUserCRUD.update(entity.entity(), entity.etag()!).then((result) => {
        result.do(
          (successResponse) => {
            this.flashMessage.setMessage(MessageType.success, "Email settings updated successfully!");
            vnode.state.currentUserVM().sync(successResponse.body.object, successResponse.body.etag);
          },
          (errorResponse) => {
            if (422 === result.getStatusCode()) {
              if (errorResponse.data) {
                vnode.state.currentUserVM().entity(CurrentUser.fromJSON(JsonUtils.toCamelCasedObject(errorResponse.data)));
              } else if (errorResponse.body) {
                this.flashMessage.alert(JSON.parse(errorResponse.body!).message);
              } else {
                this.flashMessage.alert(errorResponse.message);
              }
            }
          });
      });
    });

    vnode.state.onAddFilter    = (e: MouseEvent) => {
      e.stopPropagation();
      new CreateNotificationFilterModal(vnode.state.pipelineGroups, vnode.state.onFilterSave)
        .render();
    };
    vnode.state.onEditFilter   = (filter: NotificationFilter, e: MouseEvent) => {
      e.stopPropagation();
      new EditNotificationFilterModal(filter, vnode.state.pipelineGroups, vnode.state.onFilterSave)
        .render();
    };
    vnode.state.onDeleteFilter = (filter: NotificationFilter, e: MouseEvent) => {
      e.stopPropagation();
      this.pageState = PageState.LOADING;
      NotificationFiltersCRUD.delete(filter.id())
                             .then((result) => {
                               result.do(
                                 () => {
                                   this.flashMessage.success("Notification filter deleted successfully!");
                                   this.fetchData(vnode);
                                 },
                                 (errorResponse) => {
                                   if (errorResponse.body) {
                                     const parse = JSON.parse(errorResponse.body);
                                     this.flashMessage.alert(parse.message);
                                   } else {
                                     this.flashMessage.alert(errorResponse.message);
                                   }
                                 }
                               );
                             }).finally(() => this.pageState = PageState.OK);
    };

    vnode.state.onFilterSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };
  }

  componentToDisplay(vnode: m.Vnode<null, PreferencesState>): m.Children {
    const flashMessage = this.flashMessage.hasMessage() ?
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      : null;
    let smtpErrorMessage;
    if (!this.getMeta().smtp_configured) {
      const message    = <span>SMTP settings are currently not configured. If you are the administrator, you can configure email support at <Link
        href={"/go/admin/config/server#!email-server"} externalLinkIcon={true}>Mail Server Configuration</Link>.</span>;
      smtpErrorMessage = <FlashMessage type={MessageType.info} message={message}/>;
    }
    return [
      flashMessage,
      smtpErrorMessage,
      <PreferencesWidget {...vnode.state}/>
    ];
  }

  pageName(): string {
    return "Preferences";
  }

  fetchData(vnode: m.Vnode<null, PreferencesState>): Promise<any> {
    this.pageState = PageState.LOADING;
    return Promise.all([NotificationFiltersCRUD.all(), CurrentUserCRUD.get(), EnvironmentsAPIs.allPipelines("view", "view")])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      this.pageState = PageState.OK;
                      vnode.state.notificationVMs().sync(successResponse.body);
                    }, (errorResponse) => {
                      if (errorResponse.body) {
                        const parse = JSON.parse(errorResponse.body);
                        this.flashMessage.setMessage(MessageType.alert, parse.message);
                      } else {
                        this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                      }
                    });
                    results[1].do((successResponse) => {
                      this.pageState = PageState.OK;
                      vnode.state.currentUserVM().sync(successResponse.body.object);
                    }, this.setErrorState);
                    results[2].do((successResponse) => {
                      this.pageState = PageState.OK;
                      vnode.state.pipelineGroups(successResponse.body.groups());
                    }, this.setErrorState);
                  });
  }
}

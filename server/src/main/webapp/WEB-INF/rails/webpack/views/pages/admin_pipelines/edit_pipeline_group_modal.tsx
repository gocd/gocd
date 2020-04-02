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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroup, PipelineGroupViewModel} from "models/admin_pipelines/admin_pipelines";
import {PipelineGroupCRUD} from "models/admin_pipelines/pipeline_groups_crud";
import {PermissionForEntity} from "models/authorization/authorization";
import {Cancel, Primary, Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Table} from "views/components/table";
import {Info} from "views/components/tooltip";
import styles from "./edit_pipeline_group.scss";

class UsersProvider extends SuggestionProvider {
  private readonly usersCache: string[];

  constructor(usersCache: string[]) {
    super();
    this.usersCache = usersCache;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve, reject) => {
      resolve(this.usersCache);
    });
  }
}

class RolesProvider extends SuggestionProvider {
  private readonly rolesCache: string[];

  constructor(rolesCache: string[]) {
    super();
    this.rolesCache = rolesCache;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve, reject) => {
      resolve(this.rolesCache);
    });
  }
}

export class EditPipelineGroupModal extends Modal {
  private readonly pipelineGroupViewModel: Stream<PipelineGroupViewModel>;
  private readonly usersProvider: UsersProvider;
  private readonly rolesProvider: RolesProvider;
  private readonly userPermissionCollapseState: Stream<boolean>;
  private readonly rolePermissionCollapseState: Stream<boolean>;
  private readonly onSuccessfulSave: (msg: string) => void;
  private readonly flashMessage: FlashMessageModel;
  private etag: string;
  private readonly containsPipelinesRemotely: boolean;
  private readonly pipelineGroupName: string;

  constructor(group: PipelineGroup, etag: string, usersAutoCompleteHelper: string[], rolesAutoCompleteHelper: string[], onSuccessfulSave: (msg: string) => void, containsPipelinesRemotely: boolean) {
    super(Size.large);
    this.pipelineGroupName           = group.name();
    this.onSuccessfulSave            = onSuccessfulSave;
    this.containsPipelinesRemotely   = containsPipelinesRemotely;
    this.pipelineGroupViewModel      = Stream(new PipelineGroupViewModel(group));
    this.fixedHeight                 = true;
    this.usersProvider               = new UsersProvider(usersAutoCompleteHelper);
    this.rolesProvider               = new RolesProvider(rolesAutoCompleteHelper);
    this.userPermissionCollapseState = Stream();
    this.rolePermissionCollapseState = Stream();
    this.flashMessage                = new FlashMessageModel();
    this.etag                        = etag;
  }

  body(): m.Children {
    let flashMessageHtml;
    const infoTooltip = <div data-test-id="info-tooltip" class={styles.pipelineGroupTooltipWrapper}>
      <Info content={"Cannot rename pipeline group as it contains remotely defined pipelines"}/>
    </div>;
    if (this.flashMessage.hasMessage()) {
      flashMessageHtml = <FlashMessage dataTestId={"pipeline-group-flash-message"}
                                       type={this.flashMessage.type}
                                       message={this.flashMessage.message}/>;
    }
    return <div>
      {flashMessageHtml}
      <div class={styles.pipelineGroupNameWrapper}>
        <TextField
          title={this.containsPipelinesRemotely ? "Cannot rename pipeline group as it contains remotely defined pipelines" : ""}
          label={"Pipeline group name"}
          property={this.pipelineGroupViewModel().name}
          readonly={this.containsPipelinesRemotely}/>
        {this.containsPipelinesRemotely ? infoTooltip : ""}
      </div>
      <CollapsiblePanel header={"User permissions"}
                        expanded={true}
                        onexpand={() => this.userPermissionCollapseState(true)}
                        oncollapse={() => this.userPermissionCollapseState(false)}
                        dataTestId={"users-permissions-collapse"}
                        actions={<Secondary dataTestId={"add-user-permission"}
                                            onclick={this.addUserAuthorization.bind(this)}>Add</Secondary>}>
        <RenderErrors dataTestId="errors-on-users" errors={this.pipelineGroupViewModel().errorsOnUsers()}/>
        <Table data={this.userPermissionData()} headers={["Name", "View", "Operate", "Admin", ""]}
               data-test-id="users-permissions"/>
      </CollapsiblePanel>
      <CollapsiblePanel header={"Role permissions"}
                        expanded={true}
                        onexpand={() => this.rolePermissionCollapseState(true)}
                        oncollapse={() => this.rolePermissionCollapseState(false)}
                        dataTestId={"roles-permissions-collapse"}
                        actions={<Secondary dataTestId={"add-role-permission"}
                                            onclick={this.addRoleAuthorization.bind(this)}>Add</Secondary>}>
        <RenderErrors dataTestId="errors-on-roles" errors={this.pipelineGroupViewModel().errorsOnRoles()}/>
        <Table data={this.rolePermissionData()} headers={["Name", "View", "Operate", "Admin", ""]}
               data-test-id="roles-permissions"/>
      </CollapsiblePanel>
    </div>;
  }

  title(): string {
    return "Edit Pipeline Group";
  }

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="save-pipeline-group" onclick={this.performSave.bind(this)}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)}>Cancel</Cancel>
    ];
  }

  userPermissionData() {
    return this.pipelineGroupViewModel().authorizedUsers().map((authorizedEntity) => [
      <div class={styles.permissionNameWrapper}>
        <AutocompleteField dataTestId={"user-name"} property={authorizedEntity.name} required={true} maxItems={25}
                           provider={this.usersProvider} autoEvaluate={false}
                           errorText={authorizedEntity.errors().errorsForDisplay("name")}/>
      </div>,
      <div class={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.view} dataTestId={"view-permission"}
                       readonly={authorizedEntity.admin() || authorizedEntity.operate()}/>
      </div>,
      <div className={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.operate} dataTestId={"operate-permission"}
                       readonly={authorizedEntity.admin()}
                       onchange={() => authorizedEntity.view(true)}/>
      </div>,
      <div className={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.admin} dataTestId={"admin-permission"} onchange={(e: MouseEvent) => {
          authorizedEntity.operate((e.target as HTMLInputElement).checked);
          authorizedEntity.view(true);
        }}/>
      </div>,
      <div className={styles.cancelButtonWrapper}>
        <Cancel data-test-id="user-permission-delete" onclick={() => this.removeUser(authorizedEntity)}>
          <span class={styles.iconDelete}/>
        </Cancel>
      </div>
    ]);
  }

  rolePermissionData() {
    return this.pipelineGroupViewModel().authorizedRoles().map((authorizedEntity) => [
      <div className={styles.permissionNameWrapper}>
        <AutocompleteField dataTestId={"role-name"} property={authorizedEntity.name} required={true} maxItems={25}
                           provider={this.rolesProvider} autoEvaluate={false}
                           errorText={authorizedEntity.errors().errorsForDisplay("name")}/>
      </div>,
      <div className={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.view} dataTestId={"view-permission"}
                       readonly={authorizedEntity.admin() || authorizedEntity.operate()}/>
      </div>,
      <div className={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.operate} dataTestId={"operate-permission"}
                       readonly={authorizedEntity.admin()}
                       onchange={() => authorizedEntity.view(true)}/>
      </div>,
      <div className={styles.permissionCheckboxWrapper}>
        <CheckboxField property={authorizedEntity.admin} dataTestId={"admin-permission"} onchange={(e: MouseEvent) => {
          authorizedEntity.operate((e.target as HTMLInputElement).checked);
          authorizedEntity.view(true);
        }}/>
      </div>,
      <div className={styles.cancelButtonWrapper}>
        <Cancel data-test-id="role-permission-delete" onclick={() => this.removeRole(authorizedEntity)}>
          <span class={styles.iconDelete}/>
        </Cancel>
      </div>
    ]);
  }

  addUserAuthorization(e: MouseEvent) {
    if (this.userPermissionCollapseState()) {
      e.stopPropagation();
    }
    this.pipelineGroupViewModel().addAuthorizedUser(new PermissionForEntity("", true, false, false));
  }

  addRoleAuthorization(e: MouseEvent) {
    if (this.rolePermissionCollapseState()) {
      e.stopPropagation();
    }
    this.pipelineGroupViewModel().addAuthorizedRole(new PermissionForEntity("", true, false, false));
  }

  private removeRole(role: PermissionForEntity) {
    this.pipelineGroupViewModel().removeAuthorizedRole(role);
  }

  private removeUser(user: PermissionForEntity) {
    this.pipelineGroupViewModel().removeAuthorizedUser(user);
  }

  private performSave() {
    if (this.pipelineGroupViewModel().isValid()) {
      PipelineGroupCRUD.update(this.pipelineGroupName, this.pipelineGroupViewModel().getUpdatedPipelineGroup(), this.etag)
                       .then((result) => {
                         result.do(() => {
                           this.onSuccessfulSave(`Pipeline group ${this.pipelineGroupViewModel().name()} updated successfully.`);
                           this.close();
                         }, (errorResponse) => {
                           if (errorResponse.body) {
                             const pipelineGrpJson = JSON.parse(errorResponse.body);
                             if (pipelineGrpJson.data) {
                               const pipelineGroup = PipelineGroup.fromJSON(pipelineGrpJson.data);
                               this.pipelineGroupViewModel(new PipelineGroupViewModel(pipelineGroup));
                             }
                             this.flashMessage.setMessage(MessageType.alert, pipelineGrpJson.message);
                           } else {
                             this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                           }
                         });
                       });
    }
  }
}

interface ErrorAttrs {
  errors: string[];
  dataTestId?: string;
}

class RenderErrors extends MithrilViewComponent<ErrorAttrs> {
  view(vnode: m.Vnode<ErrorAttrs, this>): m.Children | void | null {
    if (_.isEmpty(vnode.attrs.errors)) {
      return undefined;
    }
    const errors: m.Children = [];
    vnode.attrs.errors.forEach((err) => {
      errors.push(<li>{err}</li>);
    });

    return <FlashMessage dataTestId={vnode.attrs.dataTestId ? vnode.attrs.dataTestId : "errors"}
                         type={MessageType.alert}>
      <ul class={styles.errors}>{errors}</ul>
    </FlashMessage>;
  }
}

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
import {TemplateAuthorization, TemplateAuthorizationViewModel} from "models/admin_templates/templates";
import {TemplatesCRUD} from "models/admin_templates/templates_crud";
import {PermissionForEntity} from "models/authorization/authorization";
import {Cancel, Primary, Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {CheckboxField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {SwitchBtn} from "views/components/switch";
import {Table} from "views/components/table";
import {RenderErrors} from "views/pages/admin_pipelines/edit_pipeline_group_modal";
import styles from "./modals.scss";

class UsersProvider extends SuggestionProvider {
  private readonly usersCache: string[];

  constructor(usersCache: string[]) {
    super();
    this.usersCache = usersCache;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
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
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve(this.rolesCache);
    });
  }
}

export class EditTemplatePermissionsModal extends Modal {
  private authorization: Stream<TemplateAuthorizationViewModel>;
  private readonly usersProvider: UsersProvider;
  private readonly rolesProvider: RolesProvider;
  private readonly userPermissionCollapseState: Stream<boolean>;
  private readonly rolePermissionCollapseState: Stream<boolean>;
  private readonly onSuccessfulSave: (msg: string) => void;
  private readonly flashMessage: FlashMessageModel;
  private etag: string;
  private readonly templateName: string;

  constructor(templateName: string, authorization: TemplateAuthorization, etag: string, usersAutoCompleteHelper: string[], rolesAutoCompleteHelper: string[], onSuccessfulSave: (msg: string) => void) {
    super(Size.large);
    this.templateName                = templateName;
    this.authorization               = Stream(new TemplateAuthorizationViewModel(authorization));
    this.onSuccessfulSave            = onSuccessfulSave;
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
    if (this.flashMessage.hasMessage()) {
      flashMessageHtml = <FlashMessage dataTestId={"template-flash-message"}
                                       type={this.flashMessage.type}
                                       message={this.flashMessage.message}/>;
    }
    return <div>
      {flashMessageHtml}
      <div class={styles.switchWrapper}>
        <SwitchBtn field={this.authorization().allGroupAdminsAreViewUsers}
                   label={"Allow all pipeline group administrators view access to template"}
                   small={true}/>
      </div>
      <CollapsiblePanel header={"User permissions"}
                        expanded={true}
                        onexpand={() => this.userPermissionCollapseState(true)}
                        oncollapse={() => this.userPermissionCollapseState(false)}
                        dataTestId={"users-permissions-collapse"}
                        actions={<Secondary dataTestId={"add-user-permission"}
                                            onclick={this.addUserAuthorization.bind(this)}>Add</Secondary>}>
        <RenderErrors dataTestId="errors-on-users" errors={this.authorization().errorsOnUsers()}/>
        <Table data={this.userPermissionData()} headers={["Name", "View", "Admin", ""]}
               data-test-id="users-permissions"/>
      </CollapsiblePanel>
      <CollapsiblePanel header={"Role permissions"}
                        expanded={true}
                        onexpand={() => this.rolePermissionCollapseState(true)}
                        oncollapse={() => this.rolePermissionCollapseState(false)}
                        dataTestId={"roles-permissions-collapse"}
                        actions={<Secondary dataTestId={"add-role-permission"}
                                            onclick={this.addRoleAuthorization.bind(this)}>Add</Secondary>}>
        <RenderErrors dataTestId="errors-on-roles" errors={this.authorization().errorsOnRoles()}/>
        <Table data={this.rolePermissionData()} headers={["Name", "View", "Admin", ""]}
               data-test-id="roles-permissions"/>
      </CollapsiblePanel>
    </div>;
  }

  title(): string {
    return `Edit Template Authorization for '${this.templateName}'`;
  }

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="save-pipeline-group" onclick={this.performSave.bind(this)}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)}>Cancel</Cancel>
    ];
  }

  userPermissionData() {
    return this.authorization().authorizedUsers().map((authorizedEntity) => [
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
    return this.authorization().authorizedRoles().map((authorizedEntity) => [
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
    this.authorization().addAuthorizedUser(new PermissionForEntity("", true, false, false));
  }

  addRoleAuthorization(e: MouseEvent) {
    if (this.rolePermissionCollapseState()) {
      e.stopPropagation();
    }
    this.authorization().addAuthorizedRole(new PermissionForEntity("", true, false, false));
  }

  private removeRole(role: PermissionForEntity) {
    this.authorization().removeAuthorizedRole(role);
  }

  private removeUser(user: PermissionForEntity) {
    this.authorization().removeAuthorizedUser(user);
  }

  private performSave() {
    if (this.authorization().isValid()) {
      TemplatesCRUD.updateAuthorization(this.templateName, this.authorization().getUpdatedTemplateAuthorization(), this.etag)
                   .then((result) => {
                     result.do(() => {
                       this.onSuccessfulSave(`Template '${this.templateName}' permissions updated successfully.`);
                       this.close();
                     }, (errorResponse) => {
                       if (errorResponse.body) {
                         const templateAuthJson = JSON.parse(errorResponse.body);
                         if (templateAuthJson.data) {
                           const templateAuthorization = TemplateAuthorization.fromJSON(templateAuthJson.data);
                           this.authorization(new TemplateAuthorizationViewModel(templateAuthorization));
                         }
                         this.flashMessage.setMessage(MessageType.alert, templateAuthJson.message);
                       } else {
                         this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
                       }
                     });
                   });
    }
  }
}

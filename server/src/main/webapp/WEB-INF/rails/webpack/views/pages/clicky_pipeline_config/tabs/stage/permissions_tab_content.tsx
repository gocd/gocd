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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {RolesCRUD} from "models/roles/roles_crud";
import {Secondary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {RadioField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import styles from "./permissions.scss";

export class PermissionsTabContent extends TabContent<Stage> {
  private allRoles: Stream<string[]>         = Stream([] as string[]);
  private selectedPermission: Stream<string> = Stream();

  constructor() {
    super();
    this.fetchAllRoles();
  }

  static tabName(): string {
    return "Permissions";
  }

  protected renderer(entity: Stage, templateConfig: TemplateConfig): m.Children {
    return <div class={styles.mainContainer} data-test-id="permissions-tab">
      <FlashMessage type={MessageType.info}
                    message={"All system administrators and pipeline group administrators can operate on this stage (this cannot be overridden)."}/>
      <h3 data-test-id="permissions-heading">Permissions for this stage:</h3>
      <RadioField onchange={(value) => {
        entity.approval().authorization().isInherited((value !== "local"));
      }}
                  property={this.selectedPermission}
                  readonly={this.isEntityDefinedInConfigRepository()}
                  inline={true}
                  possibleValues={[
                    {
                      label: "Inherit from the Pipeline Group",
                      value: "inherit",
                      helpText: "Inherit authorization from the pipeline group."
                    },
                    {
                      label: "Specify locally",
                      value: "local",
                      helpText: "Define specific permissions locally. This will override pipeline group authorization."
                    }
                  ]}>
      </RadioField>
      {this.localPermissionsView(entity, this.isEntityDefinedInConfigRepository())}
    </div>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    const stage = pipelineConfig.stages().findByName(routeParams.stage_name!)!;
    this.selectedPermission(stage.approval().authorization().isInherited() ? "inherit" : "local");
    return stage;
  }

  private localPermissionsView(stage: Stage, readOnly: boolean) {
    if (stage.approval().authorization().isInherited()) {
      return;
    }

    const users = stage.approval().authorization()._users;
    const roles = stage.approval().authorization()._roles;

    return <div data-test-id="users-and-roles">
      <div data-test-id="users">
        <h3>Users</h3>
        <FlashMessage message={stage.approval().authorization().errors().errorsForDisplay("users")}
                      dataTestId="users-errors"
                      type={MessageType.alert}/>
        {
          users.map((user, index) => this.getInputField("username",
                                                        user,
                                                        users,
                                                        index,
                                                        new RolesSuggestionProvider(Stream([] as string[]), []),
                                                        readOnly
          ))
        }
        {this.addEntityButton(users, readOnly)}
      </div>

      <div data-test-id="roles">
        <h3>Roles</h3>
        <FlashMessage message={stage.approval().authorization().errors().errorsForDisplay("roles")}
                      dataTestId="roles-errors"
                      type={MessageType.alert}/>
        {
          roles.map((role, index) => this.getInputField("role",
                                                        role,
                                                        roles,
                                                        index,
                                                        new RolesSuggestionProvider(this.allRoles, roles.map(s => s())),
                                                        readOnly
          ))
        }

        {this.addEntityButton(roles, readOnly)}
      </div>
    </div>;
  }

  private getInputField(placeholder: string,
                        entity: Stream<string>,
                        collection: Array<Stream<string>>,
                        index: number,
                        provider: SuggestionProvider,
                        readOnly: boolean) {
    let removeEntity: m.Children;
    if (!readOnly) {
      removeEntity = <Icons.Close iconOnly={true} onclick={() => this.removeEntity(index, collection)}/>;
    }

    return <div class={styles.inputFieldContainer}
                data-test-id={`input-field-for-${entity()}`}>
      <AutocompleteField placeholder={placeholder}
                         provider={provider}
                         readonly={readOnly}
                         property={entity}/>
      {removeEntity}
    </div>;
  }

  private removeEntity(index: number, collection: Array<Stream<string>>) {
    _.pullAt(collection, [index]);
  }

  private addEntityButton(collection: Array<Stream<string>>, readOnly: boolean) {
    if (readOnly) {
      return;
    }

    return (<Secondary small={true} onclick={() => collection.push(Stream())}>+ Add</Secondary>);
  }

  private fetchAllRoles() {
    RolesCRUD.all().then((rolesResult) => {
      rolesResult.do((successResponse) => {
        this.allRoles(successResponse.body.map(r => r.name()));
      });
    });
  }
}

export class RolesSuggestionProvider extends SuggestionProvider {
  private allRoles: Stream<string[]>;
  private configuredRoles: string[];

  constructor(allRoles: Stream<string[]>, configuredRoles: string[]) {
    super();
    this.allRoles        = allRoles;
    this.configuredRoles = configuredRoles;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve(_.difference(this.allRoles(), this.configuredRoles));
    });
  }
}

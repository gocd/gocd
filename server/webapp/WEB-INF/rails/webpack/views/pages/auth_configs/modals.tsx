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

import {bind} from "classnames/bind";
import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {AuthConfig} from "models/auth_configs/auth_configs_new";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {AuthorizationSettings, Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {Spinner} from "views/components/spinner";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);
const AngularPluginNew     = require("views/shared/angular_plugin_new");

enum ModalType {
  edit, clone, create
}

abstract class BaseAuthConfigModal extends Modal {
  protected authConfig: Stream<AuthConfig>;
  private errorMessage: null;
  private readonly pluginInfo: Stream<PluginInfo<Extension>>;
  private readonly pluginInfos: Array<PluginInfo<Extension>>;
  private readonly modalType: ModalType;

  protected constructor(authConfig: AuthConfig,
                        pluginInfos: Array<PluginInfo<Extension>>,
                        type: ModalType) {
    super(Size.large);
    this.authConfig  = stream(authConfig);
    this.pluginInfos = pluginInfos;
    this.pluginInfo  = stream(pluginInfos.find(
      (pluginInfo) => pluginInfo.id === authConfig.pluginId()) || pluginInfos[0]);
    this.modalType   = type;
  }

  abstract performSave(): void;

  abstract modalTitle(authConfig: AuthConfig): string;

  validateAndPerformSave() {
    if (!this.authConfig().isValid()) {
      return;
    }
    this.performSave();
  }

  showErrors(apiResult: ApiResult<ObjectWithEtag<AuthConfig>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      const profile = AuthConfig.fromJSON(JSON.parse(errorResponse.body).data);
      this.authConfig(profile);
    }
  }

  buttons() {
    return [<Buttons.Primary data-test-id="button-ok"
                             onclick={this.validateAndPerformSave.bind(this)}>Save</Buttons.Primary>];
  }

  body() {
    if (this.errorMessage) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage}/>);
    }

    if (!this.authConfig()) {
      return <Spinner/>;
    }

    const pluginList = _.map(this.pluginInfos, (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    const pluginSettings = (this.pluginInfo()
                                .extensionOfType(ExtensionType.AUTHORIZATION)! as AuthorizationSettings).authConfigSettings;

    return (
      <div class={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        <div>
          <FormHeader>
            <Form>
              <TextField label="Id"
                         disabled={this.modalType === ModalType.edit}
                         property={this.authConfig().id}
                         errorText={this.authConfig().errors().errorsForDisplay("id")}
                         required={true}/>

              <SelectField label="Plugin ID"
                           property={this.pluginIdProxy.bind(this)}
                           required={true}
                           errorText={this.authConfig().errors().errorsForDisplay("pluginId")}>
                <SelectFieldOptions selected={this.authConfig().pluginId()}
                                    items={pluginList}/>
              </SelectField>
            </Form>
          </FormHeader>

        </div>
        <div>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={stream(pluginSettings)}
              configuration={this.authConfig().properties()}
              key={this.pluginInfo().id}/>
          </div>
        </div>
      </div>
    );
  }

  title() {
    return this.modalTitle(this.authConfig());
  }

  private pluginIdProxy(newValue ?: string) {
    if (newValue) {
      if (this.pluginInfo().id !== newValue) {
        const pluginInfo = _.find(this.pluginInfos, (p) => p.id === newValue);
        this.pluginInfo(pluginInfo!);
        this.authConfig(new AuthConfig(this.authConfig().id(), pluginInfo!.id, new Configurations([])));
      }
    }
    return this.pluginInfo().id;
  }
}

export class NewAuthConfigModal extends BaseAuthConfigModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;

  constructor(pluginInfos: Array<PluginInfo<any>>, onSuccessfulSave: (msg: m.Children) => any) {
    const authConfig = new AuthConfig("", pluginInfos[0].id, new Configurations([]));
    super(authConfig, pluginInfos, ModalType.create);
    this.onSuccessfulSave = onSuccessfulSave;
  }

  modalTitle(authConfig: AuthConfig): string {
    return "Add new authorization configuration";
  }

  performSave(): void {
    AuthConfigsCRUD
      .create(this.authConfig())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(<span>The authorization configuration <em>{this.authConfig().id()}</em> was created successfully!</span>);
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      });
  }

}

export class EditAuthConfigModal extends BaseAuthConfigModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private etag: string;

  constructor(authConfig: ObjectWithEtag<AuthConfig>,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(authConfig.object, pluginInfos, ModalType.edit);
    this.etag             = authConfig.etag;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  modalTitle(authConfig: AuthConfig): string {
    return `Edit authorization configuration ${authConfig.id()}`;
  }

  performSave(): void {
    AuthConfigsCRUD
      .update(this.authConfig(), this.etag)
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>The authorization configuration <em>{this.authConfig().id()}</em> was updated successfully!</span>
            );
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      });
  }

}

export class CloneAuthConfigModal extends BaseAuthConfigModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly sourceProfileId: string;

  constructor(authConfig: AuthConfig,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    const _sourceProfileId = authConfig.id();
    authConfig.id("");

    super(authConfig, pluginInfos, ModalType.create);
    this.sourceProfileId  = _sourceProfileId;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  modalTitle(authConfig: AuthConfig): string {
    return `Clone authorization configuration ${this.sourceProfileId}`;
  }

  performSave(): void {
    AuthConfigsCRUD
      .create(this.authConfig())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>The authorization configuration <em>{this.authConfig().id()}</em> was created successfully!</span>
            );
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      }).finally(m.redraw);
  }

}

export class DeleteAuthConfigConfirmModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onOperationError: (errorResponse: ErrorResponse) => any;

  constructor(authConfig: AuthConfig,
              onSuccessfulSave: (msg: m.Children) => any,
              onOperationError: (errorResponse: ErrorResponse) => any) {
    super(DeleteAuthConfigConfirmModal.deleteConfirmationMessage(authConfig),
          () => this.delete(authConfig), "Are you sure?");
    this.onSuccessfulSave = onSuccessfulSave;
    this.onOperationError = onOperationError;
  }

  private static deleteConfirmationMessage(authConfig: AuthConfig) {
    return <span>
          Are you sure you want to delete the authorization configuration <strong>{authConfig.id()}</strong>?
        </span>;
  }

  private delete(obj: AuthConfig) {
    AuthConfigsCRUD.delete(obj.id())
                   .then((result) => {
                     result.do(
                       () => this.onSuccessfulSave(
                         <span>The authorization configuration <em>{obj.id()}</em> was deleted successfully!</span>
                       ),
                       this.onOperationError
                     );
                   })
                   .finally(this.close.bind(this));
  }
}

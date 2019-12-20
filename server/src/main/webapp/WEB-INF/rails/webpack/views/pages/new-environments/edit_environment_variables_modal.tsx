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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, ModalState, Size} from "views/components/modal";
import {EnvironmentVariablesWithOriginWidget} from "views/shared/environment_variables_with_origin_widget";

export class EditEnvironmentVariablesModal extends Modal {
  private _environment: EnvironmentWithOrigin;
  private environmentToUpdate: EnvironmentWithOrigin;
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  protected readonly errorMessage: Stream<string> = Stream();

  constructor(environment: EnvironmentWithOrigin, onSuccessfulSave: (msg: m.Children) => void) {
    super(Size.medium);
    this.fixedHeight         = true;
    this._environment        = environment;
    this.onSuccessfulSave    = onSuccessfulSave;
    this.environmentToUpdate = environment.clone();
  }

  body(): m.Children {
    let errMsgHtml: m.Children;
    if (this.errorMessage()) {
      errMsgHtml = <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>;
    }

    return [
      errMsgHtml,
      <EnvironmentVariablesWithOriginWidget environmentVariables={this.environmentToUpdate.environmentVariables()}/>
    ];
  }

  title(): string {
    return "Edit Environment Variables";
  }

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="save-button" onclick={this.performSave.bind(this)}
               disabled={this.isLoading()}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)} disabled={this.isLoading()}>Cancel</Cancel>
    ];
  }

  performSave() {
    if (this.environmentToUpdate.isValid()) {
      const envToAdd    = this.environmentVariablesToAdd().map((envVar) => envVar.toJSON());
      const envToRemove = this.environmentVariablesToRemove().map((envVar) => envVar.name());
      this.modalState   = ModalState.LOADING;
      EnvironmentsAPIs.patch(this.environmentToUpdate.name(), {
        environment_variables: {
          add: envToAdd,
          remove: envToRemove
        }
      }).then((result) => {
        this.modalState = ModalState.OK;
        result.do(
          () => {
            this.onSuccessfulSave("Environment variables updated successfully");
            this.close();
          },
          (errorResponse: any) => {
            this.errorMessage(JSON.parse(errorResponse.body).message);
          }
        );
      });
    } else {
      return;
    }
  }

  environmentVariablesToAdd() {
    return this.environmentToUpdate.environmentVariables().filter((envVar) => {
      const isBlankEnvVar = _.isEmpty(envVar.name()) && (_.isEmpty(envVar.value()) && _.isEmpty(envVar.encryptedValue().getOriginal()));
      if (isBlankEnvVar) {
        return false;
      }

      const oldEnvVar = this._environment.environmentVariables().find((v) => v.name() === envVar.name());
      // add new and updated variables
      return oldEnvVar === undefined || !oldEnvVar.equals(envVar);
    });
  }

  environmentVariablesToRemove() {
    return this._environment.environmentVariables().filter((envVar) => {
      const newEnvVar = this.environmentToUpdate.environmentVariables().find((v) => v.name() === envVar.name());
      // remove updated and removed variables
      return newEnvVar === undefined || !newEnvVar.equals(envVar);
    });
  }

}

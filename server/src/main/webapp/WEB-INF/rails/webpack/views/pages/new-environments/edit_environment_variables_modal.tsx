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

import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Primary} from "views/components/buttons";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";

export class EditEnvironmentVariablesModal extends Modal {
  private _environment: EnvironmentWithOrigin;
  private environmentToUpdate: EnvironmentWithOrigin;
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  protected readonly errorMessage: Stream<string> = Stream();

  constructor(environment: EnvironmentWithOrigin, onSuccessfulSave: (msg: m.Children) => void) {
    super(Size.medium);
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
      <EnvironmentVariablesWidget environmentVariables={this.environmentToUpdate.environmentVariables()}/>
    ];
  }

  title(): string {
    return "Environment Variables";
  }

  buttons(): m.ChildArray {
    return [<Primary data-test-id="button-ok" onclick={this.performSave.bind(this)}>Save</Primary>];
  }

  performSave() {
    if (this.environmentToUpdate.isValid()) {
      const envToAdd    = this.environmentVariablesToAdd().map((envVar) => envVar.toJSON());
      const envToRemove = this.environmentVariablesToRemove().map((envVar) => envVar.name());
      EnvironmentsAPIs.patch(this.environmentToUpdate.name(), {
        environment_variables: {
          add: envToAdd,
          remove: envToRemove
        }
      }).then((result) => {
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

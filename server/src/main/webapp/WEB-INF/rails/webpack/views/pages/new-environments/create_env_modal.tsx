/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {ApiResult, ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentVariablesWithOrigin} from "models/environment_variables/types";
import {Pipelines} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Origin, OriginType} from "models/origin";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Modal, ModalState} from "views/components/modal";
import styles from "views/pages/new-environments/index.scss";

export class CreateEnvModal extends Modal {
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  private environment: EnvironmentWithOrigin;
  private errorMessage: Stream<string> = Stream();
  private readonly environments: Stream<Environments>;

  constructor(onSuccessfulSave: (msg: m.Children) => void, environments: Stream<Environments>) {
    super();
    this.onSuccessfulSave = onSuccessfulSave;
    this.environments     = environments;
    this.environment      = new EnvironmentWithOrigin("",
                                                      true,
                                                      [new Origin(OriginType.GoCD)],
                                                      [],
                                                      new Pipelines(),
                                                      new EnvironmentVariablesWithOrigin());
    this.environment.validateUniquenessOf("name", this.environments);
  }

  body(): m.Children {
    let flashMsg;
    if (this.errorMessage()) {
      flashMsg = <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>;
    }
    return <Form>
      <FlashMessage type={MessageType.info}
                    message={"Pipelines, agents and environment variables can be added post creation of the environment."}/>
      <div class={styles.createEnvErrMsgContainer}>
        {flashMsg}
      </div>
      <TextField label="Environment name"
                 property={this.environment.name}
                 errorText={this.environment.errors().errorsForDisplay("name")}
                 required={true}/>
    </Form>;
  }

  title(): string {
    return "Add New Environment";
  }

  buttons() {
    return [<ButtonGroup>
      <Cancel data-test-id="button-cancel" onclick={() => this.close()} disabled={this.isLoading()}>Cancel</Cancel>
      <Primary data-test-id="button-save" onclick={this.validateAndPerformSave.bind(this)} disabled={this.isLoading()}>Save</Primary>
    </ButtonGroup>];
  }

  private validateAndPerformSave() {
    if (!this.environment.isValid()) {
      return;
    }
    this.modalState = ModalState.LOADING;
    EnvironmentsAPIs.create(this.environment)
                    .then((result) => {
                      this.modalState = ModalState.OK;
                      result.do(
                        (successResponse: SuccessResponse<ObjectWithEtag<EnvironmentWithOrigin>>) => {
                          this.onSuccessfulSave(<span>Environment <em>{this.environment.name()}</em> created successfully. Now pipelines, agents and environment variables can be added to the same.</span>);
                          this.close();
                        },
                        (errorResponse: any) => {
                          this.showErrors(result, errorResponse);
                        }
                      );
                    });
  }

  private showErrors(apiResult: ApiResult<ObjectWithEtag<EnvironmentWithOrigin>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      const json       = JSON.parse(errorResponse.body);
      this.environment = (EnvironmentWithOrigin.fromJSON(json.data));
      this.errorMessage(json.message);
    } else {
      this.errorMessage(JSON.parse(errorResponse.body!).message);
    }
  }
}

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
import {ApiResult, ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {EnvironmentVariablesWithOrigin} from "models/new-environments/environment_environment_variables";
import {Pipelines} from "models/new-environments/environment_pipelines";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Origin, OriginType} from "models/new-environments/origin";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Modal} from "views/components/modal";

export class CreateEnvModal extends Modal {
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  private environment: EnvironmentWithOrigin;
  private environments: Stream<Environments>;
  private errorMessage: Stream<string> = Stream();

  constructor(environments: Stream<Environments>, onSuccessfulSave: (msg: m.Children) => void) {
    super();
    this.environments     = environments;
    this.onSuccessfulSave = onSuccessfulSave;
    this.environment      = new EnvironmentWithOrigin("",
                                                      [new Origin(OriginType.GoCD)],
                                                      [],
                                                      new Pipelines(),
                                                      new EnvironmentVariablesWithOrigin());
  }

  body(): m.Children {
    let flashMsg;
    if (this.errorMessage()) {
      flashMsg = <FlashMessage type={MessageType.alert} message={this.errorMessage()}/>;
    }
    return <Form>
      <FlashMessage type={MessageType.info}
                    message={"Pipelines, agents and environment variables can be added post creation of the environment."}/>
      {flashMsg}
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
      <Cancel data-test-id="button-cancel" onclick={() => this.close()}>Cancel</Cancel>
      <Primary data-test-id="button-save" onclick={this.validateAndPerformSave.bind(this)}>Save</Primary>
    </ButtonGroup>];
  }

  private validateAndPerformSave() {
    if (!this.environment.isValid()) {
      return;
    }
    EnvironmentsAPIs.create(this.environment)
                   .then((result) => {
                     result.do(
                       (successResponse: SuccessResponse<ObjectWithEtag<EnvironmentWithOrigin>>) => {
                         this.environments().push(successResponse.body.object);
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

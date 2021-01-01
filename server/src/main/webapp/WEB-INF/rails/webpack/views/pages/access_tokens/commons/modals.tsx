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
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as Buttons from "views/components/buttons";
import {Size, TextAreaField} from "views/components/forms/input_fields";
import {Modal} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import styles from "views/pages/access_tokens/index.scss";
import {OperationState} from "views/pages/page_operations";

export abstract class BaseModal extends Modal {
  protected accessToken: Stream<AccessToken>;
  protected operationState = Stream(OperationState.DONE);

  protected constructor(accessTokens: Stream<AccessTokens>,
                        accessToken: Stream<AccessToken>,
                        onSuccessfulSave: (msg: m.Children) => void,
                        onError: (msg: m.Children) => void) {
    super();
    this.accessTokens     = accessTokens;
    this.onSuccessfulSave = onSuccessfulSave;
    this.onFailedSave     = onError;
    this.accessToken      = accessToken;
  }

  protected readonly accessTokens: Stream<AccessTokens>;
  protected readonly onSuccessfulSave: (msg: m.Children) => void;
  private readonly onFailedSave: (msg: m.Children) => void;

  protected performOperation(e: MouseEvent) {
    return this.operationPromise().then(this.onOperationResult.bind(this)).finally(() => {
      m.redraw();
    });
  }

  protected abstract operationPromise(): Promise<any>;

  protected abstract afterSuccess(): void;

  private onOperationResult(result: ApiResult<ObjectWithEtag<AccessToken>>) {
    result.do(this.onSuccess.bind(this), (e) => {
      this.onError(e, result.getStatusCode());
    });
  }

  private onError(errorResponse: ErrorResponse, statusCode: number) {
    if (422 === statusCode && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      if (json && json.hasOwnProperty("created_at")) {
        this.accessToken(AccessToken.fromJSON(json));
      } else {
        this.onFailedSave(json.message);
        this.close();
      }
    } else {
      this.onFailedSave(JSON.parse(errorResponse.body!).message);
      this.close();
    }
  }

  private onSuccess(successResponse: SuccessResponse<ObjectWithEtag<AccessToken>>) {
    this.accessToken(successResponse.body.object);
    this.afterSuccess();
  }
}

export abstract class RevokeTokenModal extends BaseModal {
  protected cause: Stream<string> = Stream();

  constructor(accessTokens: Stream<AccessTokens>,
              accessToken: Stream<AccessToken>,
              onSuccessfulSave: (msg: m.Children) => void,
              onError: (msg: m.Children) => void) {
    super(accessTokens, accessToken, onSuccessfulSave, onError);
    this.closeModalOnOverlayClick = false;
  }

  body(): m.Children {
    if (this.operationState() === OperationState.IN_PROGRESS) {
      return <div class={styles.spinnerContainer}>
        <Spinner/>
      </div>;
    }
    return (
      <TextAreaField helpText={"Why do you want to revoke this token?"}
                     label="Are you sure you want to revoke this token?"
                     rows={5}
                     size={Size.MATCH_PARENT}
                     property={this.cause}/>
    );
  }

  title(): string {
    return "Revoke Token";
  }

  buttons(): m.ChildArray {
    return [<Buttons.Primary data-test-id="button-revoke-token"
                             ajaxOperationMonitor={this.operationState}
                             ajaxOperation={this.performOperation.bind(this)}>Revoke token</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" ajaxOperationMonitor={this.operationState}
                      onclick={() => this.close()}>Cancel</Buttons.Cancel>];
  }

  protected afterSuccess() {
    this.onSuccessfulSave("Access token was successfully revoked.");
  }
}

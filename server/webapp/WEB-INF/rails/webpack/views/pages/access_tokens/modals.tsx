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
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {AccessTokenCRUD} from "models/access_tokens/access_token_crud";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {CopyField, Size, TextAreaField} from "views/components/forms/input_fields";
import {Modal} from "views/components/modal";

abstract class BaseModal extends Modal {
  protected accessToken: Stream<AccessToken>;

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
    this.operationPromise().then(this.onOperationResult.bind(this)).finally(() => m.redraw());
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
      if (json) {
        this.accessToken(AccessToken.fromJSON(json));
        this.accessToken().token("");
      }
    } else {
      this.onFailedSave(errorResponse.message);
      this.close();
    }
  }

  private onSuccess(successResponse: SuccessResponse<ObjectWithEtag<AccessToken>>) {
    this.accessToken(successResponse.body.object);
    this.afterSuccess();
  }
}

export class GenerateTokenModal extends BaseModal {
  constructor(accessTokens: Stream<AccessTokens>,
              onSuccessfulSave: (msg: m.Children) => void,
              onError: (msg: m.Children) => void) {
    super(accessTokens, stream(AccessToken.new()), onSuccessfulSave, onError);
    this.closeModalOnOverlayClick = false;
  }

  title(): string {
    return "Generate Token";
  }

  body(): m.Children {
    if (this.hasToken()) {
      return (<div>
        <FlashMessage type={MessageType.info}
                      message="Make sure to copy your new personal access token now. You won’t be able to see it again!"/>
        <CopyField size={Size.MATCH_PARENT} property={this.accessToken().token} buttonDisableReason=""/>
      </div>);
    } else {
      return <TextAreaField label={"Description"}
                            required={true}
                            property={this.accessToken().description}
                            resizable={false}
                            rows={3}
                            size={Size.MATCH_PARENT}
                            errorText={this.accessToken().errors().errorsForDisplay("description")}
                            helpText="What’s this token for?"/>;

    }
  }

  buttons(): m.ChildArray {
    if (this.hasToken()) {
      return [<Buttons.Cancel data-test-id="button-close" onclick={() => this.close()}>Close</Buttons.Cancel>];
    } else {
      return [
        <Buttons.Primary data-test-id="button-save"
                         onclick={this.performOperation.bind(this)}>Generate</Buttons.Primary>,
        <Buttons.Cancel data-test-id="button-cancel" onclick={() => this.close()}>Cancel</Buttons.Cancel>];
    }
  }

  protected operationPromise(): Promise<any> {
    return AccessTokenCRUD.create(this.accessToken());
  }

  protected afterSuccess() {
    this.accessTokens().push(this.accessToken);
    this.onSuccessfulSave("Access token was successfully created.");
  }

  private hasToken() {
    const token = this.accessToken().token();
    return !_.isEmpty(token);
  }
}

export class RevokeTokenModal extends BaseModal {
  private cause: Stream<string> = stream();

  constructor(accessTokens: Stream<AccessTokens>,
              accessToken: Stream<AccessToken>,
              onSuccessfulSave: (msg: m.Children) => void,
              onError: (msg: m.Children) => void) {
    super(accessTokens, accessToken, onSuccessfulSave, onError);
    this.closeModalOnOverlayClick = false;
  }

  body(): m.Children {
    return (
      <TextAreaField helpText={"Why do you want to revoke this token?"}
                     label="Are you sure you want to revoke this token?"
                     rows={3}
                     size={Size.MATCH_PARENT}
                     property={this.cause}/>
    );
  }

  title(): string {
    return "Revoke Token";
  }

  buttons(): m.ChildArray {
    return [<Buttons.Primary data-test-id="button-revoke-token"
                             onclick={this.performOperation.bind(this)}>Revoke token</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" onclick={() => this.close()}>Cancel</Buttons.Cancel>];
  }

  protected operationPromise(): Promise<any> {
    return AccessTokenCRUD.revoke(this.accessToken(), this.cause()).finally(() => this.close());
  }

  protected afterSuccess() {
    this.onSuccessfulSave("Access token was successfully revoked.");
  }
}

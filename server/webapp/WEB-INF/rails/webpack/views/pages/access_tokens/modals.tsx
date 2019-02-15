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

import {ErrorResponse, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import _ = require("lodash");
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {AccessTokenCRUD} from "models/access_tokens/access_token_crud";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as Buttons from "views/components/buttons";
import {ButtonGroup} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {CopyField, Size, TextAreaField} from "views/components/forms/input_fields";
import {Modal} from "views/components/modal";

export class GenerateTokenModal extends Modal {
  private accessToken: AccessToken;
  private readonly accessTokens: Stream<AccessTokens>;
  private readonly onSuccessfulSave: (msg: m.Children) => void;
  private readonly onFailedSave: (msg: m.Children) => void;

  constructor(accessTokens: Stream<AccessTokens>,
              onSuccessfulSave: (msg: m.Children) => void,
              onError: (msg: m.Children) => void) {
    super();
    this.closeModalOnOverlayClick = false;
    this.accessTokens             = accessTokens;
    this.onSuccessfulSave         = onSuccessfulSave;
    this.onFailedSave             = onError;
    this.accessToken              = AccessToken.new();
  }

  body(): m.Children {
    if (this.hasToken()) {
      return (<div>
        <FlashMessage type={MessageType.info}
                      message="Make sure to copy your new personal access token now. You won’t be able to see it again!"/>
        <CopyField size={Size.MATCH_PARENT} property={this.accessToken.token} buttonDisableReason=""/>
      </div>);
    } else {
      return <TextAreaField label={"Description"}
                            required={true}
                            property={this.accessToken.description}
                            resizable={false}
                            rows={3}
                            size={Size.MATCH_PARENT}
                            helpText="What’s this token for?"/>;

    }
  }

  buttons(): m.ChildArray {
    if (this.hasToken()) {
      return [<Buttons.Cancel data-test-id="button-close" onclick={() => this.close()}>Close</Buttons.Cancel>];
    } else {
      return [
        <ButtonGroup>
          <Buttons.Cancel data-test-id="button-cancel" onclick={() => this.close()}>Cancel</Buttons.Cancel>
          <Buttons.Primary data-test-id="button-save" onclick={this.performSave.bind(this)}>Generate</Buttons.Primary>
        </ButtonGroup>
      ];
    }
  }

  title(): string {
    return "Generate Token";
  }

  private hasToken() {
    const token = this.accessToken.token();
    return !_.isEmpty(token);
  }

  private performSave(e: MouseEvent) {
    AccessTokenCRUD.create(this.accessToken)
                   .then((result) => result.do(this.onSuccess.bind(this), this.onError.bind(this)))
                   .finally(() => m.redraw());
  }

  private onError(errorResponse: ErrorResponse) {
    return this.onFailedSave(errorResponse.message);
  }

  private onSuccess(successResponse: SuccessResponse<ObjectWithEtag<AccessToken>>) {
    this.accessToken = successResponse.body.object;
    this.accessTokens().push(this.accessToken);
    return this.onSuccessfulSave("Access token was successfully created.");
  }
}

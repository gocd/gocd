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

import {ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {ButtonGroup, Cancel, Secondary} from "views/components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import styles from "./re_order_entity.scss";

export class EntityReOrderHandler {
  private readonly shouldShowReorderMessage: Stream<boolean>;
  private readonly entityName: string;
  private readonly flashMessage: FlashMessageModelWithTimeout;
  private readonly pipelineConfigSave: () => any;
  private readonly pipelineConfigReset: () => any;
  readonly hasOrderChanged: () => boolean;

  constructor(entityName: string, flashMessage: FlashMessageModelWithTimeout,
              pipelineConfigSave: () => any, pipelineConfigReset: () => any, hasOrderChanged: () => boolean) {
    this.entityName               = entityName;
    this.flashMessage             = flashMessage;
    this.pipelineConfigSave       = pipelineConfigSave;
    this.pipelineConfigReset      = pipelineConfigReset;
    this.hasOrderChanged          = hasOrderChanged;
    this.shouldShowReorderMessage = Stream();
  }

  getReOrderConfirmationView(): m.Children {
    if (!this.shouldShowReorderMessage()) {
      return;
    }

    return (<div class={styles.container} data-test-id="reorder-confirmation">
      <span>Do you want to save the new {this.entityName} order?</span>
      <ButtonGroup>
        <Secondary dataTestId={'save-btn'} onclick={this.onSave.bind(this)}>Save</Secondary>
        <Cancel dataTestId={'revert-btn'} onclick={this.onRevert.bind(this)}>Revert</Cancel>
      </ButtonGroup>
    </div>);
  }

  onReOder() {
    if (this.hasOrderChanged()) {
      this.shouldShowReorderMessage(true);
    }
  }

  private onRevert() {
    this.pipelineConfigReset();
    this.shouldShowReorderMessage(false);
    m.redraw.sync();
  }

  private onSave() {
    return this.pipelineConfigSave().then(() => {
      this.flashMessage.setMessage(MessageType.success, `${this.entityName}s reordered successfully.`);
    }).catch((errorResponse: ErrorResponse) => {
      this.flashMessage.consumeErrorResponse(errorResponse);
    }).finally(() => {
      this.pipelineConfigReset();
      this.shouldShowReorderMessage(false);
      m.redraw.sync();
    });
  }
}

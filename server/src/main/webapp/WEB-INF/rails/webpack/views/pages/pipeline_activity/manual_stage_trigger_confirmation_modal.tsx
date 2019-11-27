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
import {Modal, Size} from "../../components/modal";
import {Stage} from "../../../models/pipeline_activity/pipeline_activity";
import {OperationState} from "../page_operations";
import {PipelineActivityService} from "../../../models/pipeline_activity/pipeline_activity_crud";
import * as Buttons from "../../components/buttons";
import {ButtonIcon} from "../../components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "../../components/flash_message";

export class ManualStageTriggerConfirmation extends Modal {
  private stage: Stage;
  private operationState: OperationState | undefined;
  private service: PipelineActivityService;
  private message: FlashMessageModelWithTimeout;

  constructor(stage: Stage, service: PipelineActivityService, message: FlashMessageModelWithTimeout) {
    super(Size.small);
    this.stage   = stage;
    this.service = service;
    this.message = message;
  }

  body(): m.Children {
    return <div>{`Do you want to run the stage '${this.stage.stageName()}'?`}</div>;
  }

  title(): string {
    return "Run stage";
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id='button-trigger'
                       disabled={this.operationState == OperationState.IN_PROGRESS}
                       icon={this.operationState == OperationState.IN_PROGRESS ? ButtonIcon.SPINNER : undefined}
                       onclick={() => this.runStage()}>Yes Run</Buttons.Primary>,
      <Buttons.Cancel disabled={this.operationState == OperationState.IN_PROGRESS}
                      data-test-id='button-no-delete' onclick={this.close.bind(this)}
      >No</Buttons.Cancel>
    ];
  }

  private runStage() {
    this.stage.getCanRun(false);
    this.operationState = OperationState.IN_PROGRESS;
    this.service.runStage(this.stage)
      .then((result) => {
        result.do((successResponse) => {
            const body = JSON.parse(successResponse.body);
            this.message.setMessage(MessageType.success, body.message)
          },
          (errorResponse) => {
            this.stage.getCanRun(true);
            this.message.setMessage(MessageType.alert, errorResponse.message)
          })
      })
      .finally(() => {
        this.operationState = OperationState.DONE;
        this.close();
      });
  }
}

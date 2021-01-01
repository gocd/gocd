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
import m from "mithril";
import Stream from "mithril/stream";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "views/pages/page_operations";

export class DeleteConfirmModal extends Modal {
  private readonly message: m.Children;
  private readonly modalTitle: string;
  private readonly ondelete: () => Promise<any>;
  protected errorMessage?: string;
  private operationState: Stream<OperationState> = Stream<OperationState>(OperationState.UNKNOWN);

  constructor(message: m.Children, ondelete: () => Promise<any>, title = "Are you sure?") {
    super(Size.small);
    this.message    = message;
    this.modalTitle = title;
    this.ondelete   = ondelete;
  }

  body(): m.Children {
    if (this.errorMessage !== undefined) {
      return <FlashMessage type={MessageType.alert} message={this.errorMessage}/>;
    }
    return <div>{this.message}</div>;
  }

  title(): string {
    return this.modalTitle;
  }

  buttons(): m.ChildArray {
    if (this.errorMessage !== undefined) {
      return [
        <Buttons.Primary ajaxOperationMonitor={this.operationState} data-test-id='button-no-delete'
                         onclick={this.close.bind(this)}>OK</Buttons.Primary>
      ];
    }
    return [
      <Buttons.Danger data-test-id='button-delete'
                      ajaxOperationMonitor={this.operationState}
                      ajaxOperation={this.ondelete}>Yes Delete</Buttons.Danger>,
      <Buttons.Cancel ajaxOperationMonitor={this.operationState}
                      data-test-id='button-no-delete' onclick={this.close.bind(this)}
      >No</Buttons.Cancel>
    ];
  }
}

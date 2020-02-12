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
import m from "mithril";
import Stream from "mithril/stream";
import * as Buttons from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "../../pages/page_operations";

export class DeleteConfirmModal extends Modal {
  private readonly message: m.Children;
  private readonly modalTitle: string;
  private readonly ondelete: () => Promise<any>;
  private operationState: Stream<OperationState> = Stream<OperationState>(OperationState.UNKNOWN);

  constructor(message: m.Children, ondelete: () => Promise<any>, title = "Are you sure?") {
    super(Size.small);
    this.message    = message;
    this.modalTitle = title;
    this.ondelete   = ondelete;
  }

  body(): m.Children {
    return <div>{this.message}</div>;
  }

  title(): string {
    return this.modalTitle;
  }

  buttons(): m.ChildArray {
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

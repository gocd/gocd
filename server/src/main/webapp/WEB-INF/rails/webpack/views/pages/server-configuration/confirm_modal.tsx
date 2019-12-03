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
import {Cancel, Danger} from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "views/pages/page_operations";

export class ConfirmModal extends Modal {
  private readonly message: m.Children;
  private readonly modalTitle: string;
  private readonly oncancel: () => void;
  private readonly operationState: Stream<OperationState>;

  constructor(message: m.Children, oncancel: () => void, title = "Are you sure?") {
    super(Size.small);
    this.message        = message;
    this.modalTitle     = title;
    this.oncancel       = oncancel;
    this.operationState = Stream<OperationState>(OperationState.UNKNOWN);
  }

  body(): m.Children {
    return <div>{this.message}</div>;
  }

  title(): string {
    return this.modalTitle;
  }

  buttons(): m.ChildArray {
    return [
      <Danger data-test-id='button-cancel' onclick={this.oncancel} ajaxOperationMonitor={this.operationState}>Yes</Danger>,
      <Cancel ajaxOperationMonitor={this.operationState} data-test-id='button-no-cancel' onclick={this.close.bind(this)}
      >No</Cancel>
    ];
  }
}

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
import {OperationState} from "views/pages/page_operations";

export class ToggleConfirmModal extends Modal {
  private readonly message: m.Children;
  private readonly modalTitle: string;
  private readonly confirmAction: string;
  private onsave: () => Promise<any>;
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  constructor(message: m.Children,
              onsave: () => Promise<any>,
              title         = "Are you sure?",
              confirmAction = "Save") {
    super(Size.small);
    this.message       = message;
    this.modalTitle    = title;
    this.onsave        = onsave;
    this.confirmAction = confirmAction;
  }

  body(): m.Children {
    return <div>{this.message}</div>;
  }

  title(): string {
    return this.modalTitle;
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-save"
                       ajaxOperationMonitor={this.ajaxOperationMonitor}
                       ajaxOperation={this.onsave.bind(this)}>{this.confirmAction}</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel"
                      ajaxOperationMonitor={this.ajaxOperationMonitor}
                      onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }
}

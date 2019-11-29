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
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {OperationState} from "../page_operations";

export class ConfirmationDialog extends Modal {
  private readonly callback: () => Promise<string | void>;
  private readonly _title: string;
  private readonly _body: m.Children;
  private operationState: OperationState | undefined;

  constructor(title: string, body: m.Children, onConfirm: () => Promise<string | void>) {
    super(Size.small);
    this.callback = onConfirm;
    this._title   = title;
    this._body    = body;
  }

  body(): m.Children {
    return this._body;
  }

  title(): string {
    return this._title;
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id='button-trigger'
                       disabled={this.operationState === OperationState.IN_PROGRESS}
                       icon={this.operationState === OperationState.IN_PROGRESS ? ButtonIcon.SPINNER : undefined}
                       onclick={this.perform.bind(this)}>Yes</Buttons.Primary>,
      <Buttons.Cancel disabled={this.operationState === OperationState.IN_PROGRESS}
                      data-test-id='button-no-delete' onclick={this.close.bind(this)}
      >No</Buttons.Cancel>
    ];
  }

  private perform() {
    this.callback().finally(this.close.bind(this));
  }
}

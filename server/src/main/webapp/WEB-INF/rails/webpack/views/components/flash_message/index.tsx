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

import classnames from "classnames";
import {ErrorResponse} from "helpers/api_request_builder";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import * as Icons from "../icons";
import styles from "./index.scss";

type callback = () => void;

export enum MessageType {
  info,
  success,
  warning,
  alert,
  inProgress
}

export interface Attrs {
  type: MessageType;
  message?: m.Children;
  dismissible?: boolean;
  onDismiss?: () => any;
  dataTestId?: string;
}

export interface State {
  isDismissed: boolean;
  onDismiss?: () => void;
}

export class FlashMessage extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.onDismiss = () => {
      vnode.state.isDismissed = true;
      if (vnode.attrs.onDismiss) {
        vnode.attrs.onDismiss();
      }
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.state.isDismissed) {
      return;
    }

    if (_.isEmpty(vnode.attrs.message) && _.isEmpty(vnode.children)) {
      return;
    }

    const isDismissible = vnode.attrs.dismissible;

    let closeButton: m.Children;
    if (isDismissible) {
      closeButton = (
        <button class={styles.closeCallout}>
          <Icons.Close iconOnly={true} onclick={vnode.state.onDismiss}/>
        </button>
      );
    }

    const typeElement = MessageType[vnode.attrs.type];

    // @ts-ignore
    const style: string = styles[typeElement];

    const message = vnode.attrs.message ? <p>{vnode.attrs.message}</p> : undefined;

    const dataTestId = vnode.attrs.dataTestId ? vnode.attrs.dataTestId : `flash-message-${typeElement}`;

    return (
      <div data-test-id={dataTestId} class={classnames(styles.callout, style)}>
        {message}
        {vnode.children}
        {closeButton}
      </div>
    );
  }
}

export interface FlashProvider {
  clear: () => void;

  // add more as needed
  success: (message: m.Children, onTimeout?: callback) => void;
  alert: (message: m.Children, onTimeout?: callback) => void;
}

export class FlashMessageModel implements FlashProvider {
  protected _type: MessageType;
  protected _message?: m.Children;

  constructor(type: MessageType = MessageType.success, message?: m.Children) {
    this._type    = type;
    this._message = message;
  }

  hasMessage() {
    return !_.isNil(this._message);
  }

  get type() {
    return this._type;
  }

  get message() {
    return this._message;
  }

  success(message: m.Children) {
    this.setMessage(MessageType.success, message);
  }

  alert(message: m.Children) {
    this.setMessage(MessageType.alert, message);
  }

  setMessage(type: MessageType, message: m.Children) {
    this._type    = type;
    this._message = message;
  }

  clear() {
    this._message = undefined;
  }
}

export class FlashMessageModelWithTimeout extends FlashMessageModel implements FlashProvider {
  private readonly interval: number;
  private timeoutID?: number;

  constructor(interval = 10000, type: MessageType = MessageType.success, message?: m.Children) {
    super(type, message);
    this.interval = interval;
  }

  clear() {
    super.clear();
    this.clearTimeout();
  }

  success(message: m.Children, onTimeout?: callback) {
    this.setMessage(MessageType.success, message, onTimeout);
  }

  alert(message: m.Children, onTimeout?: callback) {
    this.setMessage(MessageType.alert, message, onTimeout);
  }

  consumeErrorResponse(errorResponse: ErrorResponse) {
    const parsed            = errorResponse.body ? JSON.parse(errorResponse.body!) : {};
    let message: m.Children = parsed.message ? parsed.message : errorResponse.message;

    if (parsed.data && parsed.data.errors) {
      message = (<div>{message}
        <ul>{_.flatten(Object.values(parsed.data.errors)).map(e => <li>{e}</li>)}</ul>
      </div>);
    }

    this.setMessage(MessageType.alert, message);
  }

  setMessage(type: MessageType, message: m.Children, timeoutCallback?: callback) {
    this.clear();
    super.setMessage(type, message);
    this.timeoutID = window.setTimeout(() => {
      this.clear();
      if (timeoutCallback) {
        timeoutCallback();
      }
      m.redraw();
    }, this.interval);
  }

  private clearTimeout() {
    if ("number" === typeof this.timeoutID) {
      window.clearTimeout(this.timeoutID);
    }
  }
}

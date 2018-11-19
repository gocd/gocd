/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {MithrilComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as Icons from "../icons";

import {bind} from "classnames/bind";
import * as styles from "./index.scss";

const classnames = bind(styles);

export enum MessageType {
  info,
  success,
  warning,
  alert
}

export interface Attrs {
  type: MessageType;
  message?: m.Children;
  dismissible?: boolean;
}

export interface State {
  isDismissed: boolean;
  onDismiss?: () => void;
}

export class FlashMessage extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.onDismiss = () => {
      vnode.state.isDismissed = true;
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
        <button className={classnames(styles.closeCallout)}>
          <Icons.Close iconOnly={true} onclick={vnode.state.onDismiss}/>
        </button>
      );
    }

    const typeElement = MessageType[vnode.attrs.type];

    // @ts-ignore
    const style: string = styles[typeElement];

    return (
      <div data-test-id={`flash-message-${typeElement}`} className={classnames(styles.callout, style)}>
        <p>{vnode.attrs.message}</p>
        {vnode.children}
        {closeButton}
      </div>
    );
  }
}

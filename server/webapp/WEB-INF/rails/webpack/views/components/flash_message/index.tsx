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
import * as m from 'mithril';
import * as Icons from "../icons";

import {bind} from 'classnames/bind';
import * as styles from './index.scss';

const classnames = bind(styles);

export interface Attrs {
  message: string | undefined | null;
  dismissible?: boolean;
}

export interface State {
  isDismissed: boolean;
  onDismiss?: () => void;
}

class FlashMessage extends MithrilComponent<Attrs, State> {
  private readonly type: string;

  protected constructor(type: string) {
    super();
    this.type = type;
  }

  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.onDismiss = () => {
      vnode.state.isDismissed = true;
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.state.isDismissed || !vnode.attrs.message) {
      return;
    }

    const isDismissible = vnode.attrs.dismissible;

    let closeButton: JSX.Element | undefined;
    if (isDismissible) {
      closeButton = (
        <button className={classnames(styles.closeCallout)}>
          <Icons.Close onclick={vnode.state.onDismiss}/>
        </button>
      );
    }

    return (
      <div className={classnames(this.type, styles.callout)}>
        <p>{vnode.attrs.message}</p>
        {closeButton}
      </div>
    );
  }
}

export class InfoFlashMessage extends FlashMessage {
  constructor() {
    super(styles.info);
  }
}

export class SuccessFlashMessage extends FlashMessage {
  constructor() {
    super(styles.success);
  }
}

export class WarnFlashMessage extends FlashMessage {
  constructor() {
    super(styles.warning);
  }
}

export class AlertFlashMessage extends FlashMessage {
  constructor() {
    super(styles.alert);
  }
}

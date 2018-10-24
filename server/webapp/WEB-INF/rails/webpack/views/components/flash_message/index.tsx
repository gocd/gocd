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

import {MithrilComponent} from "../../../jsx/mithril-component";
import * as m from 'mithril';

const styles     = require('./index.scss');
const classnames = require('classnames/bind').bind(styles);

export interface Attrs {
  message: string
}

class FlashMessage extends MithrilComponent<Attrs> {
  private readonly type: string;

  protected constructor(type: string) {
    super();
    this.type = type;
  }

  view(vnode: m.Vnode<Attrs>) {

    return (
      <div className={classnames(this.type, "callout")}>
        <h5>{vnode.attrs.message}</h5>
      </div>
    );
  }
}

export class InfoFlashMessage extends FlashMessage {
  constructor() {
    super('info');
  }
}

export class SuccessFlashMessage extends FlashMessage {
  constructor() {
    super('success');
  }
}

export class WarnFlashMessage extends FlashMessage {
  constructor() {
    super('warn');
  }
}

export class AlertFlashMessage extends FlashMessage {
  constructor() {
    super('alert');
  }
}

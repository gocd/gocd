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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as styles from "./index.scss";

const classnames          = bind(styles);

export interface Attrs {
  messages: string[];
  failed: boolean;
}

export class ProgressConsole extends MithrilViewComponent<Attrs> {

  view(vnode: m.Vnode<Attrs>) {
    return (
      <div class={classnames(styles.progressConsoleContainer, this.errorClass(vnode))}>
        {vnode.attrs.messages.map((message: string) => {
          return <p class={styles.message}>{message}</p>;
        })}
      </div>
    );
  }

  errorClass(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.failed) {
      return styles.error;
    }
  }
}

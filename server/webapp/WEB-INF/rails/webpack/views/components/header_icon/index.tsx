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
import * as styles from "./index.scss";

interface Attrs {
  imageUrl?: string; //an image URL of the icon
}

export class HeaderIcon extends MithrilComponent<Attrs> {

  view(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.imageUrl) {
      return <div class={styles.headerIcon}>
        <img src={vnode.attrs.imageUrl}/>
      </div>;
    }
    if (vnode.children && !_.isEmpty(vnode.children)) {
      return <div className={styles.headerIcon}>
        {vnode.children}
      </div>;
    }
    return <div class={styles.headerIcon}>
      <span className={styles.unknownIcon}/>
    </div>;
  }

}

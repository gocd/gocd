/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import s from "underscore.string";
import style from "views/pages/agents/index.scss";
import styles from "./index.scss";

const classnames = bind(style);

interface Attrs {
  imageUrl?: string; //an image URL of the icon
  name?: string;
  noMargin?: boolean;
}

export class HeaderIcon extends MithrilComponent<Attrs> {

  view(vnode: m.Vnode<Attrs>) {
    const name       = vnode.attrs.name || "Unknown Icon";
    const dataTestId = s.slugify(name);

    if (vnode.attrs.imageUrl) {
      return <div class={classnames(styles.headerIcon, {[styles.noMargin]: vnode.attrs.noMargin})}>
        <img alt={vnode.attrs.name} data-test-id={dataTestId} src={vnode.attrs.imageUrl}/>
      </div>;
    }
    if (vnode.children && !_.isEmpty(vnode.children)) {
      return <div class={styles.headerIcon}>
        {vnode.children}
      </div>;
    }
    return <div class={styles.headerIcon}>
      <span data-test-id={dataTestId} aria-label={name} class={styles.unknownIcon}/>
    </div>;
  }

}

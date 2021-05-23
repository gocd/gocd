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
import m from "mithril";

import classNames from "classnames/bind";
import defaultStyles from "./index.scss";

const classnames = classNames.bind(defaultStyles);
type Styles = typeof defaultStyles;

import {RestyleAttrs, RestyleViewComponent} from "jsx/mithril-component";

export interface Attrs extends RestyleAttrs<Styles> {
  small?: boolean;
  dataTestId?: string;
}

export class Spinner extends RestyleViewComponent<Styles, Attrs> {
  css: Styles = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const dataTestId = vnode.attrs.dataTestId ? vnode.attrs.dataTestId : "spinner";

    return (
      <span class={classnames(this.css.pageSpinner, {[this.css.small]: isSmall})} data-test-id={dataTestId}/>
    );
  }
}

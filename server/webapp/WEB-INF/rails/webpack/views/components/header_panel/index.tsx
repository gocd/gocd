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

import * as _ from "lodash";
import * as m from "mithril";

import {MithrilViewComponent} from "jsx/mithril-component";
import * as style from "./index.scss";

export interface Attrs {
  title: m.Children;
  sectionName?: m.Children;
  buttons?: m.Children;
}

export class HeaderPanel extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let buttons: m.Children = null;

    if (!_.isEmpty(vnode.attrs.buttons)) {
      buttons = (
        <div data-test-id="pageActions">
          {vnode.attrs.buttons}
        </div>
      );
    }

    return (<header className={style.pageHeader}>
      <div class={style.pageTitle}>
        {this.maybeSection(vnode)}
        <h1 class={style.title} data-test-id="title">{vnode.attrs.title}</h1>
      </div>
      {buttons}
    </header>);
  }

  private maybeSection(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.sectionName) {
      return (
        <h1 className={style.sectionName} data-test-id="section-name">{vnode.attrs.sectionName}</h1>
      );
    }
  }
}

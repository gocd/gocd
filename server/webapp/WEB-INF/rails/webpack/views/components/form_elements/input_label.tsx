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

import * as m from "mithril";
import {MithrilComponent} from "../../../jsx/mithril-component";

const styles     = require('./index.scss');
const classnames = require('classnames/bind').bind(styles);

export interface LabelAttrs {
  property: Function;
  label: string;
  helpText: string;
}

export class InputLabel extends MithrilComponent<LabelAttrs> {
  view(vnode: m.Vnode<LabelAttrs>) {
    return (
      <li className={classnames("form-group")}>
        <label htmlFor="item" className={classnames("form-label", "required")}>{vnode.attrs.label}:</label>
        <input type="text"
               value={vnode.attrs.property()}
               oninput={(evt: any) => vnode.attrs.property(evt.currentTarget.value)}
               className={classnames("form-control")} id="item"/>
        <span className={classnames("form-help")}>{vnode.attrs.helpText}</span>
      </li>
    );
  }
}

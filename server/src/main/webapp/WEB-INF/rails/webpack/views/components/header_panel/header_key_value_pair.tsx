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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import s from "underscore.string";
import Style from "./index.scss";

interface Attrs {
  data?: { [key: string]: m.Children };
}

export class HeaderKeyValuePair extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>) {
    if (_.isEmpty(vnode.attrs.data)) {
      return;
    }
    return <ul class={Style.keyValuePairs} data-test-id={"page-header-key-value-pairs"}>
      {_.map(vnode.attrs.data, HeaderKeyValuePair.toView)}
    </ul>;
  }

  private static toView(value: string, key: string): m.Children {
    const dataTestIdForKey   = s.slugify(`key-value-key-${key}`);
    const dataTestIdForValue = s.slugify(`key-value-value-${key}`);

    return <li data-test-id={"page-header-key-value-pair"} class={Style.keyValuePair}>
      <label data-test-id={dataTestIdForKey} class={Style.key}>{key}</label>
      <span data-test-id={dataTestIdForValue} class={Style.value}>{value}</span>
    </li>;
  }
}

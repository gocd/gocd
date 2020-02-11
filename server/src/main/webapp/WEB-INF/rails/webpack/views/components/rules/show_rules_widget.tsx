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
import m from "mithril";
import Stream from "mithril/stream";
import {Rules} from "models/rules/rules";
import {Table} from "../table";

interface Attrs {
  rules: Stream<Rules>;
}

export class ShowRulesWidget extends MithrilViewComponent<Attrs> {
  static headers() {
    return [
      "Directive",
      "Action",
      "Type",
      "Resource"
    ];
  }

  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    if (!vnode.attrs.rules || vnode.attrs.rules().length === 0) {
      return <div data-test-id="no-rules-info">
        <h3>Rules</h3>
        <em>No rules have been configured</em>
      </div>;
    }
    const ruleData = vnode.attrs.rules().map((rule) => {
      return [
        rule().directive(),
        rule().action(),
        rule().type(),
        rule().resource()
      ];
    });
    return <div data-test-id="rules-info">
      <h3>Rules</h3>
      <div data-test-id="rule-table">
        <Table headers={ShowRulesWidget.headers()} data={ruleData}/>
      </div>
    </div>;
  }
}

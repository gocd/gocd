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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {GoCDRole, PluginRole} from "models/roles/roles";
import {Table} from "views/components/table";

interface PolicyAttrs {
  role: GoCDRole | PluginRole;
}

export class PolicyWidget extends MithrilViewComponent<PolicyAttrs> {
  static headers() {
    return [
      "Permission"
      , "Action"
      , "Type"
      , "Resource"
    ];
  }

  view(vnode: m.Vnode<PolicyAttrs, this>): m.Children | void | null {
    if (!vnode.attrs.role.policy || vnode.attrs.role.policy().length === 0) {
      return;
    }
    const policyData = vnode.attrs.role.policy().map((directive) => {
      return [
        directive.permission(),
        directive.action(),
        directive.type(),
        directive.resource()
      ];
    });
    return <div data-test-id="policy-info">
      <h3>Policy</h3>
      <div data-test-id="policy-table">
        <Table headers={PolicyWidget.headers()} data={policyData}/>
      </div>
    </div>;
  }
}

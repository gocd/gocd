/*
 * Copyright 2017 ThoughtWorks, Inc.
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

let m                       = require('mithril');
const AgentStateCountWidget = {
  view (vnode) {
    const args = vnode.attrs;

    return (

      <ul class="search-summary">
        <li>
          <label for="total">Total</label>
          <span class="value">{args.agents().countAgent()}</span>
        </li>

        <li>
          <label for="total">Pending</label>
          <span class="value">{args.agents().countPendingAgents()}</span>
        </li>

        <li class="enabled">
          <label for="total">Enabled</label>
          <span class="value">{args.agents().countEnabledAgents()}</span>
        </li>

        <li class="disabled">
          <label for="total">Disabled</label>
          <span class="value">{args.agents().countDisabledAgents()}</span>
        </li>

      </ul>
    );
  }
};

module.exports = AgentStateCountWidget;

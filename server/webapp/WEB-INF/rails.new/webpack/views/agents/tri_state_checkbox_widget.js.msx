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

let m   = require('mithril');
const _ = require('lodash');

const TriStateCheckboxWidget = {
  view (vnode) {
    const args = vnode.attrs;

    return (
      <li>
        <input type="checkbox"
               id={`checkbox-${  args.index.toString()}`}
               class="select-resource"
               key={args.triStateCheckbox.name()}
               checked={args.triStateCheckbox.isChecked()}
               indeterminate={args.triStateCheckbox.isIndeterminate()}
               value={args.triStateCheckbox.name()}
               onchange={args.triStateCheckbox.click}>
        </input>
        <label for={`checkbox-${  args.index.toString()}`} title={args.triStateCheckbox.name()}>
          {_.truncate(args.triStateCheckbox.name(), {'length': 25})}
        </label>

      </li>
    );
  }
};

module.exports = TriStateCheckboxWidget;

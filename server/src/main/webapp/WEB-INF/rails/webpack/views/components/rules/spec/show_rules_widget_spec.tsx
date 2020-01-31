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

import m from "mithril";
import Stream from "mithril/stream";
import {Rules} from "models/rules/rules";
import {ruleTestData} from "models/rules/specs/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ShowRulesWidget} from "../show_rules_widget";

describe('ShowRulesWidgetSpec', () => {
  const helper = new TestHelper();
  let rules: Stream<Rules>;
  afterEach((done) => helper.unmount(done));

  function mount() {
    helper.mount(() => <ShowRulesWidget rules={rules}/>);
  }

  beforeEach(() => {
    rules = Stream(Rules.fromJSON([ruleTestData()]));
  });

  it('should not render anything if rules is empty', () => {
    rules([]);
    mount();

    expect(helper.byTestId('rules-info')).not.toBeInDOM();
    expect(helper.byTestId('no-rules-info')).toBeInDOM();
    expect(helper.q('h3').textContent).toBe('Rules');
    expect(helper.q('em').textContent).toBe('No rules have been configured');
  });

  it('should render the values in tabular format', () => {
    mount();

    expect(helper.byTestId('rules-info')).toBeInDOM();
    expect(helper.q('h3').textContent).toBe('Rules');
    expect(helper.q('thead').textContent).toBe('DirectiveActionTypeResource');
    const values = helper.qa('tbody td');
    expect(values.length).toBe(4);
    expect(values[0].textContent).toBe(rules()[0]().directive());
    expect(values[1].textContent).toBe(rules()[0]().action());
    expect(values[2].textContent).toBe(rules()[0]().type());
    expect(values[3].textContent).toBe(rules()[0]().resource());
  });
});

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
import Stream from "mithril/stream";
import {Rules} from "models/rules/rules";
import {ruleTestData} from "models/rules/specs/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ConfigureRulesWidget, RuleInfos, RulesType} from "../configure_rules_widget";

describe('ConfigureRulesWidgetSpec', () => {
  const helper = new TestHelper();
  let rules: Stream<Rules>;
  let infoMsg: m.Children | undefined;
  let resourceAutocompleteHelper: Map<string, string[]>;
  let actions: RuleInfos | undefined;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    rules                      = Stream(Rules.fromJSON([ruleTestData()]));
    infoMsg                    = undefined;
    resourceAutocompleteHelper = new Map<string, string[]>();
    actions                    = undefined;
  });

  function mount() {
    helper.mount(() => <ConfigureRulesWidget
      rules={rules}
      resourceAutocompleteHelper={resourceAutocompleteHelper}
      infoMsg={infoMsg}
      types={[RulesType.ENVIRONMENT, RulesType.PIPELINE_GROUP, RulesType.PLUGGABLE_SCM]}
      actions={actions}/>);
  }

  it('should render default components', () => {
    rules([]);

    mount();

    expect(helper.q('h2').textContent).toBe('Rules');
    expect(helper.byTestId('flash-message-info').textContent).toBe('The default rule is to deny access for all GoCD entities. Configure rules below to override that behavior.');
    expect(helper.byTestId('add-rule-button')).toBeInDOM();
  });

  it('should render info msg provided', () => {
    rules([]);
    infoMsg = "some random msg";

    mount();

    expect(helper.byTestId('flash-message-info').textContent).toBe(infoMsg);
  });

  it('should render rules in editable format', () => {
    mount();

    expect(helper.byTestId('rules-table')).toBeInDOM();
    expect(helper.q('thead').textContent).toBe('DirectiveTypeResourcesResource can be the name of the entity or a wildcard which matches one or more entities.');

    expect(helper.byTestId('rule-directive')).toBeInDOM();
    expect(helper.byTestId('rule-action')).not.toBeInDOM();
    expect(helper.byTestId('rule-type')).toBeInDOM();
    expect(helper.byTestId('rule-resource')).toBeInDOM();
    expect(helper.byTestId('rule-directive').textContent).toBe('SelectDenyAllow');
    expect(helper.byTestId('rule-type').textContent).toBe('SelectAllEnvironmentPipeline GroupPluggable SCM');
    expect((helper.byTestId('rule-directive') as HTMLSelectElement).value).toBe('allow');
    expect((helper.byTestId('rule-type') as HTMLSelectElement).value).toBe('pipeline_group');
    expect((helper.byTestId('rule-resource') as HTMLInputElement).value).toBe('DeployPipelines');
  });

  it('should render delete permission icon and update model on click', () => {
    mount();

    expect(rules().length).toBe(1);
    expect(helper.byTestId('rule-delete')).toBeInDOM();
    expect(helper.byTestId('rules-table')).toBeInDOM();

    helper.clickByTestId('rule-delete');

    expect(rules().length).toBe(0);
    expect(helper.byTestId('rules-table')).not.toBeInDOM();
  });

  it('should render actions if provided', () => {
    actions = [{text: 'Custom', id: 'custom'}];
    mount();

    expect(helper.byTestId('rule-action')).toBeInDOM();
    expect(helper.byTestId('rule-action').textContent).toBe('SelectAllReferCustom');
  });

  it('should render add rule button', () => {
    mount();

    expect(rules().length).toBe(1);

    helper.clickByTestId('add-rule-button');

    expect(rules().length).toBe(2);
    expect(rules()[1]().directive()).toBe('');
    expect(rules()[1]().action()).toBe('refer');
    expect(rules()[1]().type()).toBe('');
    expect(rules()[1]().resource()).toBe('');
  });
});

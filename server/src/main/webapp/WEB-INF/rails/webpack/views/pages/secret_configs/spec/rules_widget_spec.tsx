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

import m from "mithril";
import Stream from "mithril/stream";
import {Rule, Rules} from "models/rules/rules";
import {rulesTestData, ruleTestData} from "models/rules/specs/test_data";
import {RulesWidget} from "views/pages/secret_configs/rules_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("RulesWidget", () => {
  const helper = new TestHelper();
  afterEach((done) => helper.unmount(done));

  it("should show info for rules", () => {
    const rules = new Rules();
    mount(rules);
    expect(helper.byTestId("flash-message-info")).toBeInDOM();
    const infoAboutRules = "The default rule is to deny access to this secret configuration for all GoCD entities. Configure rules below to override that behavior.";
    expect(helper.textByTestId("flash-message-info")).toContain(infoAboutRules);
  });

  it("should show rule if it is present", () => {
    const ruleJSON = ruleTestData();
    const rules    = new Rules(Stream(Rule.fromJSON(ruleJSON)));

    mount(rules);

    const tableBody   = helper.byTestId("table-body");
    const tableRow    = helper.q("tr", tableBody);
    const tableHeader = helper.byTestId("table-header");

    expect(helper.byTestId("rules-table")).toBeInDOM();
    expect(tableHeader).toBeInDOM();
    expect(helper.qa("th", tableHeader).length).toBe(5);
    expect(tableHeader).toContainText("Directive");
    expect(tableHeader).toContainText("Type");
    expect(tableHeader).toContainText("Resources");

    expect(tableBody).toBeInDOM();

    expect(helper.qa("td", tableRow).length).toBe(5);
    expect(helper.byTestId("rule-directive")).toHaveValue("allow");
    expect(helper.textByTestId("rule-directive")).toContain("DenyAllow");
    expect(helper.byTestId("rule-type")).toHaveValue("pipeline_group");
    expect(helper.byTestId("rule-resource")).toHaveValue("DeployPipelines");

  });

  it("should show more than one rule", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);
    expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(2);
  });

  it("should callback the remove function and remove rule when cancel button is clicked", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);

    expect(rules.length).toBe(2);

    helper.clickByTestId("rule-delete", helper.byTestId("table-body"));
    expect(rules.length).toBe(1);
  });

  function mount(rules: Rules, resources: Map<string, string[]> = new Map()) {
    helper.mount(() => <RulesWidget rules={Stream(rules)} resourceAutocompleteHelper={resources} minChars={0}/>);
  }
});

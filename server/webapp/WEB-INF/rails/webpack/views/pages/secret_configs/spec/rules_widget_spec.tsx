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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {Rule, Rules} from "models/secret_configs/rules";
import {rulesTestData, ruleTestData} from "models/secret_configs/spec/test_data";
import * as simulateEvent from "simulate-event";
import {RulesWidget} from "views/pages/secret_configs/rules_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("RulesWidget", () => {
  const helper = new TestHelper();
  afterEach((done) => helper.unmount(done));

  it("should show info for rules", () => {
    const rules = new Rules();
    mount(rules);
    expect(helper.findByDataTestId("flash-message-info")).toBeInDOM();
    const infoAboutRules = "Configuring Rules is required to utilize this Secret Configuration. In absence of any rules, the secret configuration is denied access of any GoCD entities.";
    expect(helper.findByDataTestId("flash-message-info")).toContainText(infoAboutRules);
  });

  it("should show rule if it is present", () => {
    const ruleJSON = ruleTestData();
    const rules    = new Rules(stream(Rule.fromJSON(ruleJSON)));

    mount(rules);

    const tableBody   = helper.findByDataTestId("table-body");
    const tableRow    = tableBody.find("tr");
    const tableHeader = helper.findByDataTestId("table-header");

    expect(helper.findByDataTestId("rules-table")).toBeInDOM();
    expect(tableHeader).toBeInDOM();
    expect(tableHeader.find("th").length).toBe(5);
    expect(tableHeader.eq(0)).toContainText("Directive");
    expect(tableHeader.eq(0)).toContainText("Type");
    expect(tableHeader.eq(0)).toContainText("Resources");

    expect(tableBody).toBeInDOM();

    expect(tableRow.find("td").length).toBe(5);
    expect(helper.findByDataTestId("rule-directive").get(0)).toHaveValue("allow");
    expect(helper.findByDataTestId("rule-directive").get(0)).toContainText("DenyAllow");
    expect(helper.findByDataTestId("rule-type").get(0)).toHaveValue("pipeline_group");
    expect(helper.findByDataTestId("rule-resource").get(0)).toHaveValue("DeployPipelines");

  });

  it("should show more than one rule", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);
    expect(helper.findByDataTestId("table-body").find("tr").length).toBe(2);
  });

  it("should callback the remove function and remove rule when cancel button is clicked", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);

    expect(rules.length).toBe(2);

    const tableBody        = helper.findByDataTestId("table-body");
    const deleteRuleButton = helper.findIn(tableBody, "rule-delete")[0];
    simulateEvent.simulate(deleteRuleButton, "click");
    expect(rules.length).toBe(1);
  });

  function mount(rules: Rules, resources: Map<string, string[]> = new Map()) {
    helper.mount(() => <RulesWidget rules={stream(rules)} resourceAutocompleteHelper={resources} minChars={0}/>);
  }
});

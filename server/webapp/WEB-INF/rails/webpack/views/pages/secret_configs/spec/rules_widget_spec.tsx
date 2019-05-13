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
import * as styles from "views/components/forms/autocomplete.scss";
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

    const tableBody   = helper.findByDataTestId("rules-table-body");
    const tableRow    = helper.findByDataTestId("rules-table-row");
    const tableHeader = helper.findByDataTestId("rules-table-header");

    expect(helper.findByDataTestId("rules-table")).toBeInDOM();
    expect(tableHeader).toBeInDOM();
    expect(tableHeader.eq(0).children().length).toBe(5);
    expect(tableHeader.eq(0)).toContainText("Directive");
    expect(tableHeader.eq(0)).toContainText("Type");
    expect(tableHeader.eq(0)).toContainText("Resources");

    expect(tableBody).toBeInDOM();

    expect(tableRow.eq(0).children().length).toBe(5);
    expect(helper.findByDataTestId("rule-directive").get(0)).toHaveValue("allow");
    expect(helper.findByDataTestId("rule-directive").get(0)).toContainText("DenyAllow");
    expect(helper.findByDataTestId("rule-type").get(0)).toHaveValue("pipeline_group");
    expect(helper.findByDataTestId("rule-resource").get(0)).toHaveValue("DeployPipelines");

  });

  it("should show more than one rule", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);
    expect(helper.findByDataTestId("rules-table-row").length).toBe(2);
  });

  it("should callback the remove function and remove rule when cancel button is clicked", () => {
    const rulesJSON = rulesTestData();
    const rules     = Rules.fromJSON(rulesJSON);
    mount(rules);

    expect(rules.length).toBe(2);

    const tableBody        = helper.findByDataTestId("rules-table-body");
    const row              = helper.findIn(tableBody, "rules-table-row").eq(0);
    const deleteRuleButton = helper.findIn(row, "rule-delete")[0];
    simulateEvent.simulate(deleteRuleButton, "click");
    expect(rules.length).toBe(1);
  });

  it("should show the possible values of pipeline groups", (done) => {
    const map = new Map();
    map.set("pipeline_group", ["env-dev", "env-prod", "test"]);
    const ruleData = Rule.fromJSON(ruleTestData());
    ruleData.resource("env");
    ruleData.setProvider(map);
    ruleData.getProvider().onFinally(() => {
      expect(rulesResource.find("ul").find("li").length).toBe(2);
      expect(rulesResource.find("ul").find("li").get(0).innerText).toBe("env-dev");
      expect(rulesResource.find("ul").find("li").get(1).innerText).toBe("env-prod");
      done();
    });
    const rules = new Rules(stream(ruleData));
    mount(rules, map);
    const rulesResource    = helper.find(`.${styles.awesomplete}`);
    const inputForResource = helper.findIn(rulesResource, "rule-resource").get(0);
    simulateEvent.simulate(inputForResource, "focus");
    m.redraw();
  });

  function mount(rules: Rules, resources: Map<string, string[]> = new Map()) {
    helper.mount(() => <RulesWidget rules={stream(rules)} resourceAutocompleteHelper={resources} minChars={0}/>);
  }
});

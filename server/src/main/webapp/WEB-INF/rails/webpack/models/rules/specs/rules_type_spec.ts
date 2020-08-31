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

import {Rule, Rules} from "models/rules/rules";
import {rulesTestData, ruleTestData} from "./test_data";

describe("RulesModelSpec", () => {

  it("should deserialize rule json", () => {
    const ruleJSON = ruleTestData();

    const rule = Rule.fromJSON(ruleJSON);

    expect(rule.directive()).toEqual(ruleJSON.directive);
    expect(rule.action()).toEqual(ruleJSON.action);
    expect(rule.type()).toEqual(ruleJSON.type);
    expect(rule.resource()).toEqual(ruleJSON.resource);
  });

  describe("Validate", () => {

    it("should validate directive", () => {
      const ruleJSON = ruleTestData();
      // @ts-ignore
      delete ruleJSON.directive;

      const rule = Rule.fromJSON(ruleJSON);

      rule.isValid();

      expect(rule.errors().hasErrors()).toBeTruthy();
      expect(rule.errors().errorsForDisplay("directive")).toEqual("Directive must be present.");
    });

    it("should validate action", () => {
      const ruleJSON = ruleTestData();
      // @ts-ignore
      delete ruleJSON.action;

      const rule = Rule.fromJSON(ruleJSON);

      rule.isValid();

      expect(rule.errors().hasErrors()).toBeTruthy();
      expect(rule.errors().errorsForDisplay("action")).toEqual("Action must be present.");
    });

    it("should validate type", () => {
      const ruleJSON = ruleTestData();
      // @ts-ignore
      delete ruleJSON.type;

      const rule = Rule.fromJSON(ruleJSON);

      rule.isValid();

      expect(rule.errors().hasErrors()).toBeTruthy();
      expect(rule.errors().errorsForDisplay("type")).toEqual("Type must be present.");
    });
  });

  it("should deserialize rules json", () => {
    const rulesJSON = rulesTestData();

    const rules = Rules.fromJSON(rulesJSON);

    expect(rules).toHaveLength(2);

    expect(rules[0]().directive()).toEqual(rulesJSON[0].directive);
    expect(rules[0]().action()).toEqual(rulesJSON[0].action);
    expect(rules[0]().type()).toEqual(rulesJSON[0].type);
    expect(rules[0]().resource()).toEqual(rulesJSON[0].resource);

    expect(rules[1]().directive()).toEqual(rulesJSON[1].directive);
    expect(rules[1]().action()).toEqual(rulesJSON[1].action);
    expect(rules[1]().type()).toEqual(rulesJSON[1].type);
    expect(rules[1]().resource()).toEqual(rulesJSON[1].resource);
  });
});

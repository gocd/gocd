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
import {Stream} from "mithril/stream";
import {Rule} from "models/secret_configs/rules";
import * as Buttons from "views/components/buttons";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {RulesWidget} from "views/pages/secret_configs/rules_widget";
import * as styles from "./index.scss";

export default class RuleWidget {
  // static provider: Map<Stream<Rule>, ResourceSuggestionProvider> = new Map();
  private readonly rule: Stream<Rule>;
  private readonly removeRule: (ruleToBeRemoved: Stream<Rule>) => void;
  // private resourceAutocompleteHelper: Map<string, string[]>;
  private readonly provider: ResourceSuggestionProvider;

  constructor(rule: Stream<Rule>,
              resourceAutocompleteHelper: Map<string, string[]>,
              removeRuleCallBack: (ruleToBeRemoved: Stream<Rule>) => void) {
    this.rule       = rule;
    this.removeRule = removeRuleCallBack;
    // this.resourceAutocompleteHelper = resourceAutocompleteHelper;
    this.provider   = new ResourceSuggestionProvider(rule, resourceAutocompleteHelper);
    // if (!RuleWidget.provider.has(rule)) {
    //   RuleWidget.provider.set(rule, this.provider);
    // }
  }

  // static getProvider(rule: Stream<Rule>) {
  //   if (RuleWidget.provider.has(rule)) {
  //     return RuleWidget.provider.get(rule);
  //   }
  // }

  getViewData() {
    return [
      <SelectField dataTestId="rule-directive"
                   property={this.rule().directive}
                   required={true}
                   errorText={this.rule().errors().errorsForDisplay("directive")}>
        <SelectFieldOptions selected={this.rule().directive()}
                            items={RulesWidget.directives()}/>
      </SelectField>,
      <SelectField
        dataTestId="rule-type"
        property={this.proxyType.bind(this, this.rule, this.provider)}
        required={true}
        errorText={this.rule().errors().errorsForDisplay("type")}>
        <SelectFieldOptions selected={this.rule().type()}
                            items={RulesWidget.types()}/>
      </SelectField>,
      <AutocompleteField
        minChars={1}
        autoFirst={true}
        dataTestId="rule-resource"
        property={this.rule().resource}
        provider={this.provider}
        errorText={this.rule().errors().errorsForDisplay("resource")}
        required={true}/>,
      <Buttons.Cancel data-test-id="rule-delete"
                      onclick={this.remove.bind(this, this.rule)}>
        <span className={styles.iconDelete}></span>
      </Buttons.Cancel>

    ];
  }

  private remove(rule: Stream<Rule>) {
    this.removeRule(rule);
  }

  private proxyType(rule: Stream<Rule>, provider: SuggestionProvider, newType?: string): string {
    if (!newType) {
      return rule().type();
    }
    rule().type(newType);
    provider.update();
    return newType;
  }
}

class ResourceSuggestionProvider extends SuggestionProvider {
  private rule: Stream<Rule>;
  private suggestion: Map<string, string[]>;

  constructor(rule: Stream<Rule>, suggestion: Map<string, string[]>) {
    super();
    this.rule       = rule;
    this.suggestion = suggestion;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    if (this.suggestion.has(this.rule().type())) {
      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve(this.suggestion.get(this.rule().type()));
      });
    }

    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([]);
    });
  }

}

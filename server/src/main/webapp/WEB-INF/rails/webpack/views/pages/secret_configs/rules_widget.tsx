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
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Rule, Rules} from "models/rules/rules";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {ResourceSuggestionProvider} from "views/components/rules/suggestion_provider";
import {Table} from "views/components/table";
import styles from "views/pages/secret_configs/index.scss";

interface Attrs {
  rules: Stream<Rules>;
}

interface AutoCompleteAttrs extends Attrs {
  resourceAutocompleteHelper: Map<string, string[]>;
  minChars?: number;
}

export class RulesWidget extends MithrilViewComponent<AutoCompleteAttrs> {
  static headers() {
    return [
      "Directive"
      , "Type"
      , <div>
        Resources
        <span class={styles.warningWrapper}>
          <i class={styles.infoIcon}/>
          <div class={styles.warningContent}>
            Resource can be the name of the entity or a wildcard which matches one or more entities.
          </div>
        </span>
      </div>
      , ""
    ];
  }

  static directives() {
    return [
      {
        id: "", text: "Select"
      }
      , {
        id: "deny", text: "Deny"
      }
      , {
        id: "allow", text: "Allow"
      }
    ];
  }

  static types() {
    return [
      {
        id: "", text: "Select"
      }
      , {
        id: "*", text: "All"
      }, {
        id: "pipeline_group", text: "Pipeline Group"
      }, {
        id: "environment", text: "Environment"
      }
    ];
  }

  view(vnode: m.Vnode<AutoCompleteAttrs, this>): m.Children | void | null {
    const removeRuleCallback = (ruleToBeRemoved: Stream<Rule>) => {
      const index = vnode.attrs.rules().findIndex((r) => r === ruleToBeRemoved);
      if (index !== -1) {
        vnode.attrs.rules().splice(index, 1);
      }
    };
    const ruleBody           = _.isEmpty(vnode.attrs.rules())
      ? undefined
      : <div data-test-id="rules-table"
             class={styles.rulesTable}>
        <Table headers={RulesWidget.headers()}
               data={new RulesWidgetBody(vnode.attrs.rules,
                                         vnode.attrs.resourceAutocompleteHelper,
                                         removeRuleCallback).getData()}
               draggable={true}
               dragHandler={this.reArrange.bind(this, vnode.attrs.rules)}/>
      </div>;
    return <div data-test-id="rules-widget">
      <h2>Rules </h2>
      <FlashMessage type={MessageType.info}
                    message="The default rule is to deny access to this secret configuration for all GoCD entities. Configure rules below to override that behavior."/>
      {ruleBody}
    </div>;
  }

  private reArrange(rules: Stream<Rules>, oldIndex: number, newIndex: number) {
    const originalRules = rules();
    originalRules.splice(newIndex, 0, originalRules.splice(oldIndex, 1)[0]);
    rules(originalRules);
    m.redraw();
  }
}

class RulesWidgetBody {
  private rules: Stream<Rules>;
  private resourceAutocompleteHelper: Map<string, string[]>;
  private removeRule: (ruleToBeRemoved: Stream<Rule>) => void;

  constructor(rules: Stream<Rules>,
              resourceAutocompleteHelper: Map<string, string[]>,
              removeRule: (ruleToBeRemoved: Stream<Rule>) => void) {
    this.rules                      = rules;
    this.resourceAutocompleteHelper = resourceAutocompleteHelper;
    this.removeRule                 = removeRule;
  }

  getData(): m.Child[][] {
    return _.map(this.rules(), (rule) => {
      const provider = Stream(new ResourceSuggestionProvider(rule, this.resourceAutocompleteHelper));
      return [<SelectField dataTestId="rule-directive"
                           property={rule().directive}
                           required={true}
                           errorText={rule().errors().errorsForDisplay("directive")}>
        <SelectFieldOptions selected={rule().directive()}
                            items={RulesWidget.directives()}/>
      </SelectField>,
        <SelectField
          dataTestId="rule-type"
          property={rule().type}
          required={true}
          onchange={() => provider().update()}
          errorText={rule().errors().errorsForDisplay("type")}>
          <SelectFieldOptions selected={rule().type()}
                              items={RulesWidget.types()}/>
        </SelectField>,
        <AutocompleteField
          key={rule().type()}
          autoEvaluate={false}
          dataTestId="rule-resource"
          property={rule().resource}
          provider={provider()}
          errorText={rule().errors().errorsForDisplay("resource")}
          required={true}/>,
        <Buttons.Cancel data-test-id="rule-delete"
                        onclick={this.removeRule.bind(this, rule)}>
          <span class={styles.iconDelete}></span>
        </Buttons.Cancel>
      ];
    });
  }
}

export class RulesInfoWidget extends MithrilViewComponent<Attrs> {
  static headers() {
    return [
      "Directive"
      , "Type"
      , "Resource"
    ];
  }

  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    if (!vnode.attrs.rules || vnode.attrs.rules().length === 0) {
      return;
    }
    const ruleData = vnode.attrs.rules().map((rule) => {
      return [
        rule().directive(),
        rule().type(),
        rule().resource()
      ];
    });
    return <div data-test-id="rules-info">
      <h3>Rules</h3>
      <div data-test-id="rule-table" class={styles.rulesWrapper}>
        <Table headers={RulesInfoWidget.headers()} data={ruleData}/>
      </div>
    </div>;
  }
}

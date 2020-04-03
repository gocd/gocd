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

import {RestyleViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Accessor} from "models/base/accessor";
import {Rule, Rules} from "models/rules/rules";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import defaultStyles from "./rules.scss";
import {ResourceSuggestionProvider} from "./suggestion_provider";

interface RuleInfo {
  id: string;
  text: string;
}

export let RulesType = {
  ENVIRONMENT:    {text: 'Environment', id: 'environment'} as RuleInfo,
  PIPELINE:       {text: 'Pipeline', id: 'pipeline'} as RuleInfo,
  PIPELINE_GROUP: {text: 'Pipeline Group', id: 'pipeline_group'} as RuleInfo,
};

export let RuleActions = {
  REFER: {text: 'Refer', id: 'refer'} as RuleInfo
};

export type RuleInfos = RuleInfo[];

interface Attrs {
  rules: Accessor<Rules>;
  resourceAutocompleteHelper: Map<string, string[]>;
  infoMsg?: m.Children;
  actions?: RuleInfos;
  types: RuleInfos;
}

type Styles = typeof defaultStyles;

export class ConfigureRulesWidget extends RestyleViewComponent<Styles, Attrs> {
  css = defaultStyles;

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

  static defaultTypes(): RuleInfos {
    return [
      {
        id: "", text: "Select"
      } as RuleInfo
      , {
        id: "*", text: "All"
      } as RuleInfo
    ];
  }

  static defaultActions(): RuleInfos {
    return [
      {
        id: "", text: "Select"
      } as RuleInfo
      , {
        id: "*", text: "All"
      } as RuleInfo,
      RuleActions.REFER
    ];
  }

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    const infoMsg          = vnode.attrs.infoMsg
      ? vnode.attrs.infoMsg
      : "The default rule is to deny access for all GoCD entities. Configure rules below to override that behavior.";
    const showActionColumn = vnode.attrs.actions !== undefined;
    const ruleBody         = _.isEmpty(vnode.attrs.rules())
      ? undefined
      : <div data-test-id="rules-table">
        <Table headers={this.headers(showActionColumn)}
               data={this.getTableData(vnode, showActionColumn)}
               draggable={true}
               dragHandler={this.rearrangeRules.bind(this, vnode.attrs.rules)}/>
      </div>;

    return <div data-test-id="rules-widget">
      <h2>Rules</h2>
      <FlashMessage type={MessageType.info} message={infoMsg}/>
      {ruleBody}
      <div className={this.css.addRule}>
        <Buttons.Secondary data-test-id="add-rule-button" onclick={this.addNewRule.bind(this, vnode)}>
          + New Permission
        </Buttons.Secondary>
      </div>
    </div>;
  }

  private addNewRule(vnode: m.Vnode<Attrs>) {
    vnode.attrs.rules().push(Stream(new Rule("", "refer", "", "")));
  }

  private headers(showActionColumn: boolean) {
    const defaultHeaders = [
      "Directive",
      "Type",
      <div>
        Resources
        <span className={this.css.warningWrapper}>
          <i class={this.css.infoIcon}/>
          <div class={this.css.warningContent}>
            Resource can be the name of the entity or a wildcard which matches one or more entities.
          </div>
        </span>
      </div>,
      ""
    ];
    if (showActionColumn) {
      defaultHeaders.splice(1, 0, "Action");
    }
    return defaultHeaders;
  }

  private rearrangeRules(rules: Accessor<Rules>, oldIndex: number, newIndex: number) {
    const originalRules = rules();
    originalRules.splice(newIndex, 0, originalRules.splice(oldIndex, 1)[0]);
    rules(originalRules);
    m.redraw();
  }

  private getTableData(vnode: m.Vnode<Attrs>, showActionColumn: boolean): m.Child[][] {
    const removeRuleCallback = (ruleToBeRemoved: Accessor<Rule>) => {
      const index = vnode.attrs.rules().findIndex((r) => r === ruleToBeRemoved);
      if (index !== -1) {
        vnode.attrs.rules().splice(index, 1);
      }
    };
    return _.map(vnode.attrs.rules(), (rule) => {
      const provider  = Stream(new ResourceSuggestionProvider(rule, vnode.attrs.resourceAutocompleteHelper));
      const ruleTypes = ConfigureRulesWidget.defaultTypes();
      ruleTypes.push(...vnode.attrs.types);
      const tableDataRows = [
        <SelectField dataTestId="rule-directive"
                     property={rule().directive}
                     required={true}
                     css={this.css}
                     errorText={rule().errors().errorsForDisplay("directive")}>
          <SelectFieldOptions selected={rule().directive()}
                              items={ConfigureRulesWidget.directives()}/>
        </SelectField>,

        <SelectField
          dataTestId="rule-type"
          property={rule().type}
          required={true}
          css={this.css}
          onchange={() => provider().update()}
          errorText={rule().errors().errorsForDisplay("type")}>
          <SelectFieldOptions selected={rule().type()}
                              items={ruleTypes}/>
        </SelectField>,

        <AutocompleteField
          key={rule().type()}
          autoEvaluate={false}
          dataTestId="rule-resource"
          property={rule().resource}
          provider={provider()}
          css={this.css}
          fieldCss={this.css}
          errorText={rule().errors().errorsForDisplay("resource")}
          required={true}/>,

        <Buttons.Cancel data-test-id="rule-delete"
                        onclick={removeRuleCallback.bind(this, rule)}>
          <span class={this.css.iconDelete}/>
        </Buttons.Cancel>
      ];
      if (showActionColumn) {
        const ruleActions = ConfigureRulesWidget.defaultActions();
        ruleActions.push(...vnode.attrs.actions!);
        const actionColumn = <SelectField dataTestId="rule-action"
                                          property={rule().action}
                                          required={true}
                                          errorText={rule().errors().errorsForDisplay("action")}>
          <SelectFieldOptions selected={rule().action()}
                              items={ruleActions}/>
        </SelectField>;
        tableDataRows.splice(1, 0, actionColumn);
      }
      return tableDataRows;
    });
  }
}

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
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {Rule, Rules} from "models/secret_configs/rules";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Table} from "views/components/table";
import * as styles from "views/pages/secret_configs/index.scss";

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
      ""
      , "Directive"
      , "Type"
      , <div>
        Resources
        <span className={styles.warningWrapper}>
          <i className={styles.infoIcon}/>
          <div className={styles.warningContent}>
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
        id: "pipeline_group", text: "Pipeline Group"
      }
    ];
  }

  view(vnode: m.Vnode<AutoCompleteAttrs, this>): m.Children | void | null {
    return <div data-test-id="rules-widget">
      <h2>Rules </h2>
      <FlashMessage type={MessageType.info}
                    message="Configuring Rules is required to utilize this Secret Configuration. In absence of any rules, the secret configuration is denied access of any GoCD entities."/>
      <div data-test-id="rules-table" className={styles.flexTable}>
        <div data-test-id="rules-table-header" className={styles.tableHeader}>
          {
            _.map(RulesWidget.headers(), (header) => {
              return <div className={styles.tableHead}>{header}</div>;
            })
          }
        </div>
        <div data-test-id="rules-table-body" className={styles.tableBody}>
          {
            _.map(vnode.attrs.rules(), (rule) => {
              if (!rule().getProvider()) {
                rule().setProvider(vnode.attrs.resourceAutocompleteHelper);
              }
              return <div data-test-id="rules-table-row" className={styles.tableRow}>
                <div className={styles.tableCell}>
                  <span className={styles.iconDrag}></span>
                </div>
                <div className={styles.tableCell}>
                  <SelectField dataTestId="rule-directive"
                               property={rule().directive}
                               required={true}
                               errorText={rule().errors().errorsForDisplay("directive")}>
                    <SelectFieldOptions selected={rule().directive()}
                                        items={RulesWidget.directives()}/>
                  </SelectField>
                </div>
                <div className={styles.tableCell}>
                  <SelectField
                    dataTestId="rule-type"
                    property={this.proxyType.bind(this, rule)}
                    required={true}
                    errorText={rule().errors().errorsForDisplay("type")}>
                    <SelectFieldOptions selected={rule().type()}
                                        items={RulesWidget.types()}/>
                  </SelectField>
                </div>
                <div className={styles.tableCell}>
                  <AutocompleteField
                    minChars={vnode.attrs.minChars || 1}
                    dataTestId="rule-resource"
                    property={rule().resource}
                    provider={rule().getProvider()}
                    errorText={rule().errors().errorsForDisplay("resource")}
                    required={true}/>
                </div>
                <div className={styles.tableCell}>
                  <Buttons.Cancel data-test-id="rule-delete"
                                  onclick={this.removeRule.bind(this, vnode, rule)}>
                    <span className={styles.iconDelete}></span>
                  </Buttons.Cancel>
                </div>
              </div>;
            })
          }
        </div>
      </div>
    </div>;
  }

  removeRule(vnode: m.Vnode<AutoCompleteAttrs, this>, ruleToBeRemoved: Stream<Rule>) {
    const index = vnode.attrs.rules().findIndex((r) => r === ruleToBeRemoved);
    if (index !== -1) {
      vnode.attrs.rules().splice(index, 1);
    }
  }

  private proxyType(rule: Stream<Rule>, newType?: string): string {
    if (!newType) {
      return rule().type();
    }
    rule().type(newType);
    rule().updateProvider();
    return newType;
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
      <div data-test-id="rule-table" className={styles.rulesWrapper}>
        <Table headers={RulesInfoWidget.headers()} data={ruleData}/>
      </div>
    </div>;
  }
}

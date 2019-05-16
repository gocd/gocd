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
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Table} from "views/components/table";
import * as styles from "views/pages/secret_configs/index.scss";
import RuleWidget from "views/pages/secret_configs/rule_widget";

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
    const tableData = _.map(vnode.attrs.rules(), (rule) => {

      const ruleWidget = new RuleWidget(rule,
                                        vnode.attrs.resourceAutocompleteHelper,
                                        (ruleToBeRemoved: Stream<Rule>) => {
                                          const index = vnode.attrs.rules().findIndex((r) => r === ruleToBeRemoved);
                                          if (index !== -1) {
                                            vnode.attrs.rules().splice(index, 1);
                                          }
                                        });
      return ruleWidget.getViewData();
    });

    return <div data-test-id="rules-widget">
      <h2>Rules </h2>
      <FlashMessage type={MessageType.info}
                    message="Configuring Rules is required to utilize this Secret Configuration. In absence of any rules, the secret configuration is denied access of any GoCD entities."/>
      <div data-test-id="rules-table" className={styles.rulesTable}>
        <Table headers={RulesWidget.headers()} data={tableData} draggable={true}
               dragHandler={this.reArrange.bind(this, vnode.attrs.rules)}/>
      </div>
    </div>;
  }

  private reArrange(rule: Stream<Rules>, oldIndex: number, newIndex: number) {
    const splicedRule = rule().splice(oldIndex, 1);
    rule().splice(newIndex, 0, splicedRule[0]);
    // rule().forEach((rule) => rule().updateProvider());
    m.redraw();
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

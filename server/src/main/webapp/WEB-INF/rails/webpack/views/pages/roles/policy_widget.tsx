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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Directive, GoCDRole, PluginRole, Policy} from "models/roles/roles";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {Table} from "views/components/table";
import styles from "./index.scss";

interface PolicyAttrs {
  role: GoCDRole | PluginRole;
}

export class PolicyWidget extends MithrilViewComponent<PolicyAttrs> {
  static headers() {
    return [
      "Permission",
      "Action",
      "Type",
      "Resource"
    ];
  }

  view(vnode: m.Vnode<PolicyAttrs, this>): m.Children | void | null {
    if (!vnode.attrs.role.policy || vnode.attrs.role.policy().length === 0) {
      return;
    }
    const policyData = vnode.attrs.role.policy().map((directive) => {
      return [
        directive().permission(),
        directive().action(),
        directive().type(),
        directive().resource()
      ];
    });
    return <div data-test-id="policy-info">
      <h3>Policy</h3>
      <div data-test-id="policy-table">
        <Table headers={PolicyWidget.headers()} data={policyData}/>
      </div>
    </div>;
  }
}

interface Attrs {
  policy: Stream<Policy>;
}

interface AutoCompleteAttrs extends Attrs {
  resourceAutocompleteHelper: Map<string, string[]>;
  minChars?: number;
}

class PolicyWidgetBody {
  private policy: Stream<Policy>;
  private resourceAutocompleteHelper: Map<string, string[]>;
  private removePermission: (ruleToBeRemoved: Stream<Directive>) => void;

  constructor(policy: Stream<Policy>,
              resourceAutocompleteHelper: Map<string, string[]>,
              removePermissionCallback: (ruleToBeRemoved: Stream<Directive>) => void) {
    this.policy                     = policy;
    this.resourceAutocompleteHelper = resourceAutocompleteHelper;
    this.removePermission           = removePermissionCallback;
  }

  getData(): m.Child[][] {
    return _.map(this.policy(), (permission) => {
      const provider = Stream(new ResourceSuggestionProvider(permission, this.resourceAutocompleteHelper));
      return [
        <SelectField dataTestId="permission-permission"
                     property={permission().permission}
                     required={true}
                     errorText={permission().errors().errorsForDisplay("permission")}>
          <SelectFieldOptions selected={permission().permission()}
                              items={CreatePolicyWidget.permissions()}/>
        </SelectField>,
        <SelectField dataTestId="permission-action"
                     property={permission().action}
                     required={true}
                     errorText={permission().errors().errorsForDisplay("action")}>
          <SelectFieldOptions selected={permission().action()}
                              items={CreatePolicyWidget.actions()}/>
        </SelectField>,
        <SelectField
          dataTestId="permission-type"
          property={permission().type}
          required={true}
          onchange={() => provider().update()}
          errorText={permission().errors().errorsForDisplay("type")}>
          <SelectFieldOptions selected={permission().type()}
                              items={CreatePolicyWidget.types()}/>
        </SelectField>,
        <AutocompleteField
          key={permission().type()}
          autoEvaluate={false}
          dataTestId="permission-resource"
          property={permission().resource}
          provider={provider()}
          errorText={permission().errors().errorsForDisplay("resource")}
          required={true}/>,
        <Buttons.Cancel data-test-id="permission-delete"
                        onclick={this.removePermission.bind(this, permission)}>
          <span class={styles.iconDelete}></span>
        </Buttons.Cancel>
      ];
    });
  }
}

export class CreatePolicyWidget extends MithrilViewComponent<AutoCompleteAttrs> {
  static headers() {
    return [
      "Permission",
      "Action",
      "Type",
      <div>
        Resources
        <span class={styles.warningWrapper}>
          <i class={styles.infoIcon}/>
          <div class={styles.warningContent}>
            Resource can be the name of the entity or a wildcard which matches one or more entities.
          </div>
        </span>
      </div>,
      ""
    ];
  }

  static permissions() {
    return [
      {
        id: "", text: "Select"
      },
      {
        id: "allow", text: "Allow"
      },
      {
        id: "deny", text: "Deny"
      }
    ];
  }

  static actions() {
    return [
      {
        id: "", text: "Select"
      },
      {
        id: "view", text: "View"
      },
      {
        id: "administer", text: "Administer"
      }
    ];
  }

  static types() {
    return [
      {
        id: "", text: "Select"
      },
      {
        id: "*", text: "All"
      },
      {
        id: "environment", text: "Environment"
      }, {
        id: "config_repo", text: "Config Repository"
      },
      {
        id: "cluster_profile", text: "Cluster Profile"
      },
      {
        id: "elastic_agent_profile", text: "Elastic Agent Profile"
      }
    ];
  }

  view(vnode: m.Vnode<AutoCompleteAttrs, this>): m.Children | void | null {
    const removePermissionCallback = (directiveToBeRemoved: Stream<Directive>) => {
      const index = vnode.attrs.policy().findIndex((directive) => directive === directiveToBeRemoved);
      if (index !== -1) {
        vnode.attrs.policy().splice(index, 1);
      }
    };
    const message                  = <span>Configure the policy below to manage access to GoCD entities for users in this role. <Link
      externalLinkIcon={true} target="_blank"
      href={docsUrl("configuration/dev_authorization.html#role-based-access-control")}>Learn More</Link></span>;
    const policyBody               = vnode.attrs.policy && _.isEmpty(vnode.attrs.policy())
                                     ? <FlashMessage type={MessageType.info}
                                                     message={message}/>
                                     : <div data-test-id="policy-table" class={styles.selectPermission}>
                                       <Table headers={CreatePolicyWidget.headers()}
                                              data={new PolicyWidgetBody(vnode.attrs.policy,
                                                                         vnode.attrs.resourceAutocompleteHelper,
                                                                         removePermissionCallback).getData()}
                                              draggable={true}
                                              dragHandler={this.reArrange.bind(this, vnode.attrs.policy)}/>
                                     </div>;
    return <div data-test-id="policy-widget">
      <h2>Policy </h2>
      {policyBody}
    </div>;
  }

  private reArrange(policy: Stream<Policy>, oldIndex: number, newIndex: number) {
    const originalPolicy = policy();
    originalPolicy.splice(newIndex, 0, originalPolicy.splice(oldIndex, 1)[0]);
    policy(originalPolicy);
    m.redraw();
  }
}

export class ResourceSuggestionProvider extends SuggestionProvider {
  private rule: Stream<Directive>;
  private suggestion: Map<string, string[]>;

  constructor(rule: Stream<Directive>, suggestion: Map<string, string[]>) {
    super();
    this.rule       = rule;
    this.suggestion = suggestion;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    if (this.rule().type() === "*") {
      const allSuggestions: Set<string> = new Set();
      this.suggestion.forEach((value, key) => {
        value.forEach((val) => allSuggestions.add(val));
      });

      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve(Array.from(allSuggestions.values()));
      });
    }
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

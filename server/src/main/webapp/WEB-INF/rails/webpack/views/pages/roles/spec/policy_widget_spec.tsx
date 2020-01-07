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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {Directive, Policy} from "models/roles/roles";
import {CreatePolicyWidget} from "views/pages/roles/policy_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe('PolicyWidgetSpecs', () => {
  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  it('should display info regarding default policy on when no policy have been configured', () => {
    mount();

    const infoText = "Configure the policy below to manage access to GoCD entities for users in this role. Learn More";
    expect(helper.byTestId("flash-message-info")).toBeInDOM();
    expect(helper.byTestId("flash-message-info")).toContainText(infoText);
    expect(helper.q("a", helper.byTestId("flash-message-info")).getAttribute("href")).toEqual(docsUrl("configuration/dev_authorization.html#role-based-access-control"));

  });

  it('should render permissions if present', () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    mount(policy);

    const tableBody   = helper.byTestId("table-body");
    const tableRow    = helper.q("tr", tableBody);
    const tableHeader = helper.byTestId("table-header");

    expect(helper.byTestId("policy-table")).toBeInDOM();
    expect(tableHeader).toBeInDOM();
    expect(helper.qa("th", tableHeader).length).toBe(6);
    expect(tableHeader).toContainText("Permission");
    expect(tableHeader).toContainText("Action");
    expect(tableHeader).toContainText("Type");
    expect(tableHeader).toContainText("Resources");

    expect(tableBody).toBeInDOM();

    expect(helper.qa("td", tableRow).length).toBe(6);
    expect(helper.byTestId("permission-permission")).toHaveValue("allow");
    expect(helper.textByTestId("permission-permission")).toContain("SelectAllowDeny");
    expect(helper.byTestId("permission-action")).toHaveValue("view");
    expect(helper.textByTestId("permission-action")).toContain("SelectViewAdminister");
    expect(helper.byTestId("permission-type")).toHaveValue("*");
    expect(helper.textByTestId("permission-type")).toContain("SelectAllEnvironment");
    expect(helper.byTestId("permission-resource")).toHaveValue("env");

  });

  it('should not display info regarding default policy if configured', () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    mount(policy);

    expect(helper.byTestId("flash-message-info")).not.toBeInDOM();
  });

  it('should render more than one permissions if present', () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    policy.push(Stream(new Directive("allow", "view", "*", "env123")));
    mount(policy);

    expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(2);

  });

  it("should callback the remove function and remove permission when cancel button is clicked", () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    policy.push(Stream(new Directive("allow", "view", "*", "env12")));
    mount(policy);

    expect(policy.length).toBe(2);

    helper.clickByTestId("permission-delete", helper.byTestId("table-body"));
    expect(policy.length).toBe(1);

  });

  it("should render 4 items in supported type dropdown", () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    mount(policy);

    const typeOptions = helper.qa("option", helper.byTestId("permission-type"));

    expect(typeOptions.length).toBe(6);
    expect(typeOptions[1]).toHaveText("All");
    expect(typeOptions[1]).toHaveValue("*");
    expect(typeOptions[2]).toHaveText("Environment");
    expect(typeOptions[2]).toHaveValue("environment");
    expect(typeOptions[3]).toHaveText("Config Repository");
    expect(typeOptions[3]).toHaveValue("config_repo");
    expect(typeOptions[4]).toHaveText("Cluster Profile");
    expect(typeOptions[4]).toHaveValue("cluster_profile");
    expect(typeOptions[5]).toHaveText("Elastic Agent Profile");
    expect(typeOptions[5]).toHaveValue("elastic_agent_profile");
  });

  it("should render 4 items in supported actions dropdown", () => {
    const policy = new Policy();
    policy.push(Stream(new Directive("allow", "view", "*", "env")));
    mount(policy);

    const actionOptions = helper.qa("option", helper.byTestId("permission-action"));

    expect(actionOptions.length).toBe(3);
    expect(actionOptions[1]).toHaveText("View");
    expect(actionOptions[1]).toHaveValue("view");
    expect(actionOptions[2]).toHaveText("Administer");
    expect(actionOptions[2]).toHaveValue("administer");
  });

  function mount(policy: Policy = new Policy(), resourceAutoComplete: Map<string, string[]> = new Map()) {
    helper.mount(() => <CreatePolicyWidget policy={Stream(policy)} resourceAutocompleteHelper={resourceAutoComplete}/>);
  }
});

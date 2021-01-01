/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import {
  AccessTokensWidgetForAdmin,
  formatTimeInformation,
  getLastUsedInformation,
  getRevokedBy
} from "views/pages/access_tokens/access_tokens_widget";
import {AccessTokenTestData} from "views/pages/access_tokens/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AccessTokensWidgetForAdminSpec", () => {
  const helper          = new TestHelper();
  const onRevoke        = jasmine.createSpy("onRevoke");
  const allAccessTokens = AccessTokens.fromJSON(AccessTokenTestData.all(
    AccessTokenTestData.new("This is active token 1"),
    AccessTokenTestData.new("This is active token 2"),
    AccessTokenTestData.newRevoked("This is revoked token 3"),
    AccessTokenTestData.new("This is active token 1", "John"),
    AccessTokenTestData.newRevoked("This is revoked token 2", "John"),
    AccessTokenTestData.newRevoked("This is revoked token 3", "John")
  ));

  beforeAll(() => {
    document.body.setAttribute("data-username", "bob");
  });

  afterAll(() => {
    document.body.removeAttribute("data-username");
  });

  function mount(accessTokens = allAccessTokens, searchText = Stream("")) {
    helper.mount(() => <AccessTokensWidgetForAdmin accessTokens={Stream(accessTokens)}
                                                   onRevoke={onRevoke}
                                                   searchText={searchText}/>);
  }

  it("should be able to render info when no access tokens have been created", () => {
    helper.mount(() => <AccessTokensWidgetForAdmin accessTokens={Stream(new AccessTokens())} onRevoke={onRevoke}
                                                   searchText={Stream("")}/>);

    const helpElement = helper.byTestId("access_token_info");
    expect(helpElement).toBeInDOM();
    expect(helpElement.textContent).toContain('Navigate to Personal Access Tokens.Click on "Generate Token" to create new personal access token.The generated token can be used to access the GoCD API. Learn More');
    expect(helper.qa('a', helpElement)[0]).toHaveAttr('href', '/go/access_tokens');
    expect(helper.qa('a', helpElement)[1]).toHaveAttr('href', docsUrl("configuration/access_tokens.html"));

    expect(helper.byTestId("tab-header-0")).toBeFalsy();
    expect(helper.byTestId("tab-header-1")).toBeFalsy();
  });

  it("should be able to render two tabs when access tokens are present", () => {
    mount();

    expect(helper.byTestId("access-token-info")).toBeFalsy();
    expect(helper.byTestId("tab-header-0")).toBeInDOM();
    expect(helper.byTestId("tab-header-0")).toHaveText("Active Tokens");
    expect(helper.byTestId("tab-header-1")).toBeInDOM();
    expect(helper.textByTestId("tab-header-0")).toContain("Active Token");
  });

  it("should be able to render active access tokens data", () => {
    const allActiveAccessTokens = allAccessTokens.activeTokens();
    mount(allActiveAccessTokens);

    const activeTokensTab = helper.byTestId("tab-header-0");
    expect(activeTokensTab).toBeInDOM();
    expect(activeTokensTab).toHaveText("Active Tokens");

    const activeTokenTableHeaders = helper.qa("th", helper.byTestId("table-header-row", helper.byTestId("tab-content-0")));
    expect(activeTokenTableHeaders.item(0)).toHaveText("Created By");
    expect(activeTokenTableHeaders.item(1)).toHaveText("Description");
    expect(activeTokenTableHeaders.item(2)).toHaveText("Created At");
    expect(activeTokenTableHeaders.item(3)).toHaveText("Last Used");
    expect(activeTokenTableHeaders.item(4)).toHaveText("Revoke");

    const activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
    expect(activeTokenTableRows.length).toBe(3);
    assertActiveTokenRow(activeTokenTableRows.item(0), allActiveAccessTokens[0]());
    assertActiveTokenRow(activeTokenTableRows.item(1), allActiveAccessTokens[1]());
    assertActiveTokenRow(activeTokenTableRows.item(2), allActiveAccessTokens[2]());

    const revokedTokensTab = helper.byTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(helper.allByTestId("table-header-row", helper.byTestId("tab-content-1"))).toHaveLength(0);
    expect(helper.byTestId("tab-content-1")).toHaveText("There are no revoked tokens.");
  });

  it("should be able to render revoked access tokens data", () => {
    const revokedTokens = allAccessTokens.revokedTokens();
    mount(revokedTokens);

    const revokedTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
    expect(revokedTokenTableRows.length).toBe(3);
    assertRevokedTokenRow(revokedTokenTableRows.item(0), revokedTokens[0]());
    assertRevokedTokenRow(revokedTokenTableRows.item(1), revokedTokens[1]());
    assertRevokedTokenRow(revokedTokenTableRows.item(2), revokedTokens[2]());

    const activeTokensTab = helper.byTestId("tab-header-0");
    expect(activeTokensTab).toHaveText("Active Tokens");
    expect(helper.allByTestId("table-header-row", helper.byTestId("tab-content-0"))).toHaveLength(0);
    expect(helper.byTestId("tab-content-0")).toHaveText("There are no active tokens.");
  });

  describe("Search By Description", () => {
    it("should filter active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
      expect(activeTokenTableRows).toHaveLength(3);

      searchText("token 1");
      helper.redraw();

      const filteredActiveAccessTokens = allAccessTokens.activeTokens().filterBySearchText("token 1");
      activeTokenTableRows             = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
      expect(activeTokenTableRows).toHaveLength(2);
      assertActiveTokenRow(activeTokenTableRows.item(0), filteredActiveAccessTokens[0]());
      assertActiveTokenRow(activeTokenTableRows.item(1), filteredActiveAccessTokens[1]());
    });

    it("should filter revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 2");
      helper.redraw();

      const filteredRevokedAccessTokens = allAccessTokens.revokedTokens().filterBySearchText("token 2");
      activeTokenTableRows              = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
      expect(activeTokenTableRows.length).toEqual(1);
      assertRevokedTokenRow(activeTokenTableRows.item(0), filteredRevokedAccessTokens[0]());
    });

    it("should show message if the search query results in empty array for active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      const activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 14");
      helper.redraw();

      const tabContent = helper.byTestId("tab-content-0");
      expect(tabContent).toHaveText("There are no active tokens matching your search query.");
    });

    it("should show message if the search query results in empty array for revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      const activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 14");
      helper.redraw();

      const tabContent = helper.byTestId("tab-content-1");
      expect(tabContent).toHaveText("There are no revoked tokens matching your search query.");
    });
  });

  describe("Search By Username", () => {
    it("should filter active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("John");
      helper.redraw();

      const filteredActiveTokens = allAccessTokens.activeTokens().filterBySearchText("John");
      activeTokenTableRows       = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
      expect(activeTokenTableRows.length).toEqual(1);
      assertActiveTokenRow(activeTokenTableRows.item(0), filteredActiveTokens[0]());
    });

    it("should filter revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("John");
      helper.redraw();

      const filteredRevokedTokens = allAccessTokens.revokedTokens().filterBySearchText("John");
      activeTokenTableRows        = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
      expect(activeTokenTableRows.length).toEqual(2);
      assertRevokedTokenRow(activeTokenTableRows.item(0), filteredRevokedTokens[0]());
      assertRevokedTokenRow(activeTokenTableRows.item(1), filteredRevokedTokens[1]());
    });
  });

  afterEach(helper.unmount.bind(helper));

  function assertActiveTokenRow(elem: Element, accessToken: AccessToken) {
    const columns = helper.qa("td", elem);
    expect(columns.item(0)).toContainText(accessToken.username());
    expect(columns.item(1)).toContainText(accessToken.description());
    expect(columns.item(2)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.item(3)).toContainText(getLastUsedInformation(accessToken));
  }

  function assertRevokedTokenRow(elem: Element, accessToken: AccessToken) {
    const columns = helper.qa("td", elem);
    expect(columns.item(0)).toContainText(accessToken.username());
    expect(columns.item(1)).toContainText(accessToken.description());
    expect(columns.item(2)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.item(3)).toContainText(getLastUsedInformation(accessToken));
    expect(columns.item(4)).toContainText(getRevokedBy(accessToken.revokedBy()));
    expect(columns.item(5)).toContainText(formatTimeInformation(accessToken.revokedAt()!));
    expect(columns.item(6)).toContainText(accessToken.revokeCause()!);
  }
});

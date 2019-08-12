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
import m from "mithril";
import Stream from "mithril/stream";

import {AccessToken, AccessTokens} from "models/access_tokens/types";
import {
  AccessTokensWidgetForAdmin,
  formatTimeInformation, getLastUsedInformation, getRevokedBy
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

    expect(helper.findByDataTestId("access_token_info")).toBeInDOM();
    expect(helper.findByDataTestId("access_token_info").text())
      .toContain(
        "Navigate to Personal Access TokensClick on \"Generate Token\" to create new personal access token.The generated token can be used to access the GoCD API.");

    expect(helper.findByDataTestId("tab-header-0")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-header-1")).not.toBeInDOM();
  });

  it("should be able to render two tabs when access tokens are present", () => {
    mount();

    expect(helper.findByDataTestId("access-token-info")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-header-0")).toBeInDOM();
    expect(helper.findByDataTestId("tab-header-0")).toHaveText("Active Tokens");
    expect(helper.findByDataTestId("tab-header-1")).toBeInDOM();
    expect(helper.findByDataTestId("tab-header-0").text()).toContain("Active Token");
  });

  it("should be able to render active access tokens data", () => {
    const allActiveAccessTokens = allAccessTokens.activeTokens();
    mount(allActiveAccessTokens);

    const activeTokensTab = helper.findByDataTestId("tab-header-0");
    expect(activeTokensTab).toBeInDOM();
    expect(activeTokensTab).toHaveText("Active Tokens");
    const activeTokenTableHeaders = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-header-row")
                                          .find("th");
    expect(activeTokenTableHeaders.eq(0)).toHaveText("Created By");
    expect(activeTokenTableHeaders.eq(1)).toHaveText("Description");
    expect(activeTokenTableHeaders.eq(2)).toHaveText("Created At");
    expect(activeTokenTableHeaders.eq(3)).toHaveText("Last Used");
    expect(activeTokenTableHeaders.eq(4)).toHaveText("Revoke");

    const activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
    expect(activeTokenTableRows.length).toEqual(3);
    assertActiveTokenRow(activeTokenTableRows.eq(0), allActiveAccessTokens[0]());
    assertActiveTokenRow(activeTokenTableRows.eq(1), allActiveAccessTokens[1]());
    assertActiveTokenRow(activeTokenTableRows.eq(2), allActiveAccessTokens[2]());

    const revokedTokensTab = helper.findByDataTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(helper.findIn(helper.findByDataTestId("tab-content-1"), "table-header-row")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-content-1")).toHaveText("There are no revoked tokens.");
  });

  it("should be able to render revoked access tokens data", () => {
    const revokedTokens = allAccessTokens.revokedTokens();
    mount(revokedTokens);

    const revokedTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
    expect(revokedTokenTableRows.length).toEqual(3);
    assertRevokedTokenRow(revokedTokenTableRows.eq(0), revokedTokens[0]());
    assertRevokedTokenRow(revokedTokenTableRows.eq(1), revokedTokens[1]());
    assertRevokedTokenRow(revokedTokenTableRows.eq(2), revokedTokens[2]());

    const activeTokensTab = helper.findByDataTestId("tab-header-0");
    expect(activeTokensTab).toHaveText("Active Tokens");
    expect(helper.findIn(helper.findByDataTestId("tab-content-0"), "table-header-row")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-content-0"))
      .toHaveText("There are no active tokens.");
  });

  describe("Search By Description", () => {
    it("should filter active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 1");
      helper.redraw();

      const filteredActiveAccessTokens = allAccessTokens.activeTokens().filterBySearchText("token 1");
      activeTokenTableRows             = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
      expect(activeTokenTableRows.length).toEqual(2);
      assertActiveTokenRow(activeTokenTableRows.eq(0), filteredActiveAccessTokens[0]());
      assertActiveTokenRow(activeTokenTableRows.eq(1), filteredActiveAccessTokens[1]());
    });

    it("should filter revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 2");
      helper.redraw();

      const filteredRevokedAccessTokens = allAccessTokens.revokedTokens().filterBySearchText("token 2");
      activeTokenTableRows              = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
      expect(activeTokenTableRows.length).toEqual(1);
      assertRevokedTokenRow(activeTokenTableRows.eq(0), filteredRevokedAccessTokens[0]());
    });

    it("should show message if the search query results in empty array for active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      const activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 14");
      helper.redraw();

      const tabContent = helper.findByDataTestId("tab-content-0");
      expect(tabContent).toHaveText("There are no active tokens matching your search query.");
    });

    it("should show message if the search query results in empty array for revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      const activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("token 14");
      helper.redraw();

      const tabContent = helper.findByDataTestId("tab-content-1");
      expect(tabContent).toHaveText("There are no revoked tokens matching your search query.");
    });
  });

  describe("Search By Username", () => {
    it("should filter active tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("John");
      helper.redraw();

      const filteredActiveTokens = allAccessTokens.activeTokens().filterBySearchText("John");
      activeTokenTableRows       = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
      expect(activeTokenTableRows.length).toEqual(1);
      assertActiveTokenRow(activeTokenTableRows.eq(0), filteredActiveTokens[0]());
    });

    it("should filter revoked tokens", () => {
      const searchText = Stream("");
      mount(allAccessTokens, searchText);

      let activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
      expect(activeTokenTableRows.length).toEqual(3);

      searchText("John");
      helper.redraw();

      const filteredRevokedTokens = allAccessTokens.revokedTokens().filterBySearchText("John");
      activeTokenTableRows        = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
      expect(activeTokenTableRows.length).toEqual(2);
      assertRevokedTokenRow(activeTokenTableRows.eq(0), filteredRevokedTokens[0]());
      assertRevokedTokenRow(activeTokenTableRows.eq(1), filteredRevokedTokens[1]());
    });
  });

  afterEach(helper.unmount.bind(helper));

  function assertActiveTokenRow(elem: any, accessToken: AccessToken) {
    const columns = elem.find("td");
    expect(columns.eq(0)).toContainText(accessToken.username());
    expect(columns.eq(1)).toContainText(accessToken.description());
    expect(columns.eq(2)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.eq(3)).toContainText(getLastUsedInformation(accessToken));
  }

  function assertRevokedTokenRow(elem: any, accessToken: AccessToken) {
    const columns = elem.find("td");
    expect(columns.eq(0)).toContainText(accessToken.username());
    expect(columns.eq(1)).toContainText(accessToken.description());
    expect(columns.eq(2)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.eq(3)).toContainText(getLastUsedInformation(accessToken));
    expect(columns.eq(4)).toContainText(getRevokedBy(accessToken.revokedBy()));
    expect(columns.eq(5)).toContainText(formatTimeInformation(accessToken.revokedAt()!));
    expect(columns.eq(6)).toContainText(accessToken.revokeCause()!);
  }
});

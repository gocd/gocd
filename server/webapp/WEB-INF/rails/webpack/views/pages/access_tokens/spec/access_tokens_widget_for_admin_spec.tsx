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
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import {AccessTokensWidgetForAdmin, formatTimeInformation} from "views/pages/access_tokens/access_tokens_widget";
import {AccessTokenTestData} from "views/pages/access_tokens/spec/test_data";

describe("AccessTokensWidgetForAdminSpec", () => {
  let $root: any, root: any;
  const onRevoke        = jasmine.createSpy("onRevoke");
  const allAccessTokens = AccessTokens.fromJSON(AccessTokenTestData.all([AccessTokenTestData.validAccessToken()]));
  const validToken      = AccessToken.fromJSON(AccessTokenTestData.validAccessToken());

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  function mount(accessTokens: Stream<AccessTokens>) {
    m.mount(root, {
      view() {
        return <AccessTokensWidgetForAdmin accessTokens={accessTokens} onRevoke={onRevoke} searchText={stream()}/>;
      }
    });
    m.redraw();
  }

  it("should be able to render info when no access tokens have been created", () => {
    mount(stream(new AccessTokens()));

    expect(findByDataTestId("access_token_info")).toBeInDOM();
    expect(findByDataTestId("access_token_info").text())
      .toContain(
        "Click on \"Generate Token\" to create new personal access token.A Generated token can be used to access the GoCD API.");

    expect(findByDataTestId("tab-header-0")).not.toBeInDOM();
    expect(findByDataTestId("tab-header-1")).not.toBeInDOM();
  });

  it("should be able to render two tabs with table when access tokens are present", () => {
    mount(stream(allAccessTokens));

    expect(findByDataTestId("access_token_info")).not.toBeInDOM();

    expect(findByDataTestId("tab-header-0")).toBeInDOM();
    expect(findByDataTestId("tab-header-1")).toBeInDOM();
    expect(findByDataTestId("tab-header-0").text()).toContain("Active Token");
    expect(findByDataTestId("tab-header-1").text()).toContain("Revoked Token");
    expect(findByDataTestId("tab-content-0")).toBeInDOM();
    expect(findByDataTestId("tab-content-1")).toBeInDOM();
    expect(findByDataTestId("tab-content-0")).toBeVisible();

    const tabContent = find("div[data-test-id=\"tab-content-0\"]");
    expect(findIn(tabContent, "table-header-row")).toBeInDOM();
    const tabContentHeaderRow = findIn(findByDataTestId("tab-content-0"),
                                       "table-header-row").find("th");
    expect(tabContentHeaderRow.length).toEqual(5);
    expect(tabContentHeaderRow.eq(0)).toHaveText("Created By");
    expect(tabContentHeaderRow.eq(1)).toHaveText("Description");
    expect(tabContentHeaderRow.eq(2)).toHaveText("Created At");
    expect(tabContentHeaderRow.eq(3)).toHaveText("Last Used");
    expect(tabContentHeaderRow.eq(4)).toHaveText("Revoke");

    expect(findByDataTestId("tab-content-1")).toBeHidden();
    const revokedTokensTab = findByDataTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(findIn(findByDataTestId("tab-content-1"), "table-header-row")).not.toBeInDOM();
    expect(findByDataTestId("tab-content-1")).toHaveText("You don't have any revoked tokens.");

  });

  it("should be able to render active access tokens data", () => {
    mount(stream(allAccessTokens));

    const accessTokensRows = find("div[data-test-id=\"tab-content-0\"] tr[data-test-id=\"table-row\"]");

    expect(accessTokensRows.length).toEqual(1);
    expect(accessTokensRows.first().find("td")[0].innerHTML).toContain(validToken.username());
    expect(accessTokensRows.first().find("td")[1].innerHTML).toContain(validToken.description());
    expect(accessTokensRows.first().find("td")[2].innerHTML).toContain(formatTimeInformation(validToken.createdAt()));
    expect(accessTokensRows.first().find("td")[4].innerHTML).toContain("Revoke");
  });

  it("should be able to render revoked access tokens data", () => {
    const revokedToken    = AccessToken.fromJSON(AccessTokenTestData.revokedAccessToken());
    const allAccessTokens = AccessTokens.fromJSON(AccessTokenTestData.all([AccessTokenTestData.validAccessToken(), AccessTokenTestData.revokedAccessToken()]));

    mount(stream(allAccessTokens));

    const accessTokensRows = find("div[data-test-id=\"tab-content-1\"] tr[data-test-id=\"table-row\"]");

    expect(accessTokensRows.length).toEqual(1);
    expect(accessTokensRows.first().find("td")[0].innerHTML).toContain(revokedToken.username());
    expect(accessTokensRows.first().find("td")[1].innerHTML).toContain(revokedToken.description());
    expect(accessTokensRows.first().find("td")[2].innerHTML).toContain(formatTimeInformation(revokedToken.createdAt()));
    const revokedAt = revokedToken.revokedAt();
    expect(accessTokensRows.first().find("td")[4].innerHTML)
      .toContain((revokedAt ? formatTimeInformation(revokedAt) : ""));
    expect(accessTokensRows.first().find("td")[5].innerHTML).toContain(revokedToken.revokeCause());
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function find(selectors: string) {
    return $root.find(selectors);
  }

  function findIn(elem: any, dataTestId: string) {
    return elem.find(`[data-test-id='${dataTestId}']`);
  }
});

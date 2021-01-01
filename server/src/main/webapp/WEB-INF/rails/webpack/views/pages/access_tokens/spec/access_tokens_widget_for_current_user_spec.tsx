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

import m from "mithril";
import Stream from "mithril/stream";
import {AccessTokenTestData} from "models/access_tokens/spec/access_token_test_data";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import {
  AccessTokensWidgetForCurrentUser,
  formatTimeInformation,
  getLastUsedInformation, getRevokedBy
} from "views/pages/access_tokens/access_tokens_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AccessTokensWidgetForCurrentUserSpec", () => {
  const helper          = new TestHelper();
  const onRevoke        = jasmine.createSpy("onRevoke");
  const allAccessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());
  const validToken      = AccessToken.fromJSON(AccessTokenTestData.validAccessToken());
  const revokedToken    = AccessToken.fromJSON(AccessTokenTestData.revokedAccessToken());

  it("should be able to render info when no access tokens have been created", () => {
    mount(Stream(new AccessTokens()));

    expect(helper.byTestId("access-token-info")).toBeInDOM();
    expect(helper.textByTestId("access-token-info")).
      toContain("Click on \"Generate Token\" to create new personal access token.A Generated token can be used to access the GoCD API.");

    expect(helper.byTestId("tab-header-0")).toBeFalsy();
    expect(helper.byTestId("tab-header-1")).toBeFalsy();
  });

  it("should render two tabs", () => {
    mount(Stream(allAccessTokens));

    expect(helper.byTestId("access-token-info")).toBeFalsy();
    expect(helper.byTestId("tab-header-0")).toBeInDOM();
    expect(helper.byTestId("tab-header-0")).toHaveText("Active Tokens");
    expect(helper.byTestId("tab-header-1")).toBeInDOM();
    expect(helper.byTestId("tab-header-1")).toHaveText("Revoked Tokens");
  });

  it("should list all active tokens under active tokens tab", () => {
    mount(Stream(new AccessTokens(Stream(validToken))));

    const activeTokensTab = helper.byTestId("tab-header-0");
    expect(activeTokensTab).toBeInDOM();
    expect(activeTokensTab).toHaveText("Active Tokens");

    const activeTokenTableHeaders = helper.qa("th", helper.byTestId("table-header-row", helper.byTestId("tab-content-0")));

    expect(activeTokenTableHeaders.item(0)).toHaveText("Description");
    expect(activeTokenTableHeaders.item(1)).toHaveText("Created At");
    expect(activeTokenTableHeaders.item(2)).toHaveText("Last Used");
    expect(activeTokenTableHeaders.item(3)).toHaveText("Revoke");

    const activeTokenTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-0"));
    expect(activeTokenTableRows.length).toBe(1);
    assertActiveTokenRow(activeTokenTableRows.item(0), validToken);

    const revokedTokensTab = helper.byTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(helper.byTestId("table-header-row", helper.byTestId("tab-content-1"))).toBeFalsy();
    expect(helper.byTestId("tab-content-1")).toHaveText("You don't have any revoked tokens.");
  });

  it("should list all revoked tokens under revoked tokens tab", () => {
    mount(Stream(new AccessTokens(Stream(revokedToken))));

    const activeTokensTab = helper.byTestId("tab-header-0");
    expect(activeTokensTab).toHaveText("Active Tokens");
    expect(helper.byTestId("table-header-row", helper.byTestId("tab-content-0"))).toBeFalsy();
    expect(helper.byTestId("tab-content-0"))
      .toHaveText("You don't have any active tokens. Click on 'Generate Token' button to create a new token.");

    const revokedTokensTab = helper.byTestId("tab-header-1");
    helper.click(revokedTokensTab);

    expect(revokedTokensTab).toBeInDOM();
    expect(revokedTokensTab).toHaveText("Revoked Tokens");

    const revokedTokenTableHeaders = helper.qa("th", helper.byTestId("table-header-row", helper.byTestId("tab-content-1")));
    expect(revokedTokenTableHeaders.item(0)).toHaveText("Description");
    expect(revokedTokenTableHeaders.item(1)).toHaveText("Created At");
    expect(revokedTokenTableHeaders.item(2)).toHaveText("Last Used");
    expect(revokedTokenTableHeaders.item(3)).toHaveText("Revoked By");
    expect(revokedTokenTableHeaders.item(4)).toHaveText("Revoked At");
    expect(revokedTokenTableHeaders.item(5)).toHaveText("Revoked Message");

    const revokedTokensTableRows = helper.allByTestId("table-row", helper.byTestId("tab-content-1"));
    expect(revokedTokensTableRows.length).toEqual(1);
    assertRevokedTokenRow(revokedTokensTableRows.item(0), revokedToken);
  });

  function mount(accessTokens: Stream<AccessTokens>) {
    helper.mount(() => <AccessTokensWidgetForCurrentUser accessTokens={accessTokens} onRevoke={onRevoke}/>);
  }

  afterEach(helper.unmount.bind(helper));

  function assertActiveTokenRow(elem: Element, accessToken: AccessToken) {
    const columns = helper.qa("td", elem);
    expect(columns.item(0)).toContainText(accessToken.description());
    expect(columns.item(1)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.item(2)).toContainText(getLastUsedInformation(accessToken));
  }

  function assertRevokedTokenRow(elem: Element, accessToken: AccessToken) {
    const columns = helper.qa("td", elem);
    expect(columns.item(0)).toContainText(accessToken.description());
    expect(columns.item(1)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.item(2)).toContainText(getLastUsedInformation(accessToken));
    expect(columns.item(3)).toContainText(getRevokedBy(accessToken.revokedBy()));
    expect(columns.item(4)).toContainText(formatTimeInformation(accessToken.revokedAt()!));
    expect(columns.item(5)).toContainText(accessToken.revokeCause()!);
  }
});

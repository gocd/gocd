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
import {AccessTokenTestData} from "models/access_tokens/spec/access_token_test_data";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as simulateEvent from "simulate-event";
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
    mount(stream(new AccessTokens()));

    expect(helper.findByDataTestId("access-token-info")).toBeInDOM();
    expect(helper.findByDataTestId("access-token-info").text())
      .toContain(
        "Click on \"Generate Token\" to create new personal access token.A Generated token can be used to access the GoCD API.");

    expect(helper.findByDataTestId("tab-header-0")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-header-1")).not.toBeInDOM();
  });

  it("should render two tabs", () => {
    mount(stream(allAccessTokens));

    expect(helper.findByDataTestId("access-token-info")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-header-0")).toBeInDOM();
    expect(helper.findByDataTestId("tab-header-0")).toHaveText("Active Tokens");
    expect(helper.findByDataTestId("tab-header-1")).toBeInDOM();
    expect(helper.findByDataTestId("tab-header-1")).toHaveText("Revoked Tokens");
  });

  it("should list all active tokens under active tokens tab", () => {
    mount(stream(new AccessTokens(stream(validToken))));

    const activeTokensTab = helper.findByDataTestId("tab-header-0");
    expect(activeTokensTab).toBeInDOM();
    expect(activeTokensTab).toHaveText("Active Tokens");
    const activeTokenTableHeaders = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-header-row")
                                          .find("th");
    expect(activeTokenTableHeaders.eq(0)).toHaveText("Description");
    expect(activeTokenTableHeaders.eq(1)).toHaveText("Created At");
    expect(activeTokenTableHeaders.eq(2)).toHaveText("Last Used");
    expect(activeTokenTableHeaders.eq(3)).toHaveText("Revoke");

    const activeTokenTableRows = helper.findIn(helper.findByDataTestId("tab-content-0"), "table-row");
    expect(activeTokenTableRows.length).toEqual(1);
    assertActiveTokenRow(activeTokenTableRows.eq(0), validToken);

    const revokedTokensTab = helper.findByDataTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(helper.findIn(helper.findByDataTestId("tab-content-1"), "table-header-row")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-content-1")).toHaveText("You don't have any revoked tokens.");
  });

  it("should list all revoked tokens under revoked tokens tab", () => {
    mount(stream(new AccessTokens(stream(revokedToken))));

    const activeTokensTab = helper.findByDataTestId("tab-header-0");
    expect(activeTokensTab).toHaveText("Active Tokens");
    expect(helper.findIn(helper.findByDataTestId("tab-content-0"), "table-header-row")).not.toBeInDOM();
    expect(helper.findByDataTestId("tab-content-0"))
      .toHaveText("You don't have any active tokens. Click on 'Generate Token' button to create a new token.");

    const revokedTokensTab = helper.findByDataTestId("tab-header-1");
    simulateEvent.simulate(revokedTokensTab.get(0), "click");
    helper.redraw();

    expect(revokedTokensTab).toBeInDOM();
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    const revokedTokenTableHeaders = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-header-row")
                                           .find("th");
    expect(revokedTokenTableHeaders.eq(0)).toHaveText("Description");
    expect(revokedTokenTableHeaders.eq(1)).toHaveText("Created At");
    expect(revokedTokenTableHeaders.eq(2)).toHaveText("Last Used");
    expect(revokedTokenTableHeaders.eq(3)).toHaveText("Revoked By");
    expect(revokedTokenTableHeaders.eq(4)).toHaveText("Revoked At");
    expect(revokedTokenTableHeaders.eq(5)).toHaveText("Revoked Message");

    const revokedTokensTableRows = helper.findIn(helper.findByDataTestId("tab-content-1"), "table-row");
    expect(revokedTokensTableRows.length).toEqual(1);
    assertRevokedTokenRow(revokedTokensTableRows.eq(0), revokedToken);
  });

  function mount(accessTokens: Stream<AccessTokens>) {
    helper.mount(() => <AccessTokensWidgetForCurrentUser accessTokens={accessTokens} onRevoke={onRevoke}/>);
  }

  afterEach(helper.unmount.bind(helper));

  function assertActiveTokenRow(elem: any, accessToken: AccessToken) {
    const columns = elem.find("td");
    expect(columns.eq(0)).toContainText(accessToken.description());
    expect(columns.eq(1)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.eq(2)).toContainText(getLastUsedInformation(accessToken));
  }

  function assertRevokedTokenRow(elem: any, accessToken: AccessToken) {
    const columns = elem.find("td");
    expect(columns.eq(0)).toContainText(accessToken.description());
    expect(columns.eq(1)).toContainText(formatTimeInformation(accessToken.createdAt()));
    expect(columns.eq(2)).toContainText(getLastUsedInformation(accessToken));
    expect(columns.eq(3)).toContainText(getRevokedBy(accessToken.revokedBy()));
    expect(columns.eq(4)).toContainText(formatTimeInformation(accessToken.revokedAt()));
    expect(columns.eq(5)).toContainText(accessToken.revokeCause());
  }
});

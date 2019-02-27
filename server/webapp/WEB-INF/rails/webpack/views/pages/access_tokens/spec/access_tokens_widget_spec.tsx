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
import {AccessTokensWidget} from "views/pages/access_tokens/access_tokens_widget";

describe("AccessTokensWidgetForCurrentUserSpec", () => {
  let $root: any, root: any;
  const onRevoke        = jasmine.createSpy("onRevoke");
  const allAccessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());
  const validToken      = AccessToken.fromJSON(AccessTokenTestData.validAccessToken());
  const revokedToken    = AccessToken.fromJSON(AccessTokenTestData.revokedAccessToken());

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  it("should be able to render info when no access tokens have been created", () => {
    mount(stream(new AccessTokens()));

    expect(findByDataTestId("access_token_info")).toBeInDOM();
    expect(findByDataTestId("access_token_info").text())
      .toContain(
        "Click on \"Generate Token\" to create new personal access token.A Generated token can be used to access the GoCD API.");

    expect(findByDataTestId("tab-header-0")).not.toBeInDOM();
    expect(findByDataTestId("tab-header-1")).not.toBeInDOM();
  });

  it("should render two tabs", () => {
    mount(stream(allAccessTokens));

    expect(findByDataTestId("access_token_info")).not.toBeInDOM();
    expect(findByDataTestId("tab-header-0")).toBeInDOM();
    expect(findByDataTestId("tab-header-0")).toHaveText("Active Tokens");
    expect(findByDataTestId("tab-header-1")).toBeInDOM();
    expect(findByDataTestId("tab-header-1")).toHaveText("Revoked Tokens");
  });

  it("should list all active tokens under active tokens tab", () => {
    mount(stream(new AccessTokens(stream(validToken))));

    const activeTokensTab = findByDataTestId("tab-header-0");
    expect(activeTokensTab).toBeInDOM();
    expect(activeTokensTab).toHaveText("Active Tokens");
    const activeTokenTableHeaders = findIn(findByDataTestId("tab-content-0"), "table-header-row").find("th");
    expect(activeTokenTableHeaders.eq(0)).toHaveText("Description");
    expect(activeTokenTableHeaders.eq(1)).toHaveText("Created At");
    expect(activeTokenTableHeaders.eq(2)).toHaveText("Last Used");
    expect(activeTokenTableHeaders.eq(3)).toHaveText("Revoke");

    const activeTokenTableRows = findIn(findByDataTestId("tab-content-0"), "table-row");
    expect(activeTokenTableRows.length).toEqual(1);
    expect(activeTokenTableRows).toContainText(validToken.description());
    expect(activeTokenTableRows)
      .toContainText(AccessTokensWidget.formatTimeInformation(validToken.createdAt()));
    expect(activeTokenTableRows)
      .toContainText(AccessTokensWidget.getLastUsedInformation(validToken));

    const revokedTokensTab = findByDataTestId("tab-header-1");
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    expect(findIn(findByDataTestId("tab-content-1"), "table-header-row")).not.toBeInDOM();
    expect(findByDataTestId("tab-content-1")).toHaveText("You don't have any revoked tokens.");
  });

  it("should list all revoked tokens under revoked tokens tab", () => {
    mount(stream(new AccessTokens(stream(revokedToken))));

    const activeTokensTab = findByDataTestId("tab-header-0");
    expect(activeTokensTab).toHaveText("Active Tokens");
    expect(findIn(findByDataTestId("tab-content-0"), "table-header-row")).not.toBeInDOM();
    expect(findByDataTestId("tab-content-0"))
      .toHaveText("You don't have any active tokens. Click on 'Generate Token' button to create a new token.");

    const revokedTokensTab = findByDataTestId("tab-header-1");
    simulateEvent.simulate(revokedTokensTab.get(0), "click");
    m.redraw();

    expect(revokedTokensTab).toBeInDOM();
    expect(revokedTokensTab).toHaveText("Revoked Tokens");
    const revokedTokenTableHeaders = findIn(findByDataTestId("tab-content-1"), "table-header-row").find("th");
    expect(revokedTokenTableHeaders.eq(0)).toHaveText("Description");
    expect(revokedTokenTableHeaders.eq(1)).toHaveText("Created At");
    expect(revokedTokenTableHeaders.eq(2)).toHaveText("Last Used");
    expect(revokedTokenTableHeaders.eq(3)).toHaveText("Revoked At");
    expect(revokedTokenTableHeaders.eq(4)).toHaveText("Revoked Message");

    const revokedTokensTableRows = findIn(findByDataTestId("tab-content-1"), "table-row");
    expect(revokedTokensTableRows.length).toEqual(1);
    expect(revokedTokensTableRows).toContainText(revokedToken.description());
    expect(revokedTokensTableRows).toContainText("04 Feb, 2019 at 12:11:58 Local Time");
    expect(revokedTokensTableRows).toContainText("04 Feb, 2019 at 13:11:58 Local Time");
    expect(revokedTokensTableRows).toContainText("05 Feb, 2019 at 12:11:58 Local Time");
    expect(revokedTokensTableRows).toContainText(revokedToken.revokeCause());
  });

  function mount(accessTokens: Stream<AccessTokens>) {
    m.mount(root, {
      view() {
        return <AccessTokensWidget accessTokens={accessTokens} onRevoke={onRevoke}/>;
      }
    });
    m.redraw();
  }

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function unmount() {
    m.mount(root, null);
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function findIn(elem: any, id: string) {
    return elem.find(`[data-test-id='${id}']`);
  }
});

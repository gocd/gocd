/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {GoCDVersion} from "gen/gocd_version";
import * as m from "mithril";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {Attrs, SiteFooter} from "views/pages/partials/site_footer";

describe("SiteFooter", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  function mount(attrs: Attrs) {
    helper.mount(() => <SiteFooter {...attrs}/>);
  }

  it("should render footer", () => {
    const attrs = {
      isServerInMaintenanceMode: false,
      isSupportedBrowser: true
    };
    mount(attrs);

    expect(helper.root).toContainHtml(`Copyright &copy; ${GoCDVersion.copyrightYear}`);
    expect(helper.root).toContainElement(`a[href="/go/assets/dependency-license-report-${GoCDVersion.fullVersion}"]`);
    expect(helper.root).toContainText(`GoCD Version: ${GoCDVersion.formattedVersion}`);
    expect(helper.root).not.toContainText("maintenance");
    expect(helper.findByDataTestId("maintenance-mode-banner")).not.toBeInDOM();
    expect(helper.root).not.toContainText("unsupported browser");
  });

  it("should render maintenance mode banner", () => {
    const attrs = {
      copyrightYear: "2000",
      formattedVersion: "x.y.z-1234-sha",
      fullVersion: "x.y.z-1234",
      goVersion: "x.y.z",
      isServerInMaintenanceMode: true,
      isSupportedBrowser: true
    };
    mount(attrs);

    expect(helper.findByDataTestId("maintenance-mode-banner")).toBeInDOM();
    expect(helper.root).toContainText("maintenance");
    expect(helper.root).not.toContainText("unsupported browser");
  });

  it("should render old browser message on IE11", () => {
    const attrs = {
      copyrightYear: "2000",
      formattedVersion: "x.y.z-1234-sha",
      fullVersion: "x.y.z-1234",
      goVersion: "x.y.z",
      isServerInMaintenanceMode: false,
      isSupportedBrowser: false
    };
    mount(attrs);

    expect(helper.findByDataTestId("maintenance-mode-banner")).not.toBeInDOM();
    expect(helper.findByDataTestId("unsupported-browser-banner")).toBeInDOM();
    expect(helper.root).not.toContainText("maintenance");
    expect(helper.root).toContainText("unsupported browser");
  });

});

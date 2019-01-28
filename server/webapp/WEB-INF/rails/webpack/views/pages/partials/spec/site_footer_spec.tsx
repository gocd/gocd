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

import * as m from "mithril";
import {Attrs, SiteFooter} from "views/pages/partials/site_footer";

describe("SiteFooter", () => {
  let $root: any, root: HTMLElement;
  beforeEach(() => {
    //@ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  //@ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount(attrs: Attrs) {
    m.mount(root, {
      view() {
        return (
          <SiteFooter {...attrs}/>
        );
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  it("should render footer", () => {
    const attrs = {
      copyrightYear: "2000",
      formattedVersion: "x.y.z-1234-sha",
      fullVersion: "x.y.z-1234",
      goVersion: "x.y.z",
      isServerInMaintenanceMode: false,
      isSupportedBrowser: true
    };
    mount(attrs);

    expect($root).toContainHtml("Copyright &copy; 2000");
    expect($root).toContainElement(`a[href="/go/assets/dependency-license-report-${attrs.fullVersion}"]`);
    expect($root).toContainText(`GoCD Version: ${attrs.formattedVersion}`);
    expect($root).not.toContainText("maintenance");
    expect(find("maintenance-mode-banner")).not.toBeInDOM();
    expect($root).not.toContainText('unsupported browser');
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

    expect(find("maintenance-mode-banner")).toBeInDOM();
    expect($root).toContainText("maintenance");
    expect($root).not.toContainText('unsupported browser');
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

    expect(find("maintenance-mode-banner")).not.toBeInDOM();
    expect(find("unsupported-browser-banner")).toBeInDOM();
    expect($root).not.toContainText("maintenance");
    expect($root).toContainText('unsupported browser');
  });

});

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
import {SiteUrls} from "models/server-configuration/server_configuration";
import {ServerManagementWidget} from "views/pages/server-configuration/server_management_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import {SiteUrlsVM} from "../../../../models/server-configuration/server_configuration_vm";

describe("ServerManagementWidget", () => {
  const helper      = new TestHelper();
  const onSaveSpy   = jasmine.createSpy("onSave");
  const onCancelSpy = jasmine.createSpy("onCancel");
  let siteUrlsVM: SiteUrlsVM;

  afterEach((done) => helper.unmount(done));

  it("should render input boxes for site url and secure site url", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    mount(siteUrls);

    expect(helper.byTestId("form-field-input-site-url")).toBeInDOM();
    expect(helper.byTestId("form-field-input-secure-site-url")).toBeInDOM();
    expect(helper.byTestId("form-field-input-site-url")).toHaveValue("http://foo.com");
    expect(helper.byTestId("form-field-input-secure-site-url")).toHaveValue("https://securefoo.com");
  });

  it("should render input boxes with proper help text", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    const docsLink = docsUrl("/installation/configuring_server_details.html#configure-site-urls");

    mount(siteUrls);
    const helpTextForSiteUrl =
            "This entry will be used by Go Server to generate links for emails, feeds etc. " +
            "Format: [protocol]://[host]:[port].";

    const helpTextForSecureSiteUrl =
            "If you wish that your primary site URL be HTTP, but still want to have HTTPS endpoints for the features " +
            "that require SSL, you can specify this attribute with a value of the base HTTPS URL." +
            " Format: https://[host]:[port].";

    const siteUrlId              = helper.byTestId("form-field-input-site-url").getAttribute("id");
    const siteUrlHelpTextElement = helper.q(`#${siteUrlId}-help-text`);

    const secureSiteUrlId              = helper.byTestId("form-field-input-secure-site-url").getAttribute("id");
    const secureSiteUrlHelpTextElement = helper.q(`#${secureSiteUrlId}-help-text`);
    const docsLinkForSiteUrl           = helper.q("a", helper.q(`#${siteUrlId}-help-text`));
    const docsLinkForSecureSiteUrl     = helper.q("a", helper.q(`#${secureSiteUrlId}-help-text`));

    expect(siteUrlHelpTextElement).toContainText(helpTextForSiteUrl);
    expect(docsLinkForSiteUrl).toHaveAttr("href", docsLink);
    expect(secureSiteUrlHelpTextElement).toContainText(helpTextForSecureSiteUrl);
    expect(docsLinkForSecureSiteUrl).toHaveAttr("href", docsLink);
  });

  it("should enable cancel button on change of input", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");

    mount(siteUrls);
    helper.oninput(helper.byTestId("form-field-input-secure-site-url"), "secure-site-url");

    expect(helper.byTestId("cancel")).not.toBeDisabled();

  });

  describe("Save", () => {
    it("should have save button", () => {
      mount(new SiteUrls());
      expect(helper.byTestId("save")).toBeInDOM();
      expect(helper.byTestId("save")).toHaveText("Save");
    });

    it("should call onSave", () => {
      mount(new SiteUrls());
      helper.click(helper.byTestId("save"));
      expect(onSaveSpy).toHaveBeenCalled();
    });
  });

  describe("Cancel", () => {
    it("should render cancel button", () => {
      mount(new SiteUrls());
      expect(helper.byTestId("cancel")).toBeInDOM();
      expect(helper.byTestId("cancel")).toHaveText("Cancel");
    });

    it("should call onCancel", () => {
      mount(new SiteUrls());
      helper.clickByTestId("cancel");
      expect(onCancelSpy).toHaveBeenCalledWith(siteUrlsVM);
    });
  });

  function mount(siteUrls: SiteUrls) {
    const savePromise: Promise<SiteUrls> = new Promise((resolve) => {
      onSaveSpy();
      resolve();
    });

    siteUrlsVM = new SiteUrlsVM();
    siteUrlsVM.sync(siteUrls, "some-etag");

    helper.mount(() => <ServerManagementWidget siteUrlsVM={Stream(siteUrlsVM)} onServerManagementSave={() => savePromise} onCancel={onCancelSpy}/>);
  }
});

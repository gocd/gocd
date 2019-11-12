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
import {SiteUrls} from "models/server-configuration/server_configuration";
import {ServerManagementWidget} from "views/pages/server-configuration/server_management_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ServerManageMentWidget", () => {
  const helper      = new TestHelper();
  const onCancelSpy = jasmine.createSpy("onCancel");
  const onSaveSpy   = jasmine.createSpy("onSave");
  afterEach((done) => helper.unmount(done));

  it("should render input boxes for site url and secure site url", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    mount(siteUrls);

    expect(helper.byTestId("form-field-input-site-url")).toBeInDOM();
    expect(helper.byTestId("form-field-input-secure-site-url")).toBeInDOM();
    expect(helper.byTestId("form-field-input-site-url")).toHaveValue("http://foo.com");
    expect(helper.byTestId("form-field-input-secure-site-url")).toHaveValue("https://securefoo.com");
  });

  it("should render cancel and save button", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    mount(siteUrls);

    expect(helper.byTestId("save")).toBeInDOM();
    expect(helper.byTestId("cancel")).toBeInDOM();
    expect(helper.byTestId("save")).toHaveText("Save");
    expect(helper.byTestId("cancel")).toHaveText("Cancel");
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
    const docsLinkForSiteUrl             = helper.q("a", helper.q(`#${siteUrlId}-help-text`));
    const docsLinkForSecureSiteUrl       = helper.q("a", helper.q(`#${secureSiteUrlId}-help-text`));

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

  it("should call 'onSave'", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    mount(siteUrls);

    helper.clickByTestId("save");

    expect(onSaveSpy).toHaveBeenCalled();
  });

  it("should call 'onCancel'", () => {
    const siteUrls = new SiteUrls("http://foo.com", "https://securefoo.com");
    mount(siteUrls);

    helper.oninput(helper.byTestId("form-field-input-site-url"), "foo");
    helper.clickByTestId("cancel");

    expect(onCancelSpy).toHaveBeenCalled();
  });

  function mount(siteUrls: SiteUrls) {
    const savePromise   = new Promise((resolve) => {
      onSaveSpy();
      resolve();
    });
    const cancelPromise = new Promise((resolve) => {
      onCancelSpy();
      resolve();
    });
    helper.mount(() => <ServerManagementWidget siteUrls={siteUrls} onCancel={() => cancelPromise}
                                               onServerManagementSave={() => savePromise}/>);
  }
});

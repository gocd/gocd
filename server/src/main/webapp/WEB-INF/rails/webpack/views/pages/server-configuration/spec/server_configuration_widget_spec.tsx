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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactConfig, MailServer, SiteUrls} from "models/server-configuration/server_configuration";
import {ArtifactConfigVM, DefaultJobTimeoutVM, MailServerVM, SiteUrlsVM} from "models/server-configuration/server_configuration_vm";
import {FlashMessageModel} from "views/components/flash_message";
import {Sections, ServerConfigurationWidget} from "views/pages/server-configuration/server_configuration_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ServerConfigurationWidget", () => {
  const helper  = new TestHelper();
  const onClick = jasmine.createSpy("onClick");
  let defaultJobTimeoutVM: DefaultJobTimeoutVM;
  let artifactConfigVM: ArtifactConfigVM;
  let siteUrlsVM: SiteUrlsVM;
  let mailServerVM: MailServerVM;

  function mount(activeConfiguration: Sections) {

    mailServerVM        = new MailServerVM(new MailServer());
    defaultJobTimeoutVM = new DefaultJobTimeoutVM();
    artifactConfigVM    = new ArtifactConfigVM(new ArtifactConfig("artifacts"));
    siteUrlsVM          = new SiteUrlsVM(new SiteUrls("http://foo.com", "https://foobar.com"));
    helper.mount(() => <ServerConfigurationWidget
      onMailServerManagementDelete={_.noop}
      route={onClick}
      activeConfiguration={activeConfiguration}
      defaultJobTimeoutVM={Stream(defaultJobTimeoutVM)}
      onDefaultJobTimeoutSave={() => Promise.resolve()}
      artifactConfigVM={Stream(artifactConfigVM)}
      onArtifactConfigSave={() => Promise.resolve()}
      onServerManagementSave={() => Promise.resolve()}
      siteUrlsVM={Stream(siteUrlsVM)}
      onMailServerManagementSave={() => Promise.resolve()}
      sendTestMail={() => Promise.resolve()}
      testMailResponse={Stream(new FlashMessageModel())}
      mailServerVM={Stream(mailServerVM)}
      onCancel={_.noop}/>);
  }

  afterEach((done) => helper.unmount(done));

  it("should have links for Artifacts Management, Mail server, Default job timeout management and Server management", () => {
    mount(Sections.SERVER_MANAGEMENT);

    expect(helper.byTestId("server-management-link")).toBeInDOM();
    expect(helper.byTestId("artifacts-management-link")).toBeInDOM();
    expect(helper.byTestId("email-server-link")).toBeInDOM();
    expect(helper.byTestId("job-timeout-link")).toBeInDOM();
  });

  it("should render Server management widget", () => {
    mount(Sections.SERVER_MANAGEMENT);

    expect(helper.byTestId("server-management-widget")).toBeInDOM();
  });

  it("should render artifact management widget", () => {
    mount(Sections.ARTIFACT_MANAGEMENT);

    expect(helper.byTestId("artifacts-management-widget")).toBeInDOM();
  });

  it("should render MailServer management widget", () => {
    mount(Sections.EMAIL_SERVER);

    expect(helper.byTestId("mail-server-management-widget")).toBeInDOM();
  });

  it("should render JobTimeout management widget", () => {
    mount(Sections.DEFAULT_JOB_TIMEOUT);

    expect(helper.byTestId("job-timeout-management-widget")).toBeInDOM();
  });

  it("should render Server management widget on click of server-management-link", () => {
    mount(Sections.ARTIFACT_MANAGEMENT);

    helper.clickByTestId("server-management-link");

    expect(onClick).toHaveBeenCalledWith(Sections.SERVER_MANAGEMENT, artifactConfigVM);
  });

  it("should render artifact management widget on click of artifacts-management-link", () => {
    mount(Sections.EMAIL_SERVER);

    helper.clickByTestId("artifacts-management-link");

    expect(onClick).toHaveBeenCalledWith(Sections.ARTIFACT_MANAGEMENT, mailServerVM);
  });

  it("should render MailServer management widget on click of mailServer-management-link", () => {
    mount(Sections.DEFAULT_JOB_TIMEOUT);
    helper.clickByTestId("email-server-link");

    expect(onClick).toHaveBeenCalledWith(Sections.EMAIL_SERVER, defaultJobTimeoutVM);
  });

  it("should render JobTimeout management widget on click of job-timeout-link", () => {
    mount(Sections.SERVER_MANAGEMENT);

    helper.clickByTestId("job-timeout-link");

    expect(onClick).toHaveBeenCalledWith(Sections.DEFAULT_JOB_TIMEOUT, siteUrlsVM);
  });
});

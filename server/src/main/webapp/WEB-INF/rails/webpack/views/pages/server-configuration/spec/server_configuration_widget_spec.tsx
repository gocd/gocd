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
import {ArtifactConfig, DefaultJobTimeout, MailServer, SiteUrls} from "models/server-configuration/server_configuration";
import {Sections, ServerConfigurationWidget} from "views/pages/server-configuration/server_configuration_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ServerConfigurationWidget", () => {
  const helper  = new TestHelper();
  const onClick = jasmine.createSpy("onClick");

  function mount(activeConfiguration: Sections,
                 artifactConfig: ArtifactConfig,
                 siteUrls: SiteUrls,
                 mailServer: MailServer) {

    helper.mount(() => <ServerConfigurationWidget
      onMailServerManagementDelete={_.noop}
      route={onClick}
      activeConfiguration={activeConfiguration}
      defaultJobTimeout={Stream(new DefaultJobTimeout(0))}
      onDefaultJobTimeoutSave={() => Promise.resolve()}
      artifactConfig={artifactConfig}
      onArtifactConfigSave={() => Promise.resolve()}
      onServerManagementSave={() => Promise.resolve()}
      siteUrls={siteUrls}
      onMailServerManagementSave={() => Promise.resolve()}
      mailServer={Stream(mailServer)}
      onCancel={() => Promise.resolve()}
      canDeleteMailServer={Stream()}
    />);
  }

  afterEach((done) => helper.unmount(done));

  it("should have links for Artifacts Management, Mail server and Server management", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("server-management-link")).toBeInDOM();
    expect(helper.byTestId("artifacts-management-link")).toBeInDOM();
    expect(helper.byTestId("email-server-link")).toBeInDOM();
  });

  it("should render Server management widget", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("server-management-widget")).toBeInDOM();
  });

  it("should render artifact management widget", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.ARTIFACT_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("artifacts-management-widget")).toBeInDOM();
  });

  it("should render MailServer management widget", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.EMAIL_SERVER, artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("mail-server-management-widget")).toBeInDOM();
  });

  it("should render JobTimeout management widget", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.DEFAULT_JOB_TIMEOUT, artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("job-timeout-management-widget")).toBeInDOM();
  });

  it("should render Server management widget on click of server-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    helper.clickByTestId("server-management-link");

    expect(onClick).toHaveBeenCalledWith(Sections.SERVER_MANAGEMENT);
  });

  it("should render artifact management widget on click of artifacts-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    helper.clickByTestId("artifacts-management-link");

    expect(onClick).toHaveBeenCalledWith(Sections.ARTIFACT_MANAGEMENT);
  });

  it("should render MailServer management widget on click of mailServer-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    helper.clickByTestId("email-server-link");

    expect(onClick).toHaveBeenCalledWith(Sections.EMAIL_SERVER);
  });

  it("should render JobTimeout management widget on click of job-timeout-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls       = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer     = new MailServer();
    mount(Sections.SERVER_MANAGEMENT, artifactConfig, siteUrls, mailServer);

    helper.clickByTestId("job-timeout-link");

    expect(onClick).toHaveBeenCalledWith(Sections.DEFAULT_JOB_TIMEOUT);
  });
});

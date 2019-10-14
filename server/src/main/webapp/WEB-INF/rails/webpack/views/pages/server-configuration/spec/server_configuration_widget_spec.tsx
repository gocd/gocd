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
import {MailServer} from "models/mail_server/types";
import {ArtifactConfig, DefaultJobTimeout, SiteUrls} from "models/server-configuration/server_configuration";
import {ServerConfigurationWidget} from "views/pages/server-configuration/server_configuration_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ServerConfigurationWidget", () => {
  const helper = new TestHelper();

  function mount(artifactConfig: ArtifactConfig, siteUrls: SiteUrls, mailServer: MailServer) {

    helper.mount(() =>
                   <ServerConfigurationWidget
                     defaultJobTimeout={Stream(new DefaultJobTimeout(0))}
                     onDefaultJobTimeoutSave={_.noop}
                     artifactConfig={artifactConfig}
                     onArtifactConfigSave={_.noop}
                     onServerManagementSave={_.noop}
                     siteUrls={siteUrls}
                     onMailServerManagementSave={_.noop}
                     mailServer={Stream(mailServer)}
                     operationState={Stream()}
                     onCancel={_.noop}
                   />);
  }

  afterEach((done) => helper.unmount(done));

  it("should have links for Artifacts Management, Mail server and Server management", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("server-management-link")).toBeInDOM();
    expect(helper.byTestId("artifacts-management-link")).toBeInDOM();
    expect(helper.byTestId("email-server-link")).toBeInDOM();
  });

  it("should by default render widget for Server management", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("server-management-widget")).toBeInDOM();
  });

  it("should render Server management widget on click of server-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);

    helper.clickByTestId("artifacts-management-link");
    expect(helper.byTestId("server-management-widget")).not.toBeInDOM();
    helper.clickByTestId("server-management-link");
    expect(helper.byTestId("server-management-widget")).toBeInDOM();

  });

  it("should render artifact management widget on click of artifacts-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);
    expect(helper.byTestId("artifacts-management-widget")).not.toBeInDOM();
    helper.clickByTestId("artifacts-management-link");
    expect(helper.byTestId("artifacts-management-widget")).toBeInDOM();

  });

  it("should render MailServer management widget on click of mailServer-management-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("mail-server-management-widget")).not.toBeInDOM();
    helper.clickByTestId("email-server-link");
    expect(helper.byTestId("mail-server-management-widget")).toBeInDOM();

  });

  it("should render JobTimeout management widget on click of job-timeout-link", () => {
    const artifactConfig = new ArtifactConfig("artifacts");
    const siteUrls           = new SiteUrls("http://foo.com", "https://foobar.com");
    const mailServer         = new MailServer();
    mount(artifactConfig, siteUrls, mailServer);

    expect(helper.byTestId("job-timeout-widget")).not.toBeInDOM();
    helper.clickByTestId("job-timeout-link");
    expect(helper.byTestId("job-timeout-management-widget")).toBeInDOM();

  });
});

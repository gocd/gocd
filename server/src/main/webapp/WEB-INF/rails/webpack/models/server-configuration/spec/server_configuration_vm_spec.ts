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

import {ArtifactConfig, DefaultJobTimeout, MailServer, SiteUrls} from "../server_configuration";
import {ArtifactConfigVM, DefaultJobTimeoutVM, MailServerVM, SiteUrlsVM} from "../server_configuration_vm";

describe('SiteUrlsVM', () => {
  it('should sync new values', () => {
    const siteUrlsVM: SiteUrlsVM = new SiteUrlsVM();

    expect(siteUrlsVM.siteUrls().siteUrl()).toBe("");
    expect(siteUrlsVM.siteUrls().secureSiteUrl()).toBe("");
    expect(siteUrlsVM.etag()).toBe(undefined);

    siteUrlsVM.sync(new SiteUrls("foo@gmail", "secureFoo@gmail"), "some-etag");

    expect(siteUrlsVM.siteUrls().siteUrl()).toBe("foo@gmail");
    expect(siteUrlsVM.siteUrls().secureSiteUrl()).toBe("secureFoo@gmail");
    expect(siteUrlsVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const siteUrlsVM: SiteUrlsVM = new SiteUrlsVM(new SiteUrls("foo@gmail", "secureFoo@gmail"));

    siteUrlsVM.siteUrls().siteUrl("bar@gmail");
    siteUrlsVM.siteUrls().secureSiteUrl("secureBar@gmail");

    siteUrlsVM.reset();

    expect(siteUrlsVM.siteUrls().siteUrl()).toBe("foo@gmail");
    expect(siteUrlsVM.siteUrls().secureSiteUrl()).toBe("secureFoo@gmail");
  });
});

describe('ArtifactConfigVM', () => {
  it('should sync new values', () => {
    const artifactConfigVM: ArtifactConfigVM = new ArtifactConfigVM();

    expect(artifactConfigVM.artifactConfig().artifactsDir()).toBe("");
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeStartDiskSpace()).toBe(undefined);
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeUptoDiskSpace()).toBe(undefined);
    expect(artifactConfigVM.etag()).toBe(undefined);

    artifactConfigVM.sync(new ArtifactConfig("artifacts", 10, 20), "some-etag");

    expect(artifactConfigVM.artifactConfig().artifactsDir()).toBe("artifacts");
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeStartDiskSpace()).toBe(10);
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeUptoDiskSpace()).toBe(20);
    expect(artifactConfigVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const artifactConfigVM: ArtifactConfigVM = new ArtifactConfigVM(new ArtifactConfig("artifacts", 10, 20));

    artifactConfigVM.artifactConfig().artifactsDir("changed");
    artifactConfigVM.artifactConfig().purgeSettings().purgeStartDiskSpace(0);
    artifactConfigVM.artifactConfig().purgeSettings().purgeUptoDiskSpace(0);

    artifactConfigVM.reset();

    expect(artifactConfigVM.artifactConfig().artifactsDir()).toBe("artifacts");
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeStartDiskSpace()).toBe(10);
    expect(artifactConfigVM.artifactConfig().purgeSettings().purgeUptoDiskSpace()).toBe(20);
  });
});

describe('DefaultJobTimeoutVM', () => {
  it('should sync new values', () => {
    const defaultJobTimeoutVM: DefaultJobTimeoutVM = new DefaultJobTimeoutVM();

    expect(defaultJobTimeoutVM.jobTimeout().defaultJobTimeout()).toBe(0);
    expect(defaultJobTimeoutVM.jobTimeout().neverTimeout()).toBe(true);
    expect(defaultJobTimeoutVM.etag()).toBe(undefined);

    defaultJobTimeoutVM.sync(new DefaultJobTimeout(10), "some-etag");

    expect(defaultJobTimeoutVM.jobTimeout().defaultJobTimeout()).toBe(10);
    expect(defaultJobTimeoutVM.jobTimeout().neverTimeout()).toBe(false);
    expect(defaultJobTimeoutVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const defaultJobTimeoutVM: DefaultJobTimeoutVM = new DefaultJobTimeoutVM(new DefaultJobTimeout(10));
    defaultJobTimeoutVM.etag("some-etag");
    defaultJobTimeoutVM.jobTimeout().defaultJobTimeout(0);
    defaultJobTimeoutVM.jobTimeout().neverTimeout(false);

    defaultJobTimeoutVM.reset();

    expect(defaultJobTimeoutVM.jobTimeout().defaultJobTimeout()).toBe(10);
    expect(defaultJobTimeoutVM.jobTimeout().neverTimeout()).toBe(false);
  });
});

describe('MailServerVM', () => {
  it('should sync new values', () => {
    const mailServerVM: MailServerVM = new MailServerVM();

    expect(mailServerVM.mailServer().hostname()).toBe(undefined);
    expect(mailServerVM.mailServer().port()).toBe(undefined);
    expect(mailServerVM.mailServer().adminEmail()).toBe(undefined);
    expect(mailServerVM.mailServer().password().isPlain()).toBe(true);
    expect(mailServerVM.mailServer().password().value()).toBe("");
    expect(mailServerVM.mailServer().senderEmail()).toBe(undefined);
    expect(mailServerVM.mailServer().tls()).toBe(undefined);
    expect(mailServerVM.mailServer().username()).toBe(undefined);

    mailServerVM.sync(new MailServer("hostname", 1234, "bob", "password", "", true, "sender@foo.com", "admin@foo.com"), "some-etag");

    expect(mailServerVM.mailServer().hostname()).toBe("hostname");
    expect(mailServerVM.mailServer().port()).toBe(1234);
    expect(mailServerVM.mailServer().adminEmail()).toBe("admin@foo.com");
    expect(mailServerVM.mailServer().password().isPlain()).toBe(true);
    expect(mailServerVM.mailServer().password().value()).toBe("password");
    expect(mailServerVM.mailServer().senderEmail()).toBe("sender@foo.com");
    expect(mailServerVM.mailServer().tls()).toBe(true);
    expect(mailServerVM.mailServer().username()).toBe("bob");
  });

  it('should reset modified values', () => {
    const mailServerVM: MailServerVM = new MailServerVM(new MailServer("hostname", 1234, "bob", "password", "", true, "sender@foo.com", "admin@foo.com"));
    mailServerVM.etag("some-etag");
    mailServerVM.mailServer().hostname("other hostname");
    mailServerVM.mailServer().port(4444);
    mailServerVM.mailServer().adminEmail("some-other@foo.com");
    mailServerVM.mailServer().password().value("some-value");
    mailServerVM.mailServer().senderEmail("other-sender@foo.com");
    mailServerVM.mailServer().tls(false);
    mailServerVM.mailServer().username("other-username");

    mailServerVM.reset();

    expect(mailServerVM.mailServer().hostname()).toBe("hostname");
    expect(mailServerVM.mailServer().port()).toBe(1234);
    expect(mailServerVM.mailServer().adminEmail()).toBe("admin@foo.com");
    expect(mailServerVM.mailServer().password().isPlain()).toBe(true);
    expect(mailServerVM.mailServer().password().value()).toBe("password");
    expect(mailServerVM.mailServer().senderEmail()).toBe("sender@foo.com");
    expect(mailServerVM.mailServer().tls()).toBe(true);
    expect(mailServerVM.mailServer().username()).toBe("bob");
  });

  it("should update new mail server object", () => {
    const mailServerVM: MailServerVM = new MailServerVM(new MailServer());
    const updatedMailServer          = new MailServer("some-hostname");

    mailServerVM.mailServer(updatedMailServer);

    expect(mailServerVM.mailServer()).toBe(updatedMailServer);
  });
});

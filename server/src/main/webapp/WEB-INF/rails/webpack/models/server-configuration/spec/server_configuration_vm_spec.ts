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

    expect(siteUrlsVM.entity().siteUrl()).toBe("");
    expect(siteUrlsVM.entity().secureSiteUrl()).toBe("");
    expect(siteUrlsVM.etag()).toBe(undefined);

    siteUrlsVM.sync(new SiteUrls("foo@gmail", "secureFoo@gmail"), "some-etag");

    expect(siteUrlsVM.entity().siteUrl()).toBe("foo@gmail");
    expect(siteUrlsVM.entity().secureSiteUrl()).toBe("secureFoo@gmail");
    expect(siteUrlsVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const siteUrlsVM: SiteUrlsVM = new SiteUrlsVM(new SiteUrls("foo@gmail", "secureFoo@gmail"));

    siteUrlsVM.entity().siteUrl("bar@gmail");
    siteUrlsVM.entity().secureSiteUrl("secureBar@gmail");

    siteUrlsVM.reset();

    expect(siteUrlsVM.entity().siteUrl()).toBe("foo@gmail");
    expect(siteUrlsVM.entity().secureSiteUrl()).toBe("secureFoo@gmail");
  });
});

describe('ArtifactConfigVM', () => {
  it('should sync new values', () => {
    const artifactConfigVM: ArtifactConfigVM = new ArtifactConfigVM();

    expect(artifactConfigVM.entity().artifactsDir()).toBe("");
    expect(artifactConfigVM.entity().purgeSettings().purgeStartDiskSpace()).toBe(undefined);
    expect(artifactConfigVM.entity().purgeSettings().purgeUptoDiskSpace()).toBe(undefined);
    expect(artifactConfigVM.etag()).toBe(undefined);

    artifactConfigVM.sync(new ArtifactConfig("artifacts", 10, 20), "some-etag");

    expect(artifactConfigVM.entity().artifactsDir()).toBe("artifacts");
    expect(artifactConfigVM.entity().purgeSettings().purgeStartDiskSpace()).toBe(10);
    expect(artifactConfigVM.entity().purgeSettings().purgeUptoDiskSpace()).toBe(20);
    expect(artifactConfigVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const artifactConfigVM: ArtifactConfigVM = new ArtifactConfigVM(new ArtifactConfig("artifacts", 10, 20));

    artifactConfigVM.entity().artifactsDir("changed");
    artifactConfigVM.entity().purgeSettings().purgeStartDiskSpace(0);
    artifactConfigVM.entity().purgeSettings().purgeUptoDiskSpace(0);

    artifactConfigVM.reset();

    expect(artifactConfigVM.entity().artifactsDir()).toBe("artifacts");
    expect(artifactConfigVM.entity().purgeSettings().purgeStartDiskSpace()).toBe(10);
    expect(artifactConfigVM.entity().purgeSettings().purgeUptoDiskSpace()).toBe(20);
  });
});

describe('DefaultJobTimeoutVM', () => {
  it('should sync new values', () => {
    const defaultJobTimeoutVM: DefaultJobTimeoutVM = new DefaultJobTimeoutVM();

    expect(defaultJobTimeoutVM.entity().defaultJobTimeout()).toBe(0);
    expect(defaultJobTimeoutVM.entity().neverTimeout()).toBe(true);
    expect(defaultJobTimeoutVM.etag()).toBe(undefined);

    defaultJobTimeoutVM.sync(new DefaultJobTimeout(10), "some-etag");

    expect(defaultJobTimeoutVM.entity().defaultJobTimeout()).toBe(10);
    expect(defaultJobTimeoutVM.entity().neverTimeout()).toBe(false);
    expect(defaultJobTimeoutVM.etag()).toBe("some-etag");
  });

  it('should reset modified values', () => {
    const defaultJobTimeoutVM: DefaultJobTimeoutVM = new DefaultJobTimeoutVM(new DefaultJobTimeout(10));
    defaultJobTimeoutVM.etag("some-etag");
    (defaultJobTimeoutVM.entity()).defaultJobTimeout(0);
    (defaultJobTimeoutVM.entity()).neverTimeout(false);

    defaultJobTimeoutVM.reset();

    expect(defaultJobTimeoutVM.entity().defaultJobTimeout()).toBe(10);
    expect(defaultJobTimeoutVM.entity().neverTimeout()).toBe(false);
  });
});

describe('MailServerVM', () => {
  it('should sync new values', () => {
    const mailServerVM: MailServerVM = new MailServerVM();

    expect(mailServerVM.entity().hostname()).toBe(undefined);
    expect(mailServerVM.entity().port()).toBe(undefined);
    expect(mailServerVM.entity().adminEmail()).toBe(undefined);
    expect(mailServerVM.entity().password().isPlain()).toBe(true);
    expect(mailServerVM.entity().password().value()).toBe("");
    expect(mailServerVM.entity().senderEmail()).toBe(undefined);
    expect(mailServerVM.entity().tls()).toBe(undefined);
    expect(mailServerVM.entity().username()).toBe(undefined);

    mailServerVM.sync(new MailServer("hostname", 1234, "bob", "password", "", true, "sender@foo.com", "admin@foo.com"), "some-etag");

    expect(mailServerVM.entity().hostname()).toBe("hostname");
    expect(mailServerVM.entity().port()).toBe(1234);
    expect(mailServerVM.entity().adminEmail()).toBe("admin@foo.com");
    expect(mailServerVM.entity().password().isPlain()).toBe(true);
    expect(mailServerVM.entity().password().value()).toBe("password");
    expect(mailServerVM.entity().senderEmail()).toBe("sender@foo.com");
    expect(mailServerVM.entity().tls()).toBe(true);
    expect(mailServerVM.entity().username()).toBe("bob");
  });

  it('should reset modified values', () => {
    const mailServerVM: MailServerVM = new MailServerVM(new MailServer("hostname", 1234, "bob", "password", "", true, "sender@foo.com", "admin@foo.com"));
    mailServerVM.etag("some-etag");
    mailServerVM.entity().hostname("other hostname");
    mailServerVM.entity().port(4444);
    mailServerVM.entity().adminEmail("some-other@foo.com");
    mailServerVM.entity().password().value("some-value");
    mailServerVM.entity().senderEmail("other-sender@foo.com");
    mailServerVM.entity().tls(false);
    mailServerVM.entity().username("other-username");

    mailServerVM.reset();

    expect(mailServerVM.entity().hostname()).toBe("hostname");
    expect(mailServerVM.entity().port()).toBe(1234);
    expect(mailServerVM.entity().adminEmail()).toBe("admin@foo.com");
    expect(mailServerVM.entity().password().isPlain()).toBe(true);
    expect(mailServerVM.entity().password().value()).toBe("password");
    expect(mailServerVM.entity().senderEmail()).toBe("sender@foo.com");
    expect(mailServerVM.entity().tls()).toBe(true);
    expect(mailServerVM.entity().username()).toBe("bob");
  });

  it("should update new mail server object", () => {
    const mailServerVM: MailServerVM = new MailServerVM(new MailServer());
    const updatedMailServer          = new MailServer("some-hostname");

    mailServerVM.entity(updatedMailServer);

    expect(mailServerVM.entity()).toBe(updatedMailServer);
  });
});

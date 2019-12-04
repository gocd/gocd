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

import {ArtifactConfig, DefaultJobTimeout, DefaultJobTimeoutJSON, MailServer, MailServerJSON, SiteUrls} from "models/server-configuration/server_configuration";

describe("ArtifactConfig", () => {
  describe("fromJSON", () => {
    it("should deserialize", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        errors: {
          artifacts_dir: ["some-error"]
        },
        purge_settings: {
          errors: {
            purge_start_disk_space: ["some-error in purge start disk space"],
            purge_upto_disk_space: ["some-error in purge upto disk space"]
          },
          purge_start_disk_space: 10,
          purge_upto_disk_space: 20
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.artifactsDir()).toBe("artifacts");
      expect(artifactConfig.errors().errors("artifactsDir")).toEqual(["some-error"]);
      expect(artifactConfig.purgeSettings().purgeStartDiskSpace()).toBe(10);
      expect(artifactConfig.purgeSettings().errors().errors("purgeStartDiskSpace")).toEqual(["some-error in purge start disk space"]);
      expect(artifactConfig.purgeSettings().purgeUptoDiskSpace()).toBe(20);
      expect(artifactConfig.purgeSettings().errors().errors("purgeUptoDiskSpace")).toEqual(["some-error in purge upto disk space"]);
    });

    it("should deserialize if purge setting is not provided", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts"
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.artifactsDir()).toBe("artifacts");
    });
  });

  describe("cleanupArtifact", () => {
    it("should enable cleanup artifact if both purgeStartDiskSpace and purgeUptoDiskSpace are non-zero", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        purge_settings: {
          purge_start_disk_space: 10,
          purge_upto_disk_space: 20
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.isArtifactsCleanupEnabled()).toBe(true);
    });

    it("should disable cleanup artifact if both purgeStartDiskSpace and purgeUptoDiskSpace are zero", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        purge_settings: {
          purge_start_disk_space: 0,
          purge_upto_disk_space: 0
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.isArtifactsCleanupEnabled()).toBe(false);
    });

    it("should disable cleanup artifact if purge setting is not specified", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        purge_settings: {
          purge_start_disk_space: 0,
          purge_upto_disk_space: 0
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.isArtifactsCleanupEnabled()).toBe(false);
    });

    it("should enable cleanup artifact if any of the purge settings is set", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        purge_settings: {
          purge_start_disk_space: 10.0,
          purge_upto_disk_space: 0.0
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.isArtifactsCleanupEnabled()).toBe(true);
    });
  });

  describe("toJSON", () => {
    it("should serialize artifact config when purge setting is not specified", () => {
      const artifactConfig     = new ArtifactConfig("foo");
      const artifactConfigJSON = artifactConfig.toJSON();
      const expectedJSON       = {
        artifacts_dir: "foo"
      };
      expect(artifactConfigJSON).toEqual(expectedJSON);
    });

    it("should serialize artifact config when purge setting is specified", () => {
      const artifactConfig = new ArtifactConfig("foo");
      artifactConfig.purgeSettings().purgeStartDiskSpace(10);
      artifactConfig.purgeSettings().purgeUptoDiskSpace(20);
      const artifactConfigJSON = artifactConfig.toJSON();
      const expectedJSON       = {
        artifacts_dir: "foo",
        purge_settings: {
          purge_start_disk_space: 10,
          purge_upto_disk_space: 20
        }
      };
      expect(artifactConfigJSON).toEqual(expectedJSON);
    });

    it("should serialize artifact config when only purgeStartDiskSpace is specified", () => {
      const artifactConfig = new ArtifactConfig("foo");
      artifactConfig.purgeSettings().purgeStartDiskSpace(10);
      const artifactConfigJSON = artifactConfig.toJSON();
      const expectedJSON       = {
        artifacts_dir: "foo",
        purge_settings: {
          purge_start_disk_space: 10
        }
      };
      expect(artifactConfigJSON).toEqual(expectedJSON);
    });
  });

  it('should clone', () => {
    const artifactConfig       = new ArtifactConfig("foo", 12, 37);
    const clonedArtifactConfig = artifactConfig.clone();

    expect(artifactConfig).not.toBe(clonedArtifactConfig);
    expect(artifactConfig.toJSON()).toEqual(clonedArtifactConfig.toJSON());
  });
});

describe("SiteUrls", () => {
  it("should deserialize", () => {
    const siteUrlsJSON = {
      site_url: "http://foo.bar",
      secure_site_url: "https://secure.com",
      errors: {
        site_url: ["some-error"],
        secure_site_url: ["some-another-error"]
      }
    };
    const siteUrls     = SiteUrls.fromJSON(siteUrlsJSON);
    expect(siteUrls.siteUrl()).toBe("http://foo.bar");
    expect(siteUrls.secureSiteUrl()).toBe("https://secure.com");
    expect(siteUrls.errors().errors("siteUrl")).toEqual(["some-error"]);
    expect(siteUrls.errors().errors("secureSiteUrl")).toEqual(["some-another-error"]);
  });

  it('should clone', () => {
    const siteUrlsJSON = {
      site_url: "http://foo.bar",
      secure_site_url: "https://secure.com"
    };
    const siteUrls     = SiteUrls.fromJSON(siteUrlsJSON);

    const clonedSiteUrls: SiteUrls = siteUrls.clone();
    expect(siteUrls).not.toBe(clonedSiteUrls);
    expect(siteUrls.siteUrl()).toBe(clonedSiteUrls.siteUrl());
    expect(siteUrls.secureSiteUrl()).toBe(clonedSiteUrls.secureSiteUrl());
  });

  it("should serialize", () => {
    const siteUrlsJSON = {
      site_url: "http://foo.bar",
      secure_site_url: "https://secure.com"
    };

    const siteUrls = new SiteUrls("http://foo.bar", "https://secure.com");
    expect(siteUrls.toJSON()).toEqual(siteUrlsJSON);
  });
});

describe("DefaultJobTimeout", () => {
  it("should deserialize", () => {
    const defaultJobTimeoutJSON: DefaultJobTimeoutJSON = {
      default_job_timeout: "15",
      errors: {
        default_job_timeout: ["some-error"]
      }
    };
    const defaultJobTimeout                            = DefaultJobTimeout.fromJSON(defaultJobTimeoutJSON);
    expect(defaultJobTimeout.defaultJobTimeout()).toBe(15);
    expect(defaultJobTimeout.errors().errors("defaultJobTimeout")).toEqual(["some-error"]);
  });

  describe("validations", () => {
    it('should not add error if valid job timeout', () => {
      const jobTimeout = new DefaultJobTimeout(0);

      expect(jobTimeout.isValid()).toBe(true);
    });

    it('should add error if invalid job timeout', () => {
      const jobTimeout = new DefaultJobTimeout(-2);

      expect(jobTimeout.isValid()).toBe(false);
      expect(jobTimeout.errors().errors("defaultJobTimeout")).toEqual(["Timeout should be positive non zero number as it represents number of minutes"]);
    });

    it('should add error if never timeout is false and job timeout is zero', () => {
      const jobTimeout = new DefaultJobTimeout(10);

      expect(jobTimeout.isValid()).toBe(true);
      jobTimeout.neverTimeout(false);

      jobTimeout.defaultJobTimeout(0);

      expect(jobTimeout.isValid()).toBe(false);
      expect(jobTimeout.errors().errors("defaultJobTimeout")).toEqual(["Timeout should be positive non zero number as it represents number of minutes"]);
    });
  });

  it('should clone', () => {
    const jobTimeout       = new DefaultJobTimeout(11);
    const clonedJobTimeout = jobTimeout.clone();

    expect(jobTimeout).not.toBe(clonedJobTimeout);
    expect(jobTimeout.neverTimeout()).toBe(false);
    expect(jobTimeout.defaultJobTimeout()).toBe(11);
  });

  it("should serialize", () => {
    const defaultJobTimeout               = new DefaultJobTimeout(10);
    const expected: DefaultJobTimeoutJSON = {
      default_job_timeout: "10"
    };
    expect(defaultJobTimeout.toJSON()).toEqual(expected);
  });
});

describe("MailServer", () => {
  describe("fromJSON", () => {
    it("should deserialize", () => {
      const mailServerJSON: MailServerJSON = {
        hostname: "hostname",
        port: 1234,
        adminEmail: "admin@foo.com",
        encryptedPassword: "",
        password: "password",
        senderEmail: "sender@foo.com",
        tls: false,
        username: "bob",
        errors: {
          hostname: ["error in hostname"],
          port: ["error in port"],
          adminEmail: ["error in admin email"],
          password: ["error in password"],
          senderEmail: ["error in sender email"],
          tls: ["error in tls"],
          username: ["error in username"],
        }
      };
      const mailServer                     = MailServer.fromJSON(mailServerJSON);
      expect(mailServer.hostname()).toBe(mailServerJSON.hostname);
      expect(mailServer.port()).toBe(mailServerJSON.port);
      expect(mailServer.adminEmail()).toBe(mailServerJSON.adminEmail);
      expect(mailServer.password().isPlain()).toBe(true);
      expect(mailServer.password().value()).toBe(mailServerJSON.password);
      expect(mailServer.senderEmail()).toBe(mailServerJSON.senderEmail);
      expect(mailServer.tls()).toBe(mailServerJSON.tls);
      expect(mailServer.username()).toBe(mailServerJSON.username);
      expect(mailServer.errors().errors("hostname")).toEqual(["error in hostname"]);
      expect(mailServer.errors().errors("port")).toEqual(["error in port"]);
      expect(mailServer.errors().errors("adminEmail")).toEqual(["error in admin email"]);
      expect(mailServer.errors().errors("password")).toEqual(["error in password"]);
      expect(mailServer.errors().errors("senderEmail")).toEqual(["error in sender email"]);
      expect(mailServer.errors().errors("tls")).toEqual(["error in tls"]);
      expect(mailServer.errors().errors("username")).toEqual(["error in username"]);
    });
  });

  describe("toJSON", () => {
    it("should serialize mail server config with plain password", () => {
      const mailServerJSON: MailServerJSON = {
        hostname: "hostname",
        port: 1234,
        adminEmail: "admin@foo.com",
        encryptedPassword: "",
        password: "password",
        senderEmail: "sender@foo.com",
        tls: false,
        username: "bob"
      };
      const mailServer                     = MailServer.fromJSON(mailServerJSON);
      const json                           = mailServer.toJSON();
      expect(json).toEqual(mailServerJSON);
    });

    it("should serialize mail server config with encrypted password", () => {
      const mailServerJSON: MailServerJSON = {
        hostname: "hostname",
        port: 1234,
        adminEmail: "admin@foo.com",
        encryptedPassword: "some-password",
        password: "",
        senderEmail: "sender@foo.com",
        tls: false,
        username: "bob"
      };

      const mailServer = MailServer.fromJSON(mailServerJSON);
      const json       = mailServer.toJSON();
      expect(json).toEqual(mailServerJSON);
    });

    it("should serialize mail server config with encrypted password dirty", () => {
      const mailServerJSON: MailServerJSON = {
        hostname: "hostname",
        port: 1234,
        adminEmail: "admin@foo.com",
        encryptedPassword: "",
        password: "some-password",
        senderEmail: "sender@foo.com",
        tls: false,
        username: "bob"
      };

      const mailServer = MailServer.fromJSON(mailServerJSON);
      mailServer.password().value("other-value");
      const json                       = mailServer.toJSON();
      mailServerJSON.password = "other-value";
      expect(json).toEqual(mailServerJSON);
    });
  });

  it('should clone', () => {
    const mailServer       = new MailServer("hostname", 1234, "bob", "password", "", false, "sender@foo.com", "admin@foo.com");
    const clonedMailServer = mailServer.clone();

    expect(clonedMailServer).not.toBe(mailServer);
    expect(clonedMailServer.hostname()).toBe(mailServer.hostname());
    expect(clonedMailServer.port()).toBe(mailServer.port());
    expect(clonedMailServer.adminEmail()).toBe(mailServer.adminEmail());
    expect(clonedMailServer.password().isPlain()).toBe(true);
    expect(clonedMailServer.password().value()).toBe(mailServer.password().value());
    expect(clonedMailServer.senderEmail()).toBe(mailServer.senderEmail());
    expect(clonedMailServer.tls()).toBe(mailServer.tls());
    expect(clonedMailServer.username()).toBe(mailServer.username());
  });
});

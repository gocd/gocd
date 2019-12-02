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

import {ArtifactConfig, DefaultJobTimeout, SiteUrls} from "models/server-configuration/server_configuration";

describe("ArtifactConfig", () => {
  describe("fromJSON", () => {
    it("should deserialize", () => {
      const artifactConfigJSON = {
        artifacts_dir: "artifacts",
        purge_settings: {
          purge_start_disk_space: 10,
          purge_upto_disk_space: 20
        }
      };
      const artifactConfig     = ArtifactConfig.fromJSON(artifactConfigJSON);
      expect(artifactConfig.artifactsDir()).toBe("artifacts");
      expect(artifactConfig.purgeSettings().purgeStartDiskSpace()).toBe(10);
      expect(artifactConfig.purgeSettings().purgeUptoDiskSpace()).toBe(20);
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
});

describe("SiteUrls", () => {
  it("should deserialize", () => {
    const siteUrlsJSON = {
      site_url: "http://foo.bar",
      secure_site_url: "https://secure.com"
    };
    const siteUrls     = SiteUrls.fromJSON(siteUrlsJSON);
    expect(siteUrls.siteUrl()).toBe("http://foo.bar");
    expect(siteUrls.secureSiteUrl()).toBe("https://secure.com");
  });
});

describe("DefaultJobTimeout", () => {
  it("should deserialize", () => {
    const defaultJobTimeoutJSON = {
      default_job_timeout: "15"
    };
    const defaultJobTimeout     = DefaultJobTimeout.fromJSON(defaultJobTimeoutJSON);
    expect(defaultJobTimeout.defaultJobTimeout()).toBe(15);
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
});

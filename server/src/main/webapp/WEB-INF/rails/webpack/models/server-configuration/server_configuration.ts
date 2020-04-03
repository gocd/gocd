/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {JsonUtils} from "helpers/json_utils";
import {mixins as s} from "helpers/string-plus";
import _ from "lodash";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {ErrorMessages} from "models/mixins/error_messages";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {EncryptedValue, plainOrCipherValue} from "views/components/forms/encrypted_value";

interface ErrorJSON {
  errors?: { [key: string]: string[] };
}

export interface SiteUrlsJSON extends ErrorJSON {
  site_url?: string;
  secure_site_url?: string;
}

export class SiteUrls extends ValidatableMixin {
  readonly siteUrl: Stream<string | undefined>;
  readonly secureSiteUrl: Stream<string | undefined>;

  constructor(siteUrl?: string, secureSiteUrl?: string, errors?: Errors) {
    super();
    this.siteUrl       = Stream(siteUrl);
    this.secureSiteUrl = Stream(secureSiteUrl);
    if (errors) {
      this.errors(errors);
    }
    this.validateUrlPattern("siteUrl");
    this.validateUrlPattern("secureSiteUrl");
  }

  static fromJSON(data: SiteUrlsJSON) {
    let errors = {};
    if (data.errors) {
      errors = JsonUtils.toCamelCasedObject(data.errors);
    }
    return new SiteUrls(data.site_url, data.secure_site_url, new Errors(errors));
  }

  clone() {
    return new SiteUrls(this.siteUrl(), this.secureSiteUrl());
  }

  toJSON() {
    return {
      site_url: this.siteUrl(),
      secure_site_url: this.secureSiteUrl()
    } as SiteUrlsJSON;
  }
}

export interface PurgeSettingsJSON extends ErrorJSON {
  purge_start_disk_space?: number;
  purge_upto_disk_space?: number;
}

export class PurgeSettings extends ValidatableMixin {
  readonly purgeStartDiskSpace: Stream<number | undefined>;
  readonly purgeUptoDiskSpace: Stream<number | undefined>;
  readonly cleanupArtifact: Stream<boolean>;

  constructor(purgeStartDiskSpace?: number, purgeUptoDiskSpace?: number, errors?: Errors) {
    super();
    this.purgeStartDiskSpace = Stream(purgeStartDiskSpace);
    this.purgeUptoDiskSpace  = Stream(purgeUptoDiskSpace);
    this.cleanupArtifact     = Stream(this.isCleanupEnabled());
    if (errors) {
      this.errors(errors);
    }
    this.validatePresenceOf("purgeStartDiskSpace",
                            {condition: () => this.cleanupArtifact(), message: "purge start disk space is empty"});
    this.validatePresenceOf("purgeUptoDiskSpace",
                            {condition: () => this.cleanupArtifact(), message: "purge upto disk space is empty"});
  }

  static fromJSON(data: PurgeSettingsJSON) {
    let errors = {};
    if (data.errors) {
      errors = JsonUtils.toCamelCasedObject(data.errors);
    }
    return new PurgeSettings(data.purge_start_disk_space, data.purge_upto_disk_space, new Errors(errors));
  }

  isCleanupEnabled(): boolean {
    return !!(this.purgeStartDiskSpace() || this.purgeUptoDiskSpace());
  }

  toJSON() {
    const purgeSettingsJSON: PurgeSettingsJSON = {};

    if (this.purgeStartDiskSpace() !== undefined) {
      purgeSettingsJSON.purge_start_disk_space = this.purgeStartDiskSpace();
    }
    if (this.purgeUptoDiskSpace() !== undefined) {
      purgeSettingsJSON.purge_upto_disk_space = this.purgeUptoDiskSpace();
    }
    return purgeSettingsJSON;
  }
}

export interface ArtifactConfigJSON extends ErrorJSON {
  artifacts_dir: string;
  purge_settings?: PurgeSettingsJSON;
}

export class ArtifactConfig extends ValidatableMixin {
  readonly artifactsDir: Stream<string>;
  readonly purgeSettings: Stream<PurgeSettings>;

  constructor(artifactsDir: string, purgeStartdiskSpace?: number, purgeUptoDiskSpace?: number) {
    super();
    this.artifactsDir  = Stream(artifactsDir);
    this.purgeSettings = Stream(new PurgeSettings(purgeStartdiskSpace, purgeUptoDiskSpace));

    this.validatePresenceOf("artifactsDir", {message: "Please provide artifacts directory location"});
    this.validateAssociated("purgeSettings");
  }

  static fromJSON(data: ArtifactConfigJSON) {
    let errors = {};
    if (data.errors) {
      errors = JsonUtils.toCamelCasedObject(data.errors);
    }
    const artifactConfig = new ArtifactConfig(data.artifacts_dir);
    if (data.purge_settings) {
      artifactConfig.purgeSettings(PurgeSettings.fromJSON(data.purge_settings));
    }
    artifactConfig.errors(new Errors(errors));
    return artifactConfig;
  }

  isArtifactsCleanupEnabled(): boolean {
    return this.purgeSettings().isCleanupEnabled();
  }

  cleanupArtifact(): boolean {
    return this.purgeSettings().cleanupArtifact();
  }

  toJSON() {
    const artifactConfigJSON: ArtifactConfigJSON = {
      artifacts_dir: this.artifactsDir(),
    };

    if (!_.isEmpty(this.purgeSettings().toJSON())) {
      artifactConfigJSON.purge_settings = this.purgeSettings().toJSON();
    }
    return artifactConfigJSON;
  }

  clone() {
    return ArtifactConfig.fromJSON(this.toJSON());
  }
}

export interface DefaultJobTimeoutJSON extends ErrorJSON {
  default_job_timeout: string;
}

export class DefaultJobTimeout extends ValidatableMixin {
  readonly defaultJobTimeout: Stream<number>;
  readonly neverTimeout: Stream<boolean>;

  constructor(defaultJobTimeout: number, errors?: Errors) {
    super();
    this.defaultJobTimeout = Stream(defaultJobTimeout);
    this.neverTimeout      = Stream(defaultJobTimeout === 0);
    if (errors) {
      this.errors(errors);
    }

    this.validatePresenceOf("defaultJobTimeout", {condition: this.neverTimeout});
  }

  static fromJSON(data: DefaultJobTimeoutJSON) {
    let errors = {};
    if (data.errors) {
      errors = JsonUtils.toCamelCasedObject(data.errors);
    }
    return new DefaultJobTimeout(parseInt(data.default_job_timeout, 10), new Errors(errors));
  }

  toJSON(): DefaultJobTimeoutJSON {
    return {
      default_job_timeout: this.defaultJobTimeout().toString()
    };
  }

  isValid(): boolean {
    const valid = super.isValid();

    if (!this.neverTimeout() && this.defaultJobTimeout() <= 0) {
      this.errors().add("defaultJobTimeout", "Timeout should be positive non zero number as it represents number of minutes");
      return false;
    }

    return valid;
  }

  clone() {
    const defaultJobTimeout = new DefaultJobTimeout(this.defaultJobTimeout());
    defaultJobTimeout.neverTimeout(this.neverTimeout());
    return defaultJobTimeout;
  }
}

export interface MailServerJSON extends ErrorJSON {
  hostname: string;
  port: number;
  username: string;
  password: string;
  encryptedPassword: string;
  tls: boolean;
  senderEmail: string;
  adminEmail: string;
}

export class MailServer extends ValidatableMixin {
  hostname: Stream<string | undefined>;
  port: Stream<number | undefined>;
  username: Stream<string | undefined>;
  password: Stream<EncryptedValue>;
  tls: Stream<boolean | undefined>;
  senderEmail: Stream<string | undefined>;
  adminEmail: Stream<string | undefined>;

  constructor(hostname?: string,
              port?: number,
              username?: string,
              password?: string,
              encryptedPassword?: string,
              tls?: boolean,
              senderEmail?: string,
              adminEmail?: string,
              errors: Errors = new Errors()) {
    super();

    this.hostname    = Stream(hostname);
    this.port        = Stream(port);
    this.username    = Stream(username);
    this.password    = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.tls         = Stream(tls);
    this.senderEmail = Stream(senderEmail);
    this.adminEmail  = Stream(adminEmail);

    this.validatePresenceOf("hostname");
    this.validatePresenceOf("port");
    this.validatePresenceOf("senderEmail");
    this.validatePresenceOf("adminEmail");

    this.validatePresenceOfPassword("password", {
      condition: () => {
        return !s.isBlank(this.username()!);
      },
      message: ErrorMessages.mustBePresentIf("password", "username is specified")
    });
    this.errors(errors);
  }

  static fromJSON(data: MailServerJSON) {
    const errors = new Errors(data.errors);
    return new MailServer(data.hostname,
                          data.port,
                          data.username,
                          data.password,
                          data.encryptedPassword,
                          data.tls,
                          data.senderEmail,
                          data.adminEmail,
                          errors);
  }

  toJSON() {
    const isPlainPassword = this.password().isPlain() || this.password().isDirty();
    return {
      hostname: this.hostname(),
      port: this.port(),
      username: this.username(),
      password: isPlainPassword ? this.password().value() : "",
      encryptedPassword: isPlainPassword ? "" : this.password().value(),
      tls: this.tls(),
      senderEmail: this.senderEmail(),
      adminEmail: this.adminEmail()
    } as MailServerJSON;
  }

  clone() {
    return MailServer.fromJSON(this.toJSON());
  }
}

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

import _ from "lodash";
import Stream from "mithril/stream";
import {ArtifactConfig, DefaultJobTimeout, MailServer, SiteUrls} from "./server_configuration";

export type ServerConfigType = ArtifactConfig | MailServer | SiteUrls | DefaultJobTimeout;
export type ServerConfigVM = MailServerVM | ArtifactConfigVM | DefaultJobTimeoutVM | SiteUrlsVM;

export class ServerConfigurationVM<T extends ServerConfigType> {
  etag: Stream<string | undefined>;
  protected readonly _original: Stream<T>;
  readonly entity: Stream<T>;

  constructor(object?: T) {
    this.etag      = Stream();
    this._original = object ? Stream(object) : Stream();
    this.entity    = object ? Stream(object.clone() as T) : Stream();
  }

  reset() {
    this.entity(this._original().clone() as T);
  }

  sync(object: T, etag?: string) {
    this._original(object);
    this.entity(object.clone() as T);
    this.etag(etag);
  }

  isModified() {
    return !_.isEqual(this._original().toJSON(), this.entity().toJSON());
  }
}

export class ArtifactConfigVM extends ServerConfigurationVM<ArtifactConfig> {

  constructor(artifactConfig?: ArtifactConfig) {
    super(artifactConfig ? artifactConfig : new ArtifactConfig(""));
  }
}

export class SiteUrlsVM extends ServerConfigurationVM<SiteUrls> {
  constructor(siteUrls?: SiteUrls) {
    super(siteUrls ? siteUrls : new SiteUrls("", ""));
  }
}

export class DefaultJobTimeoutVM extends ServerConfigurationVM<DefaultJobTimeout> {
  constructor(jobTimeout?: DefaultJobTimeout) {
    super(jobTimeout ? jobTimeout : new DefaultJobTimeout(0));
  }
}

export class MailServerVM extends ServerConfigurationVM<MailServer> {
  canDeleteMailServer: Stream<boolean>;

  constructor(mailServer?: MailServer) {
    super(mailServer ? mailServer : new MailServer());
    const flag: (val?: boolean) => Stream<boolean> = Stream;
    this.canDeleteMailServer                       = flag(false);
  }
}

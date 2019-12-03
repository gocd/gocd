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

import Stream from "mithril/stream";
import {ArtifactConfig, DefaultJobTimeout, MailServer, SiteUrls} from "./server_configuration";

type ServerConfigType = ArtifactConfig | MailServer | SiteUrls | DefaultJobTimeout;

export class ServerConfigurationVM {
  etag: Stream<string | undefined>;
  protected readonly _original: Stream<ServerConfigType>;
  protected readonly modified: Stream<ServerConfigType>;

  constructor(object?: ServerConfigType) {
    this.etag     = Stream();
    this._original = object ? Stream(object) : Stream();
    this.modified = object ? Stream(object.clone()) : Stream();
  }

  reset() {
    this.modified(this._original().clone());
  }

  sync(object: ServerConfigType, etag?: string) {
    this._original(object);
    this.modified(object.clone());
    this.etag(etag);
  }
}

export class ArtifactConfigVM extends ServerConfigurationVM {

  constructor(artifactConfig?: ArtifactConfig) {
    super(artifactConfig ? artifactConfig : new ArtifactConfig(""));
  }

  artifactConfig(): ArtifactConfig {
    return this.modified() as ArtifactConfig;
  }
}

export class SiteUrlsVM extends ServerConfigurationVM {
  constructor(siteUrls?: SiteUrls) {
    super(siteUrls ? siteUrls : new SiteUrls("", ""));
  }

  siteUrls(): SiteUrls {
    return this.modified() as SiteUrls;
  }
}

export class DefaultJobTimeoutVM extends ServerConfigurationVM {
  constructor(jobTimeout?: DefaultJobTimeout) {
    super(jobTimeout ? jobTimeout : new DefaultJobTimeout(0));
  }

  jobTimeout(): DefaultJobTimeout {
    return this.modified() as DefaultJobTimeout;
  }
}

export class MailServerVM extends ServerConfigurationVM {
  canDeleteMailServer: Stream<boolean>;

  constructor(mailServer?: MailServer) {
    super(mailServer ? mailServer : new MailServer());
    const flag: (val?: boolean) => Stream<boolean> = Stream;
    this.canDeleteMailServer                       = flag(false);
  }

  mailServer(mailServer?: MailServer): MailServer {
    if (mailServer) {
      this.modified(mailServer);
    }
    return this.modified() as MailServer;
  }
}

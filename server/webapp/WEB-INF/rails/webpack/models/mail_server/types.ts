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

import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {BackupConfig} from "models/backup_config/types";
import {ErrorMessages} from "models/mixins/error_messages";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {EncryptedValue, plainOrCipherValue} from "views/components/forms/encrypted_value";

const s = require("helpers/string-plus");

interface MailServerJSON {
  hostname: string;
  port: number;
  username: string;
  password: string;
  encryptedPassword: string;
  tls: boolean;
  senderEmail: string;
  adminEmail: string;
  errors?: { [key: string]: string[] };
}

// tslint:disable-next-line:no-empty-interface
export interface MailServer extends ValidatableMixin {

}

export class MailServer extends ValidatableMixin {
  hostname: Stream<string>;
  port: Stream<number>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;
  tls: Stream<boolean>;
  senderEmail: Stream<string>;
  adminEmail: Stream<string>;

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
    ValidatableMixin.call(this);

    this.hostname    = stream(hostname);
    this.port        = stream(port);
    this.username    = stream(username);
    this.password    = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.tls         = stream(tls);
    this.senderEmail = stream(senderEmail);
    this.adminEmail  = stream(adminEmail);

    this.validatePresenceOf("hostname");
    this.validatePresenceOf("port");
    this.validatePresenceOf("senderEmail");
    this.validatePresenceOf("adminEmail");

    this.validatePresenceOfPassword("password", {
      condition: () => {
        return !s.isBlank(this.username());
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
    const serialized                       = _.assign({}, this);
    const password: Stream<EncryptedValue> = _.get(serialized, "password");

    // remove the password field and setup the password serialization
    if (password) {
      // @ts-ignore
      delete serialized.password;

      if (password().isPlain() || password().isDirty()) {
        return _.assign({}, serialized, {password: password().value()});
      } else {
        return _.assign({}, serialized, {encrypted_password: password().value()});
      }
    }

    return serialized;
  }

}

applyMixins(BackupConfig, ValidatableMixin);

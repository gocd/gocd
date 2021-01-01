/*
 * Copyright 2021 ThoughtWorks, Inc.
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

export interface CurrentUserJSON {
  login_name: string;
  display_name: string;
  enabled: boolean;
  email: string;
  email_me: boolean;
  checkin_aliases: string[];
}

export class CurrentUser {
  readonly loginName: Stream<string>;
  readonly displayName: Stream<string>;
  readonly enabled: Stream<boolean>;
  email: Stream<string>;
  emailMe: Stream<boolean>;
  checkinAliases: Stream<string[]>;

  constructor(loginName: string, displayName: string, enabled: boolean, email: string, emailMe: boolean, checkinAliases: string[]) {
    this.loginName      = Stream(loginName);
    this.displayName    = Stream(displayName);
    this.enabled        = Stream(enabled);
    this.email          = Stream(email);
    this.emailMe        = Stream(emailMe);
    this.checkinAliases = Stream(checkinAliases);
  }

  static default() {
    return new CurrentUser("", "", true, "", false, []);
  }

  static fromJSON(data: CurrentUserJSON): CurrentUser {
    return new CurrentUser(data.login_name, data.display_name, data.enabled, data.email, data.email_me, data.checkin_aliases);
  }

  toUpdateApiJSON() {
    return {
      email:           this.email(),
      email_me:        this.emailMe(),
      checkin_aliases: this.checkinAliases()
    };
  }

  toJSON() {
    return {
      login_name:      this.loginName(),
      display_name:    this.displayName(),
      enabled:         this.enabled(),
      email:           this.email(),
      email_me:        this.emailMe(),
      checkin_aliases: this.checkinAliases()
    };
  }

  clone() {
    return CurrentUser.fromJSON(this.toJSON());
  }
}

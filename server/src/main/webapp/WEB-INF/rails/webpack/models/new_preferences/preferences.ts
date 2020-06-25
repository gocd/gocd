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
import {CurrentUser} from "./current_user";
import {NotificationFilters} from "./notification_filters";

export type Preferences = NotificationFilters | CurrentUser;
export type PreferenceVM = NotificationFilterVMs | CurrentUserVM;

export class PreferencesVM<T extends Preferences> {
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

export class NotificationFilterVMs extends PreferencesVM<NotificationFilters> {
  constructor(object?: NotificationFilters) {
    super(object || new NotificationFilters());
  }
}

export class CurrentUserVM extends PreferencesVM<CurrentUser> {
  constructor(object?: CurrentUser) {
    super(object || CurrentUser.default());
  }
}

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

import Stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface NotificationFiltersJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  filters: NotificationFilterJSON[];
}

export interface NotificationFilterJSON {
  id: number;
  pipeline: string;
  stage: string;
  event: NotificationEvent;
  match_commits: boolean;
  errors?: ErrorsJSON;
}

export type NotificationEvent = 'All' | 'Passes' | 'Fails' | 'Breaks' | 'Fixed' | 'Cancelled';

export class NotificationFilter extends ValidatableMixin {
  static readonly DEFAULT_PIPELINE = "[Any Pipeline]";
  static readonly DEFAULT_STAGE    = "[Any Stage]";
  id: Stream<number>;
  pipeline: Stream<string>;
  stage: Stream<string>;
  event: Stream<NotificationEvent>;
  matchCommits: Stream<boolean>;

  constructor(id: number, pipeline: string, stage: string, event: NotificationEvent, matchCommits: boolean, errors: Errors = new Errors()) {
    super();
    this.id           = Stream(id);
    this.pipeline     = Stream(pipeline);
    this.stage        = Stream(stage);
    this.event        = Stream(event);
    this.matchCommits = Stream(matchCommits);
    this.errors(errors);

    this.validatePresenceOf("pipeline");
    this.validatePresenceOf("stage");
    this.validatePresenceOf("event");
  }

  static fromJSON(data: NotificationFilterJSON): NotificationFilter {
    return new NotificationFilter(data.id, data.pipeline, data.stage, data.event, data.match_commits, new Errors(data.errors));
  }

  static default() {
    return new NotificationFilter(-1, NotificationFilter.DEFAULT_PIPELINE, NotificationFilter.DEFAULT_STAGE, 'All', true);
  }

  toJSON() {
    return {
      id:            this.id(),
      pipeline:      this.pipeline(),
      stage:         this.stage(),
      event:         this.event(),
      match_commits: this.matchCommits()
    };
  }

  toPartialJSON() {
    return {
      pipeline: this.pipeline(),
      stage:    this.stage(),
      event:    this.event(),
    };
  }

  clone() {
    return NotificationFilter.fromJSON(this.toJSON());
  }
}

export class NotificationFilters extends Array<NotificationFilter> {
  constructor(...vals: NotificationFilter[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(NotificationFilters.prototype));
  }

  static fromJSON(data: NotificationFilterJSON[]): NotificationFilters {
    return new NotificationFilters(...data.map((a) => NotificationFilter.fromJSON(a)));
  }

  clone() {
    return new NotificationFilters(...this.map((f) => f.clone()));
  }

  toJSON() {
    return [this.map((f) => f.toJSON())];
  }
}

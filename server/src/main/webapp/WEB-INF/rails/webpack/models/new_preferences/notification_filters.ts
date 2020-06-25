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
import {ValidatableMixin} from "../mixins/new_validatable_mixin";

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
}

export enum NotificationEvent {
  All, Passes, Fails, Breaks, Fixed, Cancelled
}

export class NotificationFilter extends ValidatableMixin {
  id: Stream<number>;
  pipeline: Stream<string>;
  stage: Stream<string>;
  event: Stream<string>;
  matchCommits: Stream<boolean>;

  constructor(id: number, pipeline: string, stage: string, event: NotificationEvent, matchCommits: boolean) {
    super();
    this.id           = Stream(id);
    this.pipeline     = Stream(pipeline);
    this.stage        = Stream(stage);
    this.event        = Stream(event.toString());
    this.matchCommits = Stream(matchCommits);

    this.validatePresenceOf("pipeline");
    this.validatePresenceOf("stage");
    this.validatePresenceOf("event");
  }

  static fromJSON(data: NotificationFilterJSON): NotificationFilter {
    return new NotificationFilter(data.id, data.pipeline, data.stage, data.event, data.match_commits);
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
}

export class NotificationFilters extends Array<NotificationFilter> {
  constructor(...vals: NotificationFilter[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(NotificationFilters.prototype));
  }

  static fromJSON(data: NotificationFilterJSON[]): NotificationFilters {
    return new NotificationFilters(...data.map((a) => NotificationFilter.fromJSON(a)));
  }
}

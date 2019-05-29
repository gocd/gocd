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

import JsonUtils from "helpers/json_utils";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Job} from "./job";
import {NameableSet} from "./nameable_set";

enum ApprovalType { success = "success", manual = "manual" }

class Approval extends ValidatableMixin {
  type: Stream<ApprovalType> = stream();
  state: (value?: boolean) => boolean;

  //authorization must be present for server side validations
  //even though it's not editable from the create pipeline page
  authorization: Stream<any> = stream({});

  constructor() {
    super();

    this.type(ApprovalType.success);
    this.validatePresenceOf("type");
    this.validatePresenceOf("authorization");

    // define like this because it will be bound later to the component
    // and `this` won't refer to an Approval object
    this.state = (value?: boolean) => {
      if ("boolean" === typeof value) {
        this.type(value ? ApprovalType.success : ApprovalType.manual);
      }
      return ApprovalType.success === this.type();
    };
  }
}

export class Stage extends ValidatableMixin {
  name: Stream<string>;
  approval: Stream<Approval> = stream(new Approval());
  jobs: Stream<NameableSet<Job>>;

  constructor(name: string, jobs: Job[]) {
    super();

    this.name = stream(name);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validateAssociated("approval");

    this.jobs = stream(new NameableSet(jobs));
    this.validateNonEmptyCollection("jobs", {message: `A stage must have at least one job`});
    this.validateAssociated("jobs");
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

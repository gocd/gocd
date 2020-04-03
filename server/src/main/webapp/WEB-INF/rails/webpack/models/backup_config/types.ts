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
import {Errors} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

interface BackupConfigJSON {
  schedule: string;
  postBackupScript: string;
  emailOnSuccess: boolean;
  emailOnFailure: boolean;
  errors?: { [key: string]: string[] };
}

export class BackupConfig extends ValidatableMixin {
  schedule: Stream<string | undefined>;
  postBackupScript: Stream<string | undefined>;
  emailOnSuccess: Stream<boolean>;
  emailOnFailure: Stream<boolean>;

  constructor(schedule?: string,
              postBackupScript?: string,
              emailOnSuccess: boolean = false,
              emailOnFailure: boolean = false,
              errors: Errors          = new Errors()) {
    super();

    this.schedule         = Stream(schedule);
    this.postBackupScript = Stream(postBackupScript);
    this.emailOnSuccess   = Stream(emailOnSuccess);
    this.emailOnFailure   = Stream(emailOnFailure);
    this.errors(errors);
  }

  static fromJSON(data: BackupConfigJSON) {
    const errors = new Errors(data.errors);
    return new BackupConfig(data.schedule,
                            data.postBackupScript,
                            data.emailOnSuccess,
                            data.emailOnFailure,
                            errors);
  }
}

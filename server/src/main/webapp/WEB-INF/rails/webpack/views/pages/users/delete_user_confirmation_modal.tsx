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

import {ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import {BulkUserOperationJSON} from "models/users/users";
import {UsersCRUD} from "models/users/users_crud";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import styles from "./index.scss";

export class DeleteUserConfirmModal extends DeleteConfirmModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onOperationError: (errorResponse: ErrorResponse) => any;

  constructor(usersToBeDeleted: BulkUserOperationJSON, onSuccessfulSave: (msg: m.Children) => any,
              onOperationError: (errorResponse: ErrorResponse) => any) {
    super(DeleteUserConfirmModal.getMessage(usersToBeDeleted.users), () => this.delete(usersToBeDeleted));
    this.onSuccessfulSave = onSuccessfulSave;
    this.onOperationError = onOperationError;

  }

  private static getMessage(usersToBeDeleted: string[]) {
    return (<div>
      <p>Are you sure you want to delete these users:
        <span class={styles.toBeDeletedUsers}>
          {usersToBeDeleted.join(",")}
        </span>
      </p>
    </div>);
  }

  private delete(usersToBeDeleted: BulkUserOperationJSON) {
    return UsersCRUD
      .bulkUserDelete(usersToBeDeleted)
      .then((apiResult) => {
        apiResult.do(() => {
                       this.onSuccessfulSave(
                         <span>The users <em>{usersToBeDeleted.users.join(",")}</em> were deleted successfully!</span>);
                     },
                     this.onOperationError);
      })
      .finally(this.close.bind(this));
  }
}

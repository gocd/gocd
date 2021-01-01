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
import {SystemAdmins} from "models/admins/types";
import {User} from "models/users/users";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";

export class UserViewHelper {
  readonly systemAdmins      = Stream(new SystemAdmins([], []));
  private readonly viewState = new Map<string, FlashMessageModelWithTimeout>();

  noAdminsConfigured(): boolean {
    return this.systemAdmins().noAdminsConfigured();
  }

  userUpdateSuccessful(user: User) {
    this.updateMessage(user, MessageType.success);
  }

  userUpdateInProgress(user: User) {
    this.updateMessage(user, MessageType.inProgress);
  }

  userUpdateFailure(user: User, errorMessage: string) {
    this.updateMessage(user, MessageType.alert, errorMessage);
  }

  hasError(user: User) {
    return this.knowsAbout(user) && this.hasMessageType(user, MessageType.alert);
  }

  errorMessageFor(user: User) {
    return this.viewState.get(user.loginName())!.message;
  }

  knowsAbout(user: User) {
    return this.viewState.has(user.loginName());
  }

  isInProgress(user: User) {
    return this.knowsAbout(user) && this.hasMessageType(user, MessageType.inProgress);
  }

  isUpdatedSuccessfully(user: User) {
    return this.knowsAbout(user) && this.hasMessageType(user, MessageType.success);
  }

  isIndividualAdmin(user: User) {
    return this.systemAdmins().isIndividualAdmin(user.loginName());
  }

  private hasMessageType(user: User, messageType: MessageType) {
    return this.viewState.get(user.loginName())!.type === messageType;
  }

  private updateMessage(user: User, messageType: MessageType, message: string = "") {
    if (this.knowsAbout(user)) {
      this.viewState.get(user.loginName())!.clear();
    }

    const messageModelWithTimeout = new FlashMessageModelWithTimeout();
    messageModelWithTimeout.setMessage(messageType, message, () => {
      this.viewState.delete(user.loginName());
    });
    this.viewState.set(user.loginName(), messageModelWithTimeout);
  }
}

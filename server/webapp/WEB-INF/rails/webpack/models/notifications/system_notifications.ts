/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import * as uuid4 from "uuid/v4";

export class Notification {
  id: string;
  read: boolean;
  message: string;
  type: string;
  link: string;
  linkText: string;

  constructor(data: any) {
    this.id       = data.id;
    this.message  = data.message;
    this.read     = data.read;
    this.type     = data.type;
    this.link     = data.link;
    this.linkText = data.linkText;
  }

  static create(data: any) {
    return new Notification(data);
  }

  static fromJSON(data: any) {
    return new Notification(data);
  }

  markAsRead() {
    this.read                           = true;
    const notifications: Notification[] = JSON.parse(localStorage.getItem("system_notifications") || "[]");

    const lookup = this.id;
    notifications.forEach((e: Notification) => {
      if (e.id === lookup) {
        e.read = true;
      }
    });

    localStorage.setItem("system_notifications", JSON.stringify(notifications));
  }

  toJSON() {
    return {
      id: this.id,
      message: this.message,
      read: this.read,
      type: this.type,
      link: this.link,
      linkText: this.linkText
    };
  }
}

export class SystemNotifications {
  private readonly allNotifications: Notification[];

  constructor(data: Notification[] = []) {
    this.allNotifications = data.map((json) => Notification.fromJSON(json));
  }

  static all() {
    return new Promise<SystemNotifications>((resolve) => {
      const notifications: Notification[] = JSON.parse(localStorage.getItem("system_notifications") || "[]");
      resolve(SystemNotifications.fromJSON(notifications));
    });
  }

  static fromJSON(notifications: Notification[]) {
    return new SystemNotifications(notifications);
  }

  static notifyNewMessage(type: string, message: string, link: string, linkText: string) {
    const systemNotifications = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem("system_notifications") || "[]"));

    const notificationMessage: Notification | undefined = systemNotifications.find((message: Notification): boolean => {
      return message.type === type;
    });

    if (notificationMessage !== undefined) {
      systemNotifications.remove(notificationMessage);
    }

    systemNotifications.add(new Notification({
                                               id: uuid4(),
                                               read: false,
                                               message, type, link, linkText
                                             } as Notification));

    const existingArrayPrototype = (Array.prototype as any).toJSON;
    delete (Array.prototype as any).toJSON;

    localStorage.setItem("system_notifications", JSON.stringify(systemNotifications.toJSON()));

    (Array.prototype as any).toJSON = existingArrayPrototype;
  }

  //model_mixin method
  find(callback: (n: Notification) => boolean): Notification | undefined {
    return _.find(this.allNotifications, callback);
  }

  //model_mixin method
  remove(messageORCallback: Notification | ((n: Notification) => boolean)) {
    return _.remove(this.allNotifications, messageORCallback);
  }

  //model_mixin method
  add(message: Notification) {
    return this.allNotifications.push(message);
  }

  //model_mixin method
  count(): number {
    return this.allNotifications.length;
  }

  //model_mixin method
  filter(callback: (n: Notification) => boolean) {
    return _.filter(this.allNotifications, callback);
  }

  //model_mixin method
  map(callback: (n: Notification) => any) {
    return _.map(this.allNotifications, callback);
  }

  //model_mixin method
  first(): Notification {
    return this.allNotifications[0];
  }

  //model_mixin method
  last(): Notification {
    return this.allNotifications[this.allNotifications.length - 1];
  }

  toJSON() {
    return this.allNotifications.map((message: Notification) => message.toJSON());
  }
}

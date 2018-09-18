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

const Stream                          = require('mithril/stream');
const Mixins                          = require('models/mixins/model_mixins');
const uuid4 = require('uuid/v4');
const $                     = require('jquery');

const SystemNotifications = function (data) {
    Mixins.HasMany.call(this, {
        factory:    SystemNotifications.Notification.create,
        as:         'SystemNotification',
        collection: data,
        uniqueOn:   'id'
    });
};

SystemNotifications.Notification = function (data) {
    this.id    = Stream(data.id);
    this.message    = Stream(data.message);
    this.read =     Stream(data.read);
    this.type =     Stream(data.type);
    this.link =     Stream(data.link);
    this.linkText =     Stream(data.linkText);
    this.parent     = Mixins.GetterSetter();

    this.markAsRead = function() {
        this.read = Stream(true);
        const notifications = JSON.parse(localStorage.getItem('system_notifications'));
        const lookup = this.id();
        notifications.forEach((e) => {
            if (e.id === lookup) {
                e.read = true;
            }
        });
        localStorage.setItem('system_notifications', JSON.stringify(notifications));
    };

    this.toJSON = function () {
        return {
            id: this.id(),
            message: this.message(),
            read: this.read(),
            type: this.type(),
            link: this.link(),
            linkText: this.linkText()
        };
    };
};

SystemNotifications.Notification.create = function (data) {
    return new SystemNotifications.Notification(data);
};

SystemNotifications.Notification.fromJSON = function (data) {
    return new SystemNotifications.Notification({
        id: data.id,
        message: data.message,
        read: data.read,
        type: data.type,
        link: data.link,
        linkText: data.linkText
    });
};

SystemNotifications.all = () => $.Deferred(function () {
    const notifications = JSON.parse(localStorage.getItem('system_notifications'));
    this.resolve(SystemNotifications.fromJSON(notifications));
}).promise();

SystemNotifications.notifyNewMessage = (type, message, link, linkText) => {
    const notifications = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));
    const notificationMessage = notifications.findSystemNotification((m) => {
        return m.type() === type;
    });
    if (notificationMessage !== undefined) {
        notifications.removeSystemNotification(notificationMessage);
    }

    notifications.addSystemNotification(new SystemNotifications.Notification({
        id: uuid4(),
        message,
        read: false,
        type,
        link,
        linkText
    }));
    localStorage.setItem('system_notifications', JSON.stringify(notifications));
};

Mixins.fromJSONCollection({
    parentType: SystemNotifications,
    childType:  SystemNotifications.Notification,
    via:        'addSystemNotification'
});

module.exports = SystemNotifications;

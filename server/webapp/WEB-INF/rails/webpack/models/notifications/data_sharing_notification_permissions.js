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

import AjaxHelper from "helpers/ajax_helper";
const Stream = require('mithril/stream');

const DataSharingNotificationPermission = function (permission) {
    this.showNotification = Stream(permission.show_notification);
};
DataSharingNotificationPermission.API_VERSION = 'v1';

DataSharingNotificationPermission.get = () => {
    return AjaxHelper.GET({
        url:        "/go/api/data_sharing/settings/notification_auth",
        apiVersion: DataSharingNotificationPermission.API_VERSION,
        type:       DataSharingNotificationPermission
    });
};

DataSharingNotificationPermission.fromJSON = function (json) {
    return new DataSharingNotificationPermission(json);
};

module.exports = DataSharingNotificationPermission;

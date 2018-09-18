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

const $                                 = require('jquery');
const SystemNotifications               = require('models/notifications/system_notifications');
const DataSharingNotificationPermission = require('models/notifications/data_sharing_notification_permissions');

/*
   Add AdditionalMetrics Notification only if `DataSharing` notification exists.

   Data Sharing was added as part of GoCD v18.7.0
   Addition data to the data sharing was added as part of GoCD v18.8.0

   Users upgrading from any version prior to v18.7.0 to v18.8.0 should get only DataSharing notification
   And, Users upgrading from v18.7.0 to v18.8.0 should get AdditionalMetrics notification
*/

const DataSharingNotification = function () {
};

DataSharingNotification.createIfNotPresent = () => $.Deferred(function () {
  SystemNotifications.all().then((allNotifications) => {
    const existingDataSharingNotification = allNotifications.findSystemNotification((m) => {
      return m.type() === 'DataSharing';
    });

    const existingAdditionalMetricsNotification = allNotifications.findSystemNotification((m) => {
      return m.type() === 'DataSharing_v18.8.0';
    });

    if (existingAdditionalMetricsNotification) {
      this.resolve({});
      return;
    }

    DataSharingNotificationPermission.get().then((permissions) => {
      if (permissions.showNotification()) {
        if (existingDataSharingNotification !== undefined) {
          const message = "GoCD’s shared data has been updated to include new metrics.";
          const link    = "/go/admin/data_sharing/settings";
          const type    = "DataSharing_v18.8.0";
          SystemNotifications.notifyNewMessage(type, message, link, "Learn more ...");
        } else {
          const message = "GoCD shares data so that it can be improved.";
          const link    = "/go/admin/data_sharing/settings";
          const type    = "DataSharing_v18.8.0";
          SystemNotifications.notifyNewMessage(type, message, link, "Learn more ...");
        }
      }
      this.resolve({});
    });
  });
}).promise();

module.exports = DataSharingNotification;


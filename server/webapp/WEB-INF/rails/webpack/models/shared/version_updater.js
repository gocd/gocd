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

const $        = require('jquery');
const _        = require('lodash');
const mrequest = require('helpers/mrequest');
const Routes   = require('gen/js-routes');
const SystemNotifications = require('models/notifications/system_notifications').SystemNotifications;

const VersionUpdater = function () {
  this.update = () => {
    if (canUpdateVersion()) {
      fetchStaleVersionInfo().then((data) => {
        _.isEmpty(data) ? markUpdateDoneAndNotify() : fetchLatestVersion(data);
      });
    }
  };

  const fetchStaleVersionInfo = () => $.ajax({
    method:     'GET',
    url:        Routes.apiv1StaleVersionInfoPath(),
    beforeSend: mrequest.xhrConfig.forVersion('v1')
  });

  const fetchLatestVersion = (versionInfo) => {
    $.ajax({
      method: 'GET',
      url:    versionInfo['update_server_url'],
      beforeSend(xhr) {
        xhr.setRequestHeader("Accept", "application/vnd.update.go.cd.v1+json");
      },
    }).then(updateLatestVersion);
  };

  const updateLatestVersion = (data) => {
    $.ajax({
      method:     'PATCH',
      beforeSend: mrequest.xhrConfig.forVersion('v1'),
      url:        Routes.apiv1UpdateServerVersionInfoPath(),
      data:       JSON.stringify(data)
    }).then(markUpdateDoneAndNotify);
  };

  const canUpdateVersion = () => {
    let versionCheckInfo = localStorage.getItem('versionCheckInfo');
    if (_.isEmpty(versionCheckInfo)) {
      return true;
    }
    versionCheckInfo   = JSON.parse(versionCheckInfo);
    const lastUpdateAt = new Date(versionCheckInfo.last_updated_at);
    const halfHourAgo  = new Date(_.now() - 30 * 60 * 1000);
    return halfHourAgo > lastUpdateAt;
  };

  const markUpdateDone = () => {
    const versionCheckInfo = JSON.stringify({last_updated_at: new Date().getTime()}); //eslint-disable-line camelcase
    localStorage.setItem('versionCheckInfo', versionCheckInfo);
  };

  const get = function () {
    return $.Deferred(function () {
        const deferred = this;

        const jqXHR = $.ajax({
            method:      "GET",
            url:         Routes.apiv1LatestVersionInfoPath(),
            timeout:     mrequest.timeout,
            beforeSend:  mrequest.xhrConfig.forVersion("v1"),
            contentType: false
        });

        const didFulfill = (data, _textStatus) => {
            const latestVersion = data.latest_version;
            deferred.resolve(latestVersion);
        };

        const didReject = () => {
            deferred.reject(null);
        };

        jqXHR.then(didFulfill, didReject);
    }).promise();
  };

  const markUpdateDoneAndNotify = () => {
    markUpdateDone();
    get().then((latestVersionNumber) => {
        if (latestVersionNumber !== undefined) {
            SystemNotifications.notifyNewMessage("UpdateCheck", `A new version of GoCD - ${latestVersionNumber} is available.`, "https://www.gocd.org/download/", "Learn more ...");
        }
    });
  };
};

module.exports = VersionUpdater;

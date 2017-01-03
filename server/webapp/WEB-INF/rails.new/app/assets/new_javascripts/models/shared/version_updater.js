/*
 * Copyright 2017 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'helpers/mrequest', 'js-routes'], function (m, _, mrequest, Routes) {

  var VersionUpdater = function () {
    this.update = function () {
      if (canUpdateVersion()) {
        fetchStaleVersionInfo().then(function (data) {
          _.isEmpty(data) ? markUpdateDone() : fetchLatestVersion(data);
        });
      }
    };

    var fetchStaleVersionInfo = function () {
      return m.request({
        method:     'GET',
        url:        Routes.apiv1StaleVersionInfoPath(),
        background: true,
        config:     mrequest.xhrConfig.v1
      });
    };

    var fetchLatestVersion = function (versionInfo) {
      m.request({
        method:  'GET',
        url:     versionInfo['update_server_url'],
        background: true,
        config: function (xhr) {
          xhr.setRequestHeader("Accept", "application/vnd.update.go.cd.v1+json");
        }}).then(function (data) {
          updateLatestVersion(data);
        }
      );
    };

    var updateLatestVersion = function (data) {
      m.request({
        method:     'PATCH',
        background: true,
        config:     mrequest.xhrConfig.v1,
        url:        Routes.apiv1UpdateServerVersionInfoPath(),
        data:       data
      }).then(function () {
        markUpdateDone();
      });
    };

    var canUpdateVersion = function () {
      var versionCheckInfo = localStorage.getItem('versionCheckInfo');
      if (_.isEmpty(versionCheckInfo)) {
        return true;
      }
      versionCheckInfo = JSON.parse(versionCheckInfo);
      var lastUpdateAt = new Date(versionCheckInfo.last_updated_at);
      var halfHourAgo = new Date(_.now() - 30 * 60 * 1000);
      return halfHourAgo > lastUpdateAt;
    };

    var markUpdateDone = function () {
      var versionCheckInfo = JSON.stringify({last_updated_at: new Date().getTime()}); //eslint-disable-line camelcase
      localStorage.setItem('versionCheckInfo', versionCheckInfo);
    };
  };

  return VersionUpdater;
});

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

const $        = require('jquery');
const _        = require('lodash');
const mrequest = require('helpers/mrequest');
const Routes   = require('gen/js-routes');

const VersionUpdater = function () {
  this.update = () => {
    if (canUpdateVersion()) {
      fetchStaleVersionInfo().then((data) => {
        _.isEmpty(data) ? markUpdateDone() : fetchLatestVersion(data);
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
    }).then(markUpdateDone);
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
};

module.exports = VersionUpdater;

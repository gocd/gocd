/*
 * Copyright 2015 ThoughtWorks, Inc.
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

var VersionUpdater = function (staleVersionInfoUrl, updateServerVersionInfoUrl) {
  this.staleVersionInfoUrl = staleVersionInfoUrl;
  this.updateServerVersionInfoUrl = updateServerVersionInfoUrl;
};

(function ($) {
  $.extend(VersionUpdater.prototype, {
    update: function () {
      if (this.canUpdateVersion()) {
        this.fetchStaleVersionInfo();
      }
    },

    fetchStaleVersionInfo: function () {
      var _this = this;
      $.ajax({
        headers: {Accept: "application/vnd.go.cd.v1+json"},
        type: 'GET',
        url: _this.staleVersionInfoUrl,
        success: function (data) {
          $.isEmptyObject(data) ? _this.markUpdateDone() : _this.fetchLatestVersion(data);
        }
      });
    },

    fetchLatestVersion: function (versionInfo) {
      var _this = this;
      $.ajax({
        headers: {Accept: "application/vnd.update.go.cd.v1+json"},
        type: 'GET',
        url:     versionInfo['update_server_url'],
        success: function (data) {
          _this.updateLatestVersion(data);
        }
      });
    },

    updateLatestVersion: function (data) {
      var _this = this;
      $.ajax({
        type: 'PATCH',
        headers: {Accept: "application/vnd.go.cd.v1+json"},
        contentType: 'application/json',
        url: _this.updateServerVersionInfoUrl,
        data: JSON.stringify(data),
        success: function () {
          _this.markUpdateDone();
        }
      })
    },

    canUpdateVersion: function () {
      var versionCheckInfo = localStorage.getItem('versionCheckInfo');
      if ($.isEmptyObject(versionCheckInfo)) {
        return true;
      }
      versionCheckInfo = JSON.parse(versionCheckInfo);
      var lastUpdateAt = new Date(versionCheckInfo.last_updated_at);
      var halfHourAgo = new Date(Date.now() - 30 * 60 * 1000);
      return halfHourAgo > lastUpdateAt;
    },

    markUpdateDone: function () {
      var versiobCheckInfo = JSON.stringify({last_updated_at: new Date().getTime()});
      localStorage.setItem('versionCheckInfo', versiobCheckInfo);
    }
  });
})(jQuery);

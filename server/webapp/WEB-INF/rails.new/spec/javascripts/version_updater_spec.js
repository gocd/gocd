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

describe("VersionUpdater", function () {
  beforeEach(function () {
    localStorage.clear();
  });

  describe("update", function () {
    it("should update the latest version", function () {
      var fortyMinutesAgo = new Date(Date.now() - 40 * 60 * 1000).getTime();
      var info = JSON.stringify({last_updated_at: fortyMinutesAgo});
      localStorage.setItem('versionCheckInfo', info);

      var versionInfo = {component_name: 'go_server', update_server_url: 'https://update.example.com/some/path?foo=bar&current_version=5.6.7-1'};
      var versionUpdater = new VersionUpdater("stale_version_info_url", "update_server_version_info_url");

      spyOn(versionUpdater, 'fetchLatestVersion').and.callFake(function (params) { });
      spyOn(jQuery, "ajax").and.callFake(function (options) {
        options.success(versionInfo);
      });

      versionUpdater.update();

      expect(jQuery.ajax).toHaveBeenCalled();
      expect(versionUpdater.fetchLatestVersion).toHaveBeenCalledWith(versionInfo);
    });

    it("should not update the latest version if updated in last half hour", function () {
      var tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).getTime();
      var info = JSON.stringify({last_updated_at: tenMinutesAgo});
      localStorage.setItem('versionCheckInfo', info);

      spyOn(jQuery, "ajax");

      var versionUpdater = new VersionUpdater("stale_version_info_url", "update_server_version_info_url");
      versionUpdater.update();

      expect(jQuery.ajax).not.toHaveBeenCalled();
    });

    it("should set the last update flag and do nothing if there are no version infos to update", function () {
      var now = new Date();
      var emptyVersionInfo = {};
      var versionUpdater = new VersionUpdater("stale_version_info_url", "update_server_version_info_url");

      spyOn(window, 'Date').and.callFake(function () {
        return now;
      });
      spyOn(versionUpdater, 'fetchLatestVersion').and.callFake(function (params) { });
      spyOn(jQuery, "ajax").and.callFake(function (options) {
        options.success(emptyVersionInfo);
      });

      versionUpdater.update();

      expect(jQuery.ajax).toHaveBeenCalled();
      expect(versionUpdater.fetchLatestVersion).not.toHaveBeenCalled();

      info = JSON.parse(localStorage.getItem('versionCheckInfo'));
      expect(info.last_updated_at).toBe(now.getTime());
    });

  });

  describe("fetchLatestVersion", function () {
    it("should fetch the latest version from update server", function () {
      var versionInfo = {component_name: 'go_server', update_server_url: 'https://update.example.com/some/path?foo=bar', installed_version: '15.1.0-123'};
      var latestVersion = {message: "{latest:123}", signature: "shashdsa"};
      var versionUpdater = new VersionUpdater("stale_version_info_url", "update_server_version_info_url");

      spyOn(versionUpdater, 'updateLatestVersion').and.callFake(function (params) { });
      spyOn(jQuery, "ajax").and.callFake(function (options) {
        options.success(latestVersion);
      });


      versionUpdater.fetchLatestVersion(versionInfo);

      expect(jQuery.ajax.calls.mostRecent().args[0]['url']).toBe(versionInfo.update_server_url);
      expect(jQuery.ajax).toHaveBeenCalled();
      expect(versionUpdater.updateLatestVersion).toHaveBeenCalledWith(latestVersion);
    })
  });

  describe("updateLatestVersion", function () {
    it("should update the latest version", function () {
      var now = new Date();
      var latestVersion = {message: "{latest:123}", signature: "shashdsa"}
      var versionUpdater = new VersionUpdater("stale_version_info_url", "update_server_version_info_url");

      spyOn(window, 'Date').and.callFake(function () {
        return now;
      });
      spyOn(jQuery, "ajax").and.callFake(function (options) {
        options.success();
      });

      versionUpdater.updateLatestVersion(latestVersion);

      expect(jQuery.ajax.calls.mostRecent().args[0]['url']).toBe("update_server_version_info_url");
      expect(jQuery.ajax).toHaveBeenCalled();
      info = JSON.parse(localStorage.getItem('versionCheckInfo'));
      expect(info.last_updated_at).toBe(now.getTime());
    })
  })
});

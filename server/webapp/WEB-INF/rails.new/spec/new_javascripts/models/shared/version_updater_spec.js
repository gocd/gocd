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

define(['mithril', 'jquery', 'models/shared/version_updater'], function (m, $, VersionUpdater) {
  describe("VersionUpdater", function () {
    describe('update', function() {
      beforeEach(function () {
        localStorage.clear();
      });

      it('should fetch the stale version info', function () {
        var deferred = $.Deferred();
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        var thirtyOneMinutesBack = new Date(Date.now() - 31 * 60 * 1000);

        localStorage.setItem('versionCheckInfo', JSON.stringify({last_updated_at: thirtyOneMinutesBack})); //eslint-disable-line camelcase
        spyOn(m, 'request').and.returnValue(deferred.promise());

        new VersionUpdater().update();

        var requestArgs = m.request.calls.mostRecent().args[0];
        requestArgs.config(xhr);

        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/version_infos/stale');
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
      });

      it('should skip updates if update tried in last half hour', function() {
        var deferred = $.Deferred();
        var twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);

        spyOn(m, 'request').and.returnValue(deferred.promise());
        localStorage.setItem('versionCheckInfo', JSON.stringify({last_updated_at: twentyNineMinutesBack})); //eslint-disable-line camelcase

        new VersionUpdater().update();

        expect(m.request).not.toHaveBeenCalled();
      });

      it('should skip updates in absence of stale version info and update local storage with last update time', function () {
        var deferred = $.Deferred();
        var myDate = jasmine.createSpyObj('Date', ['getTime']);

        spyOn(window, 'Date').and.returnValue(myDate);
        myDate.getTime.and.callFake(function() { return 123;});

        spyOn(m, 'request').and.returnValue(deferred.promise());

        deferred.resolve({});

        new VersionUpdater().update();

        expect(m.request).toHaveBeenCalledTimes(1);
        expect(localStorage.getItem('versionCheckInfo')).toEqual('{"last_updated_at":123}');
      });

      it('should fetch latest version info if can update', function () {
        var deferred = $.Deferred();
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

        spyOn(m, 'request').and.returnValue(deferred.promise());

        new VersionUpdater().update();
        deferred.resolve({'update_server_url': 'update.server.url'});

        var requestArgs = m.request.calls.all()[1].args[0];
        requestArgs.config(xhr);

        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('update.server.url');
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.update.go.cd.v1+json");
      });

      it('should post the latest version info to server', function () {
        var deferred = $.Deferred();
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
        var lastestVersionDeferred = $.Deferred();
        var myDate = jasmine.createSpyObj('Date', ['getTime']);

        spyOn(window, 'Date').and.returnValue(myDate);
        myDate.getTime.and.callFake(function() { return 123;});
        spyOn(m, 'request').and.returnValues(deferred.promise(), lastestVersionDeferred.promise(), deferred.promise());

        new VersionUpdater().update();
        deferred.resolve({'update_server_url': 'update.server.url'});
        lastestVersionDeferred.resolve({foo: 'bar'});

        var requestArgs = m.request.calls.all()[2].args[0];
        requestArgs.config(xhr);

        expect(requestArgs.method).toBe('PATCH');
        expect(requestArgs.url).toBe('/go/api/version_infos/go_server');
        expect(requestArgs.data).toEqual({foo: 'bar'});
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        expect(localStorage.getItem('versionCheckInfo')).toEqual('{"last_updated_at":123}');
      });
    });
  });
});
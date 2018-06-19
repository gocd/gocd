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

describe('Data Reporting', () => {
  const DataReporting    = require('models/shared/data_sharing/data_reporting');
  const DataReportingURL = '/go/api/internal/data_sharing/reporting';

  const dataReportingJSON = {
    "_embedded": {
      "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      "last_reported_at":        1529308350019,
      "data_sharing_server_url": "https://datasharing.gocd.org/v1"
    }
  };

  it('should deserialize data reporting from JSON', () => {
    const reportingInfo = DataReporting.fromJSON(dataReportingJSON, {getResponseHeader: () => 'ETag'});

    expect(reportingInfo.serverId()).toBe(dataReportingJSON._embedded.server_id);
    expect(reportingInfo.lastReportedAt()).toEqual(new Date(dataReportingJSON._embedded.last_reported_at));
    expect(reportingInfo.dataSharingServerUrl()).toBe(dataReportingJSON._embedded.data_sharing_server_url);
  });

  it('should fetch data reporting information', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(DataReportingURL).andReturn({
        responseText:    JSON.stringify(dataReportingJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((reportingInfo) => {
        expect(reportingInfo.serverId()).toBe(dataReportingJSON._embedded.server_id);
        expect(reportingInfo.lastReportedAt()).toEqual(new Date(dataReportingJSON._embedded.last_reported_at));
        expect(reportingInfo.dataSharingServerUrl()).toBe(dataReportingJSON._embedded.data_sharing_server_url);
      });

      DataReporting.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should patch data reporting information', () => {
    jasmine.Ajax.withMock(() => {
      const updatedDataReportingJSON = {
        "_embedded": {
          "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
          "last_reported_at":        1529308350020,
          "data_sharing_server_url": "https://datasharing.gocd.org/v1"
        }
      };

      jasmine.Ajax.stubRequest(DataReportingURL, undefined, 'PATCH').andReturn({
        responseText:    JSON.stringify(updatedDataReportingJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json',
          'ETag':         'ETag'
        }
      });

      const dataReportingSettings = DataReporting.fromJSON(dataReportingJSON, {getResponseHeader: () => 'ETag'});

      const successCallback = jasmine.createSpy().and.callFake(() => {
        expect(dataReportingSettings.lastReportedAt().getTime()).toBe(updatedDataReportingJSON._embedded.last_reported_at);
      });

      expect(dataReportingSettings.lastReportedAt().getTime()).toBe(dataReportingJSON._embedded.last_reported_at);

      dataReportingSettings.save().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});

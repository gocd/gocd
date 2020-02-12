/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {DataReporting} from "models/shared/data_sharing/data_reporting";

describe('Data Reporting', () => {
  const DataReportingInfoURL              = '/go/api/internal/data_sharing/reporting/info';
  const DataReportingStartReportingURL    = '/go/api/internal/data_sharing/reporting/start';
  const DataReportingCompleteReportingURL = '/go/api/internal/data_sharing/reporting/complete';

  const dataReportingJSON = {
    _embedded: {
      server_id:                            "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      last_reported_at:                     1529308350019,
      data_sharing_server_url:              "https://datasharing.gocd.org/v1",
      data_sharing_get_encryption_keys_url: "https://datasharing.gocd.org/encryption_keys",
      can_report:                           true
    }
  };

  it('should deserialize data reporting from JSON', () => {
    const reportingInfo = DataReporting.fromJSON(dataReportingJSON);

    expect(reportingInfo.serverId()).toBe(dataReportingJSON._embedded.server_id);
    expect(reportingInfo.lastReportedAt()).toEqual(new Date(dataReportingJSON._embedded.last_reported_at));
    expect(reportingInfo.dataSharingServerUrl()).toBe(dataReportingJSON._embedded.data_sharing_server_url);
    expect(reportingInfo.dataSharingGetEncryptionKeysUrl()).toBe(dataReportingJSON._embedded.data_sharing_get_encryption_keys_url);
    expect(reportingInfo.canReport()).toBe(dataReportingJSON._embedded.can_report);
  });

  it('should fetch data reporting information', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(DataReportingInfoURL).andReturn({
        responseText:    JSON.stringify(dataReportingJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v2+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((reportingInfo: any) => {
        expect(reportingInfo.serverId()).toBe(dataReportingJSON._embedded.server_id);
        expect(reportingInfo.lastReportedAt()).toEqual(new Date(dataReportingJSON._embedded.last_reported_at));
        expect(reportingInfo.dataSharingServerUrl()).toBe(dataReportingJSON._embedded.data_sharing_server_url);
        expect(reportingInfo.canReport()).toBe(dataReportingJSON._embedded.can_report);
      });

      DataReporting.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();

      expect(jasmine.Ajax.requests.at(0).url).toBe(DataReportingInfoURL);
      expect(jasmine.Ajax.requests.at(0).method).toBe('GET');
      expect(jasmine.Ajax.requests.at(0).requestHeaders.Accept).toBe('application/vnd.go.cd.v2+json');
    });
  });

  it('should start data reporting', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(DataReportingStartReportingURL, undefined, 'POST').andReturn({
        status:          204,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v2+json'
        }
      });

      const successCallback = jasmine.createSpy();

      DataReporting.startReporting().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should complete data reporting', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(DataReportingCompleteReportingURL, undefined, 'POST').andReturn({
        status:          204,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v2+json'
        }
      });

      const successCallback = jasmine.createSpy();

      DataReporting.completeReporting().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

});

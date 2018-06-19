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


describe('Usage Data Reporter', () => {
  const UsageDataReporter = require('models/shared/usage_data_reporter');

  const USAGE_DATA_LAST_REPORTED_TIME_KEY = "usage_data_last_reported_time";

  const usageDataURL           = '/go/api/internal/data_sharing/usagedata';
  const dataReportingURL       = '/go/api/internal/data_sharing/reporting';
  const dataSharingSettingsURL = '/go/api/data_sharing/settings';

  let reporter;
  beforeEach(() => {
    jasmine.Ajax.install();
    reporter = new UsageDataReporter();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    localStorage.removeItem(USAGE_DATA_LAST_REPORTED_TIME_KEY);
  });


  it('should do nothing when usage data is already reported for the day (lookup in local storage)', () => {
    lastReportedToday();

    reporter.report();
    expect(jasmine.Ajax.requests.count()).toBe(0);
  });

  it('should update reported time at local storage when usage data is not reported today (lookup in local storage) and data sharing is not allowed', (done) => {
    const yesterday = lastReportedYesterday();
    mockDataSharingSettingsAPIAndReturn(notAllowedDataSharingSettingsJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);

    reporter.report().then(() => {
      expect(jasmine.Ajax.requests.count()).toBe(1);
      expect(jasmine.Ajax.requests.at(0).url).toBe(dataSharingSettingsURL);

      // verify if the last reported time is updated in the local storage
      expect(new Date(+localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).getDate()).toBe(new Date().getDate());

      done();
    });
  });

  it('should update local storage when data is not reported today (lookup in local storage) and  data sharing is allowed and if latest data reporting has last usage reported time being today', (done) => {
    const yesterday = lastReportedYesterday();
    mockDataSharingSettingsAPIAndReturn(allowedDataSharingSettingsJSON);
    const reportingJSON = reportedTodayDataReportingJSON;
    mockDataReportingAPIAndReturn(reportingJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);

    reporter.report().then(() => {
      expect(jasmine.Ajax.requests.count()).toBe(2);

      expect(jasmine.Ajax.requests.at(0).url).toBe(dataSharingSettingsURL);
      expect(jasmine.Ajax.requests.at(1).url).toBe(dataReportingURL);

      expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${reportingJSON._embedded.last_reported_at}`);
      done();
    });
  });

  it('should report usage data to remote data reporting server url', (done) => {
    const yesterday = lastReportedYesterday();
    mockDataSharingSettingsAPIAndReturn(allowedDataSharingSettingsJSON);
    mockDataReportingAPIAndReturn(reportedYesterdayDataReportingJSON);
    mockUsageDataAPIAndReturn(usageDataJSON);
    mockDataSharingServerAPIAndReturn();
    mockDataReportingUpdateAPIAndReturn(reportedTodayDataReportingJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);

    reporter.report().then(() => {
      expect(jasmine.Ajax.requests.count()).toBe(5);

      expect(jasmine.Ajax.requests.at(0).url).toBe(dataSharingSettingsURL);
      expect(jasmine.Ajax.requests.at(1).url).toBe(dataReportingURL);
      expect(jasmine.Ajax.requests.at(2).url).toBe(usageDataURL);
      expect(jasmine.Ajax.requests.at(3).url).toBe(reportedYesterdayDataReportingJSON._embedded.data_sharing_server_url);

      // verify if the last reported time is updated in the local storage
      expect(new Date(+localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).getDate()).toBe(new Date().getDate());
      done();
    });
  });

  function today() {
    return new Date().getTime();
  }

  function yesterday() {
    const date = new Date();
    date.setDate(date.getDate() - 1);
    return date.getTime();
  }

  function lastReportedToday() {
    const date = today();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, date);
    return date;
  }

  function lastReportedYesterday() {
    const date = yesterday();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, date);
    return date;
  }

  function mockDataSharingSettingsAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(dataSharingSettingsURL, undefined, 'GET').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataReportingAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(dataReportingURL, undefined, 'GET').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataReportingUpdateAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(dataReportingURL, undefined, 'PATCH').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockUsageDataAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(usageDataURL, undefined, 'GET').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataSharingServerAPIAndReturn() {
    jasmine.Ajax.stubRequest(reportedYesterdayDataReportingJSON._embedded.data_sharing_server_url, undefined, 'POST').andReturn({
      responseText: JSON.stringify({}),
      status:       200
    });
  }


  const allowedDataSharingSettingsJSON = {
    "_embedded": {
      "allow":      true,
      "updated_by": "Admin",
      "updated_on": "2018-06-14T05:45:30Z"
    }
  };

  const notAllowedDataSharingSettingsJSON = {
    "_embedded": {
      "allow":      false,
      "updated_by": "Admin",
      "updated_on": "2018-06-14T05:45:30Z"
    }
  };

  const reportedTodayDataReportingJSON = {
    "_embedded": {
      "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      "last_reported_at":        today(),
      "data_sharing_server_url": "https://datasharing.gocd.org/v1"
    }
  };

  const reportedYesterdayDataReportingJSON = {
    "_embedded": {
      "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      "last_reported_at":        yesterday(),
      "data_sharing_server_url": "https://datasharing.gocd.org/v1"
    }
  };

  const usageDataJSON = {
    "_embedded": {
      "pipeline_count":                 1,
      "agent_count":                    0,
      "oldest_pipeline_execution_time": 1528887811275
    }
  };
});

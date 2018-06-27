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

  const usageDataURL          = '/go/api/internal/data_sharing/usagedata';
  const usageReportingGetURL  = '/go/api/internal/data_sharing/reporting/info';
  const usageReportingPostURL = '/go/api/internal/data_sharing/reporting/reported';

  let reporter;
  beforeEach(() => {
    jasmine.Ajax.install();
    reporter = new UsageDataReporter();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    localStorage.removeItem(USAGE_DATA_LAST_REPORTED_TIME_KEY);
  });


  it('should do nothing when usage data is already reported within 30 mins(lookup in local storage)', () => {
    triedReportingWithin30Minutes();

    reporter.report();
    expect(jasmine.Ajax.requests.count()).toBe(0);
  });

  it('should do nothing when reporting is not allowed', async () => {
    const yesterday = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(notAllowedReportingJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);
    await reporter.report();

    expect(jasmine.Ajax.requests.count()).toBe(1);
    expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);

    // verify if the last reported time is updated in the local storage
    expect(new Date(+localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).getDate()).toBe(new Date().getDate());
  });

  it('should report usage data to remote server', async () => {
    const yesterday = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(allowedReportingJSON);
    mockUsageDataAPIAndReturn(usageDataJSON);
    mockDataSharingServerAPIAndPass();
    mockDataSharingReportingPostAPIAndReturn(allowedReportingJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);
    await reporter.report();

    expect(jasmine.Ajax.requests.count()).toBe(4);
    expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);
    expect(jasmine.Ajax.requests.at(1).url).toBe(usageDataURL);
    expect(jasmine.Ajax.requests.at(2).url).toBe(allowedReportingJSON._embedded.data_sharing_server_url);
    expect(jasmine.Ajax.requests.at(2).method).toBe('POST');
    expect(jasmine.Ajax.requests.at(3).url).toBe(usageReportingPostURL);
    expect(jasmine.Ajax.requests.at(3).method).toBe('POST');

    // verify if the last reported time is updated in the local storage
    expect(new Date(+localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).getDate()).toBe(new Date().getDate());
  });

  it('should not update last reported time if reporting to remote server fails', async () => {
    const yesterday = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(allowedReportingJSON);
    mockUsageDataAPIAndReturn(usageDataJSON);
    mockDataSharingServerAPIAndFail();

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);

    try {
      await reporter.report();
    } catch (e) {
      expect(jasmine.Ajax.requests.count()).toBe(3);
      expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);
      expect(jasmine.Ajax.requests.at(1).url).toBe(usageDataURL);
      expect(jasmine.Ajax.requests.at(2).url).toBe(allowedReportingJSON._embedded.data_sharing_server_url);
      expect(jasmine.Ajax.requests.at(2).method).toBe('POST');

      // verify if the last reported time is updated in the local storage
      expect(new Date(+localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).getDate()).toBe(new Date().getDate());
    }
  });

  function today() {
    return new Date().getTime();
  }

  function yesterday() {
    const date = new Date();
    date.setDate(date.getDate() - 1);
    return date.getTime();
  }

  function triedReportingWithin30Minutes() {
    const date = today();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, date);
    return date;
  }

  function lastReportedYesterday() {
    const date = yesterday();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, date);
    return date;
  }

  function mockDataSharingReportingGetAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(usageReportingGetURL, undefined, 'GET').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataSharingReportingPostAPIAndReturn(json) {
    jasmine.Ajax.stubRequest(usageReportingPostURL, undefined, 'POST').andReturn({
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

  function mockDataSharingServerAPIAndPass() {
    jasmine.Ajax.stubRequest(allowedReportingJSON._embedded.data_sharing_server_url, undefined, 'POST').andReturn({
      responseText: JSON.stringify({}),
      status:       200
    });
  }

  function mockDataSharingServerAPIAndFail() {
    jasmine.Ajax.stubRequest(allowedReportingJSON._embedded.data_sharing_server_url, undefined, 'POST').andReturn({
      status: 500
    });
  }

  const notAllowedReportingJSON = {
    "_embedded": {
      "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      "last_reported_at":        1529308350019,
      "data_sharing_server_url": "https://datasharing.gocd.org/v1",
      "can_report":              false
    }
  };

  const allowedReportingJSON = {
    "_embedded": {
      "server_id":               "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      "last_reported_at":        1529308350019,
      "data_sharing_server_url": "https://datasharing.gocd.org/v1",
      "can_report":              true
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

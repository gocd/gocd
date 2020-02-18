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
import "jasmine-ajax";
import {EncryptionKeys} from "models/shared/data_sharing/usage_data";
import {UsageDataReporter} from "models/shared/usage_data_reporter";

describe('Usage Data Reporter', () => {
  const USAGE_DATA_LAST_REPORTED_TIME_KEY = "last_usage_data_reporting_check_time";

  const encryptedUsageDataURL     = '/go/api/internal/data_sharing/usagedata/encrypted';
  const usageReportingGetURL      = '/go/api/internal/data_sharing/reporting/info';
  const usageReportingStartURL    = '/go/api/internal/data_sharing/reporting/start';
  const usageReportingCompleteURL = '/go/api/internal/data_sharing/reporting/complete';

  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    localStorage.removeItem(USAGE_DATA_LAST_REPORTED_TIME_KEY);
  });

  it('should do nothing when usage data is already reported within 30 mins(lookup in local storage)', () => {
    triedReportingWithin30Minutes();

    UsageDataReporter.report();
    expect(jasmine.Ajax.requests.count()).toBe(0);
  });

  //@ts-ignore
  it('should do nothing when reporting is not allowed', async () => {
    const yesterday = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(notAllowedReportingJSON);

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);
    await UsageDataReporter.report();

    expect(jasmine.Ajax.requests.count()).toBe(1);
    expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);

    // verify that the last reported time is updated in the local storage
    expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getDate()).toBe(new Date().getDate());
    expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getTime()).toBeGreaterThan(yesterday);
  });

  //@ts-ignore
  it('should report usage data to remote server', async () => {
    const signature            = 'some-signature';
    const subordinatePublicKey = 'valid-public-key';
    const yesterday            = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(allowedReportingJSON);
    mockUsageDataAPIAndReturn(encryptedUsageData);
    mockDataSharingReportingStartAPI();
    mockDataSharingServerGetEncryptionKeysAndReturn(signature, subordinatePublicKey);
    mockDataSharingServerAPIAndPass();
    mockDataSharingReportingCompleteAPI();

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);
    await UsageDataReporter.report();

    expect(jasmine.Ajax.requests.count()).toBe(6);
    expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);

    expect(jasmine.Ajax.requests.at(1).url).toBe(usageReportingStartURL);
    expect(jasmine.Ajax.requests.at(1).method).toBe('POST');

    expect(jasmine.Ajax.requests.at(2).url).toBe(allowedReportingJSON._embedded.data_sharing_get_encryption_keys_url);

    expect(jasmine.Ajax.requests.at(3).url).toBe(encryptedUsageDataURL);
    expect(jasmine.Ajax.requests.at(3).method).toBe('POST');
    expect(JSON.parse(JSON.stringify(jasmine.Ajax.requests.at(3).data())) as EncryptionKeys).toEqual({
      signature,
      subordinate_public_key: subordinatePublicKey
    } as EncryptionKeys);

    expect(jasmine.Ajax.requests.at(4).url).toBe(allowedReportingJSON._embedded.data_sharing_server_url);
    expect(jasmine.Ajax.requests.at(4).method).toBe('POST');
    expect(jasmine.Ajax.requests.at(4).data()).toEqual(encryptedUsageData);

    expect(jasmine.Ajax.requests.at(5).url).toBe(usageReportingCompleteURL);
    expect(jasmine.Ajax.requests.at(5).method).toBe('POST');

    // verify that the last reported time is updated in the local storage
    expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getDate()).toBe(new Date().getDate());
    expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getTime()).toBeGreaterThan(yesterday);
  });

  //@ts-ignore
  it('should not update last reported time if reporting to remote server fails', async () => {
    const yesterday = lastReportedYesterday();
    mockDataSharingReportingGetAPIAndReturn(allowedReportingJSON);
    mockDataSharingReportingStartAPI();
    mockDataSharingServerGetEncryptionKeysAndReturn('some-signature', 'valid-public-key');
    mockUsageDataAPIAndReturn(encryptedUsageData);
    mockDataSharingServerAPIAndFail();

    expect(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY)).toBe(`${yesterday}`);

    try {
      await UsageDataReporter.report();
    } catch (e) {
      expect(jasmine.Ajax.requests.count()).toBe(5);
      expect(jasmine.Ajax.requests.at(0).url).toBe(usageReportingGetURL);

      expect(jasmine.Ajax.requests.at(1).url).toBe(usageReportingStartURL);
      expect(jasmine.Ajax.requests.at(1).method).toBe('POST');

      expect(jasmine.Ajax.requests.at(2).url).toBe(allowedReportingJSON._embedded.data_sharing_get_encryption_keys_url);

      expect(jasmine.Ajax.requests.at(3).url).toBe(encryptedUsageDataURL);
      expect(jasmine.Ajax.requests.at(3).method).toBe('POST');

      expect(jasmine.Ajax.requests.at(4).url).toBe(allowedReportingJSON._embedded.data_sharing_server_url);
      expect(jasmine.Ajax.requests.at(4).method).toBe('POST');

      expect(jasmine.Ajax.requests.filter(usageReportingCompleteURL)).toHaveLength(0);

      // verify that the last reported time is updated in the local storage
      expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getDate()).toBe(new Date().getDate());
      expect(new Date(+(localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY) || '')).getTime()).toBeGreaterThan(yesterday);
    }
  });

  function today(): number {
    return new Date().getTime();
  }

  function yesterday(): number {
    const date = new Date();
    date.setDate(date.getDate() - 1);
    return date.getTime();
  }

  function triedReportingWithin30Minutes(): number {
    const date: number = today();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, `${date}`);
    return date;
  }

  function lastReportedYesterday(): number {
    const date: number = yesterday();
    localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, `${date}`);
    return date;
  }

  function mockDataSharingReportingGetAPIAndReturn(json: any) {
    jasmine.Ajax.stubRequest(usageReportingGetURL, undefined, 'GET').andReturn({
      responseText:    JSON.stringify(json),
      status:          200,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataSharingReportingStartAPI() {
    jasmine.Ajax.stubRequest(usageReportingStartURL, undefined, 'POST').andReturn({
      status:          204,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockDataSharingReportingCompleteAPI() {
    jasmine.Ajax.stubRequest(usageReportingCompleteURL, undefined, 'POST').andReturn({
      status:          204,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      }
    });
  }

  function mockUsageDataAPIAndReturn(json: any) {
    jasmine.Ajax.stubRequest(encryptedUsageDataURL, undefined, 'POST').andReturn({
      responseText: JSON.stringify(json),
      status:       200
    });
  }

  function mockDataSharingServerAPIAndPass() {
    jasmine.Ajax.stubRequest(allowedReportingJSON._embedded.data_sharing_server_url, undefined, 'POST').andReturn({
      responseText: JSON.stringify({}),
      status:       200
    });
  }

  function mockDataSharingServerGetEncryptionKeysAndReturn(signature: string, subordinatePublicKey: string) {
    jasmine.Ajax.stubRequest(allowedReportingJSON._embedded.data_sharing_get_encryption_keys_url, undefined, 'GET').andReturn({
      responseText: JSON.stringify({
        signature,
        subordinate_public_key: subordinatePublicKey
      }),
      status:       200
    });
  }

  function mockDataSharingServerAPIAndFail() {
    jasmine.Ajax.stubRequest(allowedReportingJSON._embedded.data_sharing_server_url, undefined, 'POST').andReturn({
      status: 500
    });
  }

  const notAllowedReportingJSON = {
    _embedded: {
      server_id:                            "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      last_reported_at:                     1529308350019,
      data_sharing_server_url:              "https://datasharing.gocd.org/v1",
      data_sharing_get_encryption_keys_url: "https://datasharing.gocd.org/encryption_keys",
      can_report:                           false
    }
  };

  const allowedReportingJSON = {
    _embedded: {
      server_id:                            "621bf5cb-25fa-4c75-9dd2-097ef6b3bdd1",
      last_reported_at:                     1529308350019,
      data_sharing_server_url:              "https://datasharing.gocd.org/v1",
      data_sharing_get_encryption_keys_url: "https://datasharing.gocd.org/encryption_keys",
      can_report:                           true
    }
  };

  const encryptedUsageData = "encrypted-data";
});

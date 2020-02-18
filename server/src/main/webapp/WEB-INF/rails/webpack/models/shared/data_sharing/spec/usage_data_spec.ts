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
import {EncryptionKeys, UsageData, UsageDataJSON} from "models/shared/data_sharing/usage_data";

describe('Data Sharing Usage Data', () => {
  const dataSharingUsageDataURL          = '/go/api/internal/data_sharing/usagedata';
  const dataSharingEncryptedUsageDataURL = '/go/api/internal/data_sharing/usagedata/encrypted';

  const dataSharingUsageJSON = {
    server_id:       "some-random-string",
    message_version: 2,
    data:            {
      pipeline_count:                 1,
      agent_count:                    0,
      oldest_pipeline_execution_time: 1528887811275,
      gocd_version:                   "18.9.0"
    }
  } as UsageDataJSON;

  it('should use API version v3', () => {
    expect(UsageData.API_VERSION).toBe('v3');
  });

  it('should deserialize data sharing usage data from JSON', () => {
    const usageData = UsageData.fromJSON(dataSharingUsageJSON);
    expect(usageData.message()).toEqual(dataSharingUsageJSON);
  });

  it('should represent pretty formatted data', () => {
    const usageData = UsageData.fromJSON(dataSharingUsageJSON);
    expect(usageData.represent()).toBe(JSON.stringify(dataSharingUsageJSON, null, 4));
  });

  it('should fetch data sharing usage data', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(dataSharingUsageDataURL).andReturn({
        responseText:    JSON.stringify(dataSharingUsageJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v2+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((usageData: any) => {
        expect(usageData.represent()).toBe(JSON.stringify(dataSharingUsageJSON, null, 4));
      });

      UsageData.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should fetch data sharing encrypted usage data', () => {
    const encryptedData = "Something really secret";

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(dataSharingEncryptedUsageDataURL, undefined, 'POST').andReturn({
        responseText:    encryptedData,
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/octet-stream'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((data: any ) => {
        expect(data).toBe(encryptedData);
      });

      const encryptionKeys = {
        signature:              'some-signed-key',
        subordinate_public_key: 'some-public-key'
      } as EncryptionKeys;

      UsageData.getEncrypted(encryptionKeys).then(successCallback);
      expect(successCallback).toHaveBeenCalled();

      expect(jasmine.Ajax.requests.count()).toBe(1);
      expect(jasmine.Ajax.requests.at(0).url).toBe('/go/api/internal/data_sharing/usagedata/encrypted');
      expect(jasmine.Ajax.requests.at(0).method).toBe('POST');
      const dataFromRequest: EncryptionKeys = JSON.parse(JSON.stringify(jasmine.Ajax.requests.at(0).data()));
      expect(dataFromRequest).toEqual(encryptionKeys);
    });
  });
});

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

describe('Data Sharing Usage Data', () => {
  const UsageData               = require('models/data_sharing_settings/usage_data');
  const dataSharingUsageDataURL = '/go/api/internal/data_sharing/usagedata';

  const dataSharingUsageJSON = {
    "_embedded": {
      "pipeline_count":                 1,
      "agent_count":                    0,
      "oldest_pipeline_execution_time": 1528887811275
    }
  };

  it('should deserialize data sharing usage data from JSON', () => {
    const usageData = UsageData.fromJSON(dataSharingUsageJSON);

    expect(usageData.pipelineCount()).toBe(dataSharingUsageJSON._embedded.pipeline_count);
    expect(usageData.agentCount()).toBe(dataSharingUsageJSON._embedded.agent_count);
    expect(usageData.oldestPipelineExecutionTime()).toBe(dataSharingUsageJSON._embedded.oldest_pipeline_execution_time);
  });

  it('should represent pretty formatted data', () => {
    const usageData = UsageData.fromJSON(dataSharingUsageJSON);
    expect(usageData.represent()).toBe(JSON.stringify(dataSharingUsageJSON._embedded, null, 4));
  });

  it('should fetch data sharing usage data', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(dataSharingUsageDataURL).andReturn({
        responseText:    JSON.stringify(dataSharingUsageJSON),
        status:          200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((usageData) => {
        expect(usageData.pipelineCount()).toBe(dataSharingUsageJSON._embedded.pipeline_count);
        expect(usageData.agentCount()).toBe(dataSharingUsageJSON._embedded.agent_count);
        expect(usageData.oldestPipelineExecutionTime()).toBe(dataSharingUsageJSON._embedded.oldest_pipeline_execution_time);
      });

      UsageData.get().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});

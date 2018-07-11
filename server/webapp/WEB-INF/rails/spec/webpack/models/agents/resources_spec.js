/*
 * Copyright 2016 ThoughtWorks, Inc.
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
describe('Resources Model', () => {

  const Resources = require('models/agents/resources');

  require('jasmine-ajax');

  it("should initialize the resources in sorted order", () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/resources', undefined, 'GET').andReturn({
        "responseText": JSON.stringify(["Linux", "Firefox"]),
        "status":       200
      });

      const successCallback = jasmine.createSpy().and.callFake((resources) => {
        expect(resources.length).toBe(2);
        expect(resources[0].name()).toBe('Firefox');
        expect(resources[1].name()).toBe('Linux');
      });

      Resources.all().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it("should initialize the resources with state depending upon the checkedAgents", () => {
    const checkedAgents = [{
      resources() {
        return ['Linux'];
      }
    }, {
      resources() {
        return ['Linux', 'Firefox'];
      }
    }];

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/resources', undefined, 'GET').andReturn({
        "responseText": JSON.stringify(["Linux", "Firefox"]),
        "status":       200
      });

      const successCallback = jasmine.createSpy().and.callFake((resources) => {
        expect(resources.length).toBe(2);
        expect(resources[0].name()).toBe('Firefox');
        expect(resources[1].name()).toBe('Linux');

        expect(resources[0].isChecked()).toBe(false);
        expect(resources[0].isIndeterminate()).toBe(true);
        expect(resources[1].isChecked()).toBe(true);
        expect(resources[1].isIndeterminate()).toBe(false);
      });

      Resources.all(checkedAgents).then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});

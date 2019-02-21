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

describe('Agent Environments Model', () => {

  const Environments = require('models/agents/environments');
  const Agents = require('models/agents/agents');

  require('jasmine-ajax');

  it("should initialize the environments in sorted order", () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/environments', undefined, 'GET').andReturn({
        responseText: JSON.stringify(["QA", "Dev", "Test"]),
        status:       200
      });

      const successCallback = jasmine.createSpy().and.callFake((environments) => {
        expect(environments.length).toBe(3);
        expect(environments[0].name()).toBe('Dev');
        expect(environments[1].name()).toBe('QA');
        expect(environments[2].name()).toBe('Test');
      });

      Environments.all().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it("should initialize the environments with state depending upon the checkedAgents", () => {
    const checkedAgents = [{
      environments() {
        return [
          new Agents.Agent.Environment({name: 'Test', associatedFromConfigRepo: true}),
          new Agents.Agent.Environment({name: 'QA', associatedFromConfigRepo: false})
        ];
      },
      environmentNames() {
        return ['Test', 'QA'];
      }
    }, {
      environments() {
        return [
          new Agents.Agent.Environment({name: 'Test', associatedFromConfigRepo: false}),
          new Agents.Agent.Environment({name: 'Dev', associatedFromConfigRepo: false}),
          new Agents.Agent.Environment({name: 'QA', associatedFromConfigRepo: false})
        ];
      },
      environmentNames() {
        return ['Test', 'Dev', 'QA'];
      }
    }];

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/admin/internal/environments', undefined, 'GET').andReturn({
        responseText: JSON.stringify(["QA", "Dev", "Test"]),
        status:       200
      });

      const successCallback = jasmine.createSpy().and.callFake((environments) => {
        expect(environments.length).toBe(3);
        expect(environments[0].name()).toBe('Dev');
        expect(environments[1].name()).toBe('QA');
        expect(environments[2].name()).toBe('Test');

        expect(environments[0].isChecked()).toBe(false);
        expect(environments[0].isIndeterminate()).toBe(true);
        expect(environments[0].disabled()).toBe(false);

        expect(environments[1].isChecked()).toBe(true);
        expect(environments[1].isIndeterminate()).toBe(false);
        expect(environments[1].disabled()).toBe(false);

        expect(environments[2].isChecked()).toBe(true);
        expect(environments[2].isIndeterminate()).toBe(false);
        expect(environments[2].disabled()).toBe(true);
        expect(environments[2].tooltip()).toBe("Cannot edit Environment associated from Config Repo");
      });

      Environments.all(checkedAgents).then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });
});

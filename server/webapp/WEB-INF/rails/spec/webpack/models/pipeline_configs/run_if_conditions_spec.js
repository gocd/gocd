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
describe("RunIfConditions", () => {
  const RunIfConditions = require('models/pipeline_configs/run_if_conditions');

  describe('RunIfConditions create', () => {

    it('should create runIfConditions', () => {
      const runIf = ['passed', 'failed'];

      const runIfConditions = RunIfConditions.create(runIf);

      expect(runIfConditions.data()).toEqual(runIf);
    });

    it('should create runIfConditions with default as "passed"', () => {
      const runIfConditions = RunIfConditions.create();

      expect(runIfConditions.data()).toEqual(['passed']);
    });
  });

  describe('RunIfConditions push', () => {
    it('should add a condition', () => {
      const runIfConditions = RunIfConditions.create([]);

      runIfConditions.push('any');

      expect(runIfConditions.data()).toEqual(['any']);
    });

    it("should either have 'any' or 'passed || failed' condition", () => {
      const runIfConditions = RunIfConditions.create(['passed']);

      runIfConditions.push('any');

      expect(runIfConditions.data()).toEqual(['any']);

      runIfConditions.push('passed');

      expect(runIfConditions.data()).toEqual(['passed']);
    });
  });

  describe('RunIfConditions pop', () => {
    it('should pop out a condition', () => {
      const runIfConditions = RunIfConditions.create(['passed', 'failed']);

      runIfConditions.pop();

      expect(runIfConditions.data()).toEqual(['passed']);
    });
  });

});

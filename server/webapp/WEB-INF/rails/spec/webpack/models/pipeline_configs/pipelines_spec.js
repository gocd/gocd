/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe('Pipelines', () => {
  const Pipelines = require("models/pipeline_configs/pipelines");

  describe('all', () => {
    it('should get all pipelines', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/admin/internal/pipelines', undefined, 'GET').andReturn({
          responseText: JSON.stringify({
            _embedded: {
              pipelines: [
                {name: 'p1', stages: [{name: 's1', jobs: ['job1', 'job2']}]},
                {name: 'p2', stages: [{name: 's2', jobs: ['job3', 'job4']}]},
              ]
            }
          }),
          status:       200,
          headers:      {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((pipelines) => {
          expect(pipelines.length).toBe(2);
          expect(pipelines[0].name).toBe('p1');
          expect(pipelines[1].name).toBe('p2');
        });

        Pipelines.all().then(errorCallback);
      });
    });
  });
});

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

define(['jquery', 'mithril', 'lodash', "models/pipeline_configs/pipelines"],
  function ($, m, _, Pipelines) {
    describe('Pipelines', function () {
      describe('init', function () {
        var requestArgs;

        beforeEach(function () {
          spyOn(m, 'request').and.returnValue($.Deferred());
        });

        it('should get pipelines from admin internal api', function () {
          Pipelines.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('GET');
          expect(requestArgs.url).toBe('/go/api/admin/internal/pipelines');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          Pipelines.init();

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should unwrap the response data to return list of pipelines', function () {
          Pipelines.init();

          requestArgs = m.request.calls.mostRecent().args[0];

          var pipelines = {
            _embedded: {
              pipelines: [
                {name: 'p1', stages: [{name: 's1', jobs: ['job1', 'job2']}]},
                {name: 'p2', stages: [{name: 's2', jobs: ['job3', 'job4']}]}]
            }
          };

          expect(requestArgs.unwrapSuccess(pipelines)).toEqual(pipelines._embedded.pipelines);
        });

        it('should reject pipeline from the response', function () {
          var pipeline1 = {name: 'p1', stages: [{name: 's1', jobs: ['job1', 'job2']}]};
          var pipeline2 = {name: 'p2', stages: [{name: 's2', jobs: ['job3', 'job4']}]};
          var pipeline3 = {name: 'p3', stages: [{name: 's3', jobs: ['job5', 'job6']}]};

          Pipelines.init('p3');

          requestArgs = m.request.calls.mostRecent().args[0];

          var pipelines = {
            _embedded: {
              pipelines: [pipeline1, pipeline2, pipeline3]
            }
          };

          expect(requestArgs.unwrapSuccess(pipelines)).toEqual([pipeline1, pipeline2]);
        });
      });
    });
  });

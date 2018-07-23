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

describe("Dashboard", () => {
  describe("Pipeline Selection", () => {
    const PipelineSelection = require("models/dashboard/pipeline_selection");

    it('should fetch previously selected pipelines with appropriate headers', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/internal/pipeline_selection', undefined, 'GET').andReturn({
          responseText:    JSON.stringify(pipelineSelectionData),
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((pipelineSelection) => {
          expect(pipelineSelection.blacklist).toEqual(pipelineSelection["blacklist"]);
          expect(pipelineSelection.pipelineGroups()).toEqual(pipelineSelectionData["pipelines"]);
          expect(pipelineSelection.selections.up42()).toEqual(false);
          expect(pipelineSelection.selections.test()).toEqual(false);
          expect(pipelineSelection.selections.one()).toEqual(true);
          expect(pipelineSelection.selections.two()).toEqual(true);
          expect(pipelineSelection.selections.three()).toEqual(true);
        });

        PipelineSelection.get().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/internal/pipeline_selection');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should post selected pipelines", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest('/go/api/internal/pipeline_selection', JSON.stringify(pipelineSelectionPostData), 'PUT').andReturn({
          responseHeaders: {
            ETag:           'etag',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          204
        });

        const successCallback = jasmine.createSpy();

        PipelineSelection.fromJSON(pipelineSelectionData).update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('PUT');
        expect(request.url).toBe('/go/api/internal/pipeline_selection');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    const pipelineSelectionData = {
      "filters": [
        {
          "name": "Default",
          "pipelines": [
            "up42",
            "test"
          ],
          "type": "blacklist"
        }
      ],
      "pipelines":  {
        "first":  [
          "up42",
          "one",
          "two",
          "three"
        ],
        "second": [
          "test"
        ]
      }
    };

    const pipelineSelectionPostData = {
      "filters": [
        {
          "name": "Default",
          "pipelines": [
            "up42",
            "test"
          ],
          "type": "blacklist"
        }
      ],
    };

  });
});

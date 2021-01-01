/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {SparkRoutes} from "helpers/spark_routes";
import {Material} from "models/dashboard/material";
import Stream from "mithril/stream";

describe("Dashboard", () => {
  describe('Material Model', () => {

    it('should validate selected material when it is invalid', () => {
      const material = new Material({
        type:         'Git',
        name:         'name',
        fingerprint:  'fingerprint',
        folder:       'folder',
        revision:     'revision',
        pipelineName: 'pipelineName'
      });

      material.searchText = Stream('foo');
      material.selection  = Stream('');

      expect(material.validate()).toBe(false);
    });

    it('should validate selected material when it is valid', () => {
      const material      = new Material({
        type:         'Git',
        name:         'name',
        fingerprint:  'fingerprint',
        folder:       'folder',
        revision:     'revision',
        pipelineName: 'pipelineName'
      });
      material.searchText = Stream('foo');
      material.selection  = Stream('foo');
      expect(material.validate()).toBe(true);
    });

    it('should perform search', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(SparkRoutes.pipelineMaterialSearchPath('pipelineName', 'fingerprint', 'foo'), undefined, 'GET')
          .andReturn({
            responseText:    JSON.stringify(json),
            responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
            status:          200
          });
        const material = new Material({
          type:         'Git',
          name:         'name',
          fingerprint:  'fingerprint',
          folder:       'folder',
          revision:     'revision',
          pipelineName: 'pipelineName'
        });
        material.searchText = Stream('foo');
        material.performSearch().then(() => {
          expect(material.searchResults()).toEqual(json);
        });
      });
    });

    const json = [
      {
        "revision": {
          "date":              "2018-02-08T04:32:11Z",
          "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
          "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
          "last_run_revision": "foo"
        }
      }
    ];

  });
});

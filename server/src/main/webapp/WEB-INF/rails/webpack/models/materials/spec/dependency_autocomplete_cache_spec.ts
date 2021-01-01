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
import {DependencyMaterialAutocomplete} from "models/materials/dependency_autocomplete_cache";

describe("DependencyMaterial Suggestions Model", () => {
  it("Fetches and caches pipeline and stage suggestions", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.internalDependencyMaterialSuggestionsPath(), undefined, "GET")
        .andReturn({
          responseText: JSON.stringify([
            {name: "pipe1", stages: ["a", "b", "c"]},
            {name: "pipe2", stages: ["d", "e", "f"]},
          ]),
          status: 200,
          responseHeaders: {
            "Content-Type": "application/vnd.go.cd.v1+json",
            "ETag": "ETag"
          }
        });
      const cache = new DependencyMaterialAutocomplete<string, string>(String, String);
      expect(cache.pipelines()).toEqual([]);

      cache.prime(() => {
        expect(cache.failed()).toBe(false);
        expect(cache.pipelines()).toEqual(["pipe1", "pipe2"]);
        expect(cache.stages("pipe1")).toEqual(["a", "b", "c"]);
        expect(cache.stages("pipe2")).toEqual(["d", "e", "f"]);
        expect(cache.stages("unknown")).toEqual([]);
        done();
      }, () => { done.fail("Response should have succeeded"); });
    });
  });

  it("Reports errors", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.internalDependencyMaterialSuggestionsPath(), undefined, "GET")
        .andReturn({
          responseText: JSON.stringify({message: "Uh-oh!"}),
          status: 400,
          responseHeaders: {
            "Content-Type": "application/json"
          }
        });
      const cache = new DependencyMaterialAutocomplete<string, string>(String, String);
      expect(cache.pipelines()).toEqual([]);

      cache.prime(() => { done.fail("Response should have failed"); }, () => {
        expect(cache.failed()).toBe(true);
        expect(cache.failureReason()).toBe("Uh-oh!");
        expect(cache.pipelines()).toEqual([]);
        done();
      });
    });
  });
});

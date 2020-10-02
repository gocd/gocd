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
import {Personalization} from "models/dashboard/personalization";
import {SparkRoutes} from "helpers/spark_routes";

describe("Personalization", () => {
  it('should fetch previously selected pipelines with appropriate headers', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(pipelineSelectionData),
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          200
      });

      const successCallback = jasmine.createSpy().and.callFake((pipelineSelection) => {
        const expected = pipelineSelectionData["filters"][0];
        const filter = pipelineSelection.namedFilter("Default");

        expect(filter.name).toEqual(expected["name"]);
        expect(filter.type).toEqual(expected["type"]);
        expect(filter.pipelines).toEqual(expected["pipelines"]);
      });

      Personalization.get().then(successCallback);

      expect(successCallback).toHaveBeenCalled();

      expect(jasmine.Ajax.requests.count()).toBe(1);
      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.method).toBe('GET');
      expect(request.url).toBe(SparkRoutes.pipelineSelectionPath());
      expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
    });
  });

  it("should post selected pipelines", () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), JSON.stringify({filters: pipelineSelectionPostData}), 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          204
      });

      const successCallback = jasmine.createSpy();

      Personalization.fromJSON(pipelineSelectionData).updateFilters(pipelineSelectionPostData).then(successCallback);

      expect(successCallback).toHaveBeenCalled();

      expect(jasmine.Ajax.requests.count()).toBe(1);
      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.method).toBe('PUT');
      expect(request.url).toBe(SparkRoutes.pipelineSelectionPath());
      expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
    });
  });

  it("names() returns the names of all filters", () => {
    const filters = [{name: "Default", type: "blacklist", pipelines: ["a", "b"]}, {name: "foo", type: "whitelist", pipelines: []}];
    expect(new Personalization(filters, {}).names()).toEqual(["Default", "foo"]);
  });

  it("namedFilter() retrieves an existing filter by name", () => {
    const filters = [{name: "Default", state: [], type: "blacklist", pipelines: ["a", "b"]}, {name: "foo", state: [], type: "whitelist", pipelines: []}];
    const pers = new Personalization(filters, {});

    expect(pers.namedFilter("foo")).toEqual(filters[1]);
    expect(pers.namedFilter("FoO")).toEqual(filters[1]); // namedFilter() should be case-insensitive
  });

  it("namedFilter() returns the default filter when name isn't resolved", () => {
    const filters = [{name: "Default", state: [], type: "blacklist", pipelines: ["a", "b"]}, {name: "foo", type: "whitelist", pipelines: []}];
    const pers = new Personalization(filters, {});

    expect(pers.namedFilter("bar")).toEqual(filters[0]);
  });

  it("removeFilter() removes filter by name only if persistence to backend is successful", () => {
    const filters = [{name: "Default", type: "blacklist", pipelines: ["a", "b"]}, {name: "foo", type: "whitelist", pipelines: []}];
    let pers = new Personalization(filters, {});
    expect(pers.names()).toEqual(["Default", "foo"]); // baseline for existence

    const payload = JSON.stringify({filters: [filters[0]]});

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), payload, 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          204
      });

      const success = jasmine.createSpy();
      const failure = jasmine.createSpy();
      pers.removeFilter("foo").then(success, failure);

      expect(success).toHaveBeenCalled();
      expect(failure).toHaveBeenCalledTimes(0);
      expect(pers.names()).toEqual(["Default"]);
    });

    pers = new Personalization(filters, {});

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), payload, 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          400
      });

      const success = jasmine.createSpy();
      const failure = jasmine.createSpy();
      pers.removeFilter("foo").then(success, failure);

      expect(success).toHaveBeenCalledTimes(0);
      expect(failure).toHaveBeenCalled();
      expect(pers.names()).toEqual(["Default", "foo"]);
    });
  });

  it("addOrReplaceFilter() replaces filter (maintaining order) only if persistence to backend is successful", () => {
    const filters = [
      {name: "Default", type: "blacklist", pipelines: []},
      {name: "one", type: "blacklist", pipelines: []},
      {name: "two", type: "blacklist", pipelines: []}
    ];

    const newFilter = {name: "three", type: "whitelist", pipelines: []};

    let pers = new Personalization(filters, {});
    expect(pers.names()).toEqual(["Default", "one", "two"]); // baseline for existence

    const payload = JSON.stringify({filters: [filters[0], newFilter, filters[2]]});

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), payload, 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          204
      });

      const success = jasmine.createSpy();
      const failure = jasmine.createSpy();
      pers.addOrReplaceFilter("one", newFilter).then(success, failure);

      expect(success).toHaveBeenCalled();
      expect(failure).toHaveBeenCalledTimes(0);
      expect(pers.names()).toEqual(["Default", "three", "two"]);
    });

    pers = new Personalization(filters, {});

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), payload, 'PUT').andReturn({
        responseHeaders: {
          ETag:           'etag',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          400
      });

      const success = jasmine.createSpy();
      const failure = jasmine.createSpy();
      pers.addOrReplaceFilter("one", newFilter).then(success, failure);

      expect(success).toHaveBeenCalledTimes(0);
      expect(failure).toHaveBeenCalled();
      expect(pers.names()).toEqual(["Default", "one", "two"]);
    });
  });

  it("addOrReplaceFilter() falls back to append when existing filter is not found", () => {
    const filters = [
      {name: "Default", type: "blacklist", pipelines: []},
      {name: "one", type: "blacklist", pipelines: []},
      {name: "two", type: "blacklist", pipelines: []}
    ];

    const newFilter = {name: "three", type: "whitelist", pipelines: []};
    const pers = new Personalization(filters, {});

    let updated = false;

    pers.updateFilters = (f) => {
      updated = true;
      expect(f.names()).toEqual(["Default", "one", "two", "three"]);
    };

    pers.addOrReplaceFilter("wha?", newFilter);
    expect(updated).toBe(true);
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
  ]
};

const pipelineSelectionPostData = [
  {
    "name": "Default",
    "pipelines": [
      "up42",
      "test"
    ],
    "type": "blacklist"
  }
];

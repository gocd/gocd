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
import {ApiResult, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {NotificationEvent, NotificationFilter, NotificationFilterJSON, NotificationFilters} from "../notification_filters";
import {NotificationFiltersCRUD} from "../notification_filters_crud";

describe('NotificationFilterCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all notification filter", (done) => {
    const url = SparkRoutes.notificationFilterAPIPath();
    jasmine.Ajax.stubRequest(url).andReturn(filtersResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const filters      = (responseJSON.body as NotificationFilters);

      expect(filters[0].id()).toBe(1);
      expect(filters[0].pipeline()).toBe('up42');
      expect(filters[0].stage()).toBe('[Any Stage]');
      expect(filters[0].event()).toBe(NotificationEvent.All.toString());
      expect(filters[0].matchCommits()).toBeFalse();
      done();
    });

    NotificationFiltersCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should get the specified notification filter', (done) => {
    const url = SparkRoutes.notificationFilterAPIPath(1);
    jasmine.Ajax.stubRequest(url).andReturn(filterResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const filter       = (responseJSON.body as ObjectWithEtag<NotificationFilter>);

      expect(filter.object.id()).toBe(1);
      expect(filter.object.pipeline()).toBe('up42');
      expect(filter.object.stage()).toBe('[Any Stage]');
      expect(filter.object.event()).toBe(NotificationEvent.All.toString());
      expect(filter.object.matchCommits()).toBeFalse();
      done();
    });

    NotificationFiltersCRUD.get(1).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should create a new notification filter", () => {
    const url = SparkRoutes.notificationFilterAPIPath();
    jasmine.Ajax.stubRequest(url).andReturn(filterResponse());

    const filter1 = NotificationFilter.fromJSON(filter());
    NotificationFiltersCRUD.create(filter1);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(filter1.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a notification filter", () => {
    const url = SparkRoutes.notificationFilterAPIPath(1);
    jasmine.Ajax.stubRequest(url).andReturn(filterResponse());

    const filter1 = NotificationFilter.fromJSON(filter());
    NotificationFiltersCRUD.update(filter1, "old-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("PATCH");
    expect(request.data()).toEqual(toJSON(filter1.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should delete a notification filter", () => {
    const url = SparkRoutes.notificationFilterAPIPath(1);
    jasmine.Ajax.stubRequest(url).andReturn(deleteFilterResponse());

    NotificationFiltersCRUD.delete(1);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

  function toJSON(object: any) {
    return JSON.parse(JSON.stringify(object));
  }

  function filter() {
    return {
      id:            1,
      pipeline:      "up42",
      stage:         "[Any Stage]",
      event:         NotificationEvent.All,
      match_commits: false
    } as NotificationFilterJSON;
  }

  function filtersResponse() {
    const filtersJSON = {
      _embedded: {
        filters: [filter()]
      }
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(filtersJSON)
    };
  }

  function filterResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(filter())
    };
  }

  function deleteFilterResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8"
      },
      responseText:    JSON.stringify({message: "The notification filter was successfully deleted."})
    };
  }
});

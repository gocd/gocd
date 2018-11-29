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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";

describe("ApiResult", () => {

  const contentType = "application/vnd.go.cd.v1+json";

  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
  });

  it("should create ApiResult from a successful request", (done) => {
    mockSuccessfulRequest();
    ApiRequestBuilder.GET("/foo", ApiVersion.v1).then((result) => {
      // @ts-ignore
      expect(result.unwrap().body).toEqual(JSON.stringify({foo: "bar"}));
      expect(result.getEtag()).toEqual("etag-value");
      expect(result.getStatusCode()).toEqual(200);
      done();
    }, () => {
      done.fail("should have passed");
    });
  });

  it("should create ApiResult from a successful POST request", (done) => {
    mockSuccessfulCreatedRequest();
    ApiRequestBuilder.POST("/foo", ApiVersion.v1).then((result) => {
      // @ts-ignore
      expect(result.unwrap().body).toEqual(JSON.stringify({foo: "bar"}));
      expect(result.getEtag()).toEqual("etag-value");
      expect(result.getStatusCode()).toEqual(201);
      done();
    }, () => {
      done.fail("should have passed");
    });
  });

  it("should create ApiResult for request failing validation", (done) => {
    mockValidationFailedRequest();
    ApiRequestBuilder.PUT("/foo", ApiVersion.v1).then((result) => {
      // @ts-ignore
      expect(result.unwrap().message).toEqual("validation failed");
      expect(result.getEtag()).toBeNull();
      expect(result.getStatusCode()).toEqual(422);
      done();
    }, () => {
      done.fail("should have passed");
    });
  });

  it("should create ApiResult for request failing with an internal server error", (done) => {
    mockInternalServerError();
    ApiRequestBuilder.GET("/foo", ApiVersion.v1).then((result) => {
      // @ts-ignore
      expect(result.unwrap().message).toEqual("There was an unknown error performing the operation.");
      expect(result.getEtag()).toBeNull();
      expect(result.getStatusCode()).toEqual(500);
      done();
    }, () => {
      done.fail("should have passed");
    });
  });

  it("should map a successful response", (done) => {
    mockSuccessfulRequest();
    ApiRequestBuilder.GET("/foo", ApiVersion.v1)
      .then((result) => {
        return result.map((success) => JSON.parse(success));
      }, () => done.fail("should have passed"))
      .then((value) => {
        // @ts-ignore
        expect(value.unwrap().body.foo).toEqual("bar");
        done();
      }, () => done.fail("should have passed"));
  });

  it("should getOrThrow()", (done) => {
    mockSuccessfulRequest();
    ApiRequestBuilder.GET("/foo", ApiVersion.v1)
      .then((value) => {
        expect(value.getOrThrow()).toEqual(JSON.stringify({foo: "bar"}));
        done();
      }, () => done.fail("should have passed"));

    mockInternalServerError();
    ApiRequestBuilder.GET("/foo", ApiVersion.v1)
      .then((result) => result.getOrThrow(), () => done.fail("should have passed"))
      .then(() => done.fail("should have failed"), () => done());
  });

  function mockSuccessfulRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "GET").andReturn({
      responseText: JSON.stringify({foo: "bar"}),
      status: 200,
      responseHeaders: {
        "Content-Type": contentType,
        "Etag": "etag-value"
      }
    });
  }

  function mockSuccessfulCreatedRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "POST").andReturn({
      responseText: JSON.stringify({foo: "bar"}),
      status: 201,
      responseHeaders: {
        "Content-Type": contentType,
        "Etag": "etag-value"
      }
    });
  }

  function mockValidationFailedRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "PUT").andReturn({
      responseText: JSON.stringify({message: "validation failed"}),
      status: 422,
      responseHeaders: {
        "Content-Type": contentType,
      }
    });
  }

  function mockInternalServerError() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "GET").andReturn({
      responseText: "Message from nginx",
      status: 500,
      responseHeaders: {
        "Content-Type": "text/html",
      }
    });
  }
});

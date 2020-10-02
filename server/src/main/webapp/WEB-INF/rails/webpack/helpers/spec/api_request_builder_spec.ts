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
import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";

describe("Api Request Builder", () => {

  const contentType = "application/vnd.go.cd.v1+json";

  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
  });

  describe("ApiResult", () => {

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

    it("should create ApiResult from a successful POST request with 202 response", (done) => {
      mockSuccessfulCreateRequestWithAcceptedResponse();
      ApiRequestBuilder.POST("/foo", ApiVersion.v1).then((result) => {
        // @ts-ignore
        expect(result.getRedirectUrl()).toEqual("/go/admin/foo");
        expect(result.getRetryAfterIntervalInMillis()).toEqual(10000);
        expect(result.getStatusCode()).toEqual(202);
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

    it("should create ApiResult for request server unavailable response", (done) => {
      mockServerUnavailableRequest();
      ApiRequestBuilder.PUT("/foo", ApiVersion.v1).then((result) => {
        // @ts-ignore
        expect(result.unwrap().message).toEqual("server is in maintenance mode, please try later.");
        expect(result.getEtag()).toBeNull();
        expect(result.getStatusCode()).toEqual(503);
        done();
      }, () => {
        done.fail("should have passed");
      });
    });

    it("should create ApiResult for request failing with an internal server error", (done) => {
      mockInternalServerError();
      ApiRequestBuilder.GET("/foo", ApiVersion.v1).then((result) => {
        // @ts-ignore
        expect(result.unwrap()).toEqual("Message from nginx");
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
  });

  describe("Headers", () => {
    it("should pass headers to request", () => {
      mockSuccessfulRequest();
      ApiRequestBuilder.GET("/foo", ApiVersion.v1, {headers: {foo: "bar"}});
      const request = jasmine.Ajax.requests.mostRecent();
      expect(Object.keys(request.requestHeaders)).toHaveLength(3);
      expect(request.requestHeaders.Accept).toBe("application/vnd.go.cd.v1+json");
      expect(request.requestHeaders.foo).toBe("bar");
      expect(request.requestHeaders["X-Requested-With"]).toBe("XMLHttpRequest");
    });

    it("should pass if-none-match Header", () => {
        mockSuccessfulRequest();
        ApiRequestBuilder.GET("/foo", ApiVersion.v1, {etag: "some-hash"});
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.requestHeaders.Accept).toBe("application/vnd.go.cd.v1+json");
        expect(request.requestHeaders["If-None-Match"]).toBe("some-hash");
    });

    it("should pass if-match Header for update requests", () => {
      for (const method of ["PUT", "POST", "DELETE", "PATCH"]) {
        mockSuccessfulRequest(method, {foo: "bar"});
        // @ts-ignore
        ApiRequestBuilder[method]("/foo", ApiVersion.v1, {etag: "some-hash", payload: {foo: "bar"}});
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.requestHeaders.Accept).toBe("application/vnd.go.cd.v1+json");
        expect(request.requestHeaders["If-Match"]).toBe("some-hash");
      }
    });

    it("should pass X-GoCD-Confirm Header for an update request when payload is absent", () => {
      for (const method of ["PUT", "POST", "DELETE", "PATCH"]) {
        mockSuccessfulRequest(method, {foo: "bar"});
        // @ts-ignore
        ApiRequestBuilder[method]("/foo", ApiVersion.v1, {etag: "some-hash"});
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.requestHeaders.Accept).toBe("application/vnd.go.cd.v1+json");
        expect(request.requestHeaders["X-GoCD-Confirm"]).toBe("true");
      }
    });
  });

  function mockSuccessfulRequest(method?: string, payload?: any) {
    const _method = method || "GET";
    return jasmine.Ajax.stubRequest("/foo", payload ? JSON.stringify(payload) : undefined, _method)
                  .andReturn({
                               responseText: JSON.stringify({foo: "bar"}),
                               status: 200,
                               responseHeaders: {
                                 "Content-Type": contentType,
                                 "Etag": "etag-value"
                               }
                             });
  }

  function mockSuccessfulCreatedRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "POST")
                  .andReturn({
                               responseText: JSON.stringify({foo: "bar"}),
                               status: 201,
                               responseHeaders: {
                                 "Content-Type": contentType,
                                 "Etag": "etag-value"
                               }
                             });
  }

  function mockSuccessfulCreateRequestWithAcceptedResponse() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "POST")
                  .andReturn({
                               status: 202,
                               responseHeaders: {
                                 "Content-Type": contentType,
                                 "Location": "/go/admin/foo",
                                 "Retry-after": "10"
                               }
                             });
  }

  function mockValidationFailedRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "PUT")
                  .andReturn({
                               responseText: JSON.stringify({message: "validation failed"}),
                               status: 422,
                               responseHeaders: {
                                 "Content-Type": contentType,
                               }
                             });
  }

  function mockServerUnavailableRequest() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "PUT")
                  .andReturn({
                               responseText: JSON.stringify({message: "server is in maintenance mode, please try later."}),
                               status: 503,
                               responseHeaders: {
                                 "Content-Type": contentType,
                               }
                             });
  }

  function mockInternalServerError() {
    return jasmine.Ajax.stubRequest("/foo", undefined, "GET")
                  .andReturn({
                               responseText: "Message from nginx",
                               status: 500,
                               responseHeaders: {
                                 "Content-Type": "text/html",
                               }
                             });
  }
});

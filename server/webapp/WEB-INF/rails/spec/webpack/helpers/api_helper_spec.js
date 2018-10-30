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

import ApiHelper from "helpers/api_helper";

describe("ApiHelper", () => {
  it("should parse ETag from response", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/hello", undefined, "GET").andReturn({
        responseText:    JSON.stringify({
          data: {a: 1, b: 2}
        }),
        responseHeaders: {
          ETag:           `W/"05548388f7ef5042cd39f7fe42e85735--gzip"`,
          "Content-Type": "application/vnd.go.cd.v1+json"
        },
        status:          200
      });

      ApiHelper.GET({
        url: "/hello",
        apiVersion: "v1",
        etag: "initial"
      }).then((data, etag) => {
        expect(data).toEqual({ data: { a: 1, b: 2 }});
        expect(etag).toEqual(`W/"05548388f7ef5042cd39f7fe42e85735"`);
        done();
      }, () => done.fail("response should be successful"));
    });
  });

  it("should parse error on failure", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/hello", undefined, "GET").andReturn({
        responseHeaders: { "Content-Type": "application/vnd.go.cd.v1+json" },
        status:          500,
        responseText:    JSON.stringify({message: "Rejected"})
      });

      ApiHelper.GET({
        url: "/hello",
        apiVersion: "v1",
        etag: "initial"
      }).then(() => done.fail("request should not succeed"), (msg) => {
        expect(msg).toBe("Rejected");
        done();
      });
    });
  });
});

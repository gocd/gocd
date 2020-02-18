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

import {Frame} from "models/shared/analytics_frame";

describe("AnalyticsFrame", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should create frame with uid", () => {
    const frame = new Frame("some-uid");

    expect(frame.uid).toBe("some-uid");
  });

  describe("load", () => {
    it("should call before and after function", () => {
      const TEST_URL = "http://test.go.cd/foo",
            frame    = new Frame("some-uid");
      jasmine.Ajax.stubRequest(TEST_URL).andReturn({});

      frame.url(TEST_URL);

      let beforeCalled: boolean = false;
      let afterCalled: boolean  = false;

      const afterFn = () => {
        if (!beforeCalled) {
          throw Error("Should call before first");
        }
        afterCalled = true;
      };

      const beforeFn = () => {
        if (afterCalled) {
          throw Error("Should call before first");
        }
        beforeCalled = true;
      };

      frame.load(beforeFn, afterFn);

      expect(afterCalled).toBeTruthy();
      expect(beforeCalled).toBeTruthy();

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(TEST_URL);
      expect(request.method).toEqual("GET");
    });

    it("should set data and view on successful load", () => {
      const TEST_URL        = "http://test.go.cd/foo",
            frame           = new Frame("some-uid"),
            beforeFn        = jasmine.createSpy("beforeFn"),
            afterFn         = jasmine.createSpy("afterFn"),
            successResponse = {data: {key: "data-from-server"}, view_path: "/go/some-path-to-analytics-plugin"};
      jasmine.Ajax.stubRequest(TEST_URL, undefined, "GET").andReturn({
                                                                       responseText: JSON.stringify(successResponse),
                                                                       status: 200
                                                                     });

      frame.url(TEST_URL);

      frame.load(beforeFn, afterFn);

      expect(frame.data()).toEqual({key: "data-from-server"});
      expect(frame.view()).toEqual("/go/some-path-to-analytics-plugin?~n.o.n.c.e=0");
      expect(frame.errors()).toBeNull();
    });
    it("should set errors on failure", () => {
      const TEST_URL = "http://test.go.cd/foo",
            frame    = new Frame("some-uid"),
            beforeFn = jasmine.createSpy("beforeFn"),
            afterFn  = jasmine.createSpy("afterFn");
      jasmine.Ajax.stubRequest(TEST_URL, undefined, "GET").andReturn({
                                                                       responseText: JSON.stringify({message: "Boom!!"}),
                                                                       status: 400
                                                                     });

      frame.url(TEST_URL);

      frame.load(beforeFn, afterFn);

      expect(frame.data()).toBeUndefined();
      expect(frame.view()).toBeUndefined();
      expect(frame.errors()).not.toBeNull();
    });
  });

  describe("fetch", () => {
    it("should call handler with data on success", () => {
      const TEST_URL        = "http://test.go.cd/foo",
            frame           = new Frame("some-uid"),
            handlerFn       = jasmine.createSpy("handler"),
            successResponse = {data: {key: "data-from-server"}, view_path: "/go/some-path-to-analytics-plugin"};
      jasmine.Ajax.stubRequest(TEST_URL, undefined, "GET").andReturn({
                                                                       responseText: JSON.stringify(successResponse),
                                                                       status: 200
                                                                     });

      frame.url(TEST_URL);

      frame.fetch(frame.url(), handlerFn);

      expect(handlerFn).toHaveBeenCalledWith({key: "data-from-server"}, null);
    });
    it("should call handler with error on failure", () => {
      const TEST_URL  = "http://test.go.cd/foo",
            frame     = new Frame("some-uid"),
            handlerFn = jasmine.createSpy("handler");
      jasmine.Ajax.stubRequest(TEST_URL, undefined, "GET").andReturn({
                                                                       responseText: JSON.stringify({message: "Boom!!"}),
                                                                       status: 400
                                                                     });

      frame.url(TEST_URL);

      frame.fetch(frame.url(), handlerFn);

      expect(handlerFn).toHaveBeenCalledWith(null, frame.errors());
    });
  });

  describe("withParam", () => {
    it("should return url with param when param name and value are not null", () => {
      const TEST_URL = "http://test.go.cd/foo";
      const frame    = new Frame("some-uid");

      expect(frame.withParam(TEST_URL, "test", "bar")).toBe(`${TEST_URL}?test=bar`);
    });

    it("should return url as it is when param name is empty string", () => {
      const TEST_URL = "http://test.go.cd/foo";
      const frame    = new Frame("some-uid");

      expect(frame.withParam(TEST_URL, "", "bar")).toBe(TEST_URL);
    });

    it("should return url as it is when param name and value are empty string", () => {
      const TEST_URL = "http://test.go.cd/foo";
      const frame    = new Frame("some-uid");

      expect(frame.withParam(TEST_URL, "", "")).toBe(TEST_URL);
    });

    it("should return url with param name when param value is empty string", () => {
      const TEST_URL = "http://test.go.cd/foo";
      const frame    = new Frame("some-uid");

      expect(frame.withParam(TEST_URL, "some-param", "")).toBe(`${TEST_URL}?some-param`);
    });

    it("should append params if one already exist", () => {
      const TEST_URL = "http://test.go.cd/foo?foo=bar";
      const frame    = new Frame("some-uid");

      expect(frame.withParam(TEST_URL, "test", "foo")).toBe(`${TEST_URL}&test=foo`);
    });
  });
});

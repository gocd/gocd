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
import {Frame} from "models/shared/frame";

function FakeWindow() {
  const location = {search: ""};
  this.location = location;

  this.withQuery = function withQuery(query) {
    location.search = query;
  };
}

const TEST_URL = "http://test.tld/path";

describe("Frame", () => {

  describe("url()", () => {
    it("should only pass through ui=test query parameter", () => {

      const mockWindow = new FakeWindow();
      const f = new Frame(mockWindow);
      f.view(TEST_URL);

      mockWindow.withQuery("?ui");
      expect(f.view()).toEqual(TEST_URL);

      mockWindow.withQuery("?ui-test=true");
      expect(f.view()).toEqual(TEST_URL);

      mockWindow.withQuery("?ui=test");
      expect(f.view()).toEqual(`${TEST_URL}?ui=test`);

      mockWindow.withQuery("?foo=bar&ui=test");
      expect(f.view()).toEqual(`${TEST_URL}?ui=test`);

      mockWindow.withQuery("?baz&ui=test");
      expect(f.view()).toEqual(`${TEST_URL}?ui=test`);

      mockWindow.withQuery("?baz&ui=other");
      expect(f.view()).toEqual(TEST_URL);
    });
  });
});

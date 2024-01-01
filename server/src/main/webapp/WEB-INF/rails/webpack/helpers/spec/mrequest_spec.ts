/*
 * Copyright 2024 Thoughtworks, Inc.
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
import {mrequest} from "../mrequest";

describe("mrequest", () => {

  describe("normalizeEtag", () => {

    it("should normalize nullish tags to null", () => {
      expect(mrequest.normalizeEtag(undefined)).toBe(null);
      expect(mrequest.normalizeEtag(null)).toBe(null);
    });

    it("should normalize tags with quotes to ones without", () => {
      expect(mrequest.normalizeEtag('"hello"')).toBe("hello");
      expect(mrequest.normalizeEtag("hello")).toBe("hello");
    });

    it("should normalize weak etags to strong", () => {
      expect(mrequest.normalizeEtag('W/"hello"')).toBe("hello");
    });

    it("should normalize etags with Jetty compression suffixes", () => {
      expect(mrequest.normalizeEtag("hello--gzip")).toBe("hello");
      expect(mrequest.normalizeEtag("hello--deflate")).toBe("hello");
      expect(mrequest.normalizeEtag('"hello--gzip"')).toBe("hello");
      expect(mrequest.normalizeEtag('"hello--deflate"')).toBe("hello");
      expect(mrequest.normalizeEtag('W/"hello--gzip"')).toBe("hello");
      expect(mrequest.normalizeEtag('W/"hello--deflate"')).toBe("hello");
    });
  });
});

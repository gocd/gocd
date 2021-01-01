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

import { baseUrlProvider, currentUrlOriginAndPath } from "../base_url_provider";
import { SiteUrls } from "../server_configuration";

describe("basic_url_provider", () => {
  describe("baseUrlProvider()", () => {
    it("returns the configured site URL", () => {
      const fallback = jasmine.createSpy("fallback");
      const url = baseUrlProvider(new SiteUrls("http://my.gocd"), fallback);
      expect(url()).toBe("http://my.gocd/go");
      expect(fallback).not.toHaveBeenCalled();
    });

    it("returns the configured secure site URL", () => {
      const fallback = jasmine.createSpy("fallback");
      const url = baseUrlProvider(new SiteUrls(void 0, "https://my.secured.gocd"), fallback);
      expect(url()).toBe("https://my.secured.gocd/go");
      expect(fallback).not.toHaveBeenCalled();
    });

    it("favors the secure site URL over site URL when both are configured", () => {
      const fallback = jasmine.createSpy("fallback");
      const url = baseUrlProvider(new SiteUrls("http://my.gocd", "https://my.secured.gocd"), fallback);
      expect(url()).toBe("https://my.secured.gocd/go");
      expect(fallback).not.toHaveBeenCalled();
    });

    it("defaults to the fallback() when no site URLs are configured", () => {
      const url = baseUrlProvider(new SiteUrls(), () => "https://i.tried.my.best/go");
      expect(url()).toBe("https://i.tried.my.best/go");
    });
  });

  describe("currentUrlOriginAndPath()", () => {
    it("only extracts the origin and path of a URL or Location object", () => {
      const location = new URL("http://user:password@host.tld:8000/a/path/to/remember?a=1&b=2&b=3#fragment");

      expect(currentUrlOriginAndPath(location)).toBe("http://host.tld:8000/a/path/to/remember");
    });
  });
});

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

import { normalizePath } from "helpers/url";
import { cascading } from "helpers/utils";
import { SiteUrls } from "models/server-configuration/server_configuration";

// a subset of both Location and URL
interface LocationLike {
  readonly origin: string;
  readonly pathname: string;
}

export function baseUrlProvider(s: SiteUrls, fallback: () => string) {
  const g = (u?: string) => u ? currentUrlOriginAndPath(new URL(`${u}/go`)) : u;
  return cascading((s) => !!(s || "").trim().length,
    () => g(s.secureSiteUrl()),
    () => g(s.siteUrl()),
    fallback
  );
}

/**
 * A string provider that provides the current URL (i.e., window.location) stripped
 * of any hash fragments or query params; only the origin (protocol, host, port) and
 * the pathname components.
 *
 * The pathname is guaranteed to be normalized, with all relative segments resolved
 * and removed, no consecutive slashes, and no trailing slashes unless the path is
 * the root.
 *
 * @returns a normalized current URL origin and pathname
 */
export function currentUrlOriginAndPath({ origin, pathname }: LocationLike = window.location) {
  return new URL(normalizePath(pathname), origin).href;
}

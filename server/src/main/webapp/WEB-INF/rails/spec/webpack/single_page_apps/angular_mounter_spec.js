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

describe("Rails view plugin angular Mounter utility", () => {
  it("should mount angular", () => {
    expect(() => { require("../../../webpack/single_page_apps/angular_mounter"); }).not.toThrow();

    expect(() => { angular.noop(); }).not.toThrow();
    expect(angular.version.full).toEqual("1.0.8");
  });
});

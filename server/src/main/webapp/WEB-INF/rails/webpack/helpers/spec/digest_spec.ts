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

import {digest} from "../digest";

describe("digest()", () => {
  it("computes the hex digest of a given string input", (done) => {
    digest("SHA-256", "get down, boogie oogie oogie ~~").then((result) => {
      expect(result).toBe("bcfbcd83f1be7334b8f2337d82a5e896fde8812bb178994164036d0195ca0700");
      done();
    }).catch(done.fail);
  });
});

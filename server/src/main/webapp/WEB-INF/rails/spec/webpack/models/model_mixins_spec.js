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
import Stream from "mithril/stream";
import {Mixins} from "models/mixins/model_mixins";

describe("Model Mixins", () => {

  describe("TogglingGetterSetter", () => {
    let model;
    beforeEach(() => {
      model = Mixins.TogglingGetterSetter(Stream('foo'));
    });

    it("should set a value for the first time", () => {
      expect(model()).toBe('foo');
      expect(model()).toBe('foo');
    });

    it("should unset value when set twice", () => {
      expect(model('bar')).toBe('bar');
      expect(model('bar')).toBe(undefined);
      expect(model('bar')).toBe('bar');
    });
  });
});

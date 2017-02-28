/*
 * Copyright 2015 ThoughtWorks, Inc.
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
describe("Model Mixins", function () {

  var Stream = require('mithril/stream');
  var Mixin  = require('models/mixins/model_mixins');

  describe("TogglingGetterSetter", function () {
    var model;
    beforeEach(function () {
      model = Mixin.TogglingGetterSetter(Stream('foo'));
    });

    it("should set a value for the first time", function () {
      expect(model()).toBe('foo');
      expect(model()).toBe('foo');
    });

    it("should unset value when set twice", function () {
      expect(model('bar')).toBe('bar');
      expect(model('bar')).toBe(undefined);
      expect(model('bar')).toBe('bar');
    });
  });
});

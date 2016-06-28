/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus'], function (m, _, s) {

  var RunIfConditions = function (data) {
    this.data = m.prop(s.defaultToIfBlank(data, ['passed']));

    this.clear = function () {
      this.data([]);
    };

    this.push = function (val) {
      if (val === 'any') {
        this.clear();
      }

      if (_.includes(this.data(), 'any')) {
        this.pop('any');
      }

      this.data().push(val);
    };

    this.pop = function (val) {
      this.data().pop(val);
    };
  };

  RunIfConditions.create = function (data) {
    return new RunIfConditions(data);
  };

  return RunIfConditions;
});

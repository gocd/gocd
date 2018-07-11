/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("Duration.humanizeForGoCD", function () {

  it('should humanize a duration >= 24 hour', function () {
    expect(moment.duration(3 * 86400 + 2 * 3600 + 20 * 60 + 34 + 0.45, 's').humanizeForGoCD()).toEqual('3d 2:20:34.450');
  });

  it('should humanize a duration >= 1 hour and < 24 hour', function () {
    expect(moment.duration(2 * 3600 + 20 * 60 + 34 + 0.45, 's').humanizeForGoCD()).toEqual('2h 20m 34.450s');
  });

  it('should humanize a duration >= 1 minute and < 1 hour', function () {
    expect(moment.duration(20 * 60 + 34 + 0.45, 's').humanizeForGoCD()).toEqual('20m 34.450s');
  });

  it('should humanize a duration < 1 minute >= 1s', function () {
    expect(moment.duration(12.45, 's').humanizeForGoCD()).toEqual('12.450s');
  });

  it('should humanize a duration < 1 second', function () {
    expect(moment.duration(12.45, 'ms').humanizeForGoCD()).toEqual('0.12s');
  });
});

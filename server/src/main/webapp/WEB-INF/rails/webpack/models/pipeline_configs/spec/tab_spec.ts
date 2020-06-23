/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {Tab} from "../tab";

describe('TabSpec', () => {
  it('should throw error when name is specified but not path', () => {
    const tab = new Tab("name", "");

    expect(tab.isValid()).toBeFalse();
    expect(tab.errors().errorsForDisplay('path')).toBe('Path must be present.');
  });

  it('should not error out if name is not specified', () => {
    const tab = new Tab("", "");

    expect(tab.isValid()).toBeTrue();
    expect(tab.errors().count()).toBe(0);
  });
});

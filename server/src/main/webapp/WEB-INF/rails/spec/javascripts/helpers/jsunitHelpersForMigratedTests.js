/*
 * Copyright 2023 Thoughtworks, Inc.
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
function fail(failureMessage) {
  throw "Call to fail()" + failureMessage;
}

function failed_json(planName) {
  return construct_new_json(planName, "Waiting", "Failed");
}

function construct_new_json(projectname, current_status, result) {
  return {
    building_info: {
      name: projectname, build_completed_date: "1 day ago", current_status: current_status, result: result
    }
  };
}

function assertEquals() {
  let expected;
  let actual;
  if (arguments.length === 2) {
    expected = arguments[0];
    actual = arguments[1];
  } else {
    expected = arguments[1];
    actual = arguments[2];
  }
  expect(actual).toBe(expected);
}

function assertTrue() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).toBe(true);
}

function assert() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).toBe(true);
}

function assertFalse() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).toBe(false);
}

function assertNotNull() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).not.toBeNull();
}

function assertNull() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).toBeNull();
}

function assertUndefined() {
  let actual;
  if (arguments.length > 1) {
    actual = arguments[1];
  } else {
    actual = arguments[0];
  }
  expect(actual).toBe(undefined);
}

function assertContains() {
  let textToBeSearched;
  let mainText;

  if (arguments.length > 2) {
    textToBeSearched = arguments[1];
    mainText = arguments[2];
  } else {
    textToBeSearched = arguments[0];
    mainText = arguments[1];
  }
  expect(mainText).toContain(textToBeSearched);
}

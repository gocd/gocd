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
import {
  ServerHealthMessage,
  ServerHealthMessages
} from "../../../../../webpack/models/shared/server_health_messages/server_health_messages";

describe("ServerHealthMessages", () => {

  function createError(): ServerHealthMessage {
    return {
      level: 'ERROR',
      detail: 'foo',
      time: '2018-10-19T04:07:02Z',
      message: 'something went wrong!'
    };
  }

  function createWarning(): ServerHealthMessage {
    return {
      level: 'WARNING',
      detail: 'foo',
      time: '2018-10-19T04:07:02Z',
      message: 'something went wrong!'
    };
  }

  it('should return empty when there are no errors or warnings', () => {
    const messages = new ServerHealthMessages([]);
    expect(messages.summaryMessage()).toEqual("");
  });

  it("should return a summary message when there are both warnings and errors", () => {
    const messages = new ServerHealthMessages([createError(), createWarning()]);

    expect(messages.summaryMessage()).toEqual("1 error and 1 warning");
  });

  it("should pluralize errors and warnings", () => {
    const messages = new ServerHealthMessages([createError(), createWarning(), createError(), createWarning()]);

    expect(messages.summaryMessage()).toEqual("2 errors and 2 warnings");
  });

  it("should return a summary message when there only errors", () => {
    const messages = new ServerHealthMessages([createError(), createError()]);

    expect(messages.summaryMessage()).toEqual("2 errors");
  });

  it("should return a summary message when there only warnings", () => {
    const messages = new ServerHealthMessages([createWarning(), createWarning()]);

    expect(messages.summaryMessage()).toEqual("2 warnings");
  });

  it("should return true when there are messages", () => {
    const messages = new ServerHealthMessages([createWarning(), createWarning()]);

    expect(messages.hasMessages()).toBe(true);
  });

  it("should return false when there are no messages", () => {
    const messages = new ServerHealthMessages([]);

    expect(messages.hasMessages()).toBe(false);
  });
});

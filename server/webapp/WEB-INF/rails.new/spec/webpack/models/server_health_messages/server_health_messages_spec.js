/*
 * Copyright 2018 ThoughtWorks, Inc.
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


describe("ServerHealthMessages", () => {

  const ServerHealthMessages = require('models/server_health_messages/server_health_messages');

  it('should return empty when there are no errors or warnings', () => {
    const messages = new ServerHealthMessages([]);
    expect(messages.summaryMessage()).toEqual("");
  });

  it("should return a summary message when there are both warnings and errors", () => {
    const messages = new ServerHealthMessages([{"level": "ERROR"}, {"level": "WARNING"}]);

    expect(messages.summaryMessage()).toEqual("1 error and 1 warning");
  });

  it("should pluralize errors and warnings", () => {
    const messages = new ServerHealthMessages([{"level": "ERROR"}, {"level": "WARNING"}, {"level": "ERROR"}, {"level": "WARNING"}]);

    expect(messages.summaryMessage()).toEqual("2 errors and 2 warnings");
  });

  it("should return a summary message when there only errors", () => {
    const messages = new ServerHealthMessages([{"level": "ERROR"}, {"level": "ERROR"}]);

    expect(messages.summaryMessage()).toEqual("2 errors");
  });

  it("should return a summary message when there only warnings", () => {
    const messages = new ServerHealthMessages([{"level": "WARNING"}, {"level": "WARNING"}]);

    expect(messages.summaryMessage()).toEqual("2 warnings");
  });

  it("should return true when there are messages", () => {
    const messages = new ServerHealthMessages([{"level": "WARNING"}, {"level": "WARNING"}]);

    expect(messages.hasMessages()).toBe(true);
  });

  it("should return false when there are no messages", () => {
    const messages = new ServerHealthMessages([]);

    expect(messages.hasMessages()).toBe(false);
  });
});

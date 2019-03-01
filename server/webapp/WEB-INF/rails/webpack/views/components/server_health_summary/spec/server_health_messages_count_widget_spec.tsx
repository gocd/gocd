/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import * as $ from "jquery";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {ServerHealthMessages} from "models/shared/server_health_messages/server_health_messages";
import * as s from "underscore.string";
import {ModalManager} from "views/components/modal/modal_manager";
import {ServerHealthMessagesCountWidget} from "views/components/server_health_summary/server_health_messages_count_widget";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";

describe("ServerHealthMessagesCountWidget", () => {
  const TimeFormatter = require("helpers/time_formatter");

  const jsonData             = [
    {
      message: "Command Repository",
      detail: "Unable to upgrade command repository located at server/db/command_repository/default. Message: Could not find default command snippets zip on classpath.",
      level: "ERROR",
      time: "2018-01-30T07:34:43Z"
    },
    {
      message: "Modification check failed for material: URL: tesasdfdt-repo, Branch: master",
      detail: "Failed to run git clone command STDERR: fatal: repository 'tesasdfdt-repo' does not exist",
      level: "WARNING",
      time: "2018-01-30T07:40:47Z"
    }
  ];
  const serverHealthMessages = new ServerHealthMessages(jsonData);

  const helper = new TestHelper();

  afterEach(() => {
    ModalManager.closeAll();
    helper.unmount();
  });

  it("should render the count of errors and warnings", () => {
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={stream(serverHealthMessages)}/>);

    expect(helper.find("a")).toContainText("1 error and 1 warning");
  });

  it("should render the list of messages in modal on click", () => {
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={stream(serverHealthMessages)}/>);

    helper.click('a');

    expect($(`.component-modal-container [data-test-id='server-health-message-for-${s.slugify(jsonData[0].message)}'] [data-test-class='server-health-message_message']:first`))
      .toContainText(jsonData[0].message);
    expect($(`.component-modal-container [data-test-id='server-health-message-for-${s.slugify(jsonData[0].message)}'] [data-test-class='server-health-message_detail']:first`))
      .toContainText(jsonData[0].detail);
    expect($(`.component-modal-container [data-test-id='server-health-message-for-${s.slugify(jsonData[0].message)}'] [data-test-class='server-health-message_timestamp']:first`))
      .toContainText(TimeFormatter.format(jsonData[0].time));
  });

  it("should trust html messages in modal", () => {
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={stream(new ServerHealthMessages([{
      message: "Test Message",
      detail: `This is a <a href="http://example.com">link</a>`,
      level: "ERROR",
      time: "2018-01-30T07:34:43Z"
    }]))}
    />);

    helper.click('a');

    expect($(".component-modal-container [data-test-id='server-health-message-for-test-message'] [data-test-class='server-health-message_message']"))
      .toContainText("Test Message");
    expect($(".component-modal-container [data-test-id='server-health-message-for-test-message'] [data-test-class='server-health-message_detail']"))
      .toContainHtml(`This is a <a href="http://example.com">link</a>`);
    expect($($(".component-modal-container [data-test-id='server-health-message-for-test-message'] [data-test-class='server-health-message_timestamp']")))
      .toContainText(TimeFormatter.format("2018-01-30T07:34:43Z"));
  });

});

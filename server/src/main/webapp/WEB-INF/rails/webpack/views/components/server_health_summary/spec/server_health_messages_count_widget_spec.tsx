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

import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import Stream from "mithril/stream";
import {ServerHealthMessages} from "models/shared/server_health_messages/server_health_messages";
import s from "underscore.string";
import {ModalManager} from "views/components/modal/modal_manager";
import {ServerHealthMessagesCountWidget} from "views/components/server_health_summary/server_health_messages_count_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ServerHealthMessagesCountWidget", () => {

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
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={Stream(serverHealthMessages)}/>);

    expect(helper.byTestId("server-health-messages-count")).toContainText("1 error and 1 warning");
  });

  it("should render the list of messages in modal on click", () => {
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={Stream(serverHealthMessages)}/>);

    helper.clickByTestId("server-health-messages-count");

    const container = document.querySelector(".component-modal-container")!;
    const msgEl = helper.byTestId(`server-health-message-for-${s.slugify(jsonData[0].message)}`, container);

    expect(helper.q("[data-test-class='server-health-message_message']", msgEl)).toContainText(jsonData[0].message);
    expect(helper.q("[data-test-class='server-health-message_detail']", msgEl)).toContainText(jsonData[0].detail);
    expect(helper.q("[data-test-class='server-health-message_timestamp']", msgEl)).toContainText(timeFormatter.format(jsonData[0].time));
  });

  it("should trust html messages in modal", () => {
    helper.mount(() => <ServerHealthMessagesCountWidget serverHealthMessages={Stream(new ServerHealthMessages([{
      message: "Test Message",
      detail: `This is a <a href="http://example.com">link</a>`,
      level: "ERROR",
      time: "2018-01-30T07:34:43Z"
    }]))}
    />);

    helper.clickByTestId("server-health-messages-count");

    const container = document.querySelector(".component-modal-container")!;
    const msgEl = helper.byTestId("server-health-message-for-test-message", container);

    expect(helper.q("[data-test-class='server-health-message_message']", msgEl)).toContainText("Test Message");
    expect(helper.q("[data-test-class='server-health-message_detail']", msgEl)).toContainHtml(`This is a <a href="http://example.com">link</a>`);
    expect(helper.q("[data-test-class='server-health-message_timestamp']", msgEl)).toContainText(timeFormatter.format("2018-01-30T07:34:43Z"));
  });

});

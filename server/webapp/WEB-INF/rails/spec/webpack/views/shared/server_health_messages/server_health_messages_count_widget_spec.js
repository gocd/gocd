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

describe("ServerHealthMessagesCountWidget", () => {
  const ServerHealthMessages            = require('models/shared/server_health_messages/server_health_messages').ServerHealthMessages;
  const ServerHealthMessagesCountWidget = require('views/shared/server_health_messages/server_health_messages_count_widget');
  const TimeFormatter = require('helpers/time_formatter');

  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');

  let $root, root;

  const jsonData                 = [
    {
      "message": "Command Repository",
      "detail":  "Unable to upgrade command repository located at server/db/command_repository/default. Message: Could not find default command snippets zip on classpath.",
      "level":   "ERROR",
      "time":    "2018-01-30T07:34:43Z"
    },
    {
      "message": "Modification check failed for material: URL: tesasdfdt-repo, Branch: master",
      "detail":  "Failed to run git clone command STDERR: fatal: repository 'tesasdfdt-repo' does not exist",
      "level":   "WARNING",
      "time":    "2018-01-30T07:40:47Z"
    }
  ];
  const serverHealthMessages = new ServerHealthMessages(jsonData);

  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(window.destroyDomElementForTest);

  afterEach(() => {
    m.mount(root, null);
    m.redraw();

    expect($('.new-modal-container .reveal')).not.toExist('Did you forget to close the modal before the test?');
  });

  it('should render the count of errors and warnings', () => {
    m.mount(root, {
      view() {
        return m(ServerHealthMessagesCountWidget, {
          serverHealthMessages: Stream(serverHealthMessages)
        });
      }
    });
    m.redraw(true);

    expect($root.find('a')).toContainText('1 error and 1 warning');
  });


  it('should render the list of messages in modal on click', () => {
    m.mount(root, {
      view() {
        return m(ServerHealthMessagesCountWidget, {
          serverHealthMessages: Stream(serverHealthMessages)
        });
      }
    });
    m.redraw(true);
    simulateEvent.simulate($root.find('a').get(0), 'click');
    m.redraw(true);

    expect($('.new-modal-container .server-health-status .message:first')).toContainText(jsonData[0].message);
    expect($('.new-modal-container .server-health-status .detail:first')).toContainText(jsonData[0].detail);
    expect($('.new-modal-container .server-health-status .timestamp:first')).toContainText(TimeFormatter.format(jsonData[0].time));

    simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .close').get(0), 'click');
  });

  it('should trust html messages in modal', () => {
    m.mount(root, {
      view() {
        return m(ServerHealthMessagesCountWidget, {
          serverHealthMessages: Stream(new ServerHealthMessages([{
            "message": "Test Message",
            "detail":  `This is a <a href='http://example.com'>link</a>`,
            "level":   "ERROR",
            "time":    "2018-01-30T07:34:43Z"
          }]))
        });
      }
    });
    m.redraw(true);
    simulateEvent.simulate($root.find('a').get(0), 'click');
    m.redraw(true);

    expect($('.new-modal-container .server-health-status .message:first')).toContainText("Test Message");
    expect($('.new-modal-container .server-health-status .detail:first')).toContainHtml(`This is a <a href='http://example.com'>link</a>`);
    expect($('.new-modal-container .server-health-status .timestamp:first')).toContainText(TimeFormatter.format("2018-01-30T07:34:43Z"));

    simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .modal-buttons .close').get(0), 'click');
  });

});

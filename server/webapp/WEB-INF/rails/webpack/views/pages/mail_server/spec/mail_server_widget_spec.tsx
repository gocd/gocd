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

import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {MailServer} from "models/mail_server/types";
import {MailServerWidget} from "views/pages/mail_server/mail_server_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MailServerWidget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should show form", () => {
    mount(new MailServer());

    expect(helper.findByDataTestId("form-field-input-smtp-hostname")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-smtp-port")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-use-smtps")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-smtp-username")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-smtp-password")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-send-email-using-address")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-administrator-email")).toBeInDOM();
  });

  function mount(mailServer: MailServer) {
    helper.mount(() => <MailServerWidget mailserver={stream(mailServer)} onsave={_.noop} operationState={stream()}/>);
  }

});

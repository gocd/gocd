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

import m from "mithril";
import Stream from "mithril/stream";
import {MailServer} from "models/server-configuration/server_configuration";
import {MailServerVM} from "models/server-configuration/server_configuration_vm";
import {FlashMessageModel} from "views/components/flash_message";
import {MailServerManagementWidget} from "views/pages/server-configuration/mail_server_management_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MailServerManagementWidget", () => {
  const helper           = new TestHelper();
  const onDeleteSpy      = jasmine.createSpy("onDelete");
  const onSaveSpy        = jasmine.createSpy("onSave");
  const onCancelSpy      = jasmine.createSpy("onCancel");
  const onTestMailSpy    = jasmine.createSpy("onTestMail");
  let mailServerVM: MailServerVM;
  const testMailResponse = Stream(new FlashMessageModel());

  afterEach(helper.unmount.bind(helper));

  it("should show form", () => {
    mount(new MailServer());

    expect(helper.byTestId("form-field-input-smtp-hostname")).toBeInDOM();
    expect(helper.byTestId("form-field-input-smtp-port")).toBeInDOM();
    expect(helper.byTestId("form-field-input-use-smtps")).toBeInDOM();
    expect(helper.byTestId("form-field-input-smtp-username")).toBeInDOM();
    expect(helper.byTestId("form-field-input-smtp-password")).toBeInDOM();
    expect(helper.byTestId("form-field-input-send-email-using-address")).toBeInDOM();
    expect(helper.byTestId("form-field-input-administrator-email")).toBeInDOM();
  });

  it("should show cancel and save buttons", () => {
    mount(new MailServer());

    expect(helper.byTestId("cancel")).toBeInDOM();
    expect(helper.byTestId("save")).toBeInDOM();
    expect(helper.byTestId("cancel")).toHaveText("Cancel");
    expect(helper.byTestId("save")).toHaveText("Save");
  });

  describe("Save", () => {
    it("should have save button", () => {
      mount(new MailServer());
      expect(helper.byTestId("save")).toBeInDOM();
    });

    it("should call onSave", () => {
      mount(new MailServer());
      helper.click(helper.byTestId("save"));
      expect(onSaveSpy).toHaveBeenCalled();
    });
  });

  describe("Cancel", () => {
    it("should render cancel button", () => {
      mount(new MailServer());
      expect(helper.byTestId("cancel")).toHaveText("Cancel");
    });

    it("should call onCancel", () => {
      mount(new MailServer());
      helper.clickByTestId("cancel");
      expect(onCancelSpy).toHaveBeenCalledWith(mailServerVM);
    });
  });

  it("should call onDelete", () => {
    mount(new MailServer());
    helper.oninput(helper.byTestId("form-field-input-smtp-hostname"), "foobar");
    helper.clickByTestId("Delete");
    expect(helper.byTestId("Delete")).not.toBeDisabled();
    expect(onDeleteSpy).toHaveBeenCalled();
  });

  it("should disable onDelete", () => {
    mount(new MailServer(), false);
    expect(helper.byTestId("Delete")).toBeDisabled();
  });

  describe('TestMail', () => {
    it('should show the send test mail button', () => {
      mount(new MailServer());

      expect(helper.byTestId("send-test-email")).toBeInDOM();
    });

    it('should call the send test mail method', () => {
      mount(new MailServer());

      helper.click(helper.byTestId("send-test-email"));

      expect(onTestMailSpy).toHaveBeenCalled();
    });
  });

  function mount(mailServer: MailServer, canDeleteMailServer: boolean = true) {
    const savePromise: Promise<MailServer>                           = new Promise((resolve) => {
      onSaveSpy();
      resolve();
    });
    const testMailPromise: (mailServer: MailServer) => Promise<void> = () => new Promise((resolve) => {
      onTestMailSpy();
      resolve();
    });

    mailServerVM = new MailServerVM();
    mailServerVM.sync(mailServer);
    mailServerVM.canDeleteMailServer(canDeleteMailServer);
    helper.mount(() => <MailServerManagementWidget mailServerVM={Stream(mailServerVM)}
                                                   onMailServerManagementSave={() => savePromise}
                                                   onMailServerManagementDelete={onDeleteSpy}
                                                   sendTestMail={testMailPromise}
                                                   testMailResponse={testMailResponse}
                                                   onCancel={onCancelSpy}/>);
  }
});

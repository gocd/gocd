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

import m from "mithril";
import Stream from "mithril/stream";
import {CurrentUser} from "models/new_preferences/current_user";
import {CurrentUserVM} from "models/new_preferences/preferences";
import {TestHelper} from "views/pages/spec/test_helper";
import {EmailSettingsWidget} from "../email_settings_widget";

describe('EmailSettingsWidgetSpec', () => {
  const helper      = new TestHelper();
  const onCancelSpy = jasmine.createSpy('onCancel');
  const onSaveSpy   = jasmine.createSpy('onSave');
  let user: CurrentUserVM;

  beforeEach(() => {
    user = new CurrentUserVM(CurrentUser.fromJSON(userJSON()));
  });
  afterEach((done) => helper.unmount(done));

  it('should render input fields', () => {
    mount();

    expect(helper.byTestId('form-field-input-email')).toBeInDOM();
    expect(helper.byTestId('form-field-label-enable-email-notification')).toBeInDOM();
    expect(helper.byTestId('form-field-input-my-check-in-aliases')).toBeInDOM();

    expect(helper.byTestId('cancel')).toBeInDOM();
    expect(helper.byTestId('cancel')).toBeDisabled();
    expect(helper.byTestId('save-email-settings')).toBeInDOM();
    expect(helper.byTestId('save-email-settings')).toBeDisabled();

    expect(helpText('form-field-input-email')).toContainText('The email to which the notification is send.');
    expect(helpText('form-field-input-my-check-in-aliases')).toContainText("Usually the commits will be either in 'user' or 'username'. Specify both the values here.");
  });

  it('should call the cancel callback on button click', () => {
    mount();

    helper.oninput(helper.byTestId('form-field-input-email'), 'new-email');

    helper.clickByTestId('cancel');
    expect(onCancelSpy).toHaveBeenCalled();
  });

  function mount() {
    helper.mount(() => <EmailSettingsWidget currentUserVM={Stream(user)}
                                            onCancel={onCancelSpy}
                                            onSaveEmailSettings={onSaveSpy}/>);
  }

  function helpText(dataTestId: string) {
    return helper.q(`[data-test-id='${dataTestId}'] + span[id*='-help-text']`);
  }
});

export function userJSON() {
  return {
    login_name:      "view",
    display_name:    "view",
    enabled:         true,
    email:           "",
    email_me:        false,
    checkin_aliases: [],
  };
}

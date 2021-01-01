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

import Stream from "mithril/stream";
import {BackupConfig} from "models/backup_config/types";
import {BackupConfigModal} from "views/pages/backup/backup_config_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("BackupConfigModal", () => {
  const helper = new TestHelper();

  let backupConfig: Stream<BackupConfig>;
  let showSpinner: Stream<boolean>;
  let errorMessage: Stream<string>;
  let isSaving: Stream<boolean>;
  let onSave: (modal: BackupConfigModal) => void;
  let modal: BackupConfigModal;

  beforeEach(() => {
    backupConfig = Stream(new BackupConfig());
    showSpinner  = Stream(false) as Stream<boolean>;
    errorMessage = Stream();
    isSaving     = Stream(false) as Stream<boolean>;
    onSave       = jasmine.createSpy("callback");
    modal        = new BackupConfigModal(backupConfig, showSpinner, errorMessage, isSaving, onSave);
    helper.mount(modal.body.bind(modal));
  });
  afterEach(helper.unmount.bind(helper));

  it("should render form", () => {
    expect(modal.title()).toBe("Configure backup settings");
    expect(helper.byTestId("form-field-input-backup-schedule")).toBeInDOM();
    expect(helper.byTestId("form-field-input-post-backup-script")).toBeInDOM();
    expect(helper.byTestId("form-field-input-send-email-on-backup-failure")).toBeInDOM();
    expect(helper.byTestId("form-field-input-send-email-on-backup-success")).toBeInDOM();
  });
});

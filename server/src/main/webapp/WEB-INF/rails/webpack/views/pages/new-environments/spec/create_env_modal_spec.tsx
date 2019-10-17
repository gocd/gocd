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

import Stream from "mithril/stream";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {CreateEnvModal} from "views/pages/new-environments/create_env_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Create Env Modal", () => {
  const helper = new TestHelper();
  let modal: CreateEnvModal;

  function mountModal() {
    const environments = new Environments(EnvironmentWithOrigin.fromJSON(data.environment_json()));
    modal              = new CreateEnvModal(Stream(environments), jasmine.createSpy("onSuccessfulSave"));

    helper.mount(modal.body.bind(modal));
  }

  beforeEach(() => {
    mountModal();
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should get modal title", () => {
    expect(modal.title()).toEqual("Add New Environment");
  });

  it("should render create env modal", () => {
    const infoMessageText = "Pipelines, agents and environment variables can be added post creation of the environment.";

    expect(helper.byTestId("flash-message-info")).toBeInDOM();
    expect(helper.byTestId("flash-message-info")).toContainText(infoMessageText);
    expect(helper.byTestId("form-field-label-environment-name")).toBeInDOM();
    expect(helper.byTestId("form-field-input-environment-name")).toBeInDOM();
  });
});

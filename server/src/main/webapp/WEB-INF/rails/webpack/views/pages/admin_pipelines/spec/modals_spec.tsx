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

import {ErrorResponse, SuccessResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {Pipeline, PipelineGroups} from "models/internal_pipeline_structure/pipeline_structure";
import data from "models/new-environments/spec/test_data";
import {ModalManager} from "views/components/modal/modal_manager";
import {
  ApiService,
  ClonePipelineConfigModal,
  CreatePipelineGroupModal,
  DeletePipelineGroupModal,
  ExtractTemplateModal,
  MoveConfirmModal
} from "views/pages/admin_pipelines/modals";
import {TestHelper} from "views/pages/spec/test_helper";

describe("CreatePipelineGroupModal", () => {
  let modal: CreatePipelineGroupModal;
  let callback: (groupName: string) => void;
  let modalTestHelper: TestHelper;

  beforeEach(() => {
    callback = jasmine.createSpy("callback");
    modal    = new CreatePipelineGroupModal(callback);
    modal.render();
    m.redraw.sync();
    modalTestHelper = new TestHelper().forModal();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render modal", () => {
    expect(modal).toContainTitle("Create new pipeline group");
    expect(modal).toContainButtons(["Create"]);

    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-pipeline-group-name"), "new-pipeline-group");
    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-create"]`);
    expect(callback).toHaveBeenCalledWith("new-pipeline-group");
  });
});

describe("ClonePipelineConfigModal", () => {
  const successCallback: (newPipelineName: string) => void = jasmine.createSpy("successCallback");

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render modal", () => {
    const dummyService = new class implements ApiService {
      performOperation(onSuccess: (data: SuccessResponse<string>) => void, onError: (message: ErrorResponse) => void): Promise<void> {
        onSuccess({body: "some msg"});
        return Promise.resolve();
      }
    }();
    const modal        = new ClonePipelineConfigModal(Stream(new Pipeline("blah")), successCallback, dummyService);
    modal.render();
    m.redraw.sync();
    const modalTestHelper = new TestHelper().forModal();

    expect(modal).toContainTitle(`Clone pipeline - blah`);
    expect(modal).toContainButtons(["Clone"]);

    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-new-pipeline-name"), "new-pipeline-name");
    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-pipeline-group-name"), "new-pipeline-group");
    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-clone"]`);
    expect(successCallback).toHaveBeenCalledWith("new-pipeline-name");
  });

  it('should render error message if clone operation fails', () => {
    const dummyService = new class implements ApiService {
      performOperation(onSuccess: (data: SuccessResponse<string>) => void, onError: (message: ErrorResponse) => void): Promise<void> {
        onError({message: "some msg", body: '{"message": "some major error"}'});
        return Promise.reject();
      }
    }();
    const modal        = new ClonePipelineConfigModal(Stream(new Pipeline("blah")), successCallback, dummyService);
    modal.render();
    m.redraw.sync();
    const modalTestHelper = new TestHelper().forModal();

    expect(modalTestHelper.byTestId('flash-message-alert')).not.toBeInDOM();

    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-new-pipeline-name"), "new-pipeline-name");
    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-pipeline-group-name"), "new-pipeline-group");
    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-clone"]`);

    expect(modalTestHelper.byTestId('flash-message-alert')).toBeInDOM();
    expect(modal).toContainError("some major error");
  });
});

describe("MoveConfirmModal", () => {
  let modal: MoveConfirmModal;
  let callback: (targetGroup: string) => void;
  let modalTestHelper: TestHelper;

  const pipelineGroupsJSON = data.pipeline_groups_json();
  const pipelineGroups     = PipelineGroups.fromJSON(pipelineGroupsJSON.groups);

  beforeEach(() => {
    callback = jasmine.createSpy("callback");
    modal    = new MoveConfirmModal(pipelineGroups, pipelineGroups[0], pipelineGroups[0].pipelines()[0], callback);
    modal.render();
    m.redraw.sync();
    modalTestHelper = new TestHelper().forModal();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render modal", () => {
    expect(modal).toContainTitle(`Move pipeline ${pipelineGroups[0].pipelines()[0].name()}`);
    expect(modal).toContainButtons(["Move"]);

    const targetPipelineGroup = pipelineGroupsJSON.groups[1].name;

    expect(modalTestHelper.textAll("option")).toEqual([targetPipelineGroup]);
    modalTestHelper.onchange(modalTestHelper.byTestId("move-pipeline-group-selection"), targetPipelineGroup);
    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-move"]`);
    expect(callback).toHaveBeenCalledWith(targetPipelineGroup);
  });
});

describe("ExtractTemplateModal", () => {
  let modal: ExtractTemplateModal;
  let callback: (templateName: string) => void;
  let modalTestHelper: TestHelper;

  beforeEach(() => {
    callback = jasmine.createSpy("callback");
    modal    = new ExtractTemplateModal("foo", callback);
    modal.render();
    m.redraw.sync();
    modalTestHelper = new TestHelper().forModal();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render modal", () => {
    expect(modal).toContainTitle(`Extract template from pipeline foo`);
    expect(modal).toContainButtons(["Extract template"]);

    modalTestHelper.oninput(modalTestHelper.byTestId("form-field-input-new-template-name"), "new-template");
    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-extract-template"]`);
    expect(callback).toHaveBeenCalledWith("new-template");
  });
});

describe('DeletePipelinGroupModalSpec', () => {
  let modal: DeletePipelineGroupModal;
  const successCallBack: (msg: m.Children) => void = jasmine.createSpy("successCallBack");

  afterEach(() => {
    ModalManager.closeAll();
  });

  it('should render modal', () => {
    const dummyService = new class implements ApiService {
      performOperation(onSuccess: (data: SuccessResponse<string>) => void, onError: (message: ErrorResponse) => void): Promise<void> {
        onSuccess({body: "some msg"});
        return Promise.resolve();
      }
    }();
    modal              = new DeletePipelineGroupModal("pipeline-grp-name", successCallBack, dummyService);
    modal.render();
    m.redraw.sync();

    const modalTestHelper: TestHelper = new TestHelper().forModal();

    expect(modal).toContainTitle("Are you sure?");
    expect(modal).toContainButtons(["No", "Yes Delete"]);

    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-delete"]`);
    expect(successCallBack).toHaveBeenCalled();
  });

  it('should render error message if an error occurs', () => {
    const dummyService = new class implements ApiService {
      performOperation(onSuccess: (data: SuccessResponse<string>) => void, onError: (message: ErrorResponse) => void): Promise<void> {
        onError({message: 'some error', body: '{"message": "some major error"}'});
        return Promise.reject();
      }
    }();
    modal              = new DeletePipelineGroupModal("pipeline-grp-name", successCallBack, dummyService);
    modal.render();
    m.redraw.sync();

    const modalTestHelper: TestHelper = new TestHelper().forModal();

    modalTestHelper.clickButtonOnActiveModal(`[data-test-id="button-delete"]`);

    expect(modal).toContainError("some major error");
    expect(modal).toContainButtons(["OK"]);

  });
});

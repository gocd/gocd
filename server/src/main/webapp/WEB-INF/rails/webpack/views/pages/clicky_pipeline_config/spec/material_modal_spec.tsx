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

import {GitMaterialAttributes, Material} from "models/materials/types";
import {MaterialModal} from "views/pages/clicky_pipeline_config/modal/material_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AddMaterialModal", () => {
  const helper = new TestHelper();
  let addMaterialModal: MaterialModal;
  let onSuccessfulAdd: (material: Material) => void;

  beforeEach(() => {
    onSuccessfulAdd  = jasmine.createSpy("onSuccessfulAdd");
    addMaterialModal = MaterialModal.forAdd(onSuccessfulAdd);
    helper.mount(addMaterialModal.body.bind(addMaterialModal));
  });

  afterEach(helper.unmount.bind(helper));

  it("should have a title", () => {
    expect(addMaterialModal.title()).toBe("Add material");
  });

  it("should have a add button", () => {
    expect(addMaterialModal.buttons().length).toBe(1);
  });

  describe("addMaterial()", () => {
    describe("valid material", () => {
      it("should add material", () => {
        const url = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "http://foo.bar");

        addMaterialModal.addOrUpdateMaterial();

        expect(onSuccessfulAdd).toHaveBeenCalled();
      });

      it("should close modal", () => {
        const url = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "http://foo.bar");
        const closeSpyFunction = spyOn(addMaterialModal, "close");

        addMaterialModal.addOrUpdateMaterial();

        expect(closeSpyFunction).toHaveBeenCalled();
      });
    });

    describe("invalid material", () => {
      it("should not add material", () => {
        const url = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "");

        addMaterialModal.addOrUpdateMaterial();

        expect(onSuccessfulAdd).not.toHaveBeenCalled();
      });

      it("should not close modal", () => {
        const url = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "");
        const closeSpyFunction = spyOn(addMaterialModal, "close");

        addMaterialModal.addOrUpdateMaterial();

        expect(closeSpyFunction).not.toHaveBeenCalled();
      });
    });
  });
});

describe("EditMaterialModal", () => {
  const helper = new TestHelper();
  let material: Material;
  let editMaterialModal: MaterialModal;
  let onSuccessfulAdd: (material: Material) => void;

  beforeEach(() => {
    material          = new Material("git", new GitMaterialAttributes());
    onSuccessfulAdd   = jasmine.createSpy("onSuccessfulAdd");
    editMaterialModal = MaterialModal.forEdit(material, onSuccessfulAdd);
    helper.mount(editMaterialModal.body.bind(editMaterialModal));
  });

  afterEach(helper.unmount.bind(helper));

  it("should have a title", () => {
    expect(editMaterialModal.title()).toBe("Edit material - Git");
  });

  it("should have a update button", () => {
    expect(editMaterialModal.buttons().length).toBe(1);
  });

  describe("updateMaterial()", () => {
    describe("valid material", () => {
      it("should update material", () => {
        const url = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "http://foo.bar");

        editMaterialModal.addOrUpdateMaterial();

        expect(onSuccessfulAdd).toHaveBeenCalled();
      });

      it("should close modal", () => {
        const closeSpyFunction = spyOn(editMaterialModal, "close");
        const url              = helper.byTestId("form-field-input-repository-url");
        helper.oninput(url, "http://foo.bar");

        editMaterialModal.addOrUpdateMaterial();

        expect(closeSpyFunction).toHaveBeenCalled();
      });
    });

    describe("invalid material", () => {
      it("should not update material", () => {
        helper.oninput(helper.byTestId("form-field-input-repository-url"), "");
        editMaterialModal.addOrUpdateMaterial();

        expect(onSuccessfulAdd).not.toHaveBeenCalled();
      });

      it("should not close modal", () => {
        helper.oninput(helper.byTestId("form-field-input-repository-url"), "");

        const closeSpyFunction = spyOn(editMaterialModal, "close");

        editMaterialModal.addOrUpdateMaterial();

        expect(closeSpyFunction).not.toHaveBeenCalled();
      });
    });
  });
});

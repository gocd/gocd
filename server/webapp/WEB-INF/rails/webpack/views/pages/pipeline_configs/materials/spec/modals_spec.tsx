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

import _ from "lodash";
import {GitMaterialAttributes, Material} from "models/new_pipeline_configs/materials";
import * as simulateEvent from "simulate-event";
import {AddMaterialModal, EditMaterialModal} from "views/pages/pipeline_configs/materials/modals";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AddMaterialModal", () => {
  const helper = new TestHelper();
  let material: Material;
  let addMaterialModal: AddMaterialModal;

  beforeEach(() => {
    material         = new Material("git", new GitMaterialAttributes("http://foo.bar"));
    addMaterialModal = new AddMaterialModal(material, _.noop);
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
        // As `onSuccessfulAdd` is a private function, `spyOn` needs a type.
        const onSuccessfulAddSpyFunction = spyOn<any>(addMaterialModal, "onSuccessfulAdd");

        addMaterialModal.addMaterial();

        expect(onSuccessfulAddSpyFunction).toHaveBeenCalledWith(material);
      });

      it("should close modal", () => {
        const closeSpyFunction = spyOn(addMaterialModal, "close");

        addMaterialModal.addMaterial();

        expect(closeSpyFunction).toHaveBeenCalled();
      });
    });

    describe("invalid material", () => {
      it("should not add material", () => {
        (material.attributes() as GitMaterialAttributes).url("");
        // As `onSuccessfulAdd` is a private function, `spyOn` needs a type.
        const onSuccessfulAddSpyFunction = spyOn<any>(addMaterialModal, "onSuccessfulAdd");

        addMaterialModal.addMaterial();

        expect(onSuccessfulAddSpyFunction).not.toHaveBeenCalledWith(material);
      });

      it("should not close modal", () => {
        (material.attributes() as GitMaterialAttributes).url("");
        const closeSpyFunction = spyOn(addMaterialModal, "close");

        addMaterialModal.addMaterial();

        expect(closeSpyFunction).not.toHaveBeenCalled();
      });
    });
  });
});

describe("EditMaterialModal", () => {
  const helper = new TestHelper();
  let material: Material;
  let editMaterialModal: EditMaterialModal;

  beforeEach(() => {
    material          = new Material("git", new GitMaterialAttributes("http://foo.bar", "git-material"));
    editMaterialModal = new EditMaterialModal(material, _.noop);
    helper.mount(editMaterialModal.body.bind(editMaterialModal));
  });

  afterEach(helper.unmount.bind(helper));

  it("should have a title", () => {
    expect(editMaterialModal.title()).toBe("git-material");
  });

  it("should have a update button", () => {
    expect(editMaterialModal.buttons().length).toBe(1);
  });

  describe("updateMaterial()", () => {
    describe("valid material", () => {
      it("should update material", () => {
        // As `onSuccessfulUpdate` is a private function, `spyOn` needs a type.
        const onSuccessfulUpdateSpyFunction = spyOn<any>(editMaterialModal, "onSuccessfulEdit");

        editMaterialModal.updateMaterial();

        expect(onSuccessfulUpdateSpyFunction).toHaveBeenCalled();
      });

      it("should close modal", () => {
        const closeSpyFunction = spyOn(editMaterialModal, "close");

        editMaterialModal.updateMaterial();

        expect(closeSpyFunction).toHaveBeenCalled();
      });
    });

    describe("invalid material", () => {
      it("should not update material", () => {
        helper.findByDataTestId("form-field-input-repository-url").val("");
        simulateEvent.simulate(helper.findByDataTestId("form-field-input-repository-url").get(0), "input");
        // As `onSuccessfulUpdate` is a private function, `spyOn` needs a type.
        const onSuccessfulEditSpyFunction = spyOn<any>(editMaterialModal, "onSuccessfulEdit");

        editMaterialModal.updateMaterial();

        expect(onSuccessfulEditSpyFunction).not.toHaveBeenCalled();
      });

      it("should not close modal", () => {
        helper.findByDataTestId("form-field-input-repository-url").val("");
        simulateEvent.simulate(helper.findByDataTestId("form-field-input-repository-url").get(0), "input");

        const closeSpyFunction = spyOn(editMaterialModal, "close");

        editMaterialModal.updateMaterial();

        expect(closeSpyFunction).not.toHaveBeenCalled();
      });
    });
  });
});

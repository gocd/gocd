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
import {AddMaterialModal} from "views/pages/pipeline_configs/materials/modals";
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

    describe("valid material", () => {
      it("should add material", () => {
        (material.attributes() as GitMaterialAttributes).url("");
        // As `onSuccessfulAdd` is a private function, `spyOn` needs a type.
        const onSuccessfulAddSpyFunction = spyOn<any>(addMaterialModal, "onSuccessfulAdd");

        addMaterialModal.addMaterial();

        expect(onSuccessfulAddSpyFunction).not.toHaveBeenCalledWith(material);
      });

      it("should close modal", () => {
        (material.attributes() as GitMaterialAttributes).url("");
        const closeSpyFunction = spyOn(addMaterialModal, "close");

        addMaterialModal.addMaterial();

        expect(closeSpyFunction).not.toHaveBeenCalled();
      });
    });
  });
});

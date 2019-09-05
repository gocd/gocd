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
import {Material} from "models/new_pipeline_configs/materials";
import * as Buttons from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {MaterialEditor} from "./material_editor";

export class AddMaterialModal extends Modal {
  private readonly material: Material;
  private readonly onSuccessfulAdd: (material: Material) => void;

  constructor(material: Material, onSuccessfulAdd: (material: Material) => void) {
    super(Size.large);
    this.material        = material;
    this.onSuccessfulAdd = onSuccessfulAdd;
  }

  body(): m.Children {
    return <MaterialEditor material={this.material}/>;
  }

  title(): string {
    return "Add material";
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.addMaterial.bind(this)}>Add</Buttons.Primary>
    ];
  }

  addMaterial(): void {
    if (this.material.isValid()) {
      this.close();
      this.onSuccessfulAdd(this.material);
    }
  }
}

export class EditMaterialModal extends Modal {
  private readonly material: Material;
  private readonly onSuccessfulEdit: (oldMaterial: Material, newMaterial: Material) => void;
  private readonly _oldMaterial: Material;

  constructor(material: Material, onSuccessfulEdit: (oldMaterial: Material, newMaterial: Material) => void) {
    super(Size.large);
    this._oldMaterial     = material;
    this.material         = material.clone();
    this.onSuccessfulEdit = onSuccessfulEdit;
  }

  body(): m.Children {
    return <MaterialEditor material={this.material}/>;
  }

  title(): string {
    return this.material.name() ? this.material.name() : this.material.materialUrl();
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.updateMaterial.bind(this)}>Update</Buttons.Primary>
    ];
  }

  updateMaterial(): void {
    if (this.material.isValid()) {
      this.close();
      this.onSuccessfulEdit(this._oldMaterial, this.material);
    }
  }
}

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
import {Material} from "models/materials/types";
import s from "underscore.string";
import * as Buttons from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {MaterialEditor} from "views/pages/pipelines/material_editor";

export class MaterialModal extends Modal {
  private readonly __title: string;
  private readonly material: Material;
  private readonly onSuccessfulAdd: (material: Material) => void;

  private constructor(title: string, material: Material, onSuccessfulAdd: (material: Material) => void) {
    super(Size.large);
    this.__title         = title;
    this.material        = material;
    this.onSuccessfulAdd = onSuccessfulAdd;
  }

  static forAdd(onSuccessfulAdd: (material: Material) => void) {
    return new MaterialModal("Add material", new Material("git"), onSuccessfulAdd);
  }

  static forEdit(material: Material, onSuccessfulAdd: (material: Material) => void) {
    const type = `Edit material - ${s.capitalize(material.type()!)}`;
    return new MaterialModal(type, new Material(material.type(), material.attributes()), onSuccessfulAdd);
  }

  body(): m.Children {
    return <MaterialEditor material={this.material}/>;
  }

  title(): string {
    return this.__title;
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.addOrUpdateMaterial.bind(this)}>Save</Buttons.Primary>
    ];
  }

  addOrUpdateMaterial(): void {
    if (this.material.isValid()) {
      this.close();
      this.onSuccessfulAdd(this.material);
    }
  }
}

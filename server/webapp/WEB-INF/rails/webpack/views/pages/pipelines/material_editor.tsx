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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {Form, FormBody} from "views/components/forms/form";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {DefaultCache, DependencyFields, SuggestionCache} from "./non_scm_material_fields";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "./scm_material_fields";

interface Attrs {
  material: Material;
  group: string;
  cache?: SuggestionCache;
}

export class MaterialEditor extends MithrilViewComponent<Attrs> {
  cache: SuggestionCache = new DefaultCache();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }
  }

  view(vnode: m.Vnode<Attrs>) {
    return <FormBody>
      <SelectField label="Material Type" property={vnode.attrs.material.type} required={true}>
        <SelectFieldOptions selected={vnode.attrs.material.type()} items={this.supportedMaterials()}/>
      </SelectField>

      <Form last={true} compactForm={true}>
        {this.fieldsForType(vnode.attrs.material, this.cache, vnode.attrs.group)}
      </Form>
    </FormBody>;
  }

  supportedMaterials(): Option[] {
    return [
      {id: "git", text: "Git"},
      {id: "hg", text: "Mercurial"},
      {id: "svn", text: "Subversion"},
      {id: "p4", text: "Perforce"},
      {id: "tfs", text: "Team Foundation Server"},
      {id: "dependency", text: "Another Pipeline"},
    ];
  }

  fieldsForType(material: Material, cacheable: SuggestionCache, group: string): m.Children {
    switch (material.type()) {
      case "git":
        if (!(material.attributes() instanceof GitMaterialAttributes)) {
          material.attributes(new GitMaterialAttributes());
        }
        return <GitFields material={material} group={group}/>;
        break;
      case "hg":
        if (!(material.attributes() instanceof HgMaterialAttributes)) {
          material.attributes(new HgMaterialAttributes());
        }
        return <HgFields material={material} group={group}/>;
        break;
      case "svn":
        if (!(material.attributes() instanceof SvnMaterialAttributes)) {
          material.attributes(new SvnMaterialAttributes());
        }
        return <SvnFields material={material} group={group}/>;
        break;
      case "p4":
        if (!(material.attributes() instanceof P4MaterialAttributes)) {
          material.attributes(new P4MaterialAttributes());
        }
        return <P4Fields material={material} group={group}/>;
        break;
      case "tfs":
        if (!(material.attributes() instanceof TfsMaterialAttributes)) {
          material.attributes(new TfsMaterialAttributes());
        }
        return <TfsFields material={material} group={group}/>;
        break;
      case "dependency":
        if (!(material.attributes() instanceof DependencyMaterialAttributes)) {
          material.attributes(new DependencyMaterialAttributes());
        }
        return <DependencyFields material={material} cache={cacheable}/>;
        break;
      default:
        break;
    }
  }
}

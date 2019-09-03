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
import m from "mithril";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/new_pipeline_configs/materials";
import {Form, FormBody} from "views/components/forms/form";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {SUPPORTED_MATERIALS} from "views/pages/create_pipeline_page";
import {DefaultCache, DependencyFields, SuggestionCache} from "./non_scm_material_fields";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "./scm_material_fields";

interface Attrs {
  material: Material;
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
        <SelectFieldOptions selected={vnode.attrs.material.type()} items={SUPPORTED_MATERIALS}/>
      </SelectField>

      <Form last={true} compactForm={true}>
        {this.fieldsForType(vnode.attrs.material, this.cache)}
      </Form>
    </FormBody>;
  }

  fieldsForType(material: Material, cacheable: SuggestionCache): m.Children {
    switch (material.type()) {
      case "git":
        if (!(material.attributes() instanceof GitMaterialAttributes)) {
          material.attributes(new GitMaterialAttributes("", ""));
        }
        return <GitFields material={material}/>;
        break;
      case "hg":
        if (!(material.attributes() instanceof HgMaterialAttributes)) {
          material.attributes(new HgMaterialAttributes("", ""));
        }
        return <HgFields material={material}/>;
        break;
      case "svn":
        if (!(material.attributes() instanceof SvnMaterialAttributes)) {
          material.attributes(new SvnMaterialAttributes("", ""));
        }
        return <SvnFields material={material}/>;
        break;
      case "p4":
        if (!(material.attributes() instanceof P4MaterialAttributes)) {
          material.attributes(new P4MaterialAttributes("", "", ""));
        }
        return <P4Fields material={material}/>;
        break;
      case "tfs":
        if (!(material.attributes() instanceof TfsMaterialAttributes)) {
          material.attributes(new TfsMaterialAttributes("", ""));
        }
        return <TfsFields material={material}/>;
        break;
      case "dependency":
        if (!(material.attributes() instanceof DependencyMaterialAttributes)) {
          material.attributes(new DependencyMaterialAttributes("", "", ""));
        }
        return <DependencyFields material={material} cache={cacheable}/>;
        break;
      default:
        break;
    }
  }
}

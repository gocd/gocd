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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {DependencyMaterialAttributes, GitMaterialAttributes, HgMaterialAttributes, Material, P4MaterialAttributes, PackageMaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "models/materials/types";
import {Packages} from "models/package_repositories/package_repositories";
import {Form, FormBody} from "views/components/forms/form";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {DefaultCache, DependencyFields, PackageFields, SuggestionCache} from "./non_scm_material_fields";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "./scm_material_fields";

interface Attrs {
  material: Material;
  cache?: SuggestionCache;
  hideTestConnection?: boolean;
  showLocalWorkingCopyOptions?: boolean;
  scmOnly?: boolean;
  disabled?: boolean;
  showExtraMaterials?: boolean;
  packages?: Packages;
}

export class MaterialEditor extends MithrilViewComponent<Attrs> {
  cache: SuggestionCache = new DefaultCache();

  oninit(vnode: m.Vnode<Attrs, {}>) {
    if (vnode.attrs.cache) {
      this.cache = vnode.attrs.cache;
    }
  }

  view(vnode: m.Vnode<Attrs>) {
    const attrs                       = vnode.attrs;
    const showLocalWorkingCopyOptions = "showLocalWorkingCopyOptions" in attrs ? !!attrs.showLocalWorkingCopyOptions : true;
    const scmOnly                     = !!attrs.scmOnly;
    const hideTestConnection          = !!attrs.hideTestConnection;

    const supportedMaterials: Array<Option | string> = this.supportedMaterials(scmOnly);
    if (!!attrs.showExtraMaterials) {
      supportedMaterials.push({id: "package", text: "Package"});
    }

    return <FormBody>
      <SelectField label="Material Type" property={vnode.attrs.material.type} required={true}
                   readonly={vnode.attrs.disabled}>
        <SelectFieldOptions selected={vnode.attrs.material.type()} items={supportedMaterials}/>
      </SelectField>

      <Form last={true} compactForm={true}>
        {this.fieldsForType(attrs.material, this.cache, showLocalWorkingCopyOptions, hideTestConnection, attrs.disabled, attrs.packages)}
      </Form>
    </FormBody>;
  }

  supportedMaterials(scmOnly: boolean): Option[] {
    const options = [
      {id: "git", text: "Git"},
      {id: "hg", text: "Mercurial"},
      {id: "svn", text: "Subversion"},
      {id: "p4", text: "Perforce"},
      {id: "tfs", text: "Team Foundation Server"},
    ];

    if (!scmOnly) {
      options.push({id: "dependency", text: "Another Pipeline"});
    }

    return options;
  }

  fieldsForType(material: Material, cacheable: SuggestionCache, showLocalWorkingCopyOptions: boolean, hideTestConnection: boolean, disabled?: boolean, packages?: Packages): m.Children {
    switch (material.type()) {
      case "git":
        if (!(material.attributes() instanceof GitMaterialAttributes)) {
          material.attributes(new GitMaterialAttributes("", true));
        }
        return <GitFields material={material} hideTestConnection={hideTestConnection}
                          showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "hg":
        if (!(material.attributes() instanceof HgMaterialAttributes)) {
          material.attributes(new HgMaterialAttributes("", true));
        }
        return <HgFields material={material} hideTestConnection={hideTestConnection}
                         showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "svn":
        if (!(material.attributes() instanceof SvnMaterialAttributes)) {
          material.attributes(new SvnMaterialAttributes("", true));
        }
        return <SvnFields material={material} hideTestConnection={hideTestConnection}
                          showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "p4":
        if (!(material.attributes() instanceof P4MaterialAttributes)) {
          material.attributes(new P4MaterialAttributes("", true));
        }
        return <P4Fields material={material} hideTestConnection={hideTestConnection}
                         showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "tfs":
        if (!(material.attributes() instanceof TfsMaterialAttributes)) {
          material.attributes(new TfsMaterialAttributes("", true));
        }
        return <TfsFields material={material} hideTestConnection={hideTestConnection}
                          showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "dependency":
        if (!(material.attributes() instanceof DependencyMaterialAttributes)) {
          material.attributes(new DependencyMaterialAttributes());
        }
        return <DependencyFields material={material} cache={cacheable}
                                 showLocalWorkingCopyOptions={showLocalWorkingCopyOptions}/>;
      case "package":
        if (!(material.attributes() instanceof PackageMaterialAttributes)) {
          material.attributes(new PackageMaterialAttributes("", true, ""));
        }
        packages = packages === undefined ? new Packages() : packages;
        return <PackageFields material={material} packages={packages}
                              showLocalWorkingCopyOptions={showLocalWorkingCopyOptions}/>;
      default:
        break;
    }
  }
}

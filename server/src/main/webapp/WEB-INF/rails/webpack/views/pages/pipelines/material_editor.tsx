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
import _ from "lodash";
import m from "mithril";
import {Filter} from "models/maintenance_mode/material";
import {Scms} from "models/materials/pluggable_scm";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {PackageRepositories} from "models/package_repositories/package_repositories";
import {ExtensionTypeString, PackageRepoExtensionType, SCMExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormBody} from "views/components/forms/form";
import {Option, SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {DefaultCache, DependencyFields, PackageFields, PluginFields, SuggestionCache} from "./non_scm_material_fields";
import {GitFields, HgFields, P4Fields, SvnFields, TfsFields} from "./scm_material_fields";

interface Attrs {
  material: Material;
  cache?: SuggestionCache;
  hideTestConnection?: boolean;
  showLocalWorkingCopyOptions?: boolean;
  scmOnly?: boolean;
  disabled?: boolean;
  disabledMaterialTypeSelection?: boolean;
  showExtraMaterials?: boolean;
  packageRepositories?: PackageRepositories;
  pluginInfos?: PluginInfos;
  pluggableScms?: Scms;
  disableScmMaterials?: boolean;
  readonly?: boolean;
  parentPipelineName?: string;
  showGitMaterialShallowClone?: boolean;
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
    const readonly                    = attrs.readonly;
    const hideTestConnection          = readonly || !!attrs.hideTestConnection;
    const disableScmMaterials         = attrs.disableScmMaterials !== undefined && attrs.disableScmMaterials === true;
    const showGitMaterialShallowClone = attrs.showGitMaterialShallowClone === undefined ? true : attrs.showGitMaterialShallowClone;

    return <FormBody>
      <SelectField label="Material Type" property={vnode.attrs.material.type} required={true}
                   readonly={readonly || vnode.attrs.disabled || vnode.attrs.disabledMaterialTypeSelection}>
        <SelectFieldOptions selected={vnode.attrs.material.type()} items={this.supportedMaterials(scmOnly, !!attrs.showExtraMaterials)}/>
      </SelectField>

      <Form last={true} compactForm={true}>
        {this.fieldsForType(attrs.readonly!, attrs.material, this.cache, showLocalWorkingCopyOptions, hideTestConnection, disableScmMaterials, attrs.disabled, attrs.packageRepositories, attrs.pluginInfos, attrs.pluggableScms, attrs.parentPipelineName, showGitMaterialShallowClone)}
      </Form>
    </FormBody>;
  }

  supportedMaterials(scmOnly: boolean, showExtraMaterials: boolean): Option[] {
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
    if (showExtraMaterials) {
      options.push({id: "package", text: "Package Materials"});
      options.push({id: "plugin", text: "Plugin Materials"});
    }

    return options;
  }

  fieldsForType(readonly: boolean, material: Material, cacheable: SuggestionCache, showLocalWorkingCopyOptions: boolean, hideTestConnection: boolean, disableScmMaterials: boolean, disabled?: boolean, packageRepositories?: PackageRepositories, pluginInfos?: PluginInfos, scms?: Scms, parentPipelineName?: string, showGitMaterialShallowClone?: boolean): m.Children {
    const warningMsg = <FlashMessage type={MessageType.warning} dataTestId={"materials-destination-warning-message"}>
      In order to configure multiple SCM materials for this pipeline, each of its material needs have to a 'Alternate Checkout Path' specified.
      Please edit the existing material and specify a 'Alternate Checkout Path' in order to proceed with this operation.
    </FlashMessage>;
    switch (material.type()) {
      case "git":
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof GitMaterialAttributes)) {
          material.attributes(new GitMaterialAttributes(undefined, true));
        }
        return <GitFields material={material} hideTestConnection={hideTestConnection} readonly={readonly} parentPipelineName={parentPipelineName}
                          showGitMaterialShallowClone={showGitMaterialShallowClone} showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "hg":
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof HgMaterialAttributes)) {
          material.attributes(new HgMaterialAttributes(undefined, true));
        }
        return <HgFields material={material} hideTestConnection={hideTestConnection} readonly={readonly} parentPipelineName={parentPipelineName}
                         showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "svn":
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof SvnMaterialAttributes)) {
          material.attributes(new SvnMaterialAttributes(undefined, true));
        }
        return <SvnFields material={material} hideTestConnection={hideTestConnection} readonly={readonly} parentPipelineName={parentPipelineName}
                          showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "p4":
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof P4MaterialAttributes)) {
          material.attributes(new P4MaterialAttributes(undefined, true));
        }
        return <P4Fields material={material} hideTestConnection={hideTestConnection} readonly={readonly} parentPipelineName={parentPipelineName}
                         showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "tfs":
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof TfsMaterialAttributes)) {
          material.attributes(new TfsMaterialAttributes(undefined, true));
        }
        return <TfsFields material={material} hideTestConnection={hideTestConnection} readonly={readonly} parentPipelineName={parentPipelineName}
                          showLocalWorkingCopyOptions={showLocalWorkingCopyOptions} disabled={disabled}/>;
      case "dependency":
        if (!(material.attributes() instanceof DependencyMaterialAttributes)) {
          material.attributes(new DependencyMaterialAttributes());
        }
        return <DependencyFields material={material} cache={cacheable} readonly={readonly} parentPipelineName={parentPipelineName}
                                 showLocalWorkingCopyOptions={showLocalWorkingCopyOptions}/>;
      case "package":
        if (!(material.attributes() instanceof PackageMaterialAttributes)) {
          material.attributes(new PackageMaterialAttributes(undefined, true, ""));
        }
        pluginInfos = pluginInfos === undefined ? new PluginInfos()
          : pluginInfos!.filterForExtension(ExtensionTypeString.PACKAGE_REPO);
        if (_.isEmpty(pluginInfos)) {
          return <FlashMessage type={MessageType.warning}>
            There are no Package Repository plugins installed. Please see <Link href={new PackageRepoExtensionType().linkForDocs()} target="_blank"
                                                                                externalLinkIcon={true}>this page</Link> for a list of supported
            plugins.
          </FlashMessage>;
        }
        packageRepositories = packageRepositories === undefined ? new PackageRepositories() : packageRepositories;
        return <PackageFields material={material} readonly={readonly}
                              packageRepositories={packageRepositories} pluginInfos={pluginInfos}/>;
      case "plugin":
        pluginInfos = pluginInfos === undefined ? new PluginInfos()
          : pluginInfos!.filterForExtension(ExtensionTypeString.SCM);
        if (_.isEmpty(pluginInfos)) {
          return <FlashMessage type={MessageType.warning}>
            There are no SCM plugins installed. Please see <Link href={new SCMExtensionType().linkForDocs()} target="_blank"
                                                                 externalLinkIcon={true}>this page</Link> for a list of supported plugins.
          </FlashMessage>;
        }
        if (disableScmMaterials) {
          return warningMsg;
        }
        if (!(material.attributes() instanceof PluggableScmMaterialAttributes)) {
          material.attributes(new PluggableScmMaterialAttributes(undefined, true, "", "", new Filter([])));
        }
        scms = scms === undefined ? new Scms() : scms;
        return <PluginFields material={material} showLocalWorkingCopyOptions={showLocalWorkingCopyOptions}
                             readonly={readonly} scms={scms} pluginInfos={pluginInfos}/>;
      default:
        break;
    }
  }
}

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

import Awesomplete from "awesomplete";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {DependencyMaterialAutocomplete, PipelineNameCache} from "models/materials/dependency_autocomplete_cache";
import {Scms} from "models/materials/pluggable_scm";
import {
  DependencyMaterialAttributes,
  Material,
  MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes
} from "models/materials/types";
import {PackageRepositories, PackageRepository} from "models/package_repositories/package_repositories";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {KeyValuePair} from "views/components/key_value_pair";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {PackageRepositoryWidget} from "views/pages/package_repositories/package_repository_widget";
import {PackageWidget} from "views/pages/package_repositories/package_widget";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import styles from "./advanced_settings.scss";
import {DESTINATION_DIR_HELP_MESSAGE, IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";

interface Attrs {
  material: Material;
  cache: SuggestionCache;
  showLocalWorkingCopyOptions: boolean;
  disabled?: boolean;
}

interface State {
  provider: SuggestionProvider;
  stages(): Option[];
}

// tslint:disable-next-line
export interface SuggestionCache extends PipelineNameCache<Awesomplete.Suggestion, Option> {
}

export class DefaultCache extends DependencyMaterialAutocomplete<Awesomplete.Suggestion, Option> implements SuggestionCache {
  constructor() {
    super(String, (stage: string) => ({id: stage, text: stage} as Option));
  }
}

class DependencySuggestionProvider extends SuggestionProvider {
  private cache: SuggestionCache;

  constructor(cache: SuggestionCache) {
    super();
    this.cache = cache;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    const self = this;
    return new Promise<Awesomplete.Suggestion[]>((resolve, reject) => {
      if (!self.cache.ready()) {
        self.cache.prime(() => {
          resolve(self.cache.pipelines());
        }, () => {
          reject(self.cache.failureReason());
        });
      } else {
        resolve(self.cache.pipelines());
      }
    });
  }
}

export class DependencyFields extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.disabled) {
      return;
    }

    const mat             = vnode.attrs.material.attributes() as DependencyMaterialAttributes;
    const cache           = vnode.attrs.cache;
    const EMPTY: Option[] = [{id: "", text: "-"}];
    vnode.state.stages    = Stream(EMPTY);

    vnode.state.provider = new DependencySuggestionProvider(vnode.attrs.cache);
    vnode.state.stages   = () => mat.pipeline() ? EMPTY.concat(cache.stages(mat.pipeline()!)) : [];
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children {
    const mat = vnode.attrs.material.attributes() as DependencyMaterialAttributes;

    if (vnode.attrs.disabled) {
      return [
        <TextField label="Upstream Pipeline" property={mat.pipeline} errorText={this.errs(mat, "pipeline")}
                   required={true} readonly={true}/>,
        <SelectField label="Upstream Stage" property={mat.stage} errorText={this.errs(mat, "stage")} required={true}
                     readonly={true}>
          <SelectFieldOptions selected={mat.stage()} items={vnode.state.stages()}/>
        </SelectField>,
      ];
    }

    return [
      <AutocompleteField label="Upstream Pipeline" property={mat.pipeline} errorText={this.errs(mat, "pipeline")}
                         required={true} maxItems={25} provider={vnode.state.provider}/>,
      <SelectField label="Upstream Stage" property={mat.stage} errorText={this.errs(mat, "stage")} required={true}>
        <SelectFieldOptions selected={mat.stage()} items={vnode.state.stages()}/>
      </SelectField>,
      this.advanced(mat, vnode.attrs.showLocalWorkingCopyOptions),
    ];
  }

  advanced(mat: DependencyMaterialAttributes, showLocalWorkingCopyOptions: boolean): m.Children {
    if (showLocalWorkingCopyOptions) {
      return <AdvancedSettings forceOpen={mat.errors().hasErrors("name")}>
        <TextField label="Material Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE}
                   placeholder="A human-friendly label for this material" property={mat.name}/>

        <SwitchBtn label="Do not schedule the pipeline when this material is updated"
                   helpText="When set to true, the pipeline will not be automatically scheduled for changes to this material."
                   docLink={"/configuration/configuration_reference.html#pipeline-1"}
                   dataTestId="material-ignore-for-scheduling"
                   small={true}
                   css={styles}
                   field={mat.ignoreForScheduling}
                   errorText={this.errs(mat, "ignoreForScheduling")}/>
      </AdvancedSettings>;
    }
  }

  errs(attrs: MaterialAttributes, key: string): string {
    return attrs.errors().errorsForDisplay(key);
  }
}

interface PackageAttrs {
  material: Material;
  packageRepositories: PackageRepositories;
  pluginInfos: PluginInfos;
  disabled?: boolean;
}

interface PackageState {
  pkgRepoId: Stream<string>;
  pkgs: Stream<Option[]>;
}

export class PackageFields extends MithrilComponent<PackageAttrs, PackageState> {
  private readonly defaultPkgs         = [{id: "", text: "Select a package"}];
  private disablePkgRepoField: boolean = false;
  private disablePkgField: boolean     = true;
  private errorMessage?: m.Children    = undefined;

  oninit(vnode: m.Vnode<PackageAttrs, PackageState>): any {
    vnode.state.pkgRepoId = Stream("");
    vnode.state.pkgs      = Stream(this.defaultPkgs);

    const attrs = vnode.attrs.material.attributes() as PackageMaterialAttributes;
    if (attrs.ref()) {
      const selectedPkgRepo = vnode.attrs.packageRepositories.find((pkgRepo) => {
        const selectedPkg = pkgRepo.packages().find((pkg) => pkg.id() === attrs.ref());
        return selectedPkg !== undefined;
      });
      if (selectedPkgRepo !== undefined) {
        vnode.state.pkgRepoId(selectedPkgRepo.repoId());
        vnode.state.pkgs(this.mapAndConcatPackages(selectedPkgRepo));
      }
    }
  }

  view(vnode: m.Vnode<PackageAttrs, PackageState>): m.Children | void | null {
    const attrs                                = vnode.attrs.material.attributes() as PackageMaterialAttributes;
    const packageRepos: Array<Option | string> = [{id: "", text: "Select a package repository"}];
    packageRepos.push(...vnode.attrs.packageRepositories.map((packageRepo) => {
      return {id: packageRepo.repoId(), text: packageRepo.name()};
    }));
    const readonly = !!vnode.attrs.disabled;
    this.setErrorMessageIfApplicable(vnode, packageRepos);
    return <div className={styles.packageFields}>
      {this.errorMessage}
      <table>
        <tr>
          <td>
            <SelectField property={this.packageRepoProxy.bind(this, vnode)}
                         label="Package Repository"
                         errorText={attrs.errors().errorsForDisplay("pkgRepo")}
                         required={true} readonly={readonly || this.disablePkgRepoField}>
              <SelectFieldOptions selected={vnode.state.pkgRepoId()} items={packageRepos}/>
            </SelectField>
          </td>
          <td>
            <SelectField property={attrs.ref} label="Package" required={true}
                         errorText={attrs.errors().errorsForDisplay("ref")}
                         readonly={readonly || this.disablePkgField}>
              <SelectFieldOptions selected={attrs.ref()} items={vnode.state.pkgs()}/>
            </SelectField>
          </td>
        </tr>
        <tr>
          <td className={styles.spaceBetween}>{this.showSelectedPkgRepoConfig(vnode)}</td>
          <td>{this.showSelectedPkgConfig(vnode)}</td>
        </tr>
      </table>
    </div>;
  }

  private setErrorMessageIfApplicable(vnode: m.Vnode<PackageAttrs, PackageState>, packageRepos: Array<Option | string>) {
    this.errorMessage = undefined;
    if (packageRepos.length === 1) {
      this.errorMessage        = <FlashMessage type={MessageType.warning}>
        No package repositories defined.
      </FlashMessage>;
      this.disablePkgRepoField = true;
    }
    if (vnode.state.pkgRepoId().length > 0 && vnode.state.pkgs().length === 1) {
      this.errorMessage    = <FlashMessage type={MessageType.warning}>
        No packages defined for the selected package repository.
      </FlashMessage>;
      this.disablePkgField = true;
    }
  }

  private packageRepoProxy(vnode: m.Vnode<PackageAttrs, PackageState>, pkgRepoId?: string): any {
    if (!pkgRepoId) {
      return vnode.state.pkgRepoId();
    }

    vnode.state.pkgRepoId(pkgRepoId);
    (vnode.attrs.material.attributes() as PackageMaterialAttributes).ref("");
    const selectedPkgRepo = vnode.attrs.packageRepositories.find((pkgRepo) => pkgRepo.repoId() === pkgRepoId)!;
    const pkgs            = this.mapAndConcatPackages(selectedPkgRepo);
    vnode.state.pkgs(pkgs);
    this.resetErrorAndWarningFields(vnode);
    this.setErrorIfPluginNotPresent(vnode, selectedPkgRepo);

    return pkgRepoId;
  }

  private mapAndConcatPackages(selectedPkgRepo: PackageRepository) {
    return this.defaultPkgs
               .concat(...selectedPkgRepo.packages()
                                         .map((pkg) => {
                                           return {id: pkg.id(), text: pkg.name()};
                                         }));
  }

  private setErrorIfPluginNotPresent(vnode: m.Vnode<PackageAttrs, PackageState>, selectedPkgRepo: PackageRepository) {
    const pluginInfo = vnode.attrs.pluginInfos.findByPluginId(selectedPkgRepo.pluginMetadata().id());
    if (pluginInfo === undefined) {
      vnode.attrs.material.attributes()!
        .errors().add("pkgRepo", `Associated plugin '${selectedPkgRepo.pluginMetadata().id()}' not found. Please contact the system administrator to install the plugin.`);
      this.disablePkgField = true;
    }
  }

  private resetErrorAndWarningFields(vnode: m.Vnode<PackageAttrs, PackageState>) {
    vnode.attrs.material.attributes()!.clearErrors("pkgRepo");
    this.disablePkgField = false;
  }

  private showSelectedPkgRepoConfig(vnode: m.Vnode<PackageAttrs, PackageState>) {
    const selectedPkgRepo = vnode.attrs.packageRepositories.find((pkgRepo) => pkgRepo.repoId() === vnode.state.pkgRepoId());
    if (selectedPkgRepo !== undefined) {
      return <KeyValuePair data-test-id={"selected-pkg-repo-details"}
                           data={PackageRepositoryWidget.getPkgRepoDetails(selectedPkgRepo)}/>;
    }
  }

  private showSelectedPkgConfig(vnode: m.Vnode<PackageAttrs, PackageState>) {
    const selectedPkgRepo = vnode.attrs.packageRepositories.find((pkgRepo) => pkgRepo.repoId() === vnode.state.pkgRepoId());
    if (selectedPkgRepo !== undefined) {
      const attrs       = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
      const selectedPkg = selectedPkgRepo.packages().find((pkg) => pkg.id() === attrs.ref());
      if (selectedPkg !== undefined) {
        return <KeyValuePair data-test-id={"selected-pkg-details"}
                             data={PackageWidget.getPkgDetails(selectedPkg)}/>;
      }
    }
  }
}

interface PluginAttrs {
  material: Material;
  scms: Scms;
  pluginInfos: PluginInfos;
  showLocalWorkingCopyOptions?: boolean;
  disabled?: boolean;
}

interface PluginState {
  pluginId: Stream<string>;
  scmsForSelectedPlugin: Stream<Option[]>;
}

export class PluginFields extends MithrilComponent<PluginAttrs, PluginState> {
  readonly defaultScms              = [{id: "", text: "Select a scm"}];
  private disableScmField: boolean  = true;
  private errorMessage?: m.Children = undefined;

  oninit(vnode: m.Vnode<PluginAttrs, PluginState>): any {
    vnode.state.pluginId              = Stream("");
    vnode.state.scmsForSelectedPlugin = Stream(this.defaultScms);
  }

  view(vnode: m.Vnode<PluginAttrs, PluginState>): m.Children | void | null {
    const attrs                           = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
    const plugins: Array<Option | string> = [{id: "", text: "Select a plugin"}];
    plugins.push(..._.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    }));
    const readonly                    = !!vnode.attrs.disabled;
    const showLocalWorkingCopyOptions = !!vnode.attrs.showLocalWorkingCopyOptions;
    this.setErrorMessageIfApplicable(vnode, plugins);

    return <div className={styles.packageFields}>
      {this.errorMessage}
      <table>
        <tr>
          <td>
            <SelectField property={this.pluginIdProxy.bind(this, vnode)}
                         label="SCM Plugin"
                         errorText={attrs.errors().errorsForDisplay("pluginId")}
                         required={true} readonly={readonly}>
              <SelectFieldOptions selected={vnode.state.pluginId()} items={plugins}/>
            </SelectField>
          </td>
          <td>
            <SelectField property={attrs.ref} label="SCM" required={true}
                         errorText={attrs.errors().errorsForDisplay("ref")}
                         readonly={readonly || this.disableScmField}>
              <SelectFieldOptions selected={attrs.ref()} items={vnode.state.scmsForSelectedPlugin()}/>
            </SelectField>
          </td>
        </tr>
        <tr>
          <td className={styles.spaceBetween}>{this.showSelectedPluginConfig(vnode)}</td>
          <td>{this.showSelectedScmConfig(vnode)}</td>
        </tr>
      </table>
      {this.advanced(attrs, showLocalWorkingCopyOptions)}
    </div>;
  }

  private pluginIdProxy(vnode: m.Vnode<PluginAttrs, PluginState>, pluginId?: string): any {
    if (!pluginId) {
      return vnode.state.pluginId();
    }

    vnode.state.pluginId(pluginId);
    const scmsForPlugin = vnode.attrs.scms
                               .filter((scm) => scm.pluginMetadata().id() === pluginId)
                               .map((scm) => {
                                 return {id: scm.id(), text: scm.name()};
                               });
    const scms          = this.defaultScms.concat(...scmsForPlugin);
    vnode.state.scmsForSelectedPlugin(scms);
    (vnode.attrs.material.attributes() as PluggableScmMaterialAttributes).ref("");
    this.disableScmField = false;

    return pluginId;
  }

  private advanced(attrs: PluggableScmMaterialAttributes, showLocalWorkingCopyOptions: boolean): m.Children {
    if (showLocalWorkingCopyOptions) {
      const labelForDestination = [
        "Alternate Checkout Path",
        " ",
        <Tooltip.Help size={TooltipSize.medium} content={DESTINATION_DIR_HELP_MESSAGE}/>
      ];
      const forceOpen           = attrs.errors().hasErrors("name") || attrs.errors().hasErrors("destination");
      return <AdvancedSettings forceOpen={forceOpen}>
        <TextField label={labelForDestination} property={attrs.destination}
                   errorText={attrs.errors().errorsForDisplay("destination")}/>
        <TextField label="Material Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE}
                   placeholder="A human-friendly label for this material" property={attrs.name}/>
      </AdvancedSettings>;
    }
  }

  private setErrorMessageIfApplicable(vnode: m.Vnode<PluginAttrs, PluginState>, plugins: Array<Option | string>) {
    this.errorMessage = undefined;
    if (vnode.state.pluginId().length > 0 && vnode.state.scmsForSelectedPlugin().length === 1) {
      this.errorMessage    = <FlashMessage type={MessageType.warning}>
        There are no SCMs configured for the selected plugin.
      </FlashMessage>;
      this.disableScmField = true;
    }
  }

  private showSelectedPluginConfig(vnode: m.Vnode<PluginAttrs, PluginState>) {
    const selectedPlugin = vnode.attrs.pluginInfos.findByPluginId(vnode.state.pluginId());
    if (selectedPlugin !== undefined) {
      const data = new Map<string, string | m.Children>([
                                                          ["Id", selectedPlugin.id],
                                                          ["Name", selectedPlugin.about.name],
                                                          ["Description", selectedPlugin.about.description],
                                                        ]);
      return <KeyValuePair data-test-id={"selected-plugin-details"} data={data}/>;
    }
  }

  private showSelectedScmConfig(vnode: m.Vnode<PluginAttrs, PluginState>) {
    const attrs       = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
    const selectedScm = vnode.attrs.scms.find((scm) => scm.id() === attrs.ref());
    if (selectedScm !== undefined) {
      const scmRepoDetails = new Map([
                                       ["Id", selectedScm.id()],
                                       ["Name", selectedScm.name()],
                                       ["Plugin Id", selectedScm.pluginMetadata().id()],
                                       ...Array.from(selectedScm.configuration().asMap())
                                     ]);
      return <KeyValuePair data-test-id={"selected-scm-details"} data={scmRepoDetails}/>;
    }
  }
}

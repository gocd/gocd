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
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Filter} from "models/maintenance_mode/material";
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
import {CheckboxField, Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {SwitchBtn} from "views/components/switch";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {ConfigurationDetailsWidget} from "views/pages/package_repositories/configuration_details_widget";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import styles from "./advanced_settings.scss";
import {DENYLIST_HELP_MESSAGE, DESTINATION_DIR_HELP_MESSAGE, IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";

interface Attrs {
  material: Material;
  cache: SuggestionCache;
  showLocalWorkingCopyOptions: boolean;
  disabled?: boolean;
  readonly?: boolean;
  parentPipelineName?: string;
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
  private parentPipelineName?: string;

  constructor(cache: SuggestionCache, parentPipelineName?: string) {
    super();
    this.cache              = cache;
    this.parentPipelineName = parentPipelineName;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    const self = this;

    const removeParentPipeline = (pipelines: Awesomplete.Suggestion[]) => {
      if (this.parentPipelineName !== undefined) {
        const indexOf = pipelines.indexOf(this.parentPipelineName);
        if (indexOf >= 0) {
          pipelines.splice(indexOf, 1);
        }
      }
      return pipelines;
    };

    return new Promise<Awesomplete.Suggestion[]>((resolve, reject) => {
      if (!self.cache.ready()) {
        self.cache.prime(() => {
          resolve(removeParentPipeline(self.cache.pipelines()));
        }, () => {
          reject(self.cache.failureReason());
        });
      } else {
        resolve(removeParentPipeline(self.cache.pipelines()));
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

    vnode.state.provider = new DependencySuggestionProvider(vnode.attrs.cache, vnode.attrs.parentPipelineName);
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
                         readonly={vnode.attrs.readonly} autoEvaluate={!vnode.attrs.readonly}
                         aut required={true} maxItems={25} provider={vnode.state.provider}/>,
      <SelectField label="Upstream Stage" readonly={vnode.attrs.readonly}
                   property={mat.stage} errorText={this.errs(mat, "stage")} required={true}>
        <SelectFieldOptions selected={mat.stage()} items={vnode.state.stages()}/>
      </SelectField>,
      this.advanced(mat, vnode.attrs),
    ];
  }

  advanced(mat: DependencyMaterialAttributes, attrs: Attrs): m.Children {
    const showLocalWorkingCopyOptions: boolean = attrs.showLocalWorkingCopyOptions;
    if (showLocalWorkingCopyOptions) {
      return <AdvancedSettings forceOpen={mat.errors().hasErrors("name")}>
        <TextField label="Material Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE} readonly={attrs.readonly}
                   placeholder="A human-friendly label for this material" property={mat.name}/>

        <SwitchBtn label="Do not schedule the pipeline when this material is updated"
                   helpText="When set to true, the pipeline will not be automatically scheduled for changes to this material."
                   docLink={"/configuration/configuration_reference.html#pipeline-1"}
                   dataTestId="material-ignore-for-scheduling"
                   small={true}
                   css={styles}
                   disabled={attrs.readonly}
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
  readonly?: boolean;
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
  private pkgCreatePath                = SparkRoutes.packageRepositoriesSPA();

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
        this.resetErrorAndWarningFields(vnode);
        vnode.state.pkgRepoId(selectedPkgRepo.repoId());
        vnode.state.pkgs(this.mapAndConcatPackages(selectedPkgRepo));
        this.setErrorIfPluginNotPresent(vnode, selectedPkgRepo);
        this.setCreatePackagePath(selectedPkgRepo);
      }
    }
  }

  view(vnode: m.Vnode<PackageAttrs, PackageState>): m.Children | void | null {
    const attrs                                = vnode.attrs.material.attributes() as PackageMaterialAttributes;
    const packageRepos: Array<Option | string> = [{id: "", text: "Select a package repository"}];
    packageRepos.push(...vnode.attrs.packageRepositories.map((packageRepo) => {
      return {id: packageRepo.repoId(), text: packageRepo.name()};
    }));
    const readonly = !!vnode.attrs.disabled || vnode.attrs.readonly;
    this.setErrorMessageIfApplicable(vnode, packageRepos);

    let message;
    if (!_.isEmpty(vnode.attrs.pluginInfos)) {
      message = <span data-test-id="package-repo-msg"><Link href={SparkRoutes.packageRepositoriesSPA()}>Create New</Link> or select existing.</span>;
    }

    const pkgMessage = vnode.state.pkgRepoId() && !this.disablePkgField
      ? <span data-test-id="package-msg"><Link href={this.pkgCreatePath}>Create New</Link> or select existing.</span>
      : undefined;
    return [
      this.errorMessage,

      <div className={styles.selectionContainer}>
        <SelectField property={this.packageRepoProxy.bind(this, vnode)}
                     label="Package Repository"
                     errorText={attrs.errors().errorsForDisplay("pkgRepo")}
                     required={true} readonly={readonly || this.disablePkgRepoField}>
          <SelectFieldOptions selected={vnode.state.pkgRepoId()} items={packageRepos}/>
        </SelectField>
        <div className={styles.message}>
          {message}
        </div>
      </div>,

      this.showSelectedPkgRepoConfig(vnode),

      <div className={styles.selectionContainer}>
        <SelectField property={attrs.ref} label="Package" required={true}
                     errorText={attrs.errors().errorsForDisplay("ref")}
                     readonly={readonly || this.disablePkgField}>
          <SelectFieldOptions selected={attrs.ref()} items={vnode.state.pkgs()}/>
        </SelectField>
        <div className={styles.message}>
          {pkgMessage}
        </div>
      </div>,

      this.showSelectedPkgConfig(vnode)
    ];
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
    this.setCreatePackagePath(selectedPkgRepo);
    this.resetErrorAndWarningFields(vnode);
    this.setErrorIfPluginNotPresent(vnode, selectedPkgRepo);

    return pkgRepoId;
  }

  private setCreatePackagePath(selectedPkgRepo: PackageRepository) {
    this.pkgCreatePath = SparkRoutes.packageRepositoriesSPA() + `#!${selectedPkgRepo.name()}/create-package`;
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
      return <ConfigurationDetailsWidget header={"Package Repository Configuration"}
                                         dataTestId={"selected-pkg-repo-details"}
                                         data={selectedPkgRepo.configuration().asMap()}/>;
    }
  }

  private showSelectedPkgConfig(vnode: m.Vnode<PackageAttrs, PackageState>) {
    const selectedPkgRepo = vnode.attrs.packageRepositories.find((pkgRepo) => pkgRepo.repoId() === vnode.state.pkgRepoId());
    if (selectedPkgRepo !== undefined) {
      const attrs       = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
      const selectedPkg = selectedPkgRepo.packages().find((pkg) => pkg.id() === attrs.ref());
      if (selectedPkg !== undefined) {
        const pkgProperties = selectedPkg.configuration() ? selectedPkg.configuration()!.asMap() : [];
        const pkgDetails    = new Map([
                                        ["Auto Update", selectedPkg.autoUpdate() + ""],
                                        ...Array.from(pkgProperties)
                                      ]);
        return <ConfigurationDetailsWidget header={"Package Configuration"}
                                           dataTestId={"selected-pkg-details"}
                                           data={pkgDetails}/>;
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
  readonly?: boolean;
}

interface PluginState {
  pluginId: Stream<string>;
  scmsForSelectedPlugin: Stream<Option[]>;
}

export class PluginFields extends MithrilComponent<PluginAttrs, PluginState> {
  readonly defaultScms                = [{id: "", text: "Select a scm"}];
  private disableScmField: boolean    = true;
  private errorMessage?: m.Children   = undefined;
  private filterValue: Stream<string> = Stream("");

  oninit(vnode: m.Vnode<PluginAttrs, PluginState>): any {
    vnode.state.pluginId              = Stream("");
    vnode.state.scmsForSelectedPlugin = Stream(this.defaultScms);

    const attrs = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
    if (attrs.filter() === undefined) {
      attrs.filter(new Filter([]));
    }
    this.filterValue(attrs.filter().ignore().join(','));
    if (attrs.ref()) {
      const selectedScm = vnode.attrs.scms.find((scm) => scm.id() === attrs.ref());
      if (selectedScm !== undefined) {
        const pluginId = selectedScm.pluginMetadata().id();
        vnode.state.pluginId(pluginId);
        vnode.state.scmsForSelectedPlugin(this.mapAndConcatScms(vnode, pluginId));
        this.disableScmField = false;
      }
    }
  }

  view(vnode: m.Vnode<PluginAttrs, PluginState>): m.Children | void | null {
    const attrs                           = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
    const plugins: Array<Option | string> = [{id: "", text: "Select a plugin"}];
    plugins.push(..._.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    }));
    const readonly = !!vnode.attrs.disabled || vnode.attrs.readonly;
    this.setErrorMessageIfApplicable(vnode);

    let message;
    if (!_.isEmpty(vnode.attrs.pluginInfos)) {
      message = <span data-test-id="plugin-msg"><Link href={SparkRoutes.pluggableScmSPA()}>Create New</Link> or select existing scms below.</span>;
    }

    return [
      this.errorMessage,
      <SelectField property={this.pluginIdProxy.bind(this, vnode)}
                   label="SCM Plugin"
                   errorText={attrs.errors().errorsForDisplay("pluginId")}
                   required={true} readonly={readonly}>
        <SelectFieldOptions selected={vnode.state.pluginId()} items={plugins}/>
      </SelectField>,

      <div className={styles.selectionContainer}>
        <SelectField property={attrs.ref} label="SCM" required={true}
                     errorText={attrs.errors().errorsForDisplay("ref")}
                     readonly={readonly || this.disableScmField}>
          <SelectFieldOptions selected={attrs.ref()} items={vnode.state.scmsForSelectedPlugin()}/>
        </SelectField>
        <div className={styles.message}>
          {message}
        </div>
      </div>,

      this.showSelectedScmConfig(vnode),

      this.advanced(attrs, vnode.attrs)
    ];
  }

  private pluginIdProxy(vnode: m.Vnode<PluginAttrs, PluginState>, pluginId?: string): any {
    if (!pluginId) {
      return vnode.state.pluginId();
    }

    vnode.state.pluginId(pluginId);
    const scms = this.mapAndConcatScms(vnode, pluginId);
    vnode.state.scmsForSelectedPlugin(scms);
    (vnode.attrs.material.attributes() as PluggableScmMaterialAttributes).ref("");
    this.disableScmField = false;

    return pluginId;
  }

  private mapAndConcatScms(vnode: m.Vnode<PluginAttrs, PluginState>, pluginId: string) {
    return this.defaultScms.concat(...vnode.attrs.scms
                                           .filter((scm) => scm.pluginMetadata().id() === pluginId)
                                           .map((scm) => {
                                             return {id: scm.id(), text: scm.name()};
                                           }));
  }

  private advanced(attrs: PluggableScmMaterialAttributes, vnodeAttrs: PluginAttrs): m.Children {
    const showLocalWorkingCopyOptions = !!vnodeAttrs.showLocalWorkingCopyOptions;
    if (showLocalWorkingCopyOptions) {
      const labelForDestination = [
        "Alternate Checkout Path",
        " ",
        <Tooltip.Help size={TooltipSize.medium} content={DESTINATION_DIR_HELP_MESSAGE}/>
      ];
      const forceOpen           = attrs.errors().hasErrors("name") || attrs.errors().hasErrors("destination");
      return <AdvancedSettings forceOpen={forceOpen}>
        <TextField label={labelForDestination} property={attrs.destination} readonly={vnodeAttrs.readonly}
                   errorText={attrs.errors().errorsForDisplay("destination")}/>
        <TextField label="Denylist" helpText={DENYLIST_HELP_MESSAGE} readonly={vnodeAttrs.readonly}
                   property={this.filterValue} onchange={this.filterProxy.bind(this, attrs)}
                   errorText={attrs.errors().errorsForDisplay("filter")}/>
        <CheckboxField property={attrs.invertFilter} readonly={vnodeAttrs.readonly}
                       label="Invert the file filter, e.g. a Denylist becomes an Allowlist instead."
                       errorText={attrs.errors().errorsForDisplay("invertFilter")}/>
      </AdvancedSettings>;
    }
  }

  private filterProxy(attrs: PluggableScmMaterialAttributes) {
    const filters = this.filterValue().split(',')
                        .map((val) => val.trim())
                        .filter((val) => val.length > 0);
    attrs.filter().ignore(filters);

  }

  private setErrorMessageIfApplicable(vnode: m.Vnode<PluginAttrs, PluginState>) {
    this.errorMessage = undefined;
    if (vnode.state.pluginId().length > 0 && vnode.state.scmsForSelectedPlugin().length === 1) {
      this.errorMessage    = <FlashMessage type={MessageType.warning}>
        There are no SCMs configured for the selected plugin.
      </FlashMessage>;
      this.disableScmField = true;
    }
  }

  private showSelectedScmConfig(vnode: m.Vnode<PluginAttrs, PluginState>) {
    const attrs       = vnode.attrs.material.attributes() as PluggableScmMaterialAttributes;
    const selectedScm = vnode.attrs.scms.find((scm) => scm.id() === attrs.ref());
    if (selectedScm !== undefined) {
      const scmRepoDetails = selectedScm.configuration().asMap();
      return <ConfigurationDetailsWidget header={"SCM Configuration"} dataTestId={"selected-scm-details"}
                                         data={scmRepoDetails}/>;
    }
  }
}

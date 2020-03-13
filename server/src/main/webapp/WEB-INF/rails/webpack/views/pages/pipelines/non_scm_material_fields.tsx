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
import m from "mithril";
import Stream from "mithril/stream";
import {DependencyMaterialAutocomplete, PipelineNameCache} from "models/materials/dependency_autocomplete_cache";
import {DependencyMaterialAttributes, Material, MaterialAttributes} from "models/materials/types";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";
import styles from "./advanced_settings.scss";

interface Attrs {
  material: Material;
  cache: SuggestionCache;
  showLocalWorkingCopyOptions: boolean;
  disabled?: boolean;
}

interface State {
  provider: SuggestionProvider;
  stages: Stream<Option[]>;
}

// tslint:disable-next-line
export interface SuggestionCache extends PipelineNameCache<Awesomplete.Suggestion, Option> {}

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

    const mat = vnode.attrs.material.attributes() as DependencyMaterialAttributes;
    const cache = vnode.attrs.cache;
    const EMPTY: Option[] = [{id: "", text: "-"}];
    vnode.state.stages = Stream(EMPTY);

    vnode.state.provider = new DependencySuggestionProvider(vnode.attrs.cache);
    vnode.state.stages = mat.pipeline.map<Option[]>((val: string | undefined) => {
      mat.stage("");
      return val ? EMPTY.concat(cache.stages(val)) : [];
    });
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children {
    const mat = vnode.attrs.material.attributes() as DependencyMaterialAttributes;

    if (vnode.attrs.disabled) {
      return [
        <TextField label="Upstream Pipeline" property={mat.pipeline} errorText={this.errs(mat, "pipeline")} required={true} readonly={true}/>,
        <SelectField label="Upstream Stage" property={mat.stage} errorText={this.errs(mat, "stage")} required={true} readonly={true}>
          <SelectFieldOptions selected={mat.stage()} items={vnode.state.stages()}/>
        </SelectField>,
      ];
    }

    return [
      <AutocompleteField label="Upstream Pipeline" property={mat.pipeline} errorText={this.errs(mat, "pipeline")} required={true} maxItems={25} provider={vnode.state.provider}/>,
      <SelectField label="Upstream Stage" property={mat.stage} errorText={this.errs(mat, "stage")} required={true}>
        <SelectFieldOptions selected={mat.stage()} items={vnode.state.stages()}/>
      </SelectField>,
      this.advanced(mat, vnode.attrs.showLocalWorkingCopyOptions),
    ];
  }

  advanced(mat: DependencyMaterialAttributes, showLocalWorkingCopyOptions: boolean): m.Children {
    if (showLocalWorkingCopyOptions) {
      return <AdvancedSettings forceOpen={mat.errors().hasErrors("name")}>
        <TextField label="Material Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE} placeholder="A human-friendly label for this material" property={mat.name}/>

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

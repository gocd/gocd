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

import * as Awesomplete from "awesomplete";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {DependencyMaterialAutocomplete, PipelineNameCache} from "models/materials/dependency_autocomplete_cache";
import {DependencyMaterialAttributes, Material, MaterialAttributes} from "models/materials/types";
import {AutocompleteField, SuggestionProvider, SuggestionWriter} from "views/components/autocomplete/fields";
import {Option, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import * as css from "./components.scss";

interface Attrs {
  material: Material;
  cache: SuggestionCache;
}

//tslint:disable-next-line
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

  getData(setData: SuggestionWriter): void {
    if (!this.cache.ready()) {
      this.cache.prime(() => {
        setData(this.cache.pipelines());
      });
    } else {
      setData(this.cache.pipelines());
    }
  }
}

export class DependencyFields extends MithrilViewComponent<Attrs> {
  private suggestions?: SuggestionProvider;
  private EMPTY: Option[] = [{id: "", text: "-"}];
  private stages: Stream<Option[]> = stream(this.EMPTY);

  oninit(vnode: m.Vnode<Attrs, {}>) {
    const mat = vnode.attrs.material.attributes() as DependencyMaterialAttributes;
    const cache = vnode.attrs.cache;

    this.suggestions = new DependencySuggestionProvider(vnode.attrs.cache);
    this.stages = mat.pipeline.map<Option[]>((val: string) => {
      mat.stage("");
      return val ? this.EMPTY.concat(cache.stages(val)) : [];
    });
  }

  view(vnode: m.Vnode<Attrs, {}>): m.Children {
    const mat = vnode.attrs.material.attributes() as DependencyMaterialAttributes;
    return [
      <AutocompleteField label="Upstream Pipeline" property={mat.pipeline} errorText={this.errs(mat, "pipeline")} required={true} maxItems={25} css={css} provider={this.suggestions!}/>,
      <SelectField label="Upstream Stage" property={mat.stage} errorText={this.errs(mat, "stage")} required={true}>
        <SelectFieldOptions selected={mat.stage()} items={this.stages()}/>
      </SelectField>,
      <AdvancedSettings>
        <TextField label="Material Name" placeholder="A human-friendly label for this material" property={mat.name}/>
      </AdvancedSettings>
    ];
  }

  errs(attrs: MaterialAttributes, key: string): string {
    return attrs.errors().errorsForDisplay(key);
  }
}

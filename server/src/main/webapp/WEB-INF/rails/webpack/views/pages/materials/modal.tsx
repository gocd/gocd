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
import {MaterialModifications, MaterialWithFingerprint} from "models/materials/materials";
import {Modal, Size} from "views/components/modal";
import styles from "./index.scss";
import {MaterialWidget} from "./material_widget";

export class ShowModificationsModal extends Modal {
  private material: MaterialWithFingerprint;
  private modifications: MaterialModifications;

  constructor(material: MaterialWithFingerprint, modifications: MaterialModifications) {
    super(Size.large);
    this.material      = material;
    this.modifications = modifications;
  }

  body(): m.Children {
    return <ShowModificationsWidget material={this.material} modifications={this.modifications}/>;
  }

  title(): string {
    return `Show Modifications for '${this.material.displayName() || this.material.typeForDisplay()}'`;
  }
}

interface Attrs {
  material: MaterialWithFingerprint;
  modifications: MaterialModifications;
}

class ShowModificationsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    return <div data-test-id="modifications-widget">
      {vnode.attrs.modifications.map((mod, index) => {
        return <div data-test-id={`modification-${index}`} class={styles.modification}>
          {MaterialWidget.showLatestModificationDetails(mod)}
        </div>;
      })}
    </div>;
  }

}

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
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  MaterialAttributes,
  P4MaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {CheckboxField, PasswordField, TextField} from "views/components/forms/input_fields";
import {TestConnection} from "views/components/materials/test_connection";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";

interface Attrs {
  material: Material;
}

abstract class ScmFields extends MithrilViewComponent<Attrs> {
  errs(attrs: MaterialAttributes, key: string): string {
    return attrs.errors().errorsForDisplay(key);
  }

  view(vnode: m.Vnode<Attrs>): m.Children {
    const mattrs = vnode.attrs.material.attributes();
    return [
      this.requiredFields(mattrs),
      <TestConnection material={vnode.attrs.material}/>,
      <AdvancedSettings>
        {this.extraFields(mattrs)}
        <TextField label="Alternate Checkout Path" property={mattrs.destination}/>
        <TextField label="Material Name" placeholder="A human-friendly label for this material" property={mattrs.name}/>
      </AdvancedSettings>
    ];
  }

  abstract requiredFields(attrs: MaterialAttributes): m.Children;
  abstract extraFields(attrs: MaterialAttributes): m.Children;
}

export class GitFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as GitMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as GitMaterialAttributes;
    return [
      <TextField label="Repository Branch" property={mat.branch} placeholder="master"/>,
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
    ];
  }
}

export class HgFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as HgMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as HgMaterialAttributes;
    return [
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
    ];
  }
}

export class SvnFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as SvnMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as SvnMaterialAttributes;
    return [
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
      <CheckboxField label="Check Externals" property={mat.checkExternals}/>,
    ];
  }
}

export class P4Fields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as P4MaterialAttributes;
    return [
      <TextField label="P4 [Protocol:][Host:]Port" property={mat.port} errorText={this.errs(attrs, "port")} required={true}/>,
      <TextField label="P4 View" property={mat.view} errorText={this.errs(attrs, "view")} required={true}/>,
    ];
  }

  extraFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as P4MaterialAttributes;
    return [
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
      <CheckboxField label="Use Ticket Authentication" property={mat.useTickets}/>,
    ];
  }
}

export class TfsFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as TfsMaterialAttributes;
    return [
      <TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>,
      <TextField label="Project Path" property={mat.projectPath} errorText={this.errs(attrs, "projectPath")} required={true}/>,
      <TextField label="Username" property={mat.username} errorText={this.errs(attrs, "username")} required={true}/>,
      <PasswordField label="Password" property={mat.password} errorText={this.errs(attrs, "password")} required={true}/>,
    ];
  }

  extraFields(attrs: MaterialAttributes): m.Children {
    const mat = attrs as TfsMaterialAttributes;
    return [<TextField label="Domain" property={mat.domain}/>];
  }
}

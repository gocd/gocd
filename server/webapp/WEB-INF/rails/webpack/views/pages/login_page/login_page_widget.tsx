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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import * as s from "underscore.string";
import {Primary} from "views/components/buttons";
import {Form, FormBody, FormHeader} from "views/components/forms/form";
import {SimplePasswordField, TextField} from "views/components/forms/input_fields";
import * as styles from "./login_page_widget.scss";

//tslint:disable-next-line
export interface Credentials extends ValidatableMixin {
}

export class Credentials extends ValidatableMixin {
  readonly username: Stream<string>;
  readonly password: Stream<string>;

  constructor() {
    super();
    this.username = stream();
    this.password = stream();

    ValidatableMixin.call(this);
    this.validatePresenceOf("username");
    this.validatePresenceOf("password");
  }
}

type AuthPluginInfoWithCredentials =
  AuthPluginInfo
  & { credentials: Credentials, submitCallback: (credentials: Credentials) => void };

class LoginFormWidget extends MithrilViewComponent<AuthPluginInfoWithCredentials> {

  view(vnode: m.Vnode<AuthPluginInfoWithCredentials, this>) {
    return (
      <div onkeypress={(event: KeyboardEvent) => {
        const keycode = (event.keyCode ? event.keyCode : event.which);
        if (event.key === "Enter" || keycode === 13) {
          vnode.attrs.submitCallback(vnode.attrs.credentials);
        }
      }}>
        <FormBody>
          <TextField property={vnode.attrs.credentials.username}
                     label={"Username:"}
                     hideRequiredAsterix={true}
                     required={true}
                     errorText={vnode.attrs.credentials.errors().errorsForDisplay("username")}/>
          <SimplePasswordField property={vnode.attrs.credentials.password}
                               label={"Password:"}
                               required={true}
                               hideRequiredAsterix={true}
                               errorText={vnode.attrs.credentials.errors().errorsForDisplay("password")}/>
          <div class={styles.loginButtonWrapper}>
            <Primary onclick={() => {
              vnode.attrs.submitCallback(vnode.attrs.credentials);
            }}>Sign in</Primary>
          </div>
        </FormBody>
      </div>
    );
  }
}

class ShowAllWebBasedPluginLinks extends MithrilViewComponent<AuthPluginInfo> {
  view(vnode: m.Vnode<AuthPluginInfo, this>): m.Children | void | null {
    return (
      <div>
        <p>Login using one of the login methods:</p>
        <p>
          {vnode.attrs.webBasedPlugins.map((eachPlugin) => {
            return (
              <a
                className={styles.webLoginLink}
                data-test-id={s.slugify(`link to login using ${eachPlugin.pluginName}`)}
                title={`Login using ${eachPlugin.pluginName}`}
                href={eachPlugin.redirectUrl}>
                <img
                  data-test-id={s.slugify(`image for ${eachPlugin.pluginName}`)}
                  width="50px"
                  height="50px"
                  alt={`Login using ${eachPlugin.pluginName}`}
                  src={eachPlugin.imageUrl}/>
              </a>
            );
          })}
        </p>
      </div>
    );
  }
}

class ShowRedirectToDefaultPlugin extends MithrilComponent<AuthPluginInfo> {
  oninit(vnode: m.Vnode<AuthPluginInfo, {}>): any {
    const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];

    if (!vnode.attrs.doNotAutoRedirect) {
      window.setTimeout(() => {
        window.location.href = defaultWebBasedPlugin.redirectUrl;
      }, 5000);
    }
  }

  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];

    return (
      <div>
        You will be automatically redirected to login using the
        plugin (
        <a
          className={styles.webLoginLink}
          data-test-id={s.slugify(`link to login using ${defaultWebBasedPlugin.pluginName}`)}
          title={`Login using ${defaultWebBasedPlugin.pluginName}`}
          href={defaultWebBasedPlugin.redirectUrl}>
          <img
            data-test-id={s.slugify(`image for ${defaultWebBasedPlugin.pluginName}`)}
            width="50px"
            height="50px"
            alt={`Login using ${defaultWebBasedPlugin.pluginName}`}
            src={defaultWebBasedPlugin.imageUrl}/>
        </a>
        )
      </div>
    );
  }
}

export class LoginPageWidget extends MithrilViewComponent<AuthPluginInfoWithCredentials> {
  view(vnode: m.Vnode<AuthPluginInfoWithCredentials, this>) {
    return (
      <FormHeader>
        <Form>
          {this.maybeShowLoginFormWidget(vnode)}
          {this.maybeShowLoginIcons(vnode)}
        </Form>
      </FormHeader>
    );
  }

  private maybeShowLoginFormWidget(vnode: m.Vnode<AuthPluginInfoWithCredentials, this>) {
    if (vnode.attrs.hasPasswordPlugins) {
      return <LoginFormWidget {...vnode.attrs}/>;
    }
  }

  private maybeShowLoginIcons(vnode: m.Vnode<AuthPluginInfoWithCredentials, this>) {
    if (vnode.attrs.hasWebBasedPlugins) {
      if (vnode.attrs.webBasedPlugins.length === 1 && !vnode.attrs.hasPasswordPlugins) {
        return <ShowRedirectToDefaultPlugin {...vnode.attrs} />;
      } else {
        return <ShowAllWebBasedPluginLinks {...vnode.attrs} />;
      }
    }
  }
}

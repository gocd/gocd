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

import {docsUrl} from "gen/gocd_version";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import s from "underscore.string";
import { v4 as uuid } from 'uuid';
import {FlashMessage, MessageType} from "views/components/flash_message";
import styles from "./login_page_widget.scss";

const gocdLogo = require("./gocd_logo.svg");

class LoginFormWidget extends MithrilViewComponent<AuthPluginInfo> {
  private readonly formID = `login-form-${uuid()}`;

  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    return (

      <div class={styles.loginUser}>

        <form method="post" action="/go/auth/security_check" data-form-id={this.formID}>
          <ul class={styles.loginFormFields}>
            <li><input type="text"
                       name="j_username"
                       placeholder="Username"
                       class={styles.loginInput}
                       autofocus={true}
                       autocomplete="off"
                       autocapitalize="off"
                       autocorrect="off"
                       spellcheck={false}
                       data-test-id="form-field-input-username"/></li>
            <li><input type="password"
                       name="j_password"
                       placeholder="Password"
                       class={styles.loginInput}
                       autocomplete="off"
                       autocapitalize="off"
                       autocorrect="off"
                       spellcheck={false}
                       data-test-id="form-field-input-password"
            /></li>

            <li>
              <button class={styles.loginButton} onclick={() => {
                (document.querySelector(`[data-test-id='${this.formID}']`)! as HTMLFormElement).submit();
              }}>Sign in
              </button>
            </li>
          </ul>
        </form>
      </div>
    );
  }
}

class ShowAllWebBasedPluginLinks extends MithrilViewComponent<AuthPluginInfo> {
  view(vnode: m.Vnode<AuthPluginInfo, this>): m.Children | void | null {
    const loginWithMessage = vnode.attrs.hasPasswordPlugins ? <p>Or login using:</p> : <p>Login using:</p>;
    return (
      <div class={styles.otherLoginMethods}>
        {loginWithMessage}
        <div class={styles.loginOptions}>
          {vnode.attrs.webBasedPlugins.map((eachPlugin) => {
            return (
              <a
                class={styles.webLoginLink}
                data-test-id={s.slugify(`link to login using ${eachPlugin.pluginName}`)}
                title={`Login using ${eachPlugin.pluginName}`}
                href={eachPlugin.redirectUrl}>
                <img
                  data-test-id={s.slugify(`image for ${eachPlugin.pluginName}`)}
                  width="64px"
                  height="64px"
                  alt={`Login using ${eachPlugin.pluginName}`}
                  src={eachPlugin.imageUrl}/>
              </a>
            );
          })}
        </div>
      </div>
    );
  }
}

class ShowRedirectToDefaultPlugin extends MithrilComponent<AuthPluginInfo> {
  oninit(vnode: m.Vnode<AuthPluginInfo, {}>): any {

    if (!vnode.attrs.doNotAutoRedirect) {
      window.setTimeout(() => {
        const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];
        window.location.href        = defaultWebBasedPlugin.redirectUrl;
      }, 5000);
    }
  }

  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    const defaultWebBasedPlugin = vnode.attrs.webBasedPlugins[0];

    return (
      <div class={styles.redirect}>
        <p>You will be automatically redirected to login using</p>
        <a
          class={styles.webLoginLink}
          data-test-id={s.slugify(`link to login using ${defaultWebBasedPlugin.pluginName}`)}
          title={`Login using ${defaultWebBasedPlugin.pluginName}`}
          href={defaultWebBasedPlugin.redirectUrl}>
          <img
            data-test-id={s.slugify(`image for ${defaultWebBasedPlugin.pluginName}`)}
            width="64px"
            height="64px"
            alt={`Login using ${defaultWebBasedPlugin.pluginName}`}
            src={defaultWebBasedPlugin.imageUrl}/>
        </a>
      </div>
    );
  }
}

export class LoginPageWidget extends MithrilViewComponent<AuthPluginInfo> {
  view(vnode: m.Vnode<AuthPluginInfo, this>) {
    return (
      <span>
      <div class={styles.errorBox}>
        {this.maybeLoginError(vnode)}
      </div>
        <div class={styles.loginForm}>
          <div class={styles.loginContainer}>
          <div class={styles.loginGraphics}>
            <img src={gocdLogo}/>
          </div>
          <div class={styles.loginMethods}>
            {this.maybeShowLoginFormWidget(vnode)}
            {this.maybeShowLoginIcons(vnode)}
          </div>
        </div>
      </div>
      </span>

    );
  }

  private maybeShowLoginFormWidget(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.hasPasswordPlugins) {
      return <LoginFormWidget {...vnode.attrs}/>;
    }
  }

  private maybeShowLoginIcons(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.hasWebBasedPlugins) {
      if (vnode.attrs.webBasedPlugins.length === 1 && !vnode.attrs.hasPasswordPlugins) {
        return <ShowRedirectToDefaultPlugin {...vnode.attrs} />;
      } else {
        return <ShowAllWebBasedPluginLinks {...vnode.attrs} />;
      }
    }
  }

  private maybeLoginError(vnode: m.Vnode<AuthPluginInfo, this>) {
    if (vnode.attrs.loginError) {
      return (
        <FlashMessage type={MessageType.alert}>
          {vnode.attrs.loginError.replace(/\.$/, "")}.
          See <a target="_blank" rel="noopener" href={docsUrl("/configuration/dev_authentication.html#common-errors")}>Help
          Topic: Authentication</a>.
        </FlashMessage>
      );
    }
  }
}

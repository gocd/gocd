/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import * as s from "underscore.string";
import * as uuid from "uuid/v4";
import {OnClickHandler} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {EncryptedValue} from "views/components/forms/encrypted_value";
import {SwitchBtn} from "views/components/switch";
import * as styles from "./forms.scss";

const classnames = bind(styles);

interface RequiredFieldAttr {
  required?: boolean;
}

interface LabelAttr extends RequiredFieldAttr {
  label?: string;
}

interface ErrorTextAttr {
  errorText?: string;
}

interface HelpTextAttr {
  helpText?: string;
}

interface PlaceholderAttr {
  placeholder?: string;
}

interface DataTestIdAttr {
  dataTestId?: string;
}

interface ReadonlyAttr {
  readonly?: boolean;
}

interface SmallSizeAttr {
  small?: boolean;
}

interface BindingsAttr<T> {
  onchange?: (evt: any) => void;
  property: (newValue?: T) => T;
}

type BaseAttrs<T> = DataTestIdAttr & HelpTextAttr & ErrorTextAttr & LabelAttr & BindingsAttr<T> & ReadonlyAttr;

interface DefaultAttrs {
  [key: string]: string | boolean;
}

const textInputFieldDefaultAttrs = {
  autocomplete: "off",
  autocapitalize: "off",
  autocorrect: "off",
  spellcheck: false
};

interface FormResetButtonAttrs {
  onclick?: (e: MouseEvent) => void;
}

export interface Option {
  id: string;
  text: string;
}

interface SelectFieldAttrs {
  items: Array<Option | string>;
  selected?: string;
}

type HelpTextComponentAttrs = HelpTextAttr & { helpTextId: string };
type ErrorTextComponentAttrs = ErrorTextAttr & { errorId: string };
type LabelComponentAttrs = LabelAttr & { fieldId: string };

class RequiredLabel extends MithrilViewComponent<RequiredFieldAttr> {

  static isRequiredField(attrs: RequiredFieldAttr) {
    return attrs.required;
  }

  static defaultAttributes(attrs: RequiredFieldAttr) {
    if (this.isRequiredField(attrs)) {
      return {
        "aria-required": true,
        "required": true
      };
    }
  }

  view(vnode: m.Vnode<RequiredFieldAttr>) {
    if (RequiredLabel.isRequiredField(vnode.attrs)) {
      return (<span className={styles.formLabelRequired}>*</span>);
    }
  }

}

class Label extends MithrilViewComponent<LabelComponentAttrs> {

  static hasLabelText(attrs: LabelAttr) {
    return !_.isEmpty(attrs.label);
  }

  static defaultAttributes(attrs: LabelAttr) {
    if (this.hasLabelText(attrs)) {
      return {
        "aria-label": attrs.label as string,
      };
    }
  }

  view(vnode: m.Vnode<LabelComponentAttrs>) {
    if (Label.hasLabelText(vnode.attrs)) {
      return <label for={vnode.attrs.fieldId}
                    data-test-id={`form-field-label-${s.slugify(vnode.attrs.label as string)}`}
                    className={classnames(styles.formLabel)}>
        {vnode.attrs.label}
        <RequiredLabel {...vnode.attrs} />
      </label>;
    }
  }

}

class HelpText extends MithrilViewComponent<HelpTextComponentAttrs> {
  static hasHelpText(attrs: HelpTextComponentAttrs) {
    return !_.isEmpty(attrs.helpText);
  }

  static defaultAttributes(attrs: HelpTextComponentAttrs) {
    if (HelpText.hasHelpText(attrs)) {
      return {"aria-describedby": attrs.helpTextId};
    }
  }

  view(vnode: m.Vnode<HelpTextComponentAttrs>) {
    if (!_.isEmpty(vnode.attrs.helpText)) {
      return (<span id={vnode.attrs.helpTextId} className={classnames(styles.formHelp)}>{vnode.attrs.helpText}</span>);
    }
  }

}

class ErrorText extends MithrilViewComponent<ErrorTextComponentAttrs> {

  static hasErrorText(attrs: ErrorTextAttr) {
    return !_.isEmpty(attrs.errorText);
  }

  static defaultAttributes(attrs: ErrorTextComponentAttrs) {
    if (ErrorText.hasErrorText(attrs)) {
      return {"aria-errormessage": attrs.errorId};
    }
  }

  view(vnode: m.Vnode<ErrorTextComponentAttrs>) {
    if (ErrorText.hasErrorText(vnode.attrs)) {
      return <span className={styles.formErrorText} id={vnode.attrs.errorId}>{vnode.attrs.errorText}</span>;
    }
  }
}

class FormResetButton extends MithrilViewComponent<FormResetButtonAttrs> {

  view(vnode: m.Vnode<FormResetButtonAttrs>): any {
    return <div class={styles.formInputReset}>
      <Buttons.Reset small={true} onclick={vnode.attrs.onclick}>{vnode.children}</Buttons.Reset>
    </div>;
  }
}

type EventName = "oninput" | "onchange";

function bindingAttributes<T>(attrs: BindingsAttr<T> & ReadonlyAttr,
                              eventName: EventName,
                              propertyAttribute: string) {
  const bindingAttributes: any = {
    [propertyAttribute]: attrs.property()
  };

  if (!attrs.readonly) {
    bindingAttributes[eventName] = (evt: any) => {
      attrs.property(evt.currentTarget[propertyAttribute]);
      if (attrs.onchange) {
        attrs.onchange(evt);
      }
    };
  }

  return bindingAttributes;
}

function defaultAttributes<T, V>(attrs: BaseAttrs<T> & V,
                                 id: string,
                                 helpTextId: string,
                                 errorId: string): DefaultAttrs {
  const required = RequiredLabel.isRequiredField(attrs as RequiredFieldAttr);

  const defaultAttrs: DefaultAttrs = {
    "readonly": !!(attrs.readonly),
    "disabled": !!(attrs.readonly),
    "required": !!required,
    "id": id,
    "data-test-id": `form-field-input-${s.slugify(attrs.label as string)}`
  };

  if (attrs.dataTestId) {
    defaultAttrs["data-test-id"] = attrs.dataTestId as string;
  }

  return _.assign(defaultAttrs,
                  RequiredLabel.defaultAttributes(attrs),
                  Label.defaultAttributes(attrs),
                  HelpText.defaultAttributes({helpTextId, ...attrs}),
                  ErrorText.defaultAttributes({errorId, ...attrs})
  );
}

abstract class FormField<T, V = {}> extends MithrilViewComponent<BaseAttrs<T> & V> {
  protected readonly id: string         = `input-${uuid()}`;
  protected readonly helpTextId: string = `${this.id}-help-text`;
  protected readonly errorId: string    = `${this.id}-error-text`;

  view(vnode: m.Vnode<BaseAttrs<T> & V>) {
    return (
      <li className={classnames(styles.formGroup,
                                {[styles.formHasError]: ErrorText.hasErrorText(vnode.attrs)})}>
        <Label {...vnode.attrs} fieldId={this.id}/>
        {this.renderInputField(vnode)}
        <ErrorText {...vnode.attrs} errorId={this.errorId}/>
        <HelpText {...vnode.attrs} helpTextId={this.helpTextId}/>
      </li>
    );
  }

  protected defaultAttributes(attrs: BaseAttrs<T> & V): DefaultAttrs {
    return defaultAttributes(attrs, this.id, this.helpTextId, this.errorId);
  }

  protected bindingAttributes(attrs: BaseAttrs<T>,
                              eventName: EventName,
                              propertyAttribute: string): any {
    return bindingAttributes(attrs, eventName, propertyAttribute);
  }

  protected abstract renderInputField(vnode: m.Vnode<BaseAttrs<T> & V>): m.Children;
}

export class TextField extends FormField<string, RequiredFieldAttr & PlaceholderAttr> {

  renderInputField(vnode: m.Vnode<BaseAttrs<string> & RequiredFieldAttr & PlaceholderAttr>) {
    return (
      <input type="text"
             className={classnames(styles.formControl)}
             {...this.defaultAttributes(vnode.attrs)}
             {...this.bindingAttributes(vnode.attrs, "oninput", "value")}
      />
    );
  }

  protected defaultAttributes(attrs: BaseAttrs<string> & RequiredFieldAttr & PlaceholderAttr) {
    const defaultAttributes = super.defaultAttributes(attrs);
    if (!_.isEmpty(attrs.placeholder)) {
      defaultAttributes.placeholder = attrs.placeholder as string;
    }

    return _.assign(defaultAttributes, textInputFieldDefaultAttrs);
  }
}

export class TextAreaField extends TextField {
  renderInputField(vnode: m.Vnode<BaseAttrs<string> & RequiredFieldAttr & PlaceholderAttr>) {

    return (
      <textarea
        className={classnames(styles.formControl, styles.textArea)}
        {...this.defaultAttributes(vnode.attrs)}
        oninput={(e) => {
          vnode.attrs.property((e.target as HTMLTextAreaElement).value);
          if (vnode.attrs.onchange) {
            vnode.attrs.onchange(e);
          }
        }}>{vnode.attrs.property()}</textarea>
    );
  }

}

export class PasswordField extends FormField<EncryptedValue, RequiredFieldAttr & PlaceholderAttr> {

  renderInputField(vnode: m.Vnode<BaseAttrs<EncryptedValue> & RequiredFieldAttr & PlaceholderAttr>) {
    const input = <input type="password"
                         className={classnames(styles.formControl, styles.inline)}
                         {...this.defaultAttributes(vnode.attrs)}
                         {...this.bindingAttributes(vnode.attrs, "oninput", "value")}/>;

    return [input, PasswordField.resetOrOverride(vnode)];
  }

  protected defaultAttributes(attrs: BaseAttrs<EncryptedValue> & RequiredFieldAttr & PlaceholderAttr): any {
    return _.assign(super.defaultAttributes(attrs), textInputFieldDefaultAttrs, {
      readonly: !attrs.property().isEditing()
    });
  }

  protected bindingAttributes(attrs: BaseAttrs<EncryptedValue>,
                              eventName: string,
                              propertyAttribute: string): any {
    if (attrs.property().isEditing()) {
      return {
        [eventName]: (evt: any) => attrs.property().value(evt.currentTarget.value),
        [propertyAttribute]: attrs.property().value()
      };
    } else {
      return {
        value: "************"
      };
    }

  }

  private static resetOrOverride(vnode: m.Vnode<BaseAttrs<EncryptedValue> & RequiredFieldAttr & PlaceholderAttr>) {
    if (vnode.attrs.property().isEditing()) {
      return <FormResetButton
        onclick={vnode.attrs.property().resetToOriginal.bind(vnode.attrs.property())}>Reset</FormResetButton>;
    } else {
      return <FormResetButton
        onclick={vnode.attrs.property().edit.bind(vnode.attrs.property())}>Change</FormResetButton>;
    }
  }
}

export class SearchField extends FormField<string, PlaceholderAttr> {
  view(vnode: m.Vnode<BindingsAttr<string> & PlaceholderAttr>): any {
    return (
      <span className={classnames(styles.searchBoxWrapper)}>
      <input type="search"
             className={classnames(styles.formControl, styles.searchBoxInput)}
             {...this.defaultAttributes(vnode.attrs)}
             {...this.bindingAttributes(vnode.attrs, "oninput", "value")}/>
      </span>
    );
  }

  protected renderInputField(vnode: m.Vnode<BaseAttrs<string> & PlaceholderAttr>): m.Children {
    throw new Error("unsupported!");
  }
}

export class CheckboxField extends FormField<boolean> {
  view(vnode: m.Vnode<BaseAttrs<boolean>>) {
    return (
      <li className={classnames(styles.formGroup,
                                {[styles.formHasError]: ErrorText.hasErrorText(vnode.attrs as ErrorTextAttr)})}>
        <div className={styles.formCheck}>
          {this.renderInputField(vnode)}
          <Label {...vnode.attrs as LabelAttr} fieldId={this.id}/>
          <ErrorText {...vnode.attrs as ErrorTextAttr} errorId={this.errorId}/>
          <HelpText {...vnode.attrs as HelpTextAttr} helpTextId={this.helpTextId}/>
        </div>
      </li>
    );
  }

  renderInputField(vnode: m.Vnode<BaseAttrs<boolean>>) {
    return (
      <input type="checkbox"
             {...this.defaultAttributes(vnode.attrs)}
             {...this.bindingAttributes(vnode.attrs, "onchange", "checked")}
             className={classnames(styles.formCheckInput)}/>);
  }
}

export class TriStateCheckboxField extends FormField<TriStateCheckbox> {
  view(vnode: m.Vnode<BaseAttrs<TriStateCheckbox>>) {
    return (
      <li className={classnames(styles.formGroup,
                                {[styles.formHasError]: ErrorText.hasErrorText(vnode.attrs as ErrorTextAttr)})}>
        <div className={styles.formCheck}>
          {this.renderInputField(vnode)}
          <Label {...vnode.attrs as LabelAttr} fieldId={this.id}/>
          <ErrorText {...vnode.attrs as ErrorTextAttr} errorId={this.errorId}/>
          <HelpText {...vnode.attrs as HelpTextAttr} helpTextId={this.helpTextId}/>
        </div>
      </li>
    );
  }

  renderInputField(vnode: m.Vnode<BaseAttrs<TriStateCheckbox>>) {
    return (
      <input type="checkbox"
             {...this.defaultAttributes(vnode.attrs)}
             {...this.bindingAttributes(vnode.attrs, "onchange", "checked")}
             className={classnames(styles.formCheckInput)}/>);
  }

  protected bindingAttributes(attrs: BaseAttrs<TriStateCheckbox>,
                              eventName: string,
                              propertyAttribute: string): any {
    const triStateCheckbox = attrs.property();

    const bindingAttributes: any = {
      [propertyAttribute]: triStateCheckbox.isChecked(),
      indeterminate: triStateCheckbox.isIndeterminate()
    };

    if (!attrs.readonly) {
      bindingAttributes[eventName] = (evt: any) => {
        triStateCheckbox.click();
        if (attrs.onchange) {
          attrs.onchange(evt);
        }
      };
    }

    return bindingAttributes;
  }
}

export class Switch extends FormField<boolean, SmallSizeAttr> {
  protected renderInputField(vnode: m.Vnode<BaseAttrs<boolean> & SmallSizeAttr>): m.Children {
    return <SwitchBtn small={vnode.attrs.small} field={vnode.attrs.property}/>;
  }
}

export class SelectField extends FormField<string, RequiredFieldAttr> {
  renderInputField(vnode: m.Vnode<BaseAttrs<string> & RequiredFieldAttr>) {
    return (
      <select
        class={styles.formControl}
        {...this.defaultAttributes(vnode.attrs)}
        onchange={(e) => {
          vnode.attrs.property((e.target as HTMLInputElement).value);
          if (vnode.attrs.onchange) {
            vnode.attrs.onchange(e);
          }
        }}
        value={vnode.attrs.property()}>
        {vnode.children}
      </select>
    );
  }
}

export class SelectFieldOptions extends MithrilViewComponent<SelectFieldAttrs> {
  view(vnode: m.Vnode<SelectFieldAttrs>): m.Children {
    return _.map(vnode.attrs.items, (optionOrString: Option | string) => {
      let id: string;
      let text: string;
      if (typeof optionOrString === "string") {
        id   = optionOrString as string;
        text = optionOrString as string;
      } else {
        id   = (optionOrString as Option).id;
        text = (optionOrString as Option).text;
      }

      return <option key={id}
                     value={id}
                     selected={vnode.attrs.selected === id}>{text}</option>;
    });
  }
}

interface ButtonDisableReason {
  buttonDisableReason: string;
}

type QuickAddFieldAttrs =
  PlaceholderAttr
  & OnClickHandler
  & BindingsAttr<string>
  & ReadonlyAttr
  & DataTestIdAttr
  & ButtonDisableReason;

export class QuickAddField extends MithrilViewComponent<QuickAddFieldAttrs> {
  protected readonly id: string         = `input-${uuid()}`;
  protected readonly helpTextId: string = `${this.id}-help-text`;
  protected readonly errorId: string    = `${this.id}-error-text`;

  view(vnode: m.Vnode<QuickAddFieldAttrs>) {

    const defaultAttrs: DefaultAttrs = {};

    if (vnode.attrs.dataTestId) {
      defaultAttrs["data-test-id"] = vnode.attrs.dataTestId;
    }

    return (
      <li className={classnames(styles.formGroup, styles.formGroupTextFieldWithButton)} {...defaultAttrs}>
        {this.renderInputField(vnode)}
        {this.renderButton(vnode)}
      </li>
    );
  }

  protected defaultAttributes(attrs: QuickAddFieldAttrs): DefaultAttrs {
    const result = defaultAttributes(attrs, this.id, this.helpTextId, this.errorId);
    if (!_.isEmpty(attrs.placeholder)) {
      result.placeholder = attrs.placeholder as string;
    }

    delete result["data-test-id"];

    return _.assign(result, textInputFieldDefaultAttrs);
  }

  protected renderButton(vnode: m.Vnode<QuickAddFieldAttrs>): m.Children {
    const btnAttrs = this.btnAttrs(vnode);
    return <button onclick={vnode.attrs.onclick}
                   className={classnames(styles.quickAddButton)} {...btnAttrs}>
      Add
    </button>;
  }

  protected renderInputField(vnode: m.Vnode<QuickAddFieldAttrs>): m.Children {
    return <input type="text"
                  className={classnames(styles.formControl)}
                  {...this.defaultAttributes(vnode.attrs)}
                  {...bindingAttributes(vnode.attrs, "oninput", "value")}/>;
  }

  protected btnAttrs(vnode: m.Vnode<QuickAddFieldAttrs>): DefaultAttrs {
    const btnAttrs: DefaultAttrs = {};

    if (_.isEmpty(vnode.attrs.property())) {
      btnAttrs.disabled = true;
      btnAttrs.title    = vnode.attrs.buttonDisableReason;
    }
    return btnAttrs;
  }
}

export class SearchFieldWithButton extends QuickAddField {
  protected renderButton(vnode: m.Vnode<QuickAddFieldAttrs>) {
    const btnAttrs = this.btnAttrs(vnode);

    return <button onclick={vnode.attrs.onclick}
                   className={classnames(styles.searchButton)} {...btnAttrs} >
      Search
    </button>;
  }

  protected renderInputField(vnode: m.Vnode<QuickAddFieldAttrs>) {
    return <span className={classnames(styles.searchBoxWrapper)}>
      <input type="search"
             className={classnames(styles.formControl, styles.searchBoxInput)}
             {...this.defaultAttributes(vnode.attrs)}
             {...bindingAttributes(vnode.attrs, "oninput", "value")}/>
    </span>;
  }

}

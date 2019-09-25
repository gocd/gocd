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
import {EncryptedValue} from "views/components/forms/encrypted_value";

describe("EncryptedValue", () => {

  it("should be able to initialize itself from a clear text value", () => {
    const encryptedValue = new EncryptedValue({clearText: "password"});
    expect(encryptedValue.isPlain()).toBe(true);
    expect(encryptedValue.isSecure()).toBe(false);
    expect(encryptedValue.value()).toBe("password");
    expect(encryptedValue.isDirty()).toBe(false);
    expect(encryptedValue.isEditing()).toBe(true);
  });

  it("should be able to convert a clear text value into an encrypted value", () => {
    const encryptedValue = new EncryptedValue({clearText: "password"});
    encryptedValue.becomeSecure();

    expect(encryptedValue.isPlain()).toBe(false);
    expect(encryptedValue.isSecure()).toBe(true);
    expect(encryptedValue.value()).toBe("password");
    expect(encryptedValue.isEditing()).toBe(false);
  });

  it("should be able to initialize itself from a cipher text value", () => {
    const encryptedValue = new EncryptedValue({cipherText: "c!ph3rt3xt"});
    expect(encryptedValue.isPlain()).toBe(false);
    expect(encryptedValue.isSecure()).toBe(true);
    expect(encryptedValue.value()).toBe("c!ph3rt3xt");
    expect(encryptedValue.isEditing()).toBe(false);
  });

  it("should not initialize with cleartext and cipher text", () => {
    expect(() => {
      // tslint:disable:no-unused-expression
      new EncryptedValue({cipherText: "c!ph3rt3xt", clearText: "password"});
    }).toThrowError("You cannot initialize an encrypted value with both clear text and cipher text!");
  });

  it("should be able to update the value when it is plain text", () => {
    const originalValue  = "password";
    const encryptedValue = new EncryptedValue({clearText: originalValue});
    encryptedValue.value("new-password");

    expect(encryptedValue.isPlain()).toBe(true);
    expect(encryptedValue.isSecure()).toBe(false);
    expect(encryptedValue.getOriginal()).toBe(originalValue);
    expect(encryptedValue.value()).toBe("new-password");
    expect(encryptedValue.isDirty()).toBe(true);
  });

  it("should not allow updating the cipher text value", () => {
    const encryptedValue = new EncryptedValue({cipherText: "c!ph3rt3xt"});

    expect(() => {
      encryptedValue.value("new-password");
    }).toThrowError("You cannot edit a cipher text value!");

    expect(encryptedValue.isPlain()).toBe(false);
    expect(encryptedValue.isSecure()).toBe(true);
    expect(encryptedValue.value()).toBe("c!ph3rt3xt");
    expect(encryptedValue.isDirty()).toBe(false);
  });

  it("should allow updating the cipher text value when the cipher text is in edit mode", () => {
    const encryptedValue = new EncryptedValue({cipherText: "c!ph3rt3xt"});
    expect(encryptedValue.isEditing()).toBe(false);
    encryptedValue.edit();

    expect(encryptedValue.isEditing()).toBe(true);

    encryptedValue.value("new-password");

    expect(encryptedValue.isPlain()).toBe(false);
    expect(encryptedValue.isSecure()).toBe(true);
    expect(encryptedValue.value()).toBe("new-password");
    expect(encryptedValue.isDirty()).toBe(true);
  });

  it("should allow resetting to original values after an edit", () => {
    const encryptedValue = new EncryptedValue({cipherText: "c!ph3rt3xt"});
    encryptedValue.edit();
    encryptedValue.value("new-password");
    encryptedValue.resetToOriginal();

    expect(encryptedValue.value()).toBe("c!ph3rt3xt");
    expect(encryptedValue.isPlain()).toBe(false);
    expect(encryptedValue.isEditing()).toBe(false);
    expect(encryptedValue.isSecure()).toBe(true);
    expect(encryptedValue.isDirty()).toBe(false);
  });

  it("should show display value", () => {
    const encryptedValue = new EncryptedValue({cipherText: "c!ph3rt3xt"});
    expect(encryptedValue.valueForDisplay()).toEqual("**********");

    const encryptedEmptyValue = new EncryptedValue({clearText: ""});
    expect(encryptedEmptyValue.valueForDisplay()).toEqual(undefined);
  });

});

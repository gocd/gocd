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

import _ from "lodash";
import {EncryptedValue} from "../../forms/encrypted_value";
import {EntriesVM, EntryVM} from "../vms";

describe("EntriesVM", () => {
  it("appendBlank() adds an empty EntryVM to the end of the list", () => {
    const vm = new EntriesVM();
    expect(vm.entries.length).toBe(0);

    vm.appendBlank();

    expect(vm.entries.length).toBe(1);
    expect(vm.entries[0] instanceof EntryVM).toBe(true);
    expect(vm.entries[0].isBlank()).toBe(true);
  });

  it("consumes a set of property-like objects and constructs view models for each", () => {
    const vm = new EntriesVM([
      plain("a", "1"),
      encr("b", "AES:2"),
      plain("c", "3"),
    ]);

    expect(vm.entries.length).toBe(3);
    expect(_.every(vm.entries, (v) => v instanceof EntryVM)).toBe(true);
    expect(vm.toJSON()).toEqual([
      { key: "a", value: "1" },
      { key: "b", encrypted_value: "AES:2" },
      { key: "c", value: "3" },
    ]);
  });

  it("excise() removes the specified entry", () => {
    const vm = new EntriesVM([
      plain("a", "1"),
      encr("b", "AES:2"),
      plain("c", "3"),
    ]);

    expect(vm.entries.length).toBe(3);

    const toRemove = vm.entries[1];
    vm.excise(toRemove);

    expect(vm.entries.length).toBe(2);
    expect(vm.toJSON()).toEqual([
      { key: "a", value: "1" },
      { key: "c", value: "3" },
    ]);
  });

  it("does not serialize blank entries", () => {
    const vm = new EntriesVM([
      plain("a", "1"),
    ]);

    vm.appendBlank();
    vm.appendBlank();
    vm.appendBlank();
    vm.entries.push(new EntryVM(plain("b", "2")));

    expect(vm.entries.length).toBe(5);
    expect(vm.toJSON()).toEqual([
      { key: "a", value: "1" },
      { key: "b", value: "2" },
    ]);
  });

  describe("with a namespace", () => {
    it("filters the initial set of properties by namespace", () => {
      const vm = new EntriesVM([
        plain("prefix.a", "1"),
        encr("nothing.b", "AES:2"),
        plain("prefix.c", "3"),
      ], "prefix");

      expect(vm.entries.length).toBe(2);
      expect(vm.toJSON()).toEqual([
        { key: "prefix.a", value: "1" },
        { key: "prefix.c", value: "3" },
      ]);
    });

    it("appendBlank() creates a blank view model that honors the namespace", () => {
      const vm = new EntriesVM([], "prefix");
      expect(vm.entries.length).toBe(0);

      vm.appendBlank();

      expect(vm.entries.length).toBe(1);

      const first = vm.entries[0];
      expect(first.isBlank()).toBe(true);

      // demonstrate that this view model is namespace-aware
      first.name("hello");
      expect(first.isBlank()).toBe(false);
      expect(first.name()).toBe("hello");
      expect(first.key()).toBe("prefix.hello");
      expect(first.toJSON()).toEqual({ key: "prefix.hello", value: "" });
    });
  });
});

describe("EntryVM", () => {
  it("consumes a property-like object", () => {
    const vm = new EntryVM(plain("hi", "world"));

    expect(vm.key()).toBe("hi");
    expect(vm.value()).toBe("world");
    expect(vm.isSecure()).toBe(false);
    expect(vm.isBlank()).toBe(false);

    expect(vm.toJSON()).toEqual({ key: "hi", value: "world" });
  });

  it("consumes encrypted properties", () => {
    const vm = new EntryVM(encr("super-secret", "AES:hush!"));

    expect(vm.key()).toBe("super-secret");
    expect(vm.value()).toBe("");
    expect(vm.secretValue().value()).toBe("AES:hush!");
    expect(vm.isSecure()).toBe(true);
    expect(vm.isBlank()).toBe(false);
    expect(vm.valueAlreadyEncrypted()).toBe(true);

    expect(vm.toJSON()).toEqual({ key: "super-secret", encrypted_value: "AES:hush!" });
  });

  it("serializes plaintext values with the intent to encrypt", () => {
    const vm = new EntryVM();

    vm.key("secret");
    vm.value("I'll be encrypted by the server upon request");
    vm.isSecure(true);

    expect(vm.key()).toBe("secret");
    expect(vm.value()).toBe("");
    expect(vm.secretValue().value()).toBe("I'll be encrypted by the server upon request");
    expect(vm.isSecure()).toBe(true);
    expect(vm.valueAlreadyEncrypted()).toBe(false);

    expect(vm.toJSON()).toEqual({ key: "secret", value: "I'll be encrypted by the server upon request", secure: true });
  });

  it("isBlank() tests whether both key, value, and secret value are empty", () => {
    expect(new EntryVM().isBlank()).toBe(true);
    expect(new EntryVM(plain("", "")).isBlank()).toBe(true);
    expect(new EntryVM(encr("", "")).isBlank()).toBe(true);

    const vm = new EntryVM();
    vm.key("a");
    expect(vm.isBlank()).toBe(false);

    vm.key("");
    expect(vm.isBlank()).toBe(true);

    vm.value("ok");
    expect(vm.isBlank()).toBe(false);

    vm.value("");
    expect(vm.isBlank()).toBe(true);

    vm.secretValue(new EncryptedValue({ clearText: "hi" }));
    expect(vm.isBlank()).toBe(false);

    vm.secretValue(new EncryptedValue({ clearText: "" }));
    expect(vm.isBlank()).toBe(true);
  });

  it("valueAlreadyEncrypted() determines if the current value is the same as the initial `encrypted_value` initialized from the server", () => {
    const vm = new EntryVM(encr("super-secret", "AES:hush!"));

    // a pristinely initialized view model with an encrypted value is by definition already encrypted
    expect(vm.valueAlreadyEncrypted()).toBe(true);
    expect(vm.toJSON()).toEqual({ key: "super-secret", encrypted_value: "AES:hush!" });

    // changing the key has no effect
    vm.key("renamed");
    expect(vm.valueAlreadyEncrypted()).toBe(true);
    expect(vm.toJSON()).toEqual({ key: "renamed", encrypted_value: "AES:hush!" });

    // the value is now different
    vm.secretValue(new EncryptedValue({ clearText: "not-yet-secret" }));
    expect(vm.isSecure()).toBe(true);
    expect(vm.valueAlreadyEncrypted()).toBe(false);

    // this is NOT equivalent, even though the value looks the same; this serializes differently because it was sourced from `clearText`
    vm.secretValue(new EncryptedValue({ clearText: "AES:hush!" }));
    expect(vm.valueAlreadyEncrypted()).toBe(false);
    expect(vm.toJSON()).toEqual({ key: "renamed", value: "AES:hush!", secure: true });
  });

  describe("isSecure()", () => {
    it("wipes the initialized `encrypted_value` when toggling to `false`", () => {
      const vm = new EntryVM(encr("super-secret", "AES:hush!"));

      vm.isSecure(false);
      expect(vm.secretValue().value()).toBe("");
      expect(vm.value()).toBe("");
      expect(vm.isSecure()).toBe(false);
      expect(vm.toJSON()).toEqual({ key: "super-secret", value: "" });
    });

    it("does not the initialized plaintext value when toggling", () => {
      const vm = new EntryVM(plain("not-so-secret", "loudmouth"));

      vm.isSecure(true);
      expect(vm.secretValue().value()).toBe("loudmouth");
      expect(vm.isSecure()).toBe(true);
      expect(vm.toJSON()).toEqual({ key: "not-so-secret", value: "loudmouth", secure: true });
    });

    it("swaps any user-entered value with secretValue and vice-versa when toggling its value", () => {
      const vm = new EntryVM();

      vm.key("Bonjour");
      vm.value("le monde!");

      expect(vm.value()).toBe("le monde!");
      expect(vm.secretValue().value()).toBe("");
      expect(vm.isSecure()).toBe(false);
      expect(vm.toJSON()).toEqual({ key: "Bonjour", value: "le monde!" });

      vm.isSecure(true);

      expect(vm.secretValue().value()).toBe("le monde!");
      expect(vm.value()).toBe("");
      expect(vm.isSecure()).toBe(true);
      expect(vm.toJSON()).toEqual({ key: "Bonjour", value: "le monde!", secure: true });

      vm.isSecure(false);

      expect(vm.value()).toBe("le monde!");
      expect(vm.secretValue().value()).toBe("");
      expect(vm.isSecure()).toBe(false);
      expect(vm.toJSON()).toEqual({ key: "Bonjour", value: "le monde!" });
    });
  });

  it("reset() restores the view model back to its original initialized data, if present", () => {
    const vm = new EntryVM(encr("super-secret", "AES:hush!"));

    expect(vm.valueAlreadyEncrypted()).toBe(true);
    expect(vm.toJSON()).toEqual({ key: "super-secret", encrypted_value: "AES:hush!" });

    vm.isSecure(false);
    vm.key("whatever");
    expect(vm.toJSON()).toEqual({ key: "whatever", value: "" });

    vm.value("whenever");
    vm.isSecure(true);
    expect(vm.valueAlreadyEncrypted()).toBe(false);
    expect(vm.toJSON()).toEqual({ key: "whatever", value: "whenever", secure: true });

    vm.reset();

    expect(vm.valueAlreadyEncrypted()).toBe(true);
    expect(vm.toJSON()).toEqual({ key: "super-secret", encrypted_value: "AES:hush!" });
  });

  it("reset() clears view model if it was initially blank", () => {
    const vm = new EntryVM();

    expect(vm.isBlank()).toBe(true);

    vm.key("whatever");
    expect(vm.toJSON()).toEqual({ key: "whatever", value: "" });

    vm.value("whenever");
    vm.isSecure(true);
    expect(vm.toJSON()).toEqual({ key: "whatever", value: "whenever", secure: true });

    vm.reset();

    expect(vm.isBlank()).toBe(true);
  });

  it("name() is the same as key when there is no namespace", () => {
      const vm = new EntryVM(plain("Bonjour", "le monde!"));

      expect(vm.key()).toBe("Bonjour");
      expect(vm.name()).toBe(vm.key());

      vm.name("Au revoir");

      expect(vm.key()).toBe("Au revoir");
      expect(vm.name()).toBe(vm.key());

      expect(vm.toJSON()).toEqual({ key: "Au revoir", value: "le monde!" });
  });

  describe("with a namespace", () => {
    it("name() sets and gets while transparently handling a prefix", () => {
      const vm = new EntryVM(undefined, "ns");
      vm.name("hello");
      vm.value("world");

      expect(vm.name()).toBe("hello");
      expect(vm.key()).toBe("ns.hello");
      expect(vm.toJSON()).toEqual({ key: "ns.hello", value: "world" });
    });

    it("name() clears the key when given an empty string", () => {
      const vm = new EntryVM(plain("ns.a", "1"), "ns");

      expect(vm.name()).toBe("a");
      expect(vm.key()).toBe("ns.a");

      vm.name("");
      expect(vm.name()).toBe("");
      expect(vm.key()).toBe("");
    });
  });
});

function plain(key: string, value: string) {
  return { key, value, encrypted: false };
}

function encr(key: string, value: string) {
  return { key, value, encrypted: true };
}

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

import {MaterialModification} from "models/config_repos/types";
import {Filter} from "models/maintenance_mode/material";
import {Materials, MaterialWithFingerprint, MaterialWithModification} from "models/materials/materials";
import {
  DependencyMaterialAttributes,
  GitMaterialAttributes,
  HgMaterialAttributes,
  P4MaterialAttributes,
  PackageMaterialAttributes,
  PluggableScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {DummyCache} from "../../spec/material_usage_widget_spec";
import {MaterialVM, MaterialVMs} from "../material_view_model";

describe('MaterialVMSpec', () => {
  it('should fetch data when expand is notified', () => {
    const vm = new MaterialVM(new MaterialWithModification(new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes()), null), new DummyCache({}));
    expect(vm.results.prime).not.toHaveBeenCalled();

    vm.notify("expand");

    expect(vm.results.prime).toHaveBeenCalled();
  });

  it('should return true if search string matches name, type or display url of the config', () => {
    const material   = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("some-name", false, "http://svn.com/gocd/gocd", "master"));
    const materialVM = new MaterialVM(new MaterialWithModification(material, null));

    expect(materialVM.matches("git")).toBeTrue();
    expect(materialVM.matches("name")).toBeTrue();
    expect(materialVM.matches("gocd")).toBeTrue();
    expect(materialVM.matches("mas")).toBeTrue();
    expect(materialVM.matches("abc")).toBeFalse();
  });

  it('should return true if search string matches username, revision or comment for the latest modification', () => {
    const material   = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("", false, "some-url", "master"));
    const materialVM = new MaterialVM(new MaterialWithModification(material, new MaterialModification("username", "email_address", "some-revision", "a very very long comment with abc", "")));

    expect(materialVM.matches("revision")).toBeTrue();
    expect(materialVM.matches("comment")).toBeTrue();
    expect(materialVM.matches("name")).toBeTrue();
    expect(materialVM.matches("abc")).toBeTrue();
    expect(materialVM.matches("123")).toBeFalse();
  });

  it('should return type as config.type', () => {
    const material   = new MaterialWithFingerprint("git", "fingerprint", new GitMaterialAttributes("some-name", false, "http://svn.com/gocd/gocd", "master"));
    const materialVM = new MaterialVM(new MaterialWithModification(material, null));

    expect(materialVM.type()).toBe(material.type());
  });
});

describe('MaterialsVMsSpec', () => {
  it('should sort based on type', () => {
    const materials = new Materials();
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("git", "some", new GitMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("hg", "some", new HgMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("svn", "some", new SvnMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("p4", "some", new P4MaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("tfs", "some", new TfsMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("dependency", "some", new DependencyMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("package", "some", new PackageMaterialAttributes()), null));
    materials.push(new MaterialWithModification(new MaterialWithFingerprint("plugin", "some", new PluggableScmMaterialAttributes(undefined, undefined, "", "", new Filter([]))), null));

    const materialVMs = MaterialVMs.fromMaterials(materials);
    materialVMs.sortOnType();

    expect(materialVMs[0].type()).toBe('dependency');
    expect(materialVMs[1].type()).toBe('git');
    expect(materialVMs[2].type()).toBe('hg');
    expect(materialVMs[3].type()).toBe('p4');
    expect(materialVMs[4].type()).toBe('package');
    expect(materialVMs[5].type()).toBe('plugin');
    expect(materialVMs[6].type()).toBe('svn');
    expect(materialVMs[7].type()).toBe('tfs');
  });
});

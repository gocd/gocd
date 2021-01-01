/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {ChangeMaterial, DependencyMaterialAttributes, GitMaterialAttributes, HgMaterialAttributes, P4MaterialAttributes, PackageMaterialAttributes, PluggableScmMaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "../material";
import {MaterialData} from "./test_data";

describe('MaterialModelSpec', () => {
  it('should deserialize git material', () => {
    const gitMaterial = ChangeMaterial.fromJSON(MaterialData.git());

    expect(gitMaterial.type()).toBe('git');
    expect(gitMaterial.attributes()).toBeInstanceOf(GitMaterialAttributes);

    const attrs = gitMaterial.attributes() as GitMaterialAttributes;

    expect(attrs.name()).toBeNull();
    expect(attrs.url()).toBe('git@github.com:sample_repo/example.git');
    expect(attrs.displayType()).toBe('Git');
    expect(attrs.description()).toBe('URL: git@github.com:sample_repo/example.git, Branch: master');
  });

  it('should deserialize svn material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.svn());

    expect(material.type()).toBe('svn');
    expect(material.attributes()).toBeInstanceOf(SvnMaterialAttributes);

    const attrs = material.attributes() as SvnMaterialAttributes;

    expect(attrs.name()).toBe('SvnMaterial');
    expect(attrs.url()).toBe('url');
    expect(attrs.displayType()).toBe('Subversion');
    expect(attrs.description()).toBe('URL: url, Username: user, CheckExternals: true');
  });

  it('should deserialize hg material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.hg());

    expect(material.type()).toBe('hg');
    expect(material.attributes()).toBeInstanceOf(HgMaterialAttributes);

    const attrs = material.attributes() as HgMaterialAttributes;

    expect(attrs.name()).toBeNull();
    expect(attrs.url()).toBe('hg-url');
    expect(attrs.destination()).toBe('foo_bar');
    expect(attrs.displayType()).toBe('Mercurial');
    expect(attrs.description()).toBe('URL: hg-url');
  });

  it('should deserialize p4 material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.p4());

    expect(material.type()).toBe('p4');
    expect(material.attributes()).toBeInstanceOf(P4MaterialAttributes);

    const attrs = material.attributes() as P4MaterialAttributes;

    expect(attrs.name()).toBe('Dummy git');
    expect(attrs.port()).toBe('some-port');
    expect(attrs.destination()).toBe('bar');
    expect(attrs.displayType()).toBe('Perforce');
    expect(attrs.description()).toBe('URL: some-port, View: some-view, Username: ');
  });

  it('should deserialize tfs material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.tfs());

    expect(material.type()).toBe('tfs');
    expect(material.attributes()).toBeInstanceOf(TfsMaterialAttributes);

    const attrs = material.attributes() as TfsMaterialAttributes;

    expect(attrs.name()).toBe('Dummy tfs');
    expect(attrs.url()).toBe('foo/bar');
    expect(attrs.destination()).toBe('bar');
    expect(attrs.displayType()).toBe('Tfs');
    expect(attrs.description()).toBe('URL: foo/bar, Username: bob, Domain: foo.com, ProjectPath: /var/project');
  });

  it('should deserialize dependency material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.dependency());

    expect(material.type()).toBe('dependency');
    expect(material.attributes()).toBeInstanceOf(DependencyMaterialAttributes);

    const attrs = material.attributes() as DependencyMaterialAttributes;

    expect(attrs.name()).toBe('upstream_material');
    expect(attrs.pipeline()).toBe('upstream');
    expect(attrs.stage()).toBe('upstream_stage');
    expect(attrs.displayType()).toBe('Pipeline');
    expect(attrs.description()).toBe('upstream [ upstream_stage ]');
  });

  it('should deserialize package material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.package());

    expect(material.type()).toBe('package');
    expect(material.attributes()).toBeInstanceOf(PackageMaterialAttributes);

    const attrs = material.attributes() as PackageMaterialAttributes;

    expect(attrs.name()).toBeUndefined();
    expect(attrs.ref()).toBe('pkg-id');
    expect(attrs.displayType()).toBe('Package');
    expect(attrs.description()).toBe('Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]');
  });

  it('should deserialize pluggable material', () => {
    const material = ChangeMaterial.fromJSON(MaterialData.pluggable());

    expect(material.type()).toBe('plugin');
    expect(material.attributes()).toBeInstanceOf(PluggableScmMaterialAttributes);

    const attrs = material.attributes() as PluggableScmMaterialAttributes;

    expect(attrs.name()).toBeUndefined();
    expect(attrs.ref()).toBe('scm-id');
    expect(attrs.filter().ignore()).toEqual(["**/*.html", "**/foobar/"]);
    expect(attrs.destination()).toBe('des-folder');
    expect(attrs.displayType()).toBe('Github');
    expect(attrs.description()).toBe('k1:v1');
  });
});

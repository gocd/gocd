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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Arrays;
import java.util.List;

public class MaterialsMother {

    public static Materials defaultMaterials() {
        return defaultSvnMaterialsWithUrl("http://some/svn/url");
    }

    public static Materials defaultSvnMaterialsWithUrl(String svnUrl) {
        return new Materials(svnMaterial(svnUrl, "svnDir", null, null, false, null));
    }

    public static Materials multipleMaterials() {
        Materials materials = new Materials();
        materials.add(svnMaterial("http://svnurl"));
        materials.add(hgMaterial("http://hgurl", "hgdir"));
        materials.add(dependencyMaterial("cruise", "dev"));
        return materials;
    }

     public static Materials twoMaterials() {
         Materials materials = new Materials();
         materials.add(svnMaterial("http://svnurl"));
         materials.add(hgMaterial("http://hgurl", "hgdir"));
         return materials;
    }

    public static PackageMaterial packageMaterial(){
        PackageMaterial material = new PackageMaterial("p-id");
        material.setId(1);
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        repository.getPackages().add(packageDefinition);
        return material;
    }

    public static PackageMaterial packageMaterial(String repoId, String repoName, String pkgId, String pkgName, ConfigurationProperty... properties) {
        return packageMaterial(repoId, repoName, pkgId, pkgName, "pluginid", "version", Arrays.asList(properties), Arrays.asList(properties));
    }

    public static PackageMaterial packageMaterial(String repoId, String repoName, String pkgId, String pkgName, final String pluginid, final String version, List<ConfigurationProperty> repoProperties,
                                                  List<ConfigurationProperty> packageProperties) {
        PackageRepository repository = PackageRepositoryMother.create(repoId, repoName, pluginid, version, new Configuration(repoProperties));
        PackageDefinition packageDefinition = PackageDefinitionMother.create(pkgId, pkgName, new Configuration(packageProperties), repository);
        repository.getPackages().add(packageDefinition);

        PackageMaterial material = new PackageMaterial(pkgId);
        material.setId(1);
        material.setPackageDefinition(packageDefinition);
        return material;
    }

    public static PluggableSCMMaterial pluggableSCMMaterial() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "v2");
        return pluggableSCMMaterial("scm-id", "scm-name", k1, k2);
    }

    public static PluggableSCMMaterial pluggableSCMMaterial(String scmId, String scmName, ConfigurationProperty... properties) {
        return pluggableSCMMaterial(scmId, scmName, "pluginid", "version", Arrays.asList(properties));
    }

    public static PluggableSCMMaterial pluggableSCMMaterial(String scmId, String scmName, final String pluginid, final String version, List<ConfigurationProperty> properties) {
        PluggableSCMMaterial material = new PluggableSCMMaterial(scmId);
        material.setId(1);
        SCM scmConfig = SCMMother.create(scmId, scmName, pluginid, version, new Configuration(properties));
        material.setSCMConfig(scmConfig);
        return material;
    }

    public static DependencyMaterial dependencyMaterial(String pipelineName, String stageName) {
        return new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    public static DependencyMaterial dependencyMaterial() {
        return new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
    }

    public static Materials hgMaterials(String url) {
        return hgMaterials(url, null);
    }

    public static Materials hgMaterials(String url, String folder) {
        return new Materials(hgMaterial(url, folder));
    }

    public static HgMaterial hgMaterial(String url, String folder) {
        final HgMaterial material = new HgMaterial(url, folder);
        material.setAutoUpdate(true);
        return material;
    }

    public static HgMaterial hgMaterial() {
        return new HgMaterial("hg-url", null);
    }

    public static HgMaterial hgMaterial(String url) {
        return hgMaterial(url, null);
    }

    public static Materials gitMaterials(String url) {
        return gitMaterials(url, null, null);
    }

    public static Materials gitMaterials(String url, String branch) {
        return gitMaterials(url, null, branch);
    }

    public static Materials gitMaterials(String url, String submoduleFolder, String branch) {
        return new Materials(gitMaterial(url, submoduleFolder, branch));
    }

    public static GitMaterial gitMaterial(String url) {
        return gitMaterial(url, null, null);
    }

    public static GitMaterial gitMaterial(String url, String submoduleFolder, String branch) {
        GitMaterial gitMaterial = new GitMaterial(url, branch);
        gitMaterial.setSubmoduleFolder(submoduleFolder);
        return gitMaterial;
    }

    public static Materials p4Materials(String view) {
        P4Material material = p4Material("localhost:1666", "user", "password", view, true);
        return new Materials(material);
    }

    public static P4Material p4Material() {
        return p4Material("serverAndPort", null, null, "view", false);
    }

    public static P4Material p4Material(String serverAndPort, String userName, String password, String view, boolean useTickets) {
        final P4Material material = new P4Material(serverAndPort, view, userName);
        material.setAutoUpdate(true);
        material.setPassword(password);
        material.setUseTickets(useTickets);
        return material;
    }

    public static TfsMaterial tfsMaterial(String url) {
        return new TfsMaterial(new GoCipher(), new UrlArgument(url), "username", "domain", "password", "project-path");
    }

    public static SvnMaterial svnMaterial(String svnUrl, String folder) {
        return svnMaterial(svnUrl, folder, "user", "pass", true, "*.doc");
    }

    public static SvnMaterial svnMaterial(String svnUrl, String folder, String userName, String password, boolean checkExternals, String filterPattern) {
        SvnMaterial svnMaterial = new SvnMaterial(svnUrl, userName, password, checkExternals, folder);
        if (filterPattern != null)
            svnMaterial.setFilter(new Filter(new IgnoredFiles(filterPattern)));
        return svnMaterial;
    }

    public static SvnMaterial svnMaterial(String svnUrl) {
        return svnMaterial(svnUrl, "svnDir");
    }

    public static SvnMaterial svnMaterial() {
        return svnMaterial("url");
    }

    public static Material filteredHgMaterial(String pattern) {
        HgMaterial material = hgMaterial();
        material.setFilter(new Filter(new IgnoredFiles(pattern)));
        return material;
    }
}

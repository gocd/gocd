/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.FilterMother.filterFor;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class MagicalMaterialAndMaterialConfigConversionTest {
    private static PackageRepository packageRepo = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(create("k1", false, "v1")));
    private static PackageDefinition packageDefinition = PackageDefinitionMother.create("id", "name1", new Configuration(create("k2", false, "v2")), packageRepo);
    public static SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(create("k1", false, "v1")));

    private static Map<Class, String[]> fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack = new HashMap<Class, String[]>();
    private MaterialConfigConverter materialConfigConverter = new MaterialConfigConverter();

    @DataPoint public static MaterialConfig gitMaterialConfig = new GitMaterialConfig(url("git-url"), "branch", "submodule", true, filterFor("*.doc"), false, "folder", cis("gitMaterial"), false);
    @DataPoint public static MaterialConfig hgMaterialConfig = new HgMaterialConfig(new HgUrlArgument("hg-url"), true, filterFor("*.png"), false, "folder", cis("hgMaterial"));
    @DataPoint public static MaterialConfig svnMaterialConfig = new SvnMaterialConfig(url("svn-url"), "user", "pass", true, new GoCipher(), true, filterFor("*.txt"), false, "folder", cis("name1"));
    @DataPoint public static MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:9090", "user", "pass", true, "view", new GoCipher(), cis("p4Material"), true, filterFor("*.jpg"), false, "folder");
    @DataPoint public static MaterialConfig tfsMaterialConfig = new TfsMaterialConfig(url("tfs-url"), "user", "domain", "pass", "prj-path", new GoCipher(), true, filterFor("*.txt"), false, "folder", cis("tfsMaterial"));
    @DataPoint public static MaterialConfig pkgMaterialConfig = new PackageMaterialConfig(cis("name"), "pkg-id", packageDefinition);
    @DataPoint public static MaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(cis("name"), scmConfig, "folder", filterFor("*.txt"));
    @DataPoint public static MaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("name1"), cis("pipeline1"), cis("stage1"));

    static {
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(GitMaterialConfig.class, new String[]{"filter"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(HgMaterialConfig.class, new String[]{"filter"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(SvnMaterialConfig.class, new String[]{"filter", "encryptedPassword", "goCipher"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(P4MaterialConfig.class, new String[]{"filter", "encryptedPassword", "goCipher"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(TfsMaterialConfig.class, new String[]{"filter", "encryptedPassword", "goCipher"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(PackageMaterialConfig.class, new String[]{"filter", "packageId", "packageDefinition", "fingerprint"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(PluggableSCMMaterialConfig.class, new String[]{"filter", "scmId", "scmConfig", "fingerprint"});
        fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.put(DependencyMaterialConfig.class, new String[]{"filter", "encryptedPassword", "goCipher"});
    }

    @Theory
    public void shouldBeSameObject_WhenConversionIsDoneFromMaterialConfigToMaterialAndBack(MaterialConfig materialConfig) throws Exception {
        Material materialFromConfig = materialConfigConverter.toMaterial(materialConfig);
        MaterialConfig materialConfigConvertedBackFromMaterial = materialFromConfig.config();

        assertThat(materialConfigConvertedBackFromMaterial, is(materialConfig));
        assertTrue(message("Material <-> MaterialConfig conversion failed.", materialConfigConvertedBackFromMaterial, materialConfig),
                reflectionEquals(materialConfigConvertedBackFromMaterial, materialConfig));

        assertThat(materialFromConfig.getFingerprint(), is(materialConfig.getFingerprint()));
        assertThat(materialFromConfig.isAutoUpdate(), is(materialConfig.isAutoUpdate()));
        assertThat(materialConfigConvertedBackFromMaterial.getFingerprint(), is(materialConfig.getFingerprint()));
        assertPasswordIsCorrect(materialConfig);
        assertPasswordIsCorrect(materialFromConfig);
        assertPasswordIsCorrect(materialConfigConvertedBackFromMaterial);
    }

    @Theory
    public void shouldBeSameObject_WhenConversionIsDoneFromMaterialToMaterialInstanceAndBack(MaterialConfig materialConfig) throws Exception {
        Material material = materialConfigConverter.toMaterial(materialConfig);

        MaterialInstance materialInstance = material.createMaterialInstance();
        Material materialConvertedBackFromInstance = materialInstance.toOldMaterial(materialConfig.getName().toString(), materialConfig.getFolder(), "pass");

        assertTrue(message("Material <-> MaterialInstance conversion failed.", material, materialConvertedBackFromInstance),
                reflectionEquals(material, materialConvertedBackFromInstance, fieldsWhichShouldBeIgnoredWhenSavedInDbAndGotBack.get(materialConfig.getClass())));

        assertThat(materialInstance.getFingerprint(), is(material.getFingerprint()));
        assertThat(materialConvertedBackFromInstance.getFingerprint(), is(materialInstance.getFingerprint()));
        assertPasswordIsCorrect(material);
        assertPasswordIsCorrect(materialConvertedBackFromInstance);
    }

    @Test
    public void failIfNewTypeOfMaterialIsNotAddedInTheAboveTest() throws Exception {
        Reflections reflections = new Reflections("com.thoughtworks");
        List<Class> reflectionsSubTypesOf = new ArrayList<Class>(reflections.getSubTypesOf(MaterialConfig.class));

        Iterator<Class> iterator = reflectionsSubTypesOf.iterator();
        while (iterator.hasNext()) {
            if (isNotAConcrete_NonTest_MaterialConfigImplementation(iterator.next())) {
                iterator.remove();
            }
        }

        List<Class> allExpectedMaterialConfigImplementations = allMaterialConfigsWhichAreDataPointsInThisTest();

        assertThatAllMaterialConfigsInCodeAreTestedHere(reflectionsSubTypesOf, allExpectedMaterialConfigImplementations);
    }

    private void assertThatAllMaterialConfigsInCodeAreTestedHere(List<Class> reflectionsSubTypesOf, List<Class> allExpectedMaterialConfigImplementations) {
        List<Class> missingImplementations = new ArrayList<Class>(reflectionsSubTypesOf);
        missingImplementations.removeAll(allExpectedMaterialConfigImplementations);
        String message = "You need to add a DataPoint for these materials in this test: " + missingImplementations;

        assertThat(message, reflectionsSubTypesOf.size(), is(allExpectedMaterialConfigImplementations.size()));
        assertThat(message, reflectionsSubTypesOf, hasItems(allExpectedMaterialConfigImplementations.toArray(new Class[allExpectedMaterialConfigImplementations.size()])));
    }

    private List<Class> allMaterialConfigsWhichAreDataPointsInThisTest() throws Exception {
        Set<Field> fields = Reflections.getAllFields(getClass(), Reflections.withAnnotation(DataPoint.class));

        ArrayList<Class> allDataPointMaterialConfigClasses = new ArrayList<Class>();
        for (Field field : fields) {
            allDataPointMaterialConfigClasses.add(field.get(this).getClass());
        }
        return allDataPointMaterialConfigClasses;
    }

    private boolean isNotAConcrete_NonTest_MaterialConfigImplementation(Class aClass) {
        return Pattern.matches(".*(Test|Dummy).*", aClass.toString()) || Modifier.isAbstract(aClass.getModifiers());
    }

    private void assertPasswordIsCorrect(Material material) {
        if (material instanceof PasswordAwareMaterial) {
            assertThat("Password setting is wrong for: " + material.getClass(), ((PasswordAwareMaterial) material).getPassword(), is("pass"));
            assertThat("Password setting is wrong for: " + material.getClass(), ReflectionUtil.getField(material, "password"), is(nullValue()));
            assertThat("Password setting is wrong for: " + material.getClass(), ReflectionUtil.getField(material, "encryptedPassword"), is(not(nullValue())));
        }
    }

    private void assertPasswordIsCorrect(MaterialConfig materialConfig) {
        if (materialConfig instanceof PasswordAwareMaterial) {
            assertThat("Password setting is wrong for: " + materialConfig.getClass(), ((PasswordAwareMaterial) materialConfig).getPassword(), is("pass"));
            assertThat("Password setting is wrong for: " + materialConfig.getClass(), ReflectionUtil.getField(materialConfig, "password"), is(nullValue()));
            assertThat("Password setting is wrong for: " + materialConfig.getClass(), ReflectionUtil.getField(materialConfig, "encryptedPassword"), is(not(nullValue())));
        }
    }

    private String message(String prefix, Object expected, Object actual) {
        return prefix + "\nExpected: " + reflectionToString(expected) + "\n  Actual: " + reflectionToString(actual);
    }

    private static CaseInsensitiveString cis(String value) {
        return new CaseInsensitiveString(value);
    }

    private static UrlArgument url(String url) {
        return new UrlArgument(url);
    }
}

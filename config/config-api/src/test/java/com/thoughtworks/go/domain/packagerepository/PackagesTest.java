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
package com.thoughtworks.go.domain.packagerepository;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PackagesTest {

    @Test
    public void shouldCheckForEqualityOfPackages() {
        PackageDefinition packageDefinition = new PackageDefinition();
        Packages packages = new Packages(packageDefinition);
        assertThat(packages, is(new Packages(packageDefinition)));
    }

    @Test
    public void shouldFindPackageGivenThePkgId() throws Exception {
        PackageRepository repository = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition p1 = PackageDefinitionMother.create("id1", "pkg1", null, repository);
        PackageDefinition p2 = PackageDefinitionMother.create("id2", "pkg2", null, repository);
        Packages packages = new Packages(p1, p2);
        assertThat(packages.find("id2"), is(p2));
    }

    @Test
    public void shouldReturnNullIfNoMatchingPkgFound() throws Exception {
        Packages packages = new Packages();
        assertThat(packages.find("id2"), is(nullValue()));
    }
    @Test
    public void shouldValidateForCaseInsensitiveNameUniqueness(){
        PackageDefinition p1 = PackageDefinitionMother.create("id1", "pkg1", null, null);
        PackageDefinition p2 = PackageDefinitionMother.create("id2", "pkg1", null, null);
        PackageDefinition p3 = PackageDefinitionMother.create("id3", "pkg3", null, null);
        Packages packages = new Packages(p1, p2, p3);

        packages.validate(null);
        assertThat(p1.errors().isEmpty(), is(true));
        String nameError = String.format("You have defined multiple packages called '%s'. Package names are case-insensitive and must be unique within a repository.", p2.getName());
        assertThat(p2.errors().isEmpty(), is(false));
        assertThat(p2.errors().getAllOn(PackageRepository.NAME).contains(nameError), is(true));
        assertThat(p3.errors().isEmpty(), is(true));
    }
}

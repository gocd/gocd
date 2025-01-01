/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.helper.ModificationsMother.aCheckIn;
import static com.thoughtworks.go.helper.ModificationsMother.multipleCheckin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ModificationsTest {

    @Test
    public void shouldReturnUnknownForEmptyList() {
        assertThat(new Modifications().getUsername()).isEqualTo("Unknown");
    }

    @Test
    public void shouldReturnFirstUsername() {
        Modification modification1 = new Modification("username1", "", null, new Date(), "1");
        Modification modification2 = new Modification("username2", "", null, new Date(), "2");
        assertThat(new Modifications(modification1, modification2).getUsername()).isEqualTo("username1");
    }

    @Test
    public void shouldReturnUnknownRevisionForEmptyList() {
        assertThat(new Modifications().getRevision()).isEqualTo("Unknown");
    }

    @Test
    public void shouldReturnFirstRevision() {
        Modification modification1 = new Modification(new Date(), "cruise/1.0/dev/1", "MOCK_LABEL-12", null);
        Modification modification2 = new Modification(new Date(), "cruise/1.0/dev/2", "MOCK_LABEL-12", null);
        assertThat(new Modifications(modification1, modification2).getRevision()).isEqualTo("cruise/1.0/dev/1");
    }

    @Test
    public void shouldReturnRevisionsWithoutSpecifiedRevision() {
        final Modification modification1 = new Modification(new Date(), "1", "MOCK_LABEL-12", null);
        final Modification modification2 = new Modification(new Date(), "2", "MOCK_LABEL-12", null);

        List<Modification> modifications = new ArrayList<>();
        modifications.add(modification1);
        modifications.add(modification2);

        List<Modification> filtered = Modifications.filterOutRevision(modifications, new StringRevision("1"));
        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0)).isEqualTo(modification2);
    }

    @Test
    public void hasModificationShouldReturnCorrectResults() {
        Modifications modifications = modificationWithIds();
        assertThat(modifications.hasModification(3)).isTrue();
        assertThat(modifications.hasModification(2)).isTrue();
        assertThat(modifications.hasModification(5)).isFalse();
        assertThat(modifications.hasModification(0)).isFalse();
    }

    @Test
    public void hasModifcationResults() {
        Modifications modifications = modificationWithIds();
        assertThat(modifications.since(3)).isEqualTo(new Modifications(modifcation(4)));
        assertThat(modifications.since(2)).isEqualTo(new Modifications(modifcation(4), modifcation(3)));
        try {
            modifications.since(10);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Could not find modification 10 in " + modifications);
        }
        try {
            modifications.since(6);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Could not find modification 6 in " + modifications);
        }

    }

    @Test
    public void shouldUnderstandIfContainsModificationWithSameRevision() {
        MaterialInstance materialInstance = MaterialsMother.hgMaterial().createMaterialInstance();

        final Modification modification = modificationWith(materialInstance, "1");
        final Modification sameModification = modificationWith(materialInstance, "1");
        final Modification modificationWithDifferentRev = modificationWith(materialInstance, "2");
        final Modification modificationWithDifferentMaterial = modificationWith(MaterialsMother.hgMaterial("http://foo.com").createMaterialInstance(), "1");

        Modifications modifications = new Modifications(modification);
        assertThat(modifications.containsRevisionFor(modification)).isTrue();
        assertThat(modifications.containsRevisionFor(sameModification)).isTrue();
        assertThat(modifications.containsRevisionFor(modificationWithDifferentRev)).isFalse();
        assertThat(modifications.containsRevisionFor(modificationWithDifferentMaterial)).isTrue(); //note that its checking for revision and not material instance
    }

    @Test
    public void shouldGetLatestModificationsForPackageMaterial() {
        Date timestamp = new Date();
        String revisionString = "123";
        Map<String, String> data = new HashMap<>();
        data.put("1", "one");
        data.put("2", "two");
        Modification modification = new Modification(null, null, null, timestamp, revisionString, JsonHelper.toJsonString(data));
        Modifications modifications = new Modifications(modification);

        Revision revision = modifications.latestRevision(new PackageMaterial());

        assertThat(revision instanceof PackageMaterialRevision).isTrue();
        PackageMaterialRevision packageMaterialRevision = (PackageMaterialRevision) revision;
        assertThat(packageMaterialRevision.getRevision()).isEqualTo(revisionString);
        assertThat(packageMaterialRevision.getTimestamp()).isEqualTo(timestamp);
        assertThat(packageMaterialRevision.getData().size()).isEqualTo(data.size());
        assertThat(packageMaterialRevision.getData().get("1")).isEqualTo(data.get("1"));
        assertThat(packageMaterialRevision.getData().get("2")).isEqualTo(data.get("2"));
    }

    @Test
    public void shouldGetLatestModificationsForPluggableSCMMaterial() {
        String revisionString = "123";
        Date timestamp = new Date();
        Map<String, String> data = new HashMap<>();
        data.put("1", "one");
        data.put("2", "two");
        Modification modification = new Modification(null, null, null, timestamp, revisionString, JsonHelper.toJsonString(data));
        Modifications modifications = new Modifications(modification);

        Revision revision = modifications.latestRevision(new PluggableSCMMaterial());

        assertThat(revision instanceof PluggableSCMMaterialRevision).isTrue();
        PluggableSCMMaterialRevision pluggableSCMMaterialRevision = (PluggableSCMMaterialRevision) revision;
        assertThat(pluggableSCMMaterialRevision.getRevision()).isEqualTo(revisionString);
        assertThat(pluggableSCMMaterialRevision.getTimestamp()).isEqualTo(timestamp);
        assertThat(pluggableSCMMaterialRevision.getData().size()).isEqualTo(data.size());
        assertThat(pluggableSCMMaterialRevision.getData().get("1")).isEqualTo(data.get("1"));
        assertThat(pluggableSCMMaterialRevision.getData().get("2")).isEqualTo(data.get("2"));
    }

    @Test
    public void shouldNeverIgnorePackageMaterialModifications() {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig();
        Filter filter = packageMaterialConfig.filter();
        assertThat(filter).isNotNull();
        assertThat(new Modifications().shouldBeIgnoredByFilterIn(packageMaterialConfig)).isFalse();
    }

    @Test
    public void shouldIncludeModificationsIfAnyFileIsNotIgnored() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("*.doc"), new IgnoredFiles("*.pdf")));
        materialConfig.setFilter(filter);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isFalse();
    }

    @Test
    public void shouldIncludeModificationsIfAnyFileIsNotIgnored1() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("*.doc"), new IgnoredFiles("*.pdf")));
        materialConfig.setFilter(filter);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf"), aCheckIn("100", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isFalse();
    }

    @Test
    public void shouldIgnoreModificationsIfAllTheIgnoresMatch() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("*.doc"), new IgnoredFiles("*.pdf")));
        materialConfig.setFilter(filter);

        assertThat(new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf"))).shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
        assertThat(new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.doc"))).shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
        assertThat(new Modifications(multipleCheckin(aCheckIn("100", "a.pdf", "b.pdf"), aCheckIn("100", "a.doc", "b.doc"))).shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
    }

    @Test
    public void shouldIgnoreModificationsIfInvertFilterAndEmptyIgnoreList() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter();
        materialConfig.setFilter(filter);
        materialConfig.setInvertFilter(true);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
    }

    @Test
    public void shouldIgnoreModificationsIfWildcardBlacklist() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("**/*")));
        materialConfig.setFilter(filter);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
    }

    @Test
    public void shouldIncludeModificationsIfInvertFilterAndWildcardBlacklist() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("**/*")));
        materialConfig.setFilter(filter);
        materialConfig.setInvertFilter(true);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isFalse();
    }

    @Test
    public void shouldIgnoreModificationsIfInvertFilterAndSpecificFileNotChanged() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("*.foo")));
        materialConfig.setFilter(filter);
        materialConfig.setInvertFilter(true);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.doc", "a.pdf", "a.java")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
    }

    @Test
    public void shouldIgnoreModificationsIfInvertFilterAndSpecificFileNotChanged2() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("foo/bar.baz")));
        materialConfig.setFilter(filter);
        materialConfig.setInvertFilter(true);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("100", "a.java", "foo", "bar.baz", "foo/bar.qux")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isTrue();
    }

    @Test
    public void shouldIncludeModificationsIfInvertFilterAndSpecificIsChanged() {
        HgMaterialConfig materialConfig = MaterialConfigsMother.hgMaterialConfig();
        Filter filter = new Filter(List.of(new IgnoredFiles("foo/bar.baz")));
        materialConfig.setFilter(filter);
        materialConfig.setInvertFilter(true);

        Modifications modifications = new Modifications(multipleCheckin(aCheckIn("101", "foo/bar.baz")));
        assertThat(modifications.shouldBeIgnoredByFilterIn(materialConfig)).isFalse();
    }

    private Modifications modificationWithIds() {
        return new Modifications(modifcation(4), modifcation(3), modifcation(2), modifcation(1));
    }

    private Modification modifcation(long id) {
        Modification modification = new Modification();
        modification.setId(id);
        return modification;
    }


    private Modification modificationWith(MaterialInstance materialInstance, String revision) {
        final Modification modification = new Modification(new Date(), revision, "MOCK_LABEL-12", null);
        modification.setMaterialInstance(materialInstance);
        return modification;
    }
}

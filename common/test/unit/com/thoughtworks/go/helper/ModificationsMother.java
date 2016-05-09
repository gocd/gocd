/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.helper;

import java.util.*;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConstants;
import org.joda.time.DateTime;

public class ModificationsMother {
    public static final String MOD_COMMENT = "Fixing the not checked in files";
    public static final String MOD_COMMENT_2 = "Added the README file";
    public static final String MOD_COMMENT_3 = "Added the README file with <html />";
    public static final String MOD_USER = "lgao";
    public static final String MOD_USER_COMMITTER = "committer";
    public static final String MOD_USER_WITH_HTML_CHAR = "committer <html />";
    public static final Date TWO_DAYS_AGO_CHECKIN = new DateTime().minusDays(2).toDate();
    public static final Date YESTERDAY_CHECKIN = new DateTime().minusDays(1).toDate();

    public static final Date TODAY_CHECKIN = new Date();
    public static final ModifiedAction MOD_MODIFIED_ACTION = ModifiedAction.added;
    public static final String MOD_TYPE = "svn";
    public static final String MOD_FILE_BUILD_XML = "build.xml";
    public static final String MOD_FILE_OLD_BUILD_XML = "oldbuild.xml";
    public static final String MOD_FILE_READ_ME = "README.txt";
    public static final String EMAIL_ADDRESS = "foo@bar.com";

    private static int revision = 0;

    public static String currentRevision() {
        return Integer.toString(revision);
    }


    protected ModificationsMother() {
    }

    public static BuildCause modifySomeFiles(PipelineConfig pipelineConfig) {
        return modifySomeFilesAndTriggerAs(pipelineConfig, GoConstants.DEFAULT_APPROVED_BY);
    }

    public static BuildCause modifySomeFilesAndTriggerAs(PipelineConfig pipelineConfig, String approver) {
        return BuildCause.createWithModifications(modifyOneFile(pipelineConfig), approver);
    }

    public static BuildCause modifySomeFiles(PipelineConfig pipelineConfig, String revision) {
        return buildCauseForOneModifiedFile(pipelineConfig, revision);
    }

    public static BuildCause forceBuild(PipelineConfig pipelineConfig) {
        return BuildCause.createManualForced(modifyOneFile(pipelineConfig), Username.ANONYMOUS);
    }

    public static MaterialRevisions modifyOneFile(PipelineConfig pipelineConfig) {
        return modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs()), nextRevision());
    }

    public static MaterialRevisions modifyOneFile(Materials materials) {
        return modifyOneFile(materials, nextRevision());
    }

    public static String nextRevision() {
        revision += 1;
        return Integer.toString(revision);
    }

    public static BuildCause buildCauseForOneModifiedFile(PipelineConfig pipelineConfig, String revision, String comment, String committer) {
        return BuildCause.createWithModifications(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs()), revision, comment, committer), "");
    }

    public static BuildCause buildCauseForOneModifiedFile(PipelineConfig pipelineConfig, String revision) {
        return BuildCause.createWithModifications(modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs()), revision), "");
    }

    public static MaterialRevisions multipleModificationsInHg(PipelineConfig pipelineConfig) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (Material material : MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs())) {
            materialRevisions.addRevision(material, multipleModificationsInHg());
        }
        return materialRevisions;
    }

    public static MaterialRevisions createHgMaterialRevisions() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(MaterialsMother.hgMaterial(), multipleModificationsInHg());
        return materialRevisions;
    }

    public static MaterialRevision createPackageMaterialRevision(String revision) {
        return createPackageMaterialRevision(revision, "user", "");
    }

    public static MaterialRevision createPackageMaterialRevision(String revision, String user, String comment) {
        Material material = MaterialsMother.packageMaterial();
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(new Modification(user, comment, null, new Date(), revision));
        return new MaterialRevision(material, modifications);
    }

    public static MaterialRevision createPipelineMaterialRevision(String stageIdentifier) {
        Material material = MaterialsMother.dependencyMaterial();
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(new Modification(new Date(), stageIdentifier, "123", 1L));
        return new MaterialRevision(material, modifications);
    }

    public static List<Modification> multipleModificationsInHg() {
        final ArrayList<Modification> modifications = new ArrayList<Modification>();

        modifications.add(new Modification("user2", "comment2", "email2", TODAY_CHECKIN, "9fdcf27f16eadc362733328dd481d8a2c29915e1"));
        modifications.add(new Modification("user1", "comment1", "email1", TWO_DAYS_AGO_CHECKIN, "eef77acd79809fc14ed82b79a312648d4a2801c6"));


        return modifications;
    }

    public static MaterialRevisions modifyOneFile(Materials materials, String revision) {
        return modifyOneFile(materials, revision, MOD_COMMENT);
    }

    public static MaterialRevisions modifyOneFile(Materials materials, String revision, String comment) {
        return modifyOneFile(materials, revision, comment, MOD_USER);
    }
    
    public static MaterialRevisions modifyOneFile(Materials materials, String revision, String comment, String committer) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        Materials expandedMaterials = new Materials();

        for (Material material : materials) {
            expandedMaterials.add(material);
        }

        for (Material material : expandedMaterials) {
            Modification modification;
            if (material instanceof DependencyMaterial) {
                DependencyMaterial dependencyMaterial = (DependencyMaterial) material;
                modification = oneModifiedFile(committer, dependencyMaterial.getPipelineName() + "/1/" + dependencyMaterial.getStageName() + "/" + revision, TWO_DAYS_AGO_CHECKIN,
                        dependencyMaterial.getPipelineName() + "-1.2.3");
            } else {
                modification = oneModifiedFile(committer, revision, comment, TWO_DAYS_AGO_CHECKIN);
            }
            materialRevisions.addRevision(new MaterialRevision(material, true, modification));
        }
        return materialRevisions;
    }

    private static Modification oneModifiedFile(String user, String revision, Date date, String pipelineLabel) {
        Modification modification = new Modification(user, MOD_COMMENT, EMAIL_ADDRESS, date, revision);
        modification.setPipelineLabel(pipelineLabel);
        modification.createModifiedFile(MOD_FILE_BUILD_XML, "\\build", MOD_MODIFIED_ACTION);
        return modification;
    }

    public static MaterialRevision dependencyMaterialRevision(String pipelineName, int pipelineCounter, String pipelineLabel, String stageName, int stageCounter, Date modifiedTime) {
        return dependencyMaterialRevision(pipelineCounter, pipelineLabel, stageCounter, new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName)), modifiedTime);
    }

    public static MaterialRevision changedDependencyMaterialRevision(String pipelineName, int pipelineCounter, String pipelineLabel, String stageName, int stageCounter, Date modifiedTime) {
        MaterialRevision materialRevision = dependencyMaterialRevision(pipelineName, pipelineCounter, pipelineLabel, stageName, stageCounter, modifiedTime);
        materialRevision.markAsChanged();
        return materialRevision;
    }

    public static MaterialRevision dependencyMaterialRevision(int pipelineCounter, String pipelineLabel, int stageCounter, DependencyMaterial material, Date modifiedTime) {
        return DependencyMaterialRevision.create(CaseInsensitiveString.str(material.getPipelineName()), pipelineCounter, pipelineLabel, CaseInsensitiveString.str(material.getStageName()), stageCounter).convert(material, modifiedTime);
    }

    public static MaterialRevisions oneUserOneFile() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        SvnMaterial material = MaterialsMother.svnMaterial();
        material.setName(new CaseInsensitiveString("svnMaterial"));
        materialRevisions.addRevision(material, oneModifiedFile(nextRevision()));
        return materialRevisions;
    }

    public static Modification withModifiedFileWhoseNameLengthIsOneK() {
        Modification modification = new Modification(MOD_USER, MOD_COMMENT, EMAIL_ADDRESS,
                TWO_DAYS_AGO_CHECKIN, "rev_1");
        modification.createModifiedFile(generateString(1024), "\\build", MOD_MODIFIED_ACTION);
        return modification;
    }

    public static Modification withModifiedFileWhoseNameLengthIsMoreThanOneK() {
        Modification modification = new Modification(MOD_USER, MOD_COMMENT, EMAIL_ADDRESS,
                TWO_DAYS_AGO_CHECKIN, "rev_1");
        modification.createModifiedFile(generateString(1024 + 1), "\\build", MOD_MODIFIED_ACTION);
        return modification;
    }

    private static String generateString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append('a');
        }
        return builder.toString();
    }

    public static Modification oneModifiedFile(String modRevision) {
        return oneModifiedFile(MOD_USER, modRevision, TWO_DAYS_AGO_CHECKIN);
    }

    public static Modification oneModifiedFile(String modRevision, Date date) {
        return oneModifiedFile(MOD_USER, modRevision, date);
    }

    public static Modification oneModifiedFile(String modUser, String modRevision, Date date) {
        return oneModifiedFile(modUser, modRevision, MOD_COMMENT, date);
    }

    public static Modification oneModifiedFile(String modUser, String modRevision, String comment, Date date) {
        Modification modification = new Modification(modUser, comment, EMAIL_ADDRESS, date, modRevision);
        modification.createModifiedFile(MOD_FILE_BUILD_XML, "\\build", MOD_MODIFIED_ACTION);
        return modification;
    }

    public static MaterialRevisions multipleModifications() {
        return multipleModifications("svn-material");
    }

    public static MaterialRevisions multipleModifications(String name) {
        SvnMaterial material = MaterialsMother.svnMaterial("http://foo/bar/baz", "folder", "username", "password", false, "*.txt");
        material.setName(new CaseInsensitiveString(name));
        return multipleModifications(material);
    }

    public static MaterialRevisions multipleModifications(Material material) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(material, multipleModificationList());
        return materialRevisions;
    }

    public static MaterialRevisions multipleModifications(PipelineConfig pipelineConfig) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (Material material : MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs())) {
            materialRevisions.addRevision(material, multipleModificationList());
        }
        return materialRevisions;
    }

    public static List<Modification> multipleModificationList() {
        return multipleModificationList(new RevisionToUse() {
            public String next() {
                return nextRevision();
            }
        });
    }

    public static List<Modification> multipleModificationList(final int initialRevision) {
        return multipleModificationList(new RevisionToUse() {
            int revision = initialRevision;

            public String next() {
                return String.valueOf(revision++);
            }
        });
    }

    public static List<Modification> multipleModificationList(RevisionToUse revisionToUse) {
        Modification modification1 = new Modification(MOD_USER, MOD_COMMENT, EMAIL_ADDRESS,
                TWO_DAYS_AGO_CHECKIN, revisionToUse.next());
        modification1.createModifiedFile(MOD_FILE_BUILD_XML, "\\build", MOD_MODIFIED_ACTION);

        Modification modification2 = new Modification(MOD_USER_COMMITTER, MOD_COMMENT_2, EMAIL_ADDRESS,
                YESTERDAY_CHECKIN, revisionToUse.next());
        modification2.createModifiedFile(MOD_FILE_OLD_BUILD_XML, "\\build", MOD_MODIFIED_ACTION);

        Modification modification3 = new Modification(MOD_USER_WITH_HTML_CHAR, MOD_COMMENT_3, EMAIL_ADDRESS,
                TODAY_CHECKIN, revisionToUse.next());
        modification3.createModifiedFile(MOD_FILE_READ_ME, "\\build", MOD_MODIFIED_ACTION);

        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(modification3);
        modifications.add(modification2);
        modifications.add(modification1);
        return modifications;
    }

    public static MaterialRevisions empty() {
        return new MaterialRevisions();
    }

    public static BuildCause modifyNoFiles(PipelineConfig config) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (Material material : MaterialsMother.createMaterialsFromMaterialConfigs(config.materialConfigs())) {
            ArrayList<Modification> list = new ArrayList<Modification>();
            list.add(new Modification("no-user", "comment", "dummy-email", new Date(), "Dummy Modification"+ UUID.randomUUID().toString()));
            materialRevisions.addRevision(material, list);
        }
        return BuildCause.createWithModifications(materialRevisions, "");
    }


    public static Modification aCheckIn(String revision, String... files) {
        return checkinWithComment(revision, MOD_COMMENT_2, TODAY_CHECKIN, files);
    }

    public static Modification checkinWithComment(String revision, String comment, Date checkinTime, String... files) {
        return checkinWithComment(revision, comment, MOD_USER_COMMITTER, EMAIL_ADDRESS, checkinTime, files);
    }

    public static Modification checkinWithComment(String revision, String comment, String user, String email, Date checkinTime, String... files) {
        Modification modification = new Modification(user, comment, email, checkinTime, revision);
        for (String file : files) {
            modification.createModifiedFile(file, null, ModifiedAction.added);
        }
        return modification;
    }

    public static List<Modification> multipleCheckin(Modification... modifications) {
        return Arrays.asList(modifications);
    }

    public static MaterialRevisions getMaterialRevisions(HashMap<Material, String> checkins) {
        MaterialRevisions revisions = new MaterialRevisions();
        for (Material material : checkins.keySet()) {
            revisions.addRevision(material, aCheckIn(checkins.get(material), "file1.txt"));
        }
        return revisions;
    }

    public static MaterialRevisions createSvnMaterialRevisions(Modification modification) {
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();
        return createMaterialRevisions(svnMaterial, modification);
    }

    public static MaterialRevisions createMaterialRevisions(SvnMaterial svnMaterial, Modification modification) {
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(modification);
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(svnMaterial, modifications);
        return revisions;
    }

    public static MaterialRevisions createP4MaterialRevisions(Modification modification) {
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(modification);
        Material svnMaterial = MaterialsMother.p4Material();
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(svnMaterial, modifications);
        return revisions;
    }

    public static MaterialRevisions createSvnMaterialWithMultipleRevisions(long id, Modification... modifications) {
        return multipleRevisions(MaterialsMother.svnMaterial(), id, modifications);
    }

    public static MaterialRevisions createHgMaterialWithMultipleRevisions(long id, Modification... modifications) {
        return multipleRevisions(MaterialsMother.hgMaterial(), id, modifications);
    }

    public static MaterialRevisions multipleRevisions(Material material, long id, Modification... modifications) {
        material.setId(id);
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(material, modifications);
        return revisions;
    }

    private static interface RevisionToUse {
        String next();
    }
}

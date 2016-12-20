/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.ModificationVisitor;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 * <p/>
 * <modification type="" date="" user="" email="">
 * <comment></comment>
 * <file >
 * </modification>
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class Modification extends PersistentObject implements Comparable, Serializable {

    private static final long serialVersionUID = 6102576575583133520L;

    public static final Modification NEVER = new Modification(GoConstants.NEVER);
    public static final String ANONYMOUS = "anonymous";

    private String userName = "";
    private String comment = "";
    private String emailAddress;
    private String revision;
    private String additionalData;
    private HashMap<String, String> additionalDataMap;

    private Date modifiedTime;
    private Set<ModifiedFile> files = new LinkedHashSet<>();
    private MaterialInstance materialInstance;
    private String pipelineLabel;
    private Long pipelineId;

    public Modification() {
    }

    private Modification(Date datetime) {
        this.modifiedTime = datetime;
    }

    public Modification(Date date, String revision, String pipelineLabel, Long pipelineId) {
        this("Unknown", "Unknown", null, date, revision);
        this.pipelineLabel = pipelineLabel;
        this.pipelineId = pipelineId;
    }

    public Modification(String user, String comment, String email, Date dateTime, String revision) {
        this.userName = user;
        this.comment = comment;
        this.emailAddress = email;
        this.modifiedTime = dateTime;
        this.revision = revision;
    }

    public Modification(Modification modification) {
        this(modification, true);
    }

    public Modification(String user, String comment, String email, Date dateTime, String revision, String additionalData) {
        this(user, comment, email, dateTime, revision);
        setAdditionalData(additionalData);
    }

    public Modification(Modification modification, boolean shouldCopyModifiedFiles) {
        this(modification.userName, modification.comment, modification.emailAddress, modification.modifiedTime, modification.getRevision());
        this.id = modification.id;
        if(shouldCopyModifiedFiles){
            this.files = modification.files;
        }
        this.pipelineLabel = modification.pipelineLabel;
        this.pipelineId = modification.pipelineId;
        this.materialInstance = modification.materialInstance;
        this.additionalData = modification.additionalData;
        this.additionalDataMap = modification.additionalDataMap;
    }

    public final ModifiedFile createModifiedFile(String filename, String folder, ModifiedAction modifiedAction) {
        ModifiedFile file = new ModifiedFile(filename, folder, modifiedAction);
        files.add(file);
        return file;
    }

    public HashMap<String, String> getAdditionalDataMap() {
        return additionalDataMap == null ? new HashMap<String, String>(): additionalDataMap;
    }


    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        this.additionalDataMap = JsonHelper.safeFromJson(this.additionalData, HashMap.class);
    }

    /**
     * @deprecated used only by material parsers and in test
     */
    public void setUserName(String name) {
        this.userName = name;
    }

    /**
     * @deprecated used only by material parsers and in tests
     */
    public void setEmailAddress(String email) {
        this.emailAddress = email;
    }

    /**
     * @deprecated used only by material parsers and in tests
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @deprecated used only by material parsers and in tests
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * @deprecated used only by material parsers and in tests
     */
    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    /**
     * @deprecated used only in tests
     */
    public void setModifiedFiles(List<ModifiedFile> files) {
        this.files = files == null ? new LinkedHashSet<ModifiedFile>() : new LinkedHashSet<>(files);
    }

    /**
     * Returns the list of modified files for this modification set.
     *
     * @return list of {@link ModifiedFile} objects. If there are no files, this returns an empty list
     *         (<code>null</code> is never returned).
     */
    public List<ModifiedFile> getModifiedFiles() {
        return Collections.unmodifiableList(new ArrayList<>(files));
    }

    public int compareTo(Object o) {
        Modification modification = (Modification) o;
        return modifiedTime.compareTo(modification.modifiedTime);
    }


    public Date getModifiedTime() {
        return modifiedTime;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserDisplayName() {
        return StringUtils.isBlank(userName) ? ANONYMOUS : userName;
    }

    public String getRevision() {
        return revision;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public String getComment() {
        return comment;
    }

    public String toString() {
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        if (materialInstance != null) {
            sb.append("Material: ").append(materialInstance).append('\n');
        }
        String timeString = modifiedTime == null ? "" : formatter.format(modifiedTime);
        sb.append("Last Modified: ").append(timeString).append('\n');
        sb.append("Revision: ").append(revision).append('\n');
        sb.append("UserName: ").append(userName).append('\n');
        sb.append("EmailAddress: ").append(emailAddress).append('\n');
        sb.append("Comment: ").append(comment).append('\n');
        sb.append("PipelineLabel: ").append(pipelineLabel).append('\n');
        return sb.toString();
    }

    public static Revision latestRevision(List<Modification> modifications) {
        if (modifications.isEmpty()) {
            throw new RuntimeException("Cannot find latest revision.");
        } else {
            return new StringRevision(modifications.get(0).getRevision());
        }
    }

    public void accept(ModificationVisitor visitor) {
        visitor.visit(this);
        for (ModifiedFile file : files) {
            visitor.visit(file);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Modification)) {
            return false;
        }

        Modification that = (Modification) o;

        if (comment != null ? !comment.equals(that.comment) : that.comment != null) {
            return false;
        }
        if (emailAddress != null ? !emailAddress.equals(that.emailAddress) : that.emailAddress != null) {
            return false;
        }
        if (files != null ? !files.equals(that.files) : that.files != null) {
            return false;
        }
        if (modifiedTime != null ? !modifiedTime.equals(that.modifiedTime) : that.modifiedTime != null) {
            return false;
        }
        if (pipelineId != null ? !pipelineId.equals(that.pipelineId) : that.pipelineId != null) {
            return false;
        }
        if (pipelineLabel != null ? !pipelineLabel.equals(that.pipelineLabel) : that.pipelineLabel != null) {
            return false;
        }
        if (revision != null ? !revision.equals(that.revision) : that.revision != null) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0);
        result = 31 * result + (revision != null ? revision.hashCode() : 0);
        result = 31 * result + (modifiedTime != null ? modifiedTime.hashCode() : 0);
        result = 31 * result + (files != null ? files.hashCode() : 0);
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (pipelineId != null ? pipelineId.hashCode() : 0);
        result = 31 * result + (additionalData != null ? additionalData.hashCode() : 0);
        return result;
    }

    public void setMaterialInstance(MaterialInstance materialInstance) {
        this.materialInstance = materialInstance;
    }

    public MaterialInstance getMaterialInstance() {
        return materialInstance;
    }

    public static ArrayList<Modification> modifications(Modification modification) {
        ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(modification);
        return modifications;
    }

    /**
     * @deprecated Remove this when we do not need to serialize these to the db and agent
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(userName);
        out.writeObject(comment);
        out.writeObject(emailAddress);
        out.writeObject(revision);
        out.writeObject(additionalData);
        out.writeObject(additionalDataMap);
        out.writeObject(modifiedTime);
        out.writeObject(pipelineLabel);
        out.writeObject(materialInstance);
        out.writeObject(new LinkedHashSet(files));
    }

    /**
     * @deprecated Remove this when we do not need to serialize these to the db and agent
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        userName = (String) in.readObject();
        comment = (String) in.readObject();
        emailAddress = (String) in.readObject();
        revision = (String) in.readObject();
        additionalData = (String) in.readObject();
        additionalDataMap = (HashMap) in.readObject();
        modifiedTime = (Date) in.readObject();
        pipelineLabel = (String) in.readObject();
        materialInstance = (MaterialInstance) in.readObject();
        Set files = (Set) in.readObject();
        if (files == null) {
            this.files = new LinkedHashSet<>();
        } else {
            this.files = new LinkedHashSet<>(files);
        }
    }

    /**
     * @deprecated Remove this when we do not need to serialize these to the db and agent
     */
    private void readObjectNoData() throws ObjectStreamException {
    }

    public boolean isSameRevision(Modification that) {
        return revision != null ? revision.equals(that.revision) : that.revision == null;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    /**
     * @deprecated for tests only
     */
    public void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    public Set<String> getCardNumbersFromComment() {
        Set<String> cardNumbers = new TreeSet<>();
        Pattern pattern = Pattern.compile("#(\\d+)");
        String comment = this.comment == null ? "" : this.comment;
        Matcher matcher = pattern.matcher(comment);

        while (hasMatch(matcher)) {
            cardNumbers.add(id(matcher));
            matcher.end();
        }

        return cardNumbers;
    }

    private boolean hasMatch(Matcher matcher) {
        return matcher.find() && id(matcher) != null;
    }

    public String id(Matcher matcher) {
        return matcher.groupCount() > 0 ? contentsOfFirstGroupThatMatched(matcher) : matcher.group();
    }

    private String contentsOfFirstGroupThatMatched(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String groupContent = matcher.group(i);
            if (groupContent != null) {
                return groupContent;
            }
        }
        return null;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public static Revision oldestRevision(Modifications modifications) {
        if (modifications.isEmpty()) {
            throw new RuntimeException("Cannot find oldest revision.");
        } else {
            return new StringRevision(modifications.get(modifications.size()-1).getRevision());
        }
    }

    public String getAdditionalData() {
        return additionalData;
    }
}

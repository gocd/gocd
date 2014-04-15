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

package com.thoughtworks.go.domain.materials.tfs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("changeset")
public class Changeset {

    @XStreamAsAttribute
    private String id;

    @XStreamAsAttribute
    private String committer;

    @XStreamAsAttribute
    private String date;

    @XStreamAlias("comment")
    private Comment comment;

    @XStreamImplicit
    private List<Item> items;

    @XStreamImplicit
    private List<CheckInNote> checkInNotes;

    private static final SimpleDateFormat CHANGESET_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public Changeset(String id) {
        this.id = id;
    }

    public Modification getModification() {
        Date parse = parseDate(date, CHANGESET_DATE_FORMAT);
        Modification modification = new Modification(committer, getComment(), null, new Date(parse.getYear(), parse.getMonth(), parse.getDate(), parse.getHours(), parse.getMinutes(), parse.getSeconds()), id);
        ArrayList<ModifiedFile> files = new ArrayList<ModifiedFile>();
        for (Item item : items) {
            files.add(item.getModifiedFile());
        }
        modification.setModifiedFiles(files);
        return modification;
    }

    private String getComment() {
        return comment == null ? "" : comment.getContent();
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    public void setDate(String dateValue) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy h:mm:ss a");
        this.date = CHANGESET_DATE_FORMAT.format(parseDate(dateValue, simpleDateFormat));
    }

    public void setComment(String comment) {
        this.comment = new Comment(comment);
    }

    private Date parseDate(String date, SimpleDateFormat dateFormat) {
        Date parse = null;
        try {
            parse = dateFormat.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return parse;
    }

    @Override public String toString() {
        return "Changeset{" +
                "id='" + id + '\'' +
                ", committer='" + committer + '\'' +
                ", date='" + date + '\'' +
                ", comment=" + comment +
                ", items=" + items +
                '}';
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}

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

package com.thoughtworks.studios.shine.cruise.builder;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;

public class ChangeSetXML {

  private String revision = "revision";
  private String message = "checkin message";
  private String user = "pick-e-reader";
  private String time = "long time ago";
  private String changesetUri = "http://changeset/revision";

  public ChangeSetXML(String uri) {
    this.changesetUri = uri;
  }

  public ChangeSetXML revision(String revision) {
    this.revision = revision;
    return this;
  }

  public ChangeSetXML message(String message) {
    this.message = message;
    return this;
  }

  public ChangeSetXML user(String user) {
    this.user = user;
    return this;
  }

  public ChangeSetXML time(String time) {
    this.time = time;
    return this;
  }

  public static ChangeSetXML changesetXML(String uri) {
    return new ChangeSetXML(uri);
  }

  public static ChangeSetXML changesetXML(int rev) {
    return changesetXMLs(rev, rev)[0];
  }
  public static ChangeSetXML[] changesetXMLs(int from, int to) {
    ArrayList<ChangeSetXML> result = new ArrayList<>();

    for (int rev = from; rev <= to; rev++) {
      String time = new DateTime(3600 * rev * 1000).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z"));
      result.add(changesetXML("http://changeset/revision-" + rev).message("message " + rev).revision("" + rev).user("user" + rev).time(time));
    }

    return result.toArray(new ChangeSetXML[]{});

  }

  public String toString() {
    return "" +
      "<changeset changesetUri='" + changesetUri + "'>" +
      "  <user><![CDATA[" + this.user + "]]></user>" +
      "  <checkinTime>" + this.time + "</checkinTime>" +
      "  <revision>" + this.revision + "</revision>" +
      "  <message><![CDATA[" + this.message + "]]></message>" +
      "</changeset>";
  }

  public String getURL() {
    return this.changesetUri;
  }

}

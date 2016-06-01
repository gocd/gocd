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

import java.util.ArrayList;
import java.util.List;

public class MaterialXML {
  private String url;
  private List<ChangeSetXML> changeSets = new ArrayList<>();

  public MaterialXML(String url) {
    this.url = url;
  }

  public MaterialXML changeSets(ChangeSetXML... newChangeSets) {
    changeSets.clear();
    initChangeSets(newChangeSets);
    return this;
  }

  private void initChangeSets(ChangeSetXML... newChangeSets) {
    for (ChangeSetXML newChangeSet : newChangeSets) {
      changeSets.add(newChangeSet);
    }
  }

  public String toString(){
    String results = "" +
      "<material type='HgMaterial' url='" + url + "' >" +
      "<modifications>" +
      changeSetXML()+
      "</modifications>" +
      "</material>";

    return results;

  }

  public static MaterialXML materialXML(String url){
    return new MaterialXML(url);
  }

  private String changeSetXML() {
    String changeSetsAsString = "";
    for (ChangeSetXML cs: changeSets) {
      changeSetsAsString += cs.toString();
    }
    return changeSetsAsString;
  }
}

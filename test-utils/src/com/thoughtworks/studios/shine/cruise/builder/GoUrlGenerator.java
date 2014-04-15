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

public class GoUrlGenerator implements URLGenerator {

  private final String baseUrl;

  public GoUrlGenerator(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public GoUrlGenerator() {
    this("http://localhost:3000/go/api");
  }

  public String jobUrl(int jobId) {
    return baseUrl + "/jobs/" + jobId + ".xml";
  }


  public String stageUrl(long stageId) {
    return baseUrl + "/stages/" + stageId + ".xml";
  }

  public String pipelineUrl(String pipelineId) {
    return baseUrl + "/pipelines/" + pipelineId + ".xml";
  }

  public String artifactsURL() {
    return baseUrl + "/artifacts";
  }
}

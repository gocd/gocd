##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require File.expand_path(File.dirname(__FILE__) + '/../../../spec_helper')

describe "/api/server/server.xml" do
  include GoUtil

  it "should report server details" do
    assigns[:base_url] = "http://foo:667/go"
    assigns[:base_ssl_url] = "https://foo:666/go"
    assigns[:artifacts_dir] = "/var/lib/cruise-server"
    assigns[:shine_db_path] = "path_to_shine_tdb_directory"
    assigns[:config_dir] = "/etc/cruise"
    render 'api/server/info.xml'
    response.should have_tag("server") do
      with_tag "baseUrl", "http://foo:667/go"
      with_tag "baseUrlSsl", "https://foo:666/go"
      with_tag "artifactsDir", "/var/lib/cruise-server"
      with_tag "shineDbDirectory", "path_to_shine_tdb_directory"
      with_tag "configDirectory", "/etc/cruise"
    end
  end

  it "should handle xml sensitive as values" do
    assigns[:base_url] = "http://foo:667/go"
    assigns[:base_ssl_url] = "https://foo:666/go"
    assigns[:artifacts_dir] = "/var/lib/cruise<server"
    assigns[:keystore_path] = "path to < keystore"
    assigns[:shine_db_path] = "path to > shine tdb dir"
    assigns[:config_dir] = "/etc/c>ru<ise"

    render 'api/server/info.xml'

    root = dom4j_root_for(response.body)
    root.valueOf("//artifactsDir/.").should == "/var/lib/cruise<server"
    root.valueOf("//shineDbDirectory/.").should == "path to > shine tdb dir"
    root.valueOf("//shineDbDirectory/.").should == "path to > shine tdb dir"
    root.valueOf("//configDirectory/.").should == "/etc/c>ru<ise"
  end
end

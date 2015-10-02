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

require 'spec_helper'

describe "/api/server/server.xml" do
  include GoUtil

  it "should report server details" do
    assign(:base_url, "http://foo:667/go")
    assign(:base_ssl_url, "https://foo:666/go")
    assign(:artifacts_dir, "/var/lib/cruise-server")
    assign(:shine_db_path, "path_to_shine_tdb_directory")
    assign(:config_dir, "/etc/cruise")

    render :template => 'api/server/info.xml.erb'
    server = Nokogiri::XML(response.body).xpath("server")

    expect(server.xpath("baseUrl").text).to eq("http://foo:667/go")
    expect(server.xpath("baseUrlSsl").text).to eq("https://foo:666/go")
    expect(server.xpath("configDirectory").text).to eq("/etc/cruise")
    expect(server.xpath("artifactsDir").text).to eq("/var/lib/cruise-server")
    expect(server.xpath("shineDbDirectory").text).to eq("path_to_shine_tdb_directory")
  end

  it "should handle xml sensitive as values" do
    assign(:base_url, "http://foo:667/go")
    assign(:base_ssl_url, "https://foo:666/go")
    assign(:artifacts_dir, "/var/lib/cruise<server")
    assign(:keystore_path, "path to < keystore")
    assign(:shine_db_path, "path to > shine tdb dir")
    assign(:config_dir, "/etc/c>ru<ise")

    render :template => 'api/server/info.xml.erb'

    root = dom4j_root_for(response.body)
    expect(root.valueOf("//artifactsDir/.")).to eq("/var/lib/cruise<server")
    expect(root.valueOf("//shineDbDirectory/.")).to eq("path to > shine tdb dir")
    expect(root.valueOf("//shineDbDirectory/.")).to eq("path to > shine tdb dir")
    expect(root.valueOf("//configDirectory/.")).to eq("/etc/c>ru<ise")
  end
end

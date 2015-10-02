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
load File.join(File.dirname(__FILE__), 'material_controller_examples.rb')

describe Admin::Materials::SvnController do
  before do
    @material = MaterialConfigsMother.gitMaterialConfig("svn://foo.com")
    @short_material_type = 'svn'
  end

  it_should_behave_like :material_controller

  def new_material
    SvnMaterialConfig.new("", "", "", false)
  end

  def assert_successful_create
    svn_material_config = SvnMaterialConfig.new("new-url", "loser", "secret", true)
    svn_material_config.setName(CaseInsensitiveString.new('new-some-kinda-material'))
    svn_material_config.setConfigAttributes({SvnMaterialConfig::FOLDER => "folder", SvnMaterialConfig::CHECK_EXTERNALS => 'true'})
    @pipeline.materialConfigs().get(1).should == svn_material_config
  end

  def update_payload
    {:materialName =>  "new-some-kinda-material", :url => "new-url", :folder => "folder", :userName => "loser", :password => "secret", :checkExternals => "true"}
  end

  def assert_successful_update
    @pipeline.materialConfigs().get(0).getUrl().should == "new-url"
    @pipeline.materialConfigs().get(0).getName().should == CaseInsensitiveString.new("new-some-kinda-material")
  end

  def setup_other_form_objects
    #nothing to load
  end

  def setup_for_new_material
    #nothing to load
  end
end

#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'
require_relative 'material_controller_examples'

describe Admin::Materials::TfsController do

  import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig unless defined? TfsMaterialConfig

  before do
    @material = com.thoughtworks.go.helper.MaterialConfigsMother.tfs(GoCipher.new, UrlArgument.new("http://10.4.4.101:8080/tfs/Sample"), "loser", "domain", "passwd", "walk_this_path")
    @short_material_type = 'tfs'
  end

  it_should_behave_like :material_controller

  def new_material
    com.thoughtworks.go.helper.MaterialConfigsMother.tfs(GoCipher.new, UrlArgument.new(""), "", "", "", "")
  end

  def assert_successful_create
    tfs_material_config = com.thoughtworks.go.helper.MaterialConfigsMother.tfs(GoCipher.new, UrlArgument.new("http://10.4.4.101:8080/tfs/Sample"), "loser", "domain", "passwd", "walk_this_path")
    tfs_material_config.setName(CaseInsensitiveString.new('new-some-kinda-material'))
    tfs_material_config.setConfigAttributes({ScmMaterialConfig::FOLDER => "my-folder"})
    expect(@pipeline.materialConfigs().get(1)).to eq(tfs_material_config)
  end

  def update_payload
    {:materialName =>  "new-some-kinda-material", :url => "http://10.4.4.101:8080/tfs/Sample", :folder => "my-folder", :username => "loser", :password => "passwd", :projectPath => "walk_this_path", :domain => "domain"}
  end

  def assert_successful_update
    expect(@pipeline.materialConfigs().get(0).getUrl()).to eq("http://10.4.4.101:8080/tfs/Sample")
    expect(@pipeline.materialConfigs().get(0).getName()).to eq(CaseInsensitiveString.new("new-some-kinda-material"))
  end

  def setup_other_form_objects
    #nothing to load
  end

  def setup_for_new_material
    #nothing to load
  end
end

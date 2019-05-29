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

describe Admin::Materials::P4Controller do
  before do
    @material = MaterialConfigsMother.p4MaterialConfig("tele.port:8154", "loser", "secret", "through_window", true)
    @short_material_type = 'p4'
  end

  it_should_behave_like :material_controller

  def new_material
    com.thoughtworks.go.helper.MaterialConfigsMother.p4("", "")
  end

  def assert_successful_create
    p4_material_config = MaterialConfigsMother.p4MaterialConfig("tele.port:8154", "loser", "secret", "through_window", true)
    p4_material_config.setName(CaseInsensitiveString.new("new-some-kinda-material"))
    p4_material_config.setAutoUpdate(true)
    p4_material_config.setConfigAttributes({P4MaterialConfig::FOLDER => "folder", P4MaterialConfig::USE_TICKETS => 'true'})
    expect(@pipeline.materialConfigs().get(1)).to eq(p4_material_config)
  end

  def update_payload
    {:materialName => "new-some-kinda-material", :serverAndPort => "tele.port:8154", :folder => "folder", :userName => "loser", :password => "secret", :view => "through_window", :useTickets => 'true'}
  end

  def assert_successful_update
    expect(@pipeline.materialConfigs().get(0).getServerAndPort()).to eq("tele.port:8154")
    expect(@pipeline.materialConfigs().get(0).getView()).to eq("through_window")
    expect(@pipeline.materialConfigs().get(0).getName()).to eq(CaseInsensitiveString.new("new-some-kinda-material"))
  end

  def setup_other_form_objects
    #nothing to load
  end

  def setup_for_new_material
    #nothing to load
  end
end

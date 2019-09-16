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

module Admin::Materials
  class SvnController < ::Admin::MaterialsController
    private

    def load_new_material(cruise_config)
      svnMaterialConfig = SvnMaterialConfig.new()
      svnMaterialConfig.setUrl("")
      svnMaterialConfig.setUserName("")
      svnMaterialConfig.setPassword("")
      svnMaterialConfig.setCheckExternals(false)
      assert_load :material, svnMaterialConfig
    end
  end
end
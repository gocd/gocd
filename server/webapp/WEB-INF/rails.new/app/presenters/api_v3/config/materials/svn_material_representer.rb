##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

module ApiV3
  module Config
    module Materials
      class SvnMaterialRepresenter < ScmMaterialRepresenter
        include ApiV3::Config::Materials::EncryptedPasswordSupport #password property is parsed and encrypted in the module

        property :check_externals
        property :user_name, as: :username
        property :encrypted_password, skip_nil: true, skip_parse: true

      end

    end
  end
end

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

module ApiV4
  module Admin
    module Pipelines
      module Materials
        module EncryptedPasswordSupport
          def from_hash(data, options={})
            super
            data = data.with_indifferent_access
            encrypted_password = Services.password_deserializer.deserialize(data[:password], data[:encrypted_password], represented)
            represented.setEncryptedPassword(encrypted_password)
            represented
          end
        end
      end
    end
  end
end
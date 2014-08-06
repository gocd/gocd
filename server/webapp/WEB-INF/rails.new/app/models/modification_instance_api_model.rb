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

class ModificationInstanceAPIModel
  attr_reader :id, :revision, :modified_time, :user_name, :comment, :email_address, :modified_files

  def initialize(modification_instance_model)
    @id = modification_instance_model.getId() unless modification_instance_model.getId() == nil
    @revision = modification_instance_model.getRevision() unless modification_instance_model.getRevision() == nil
    @modified_time = modification_instance_model.getModifiedTime().getTime() unless modification_instance_model.getModifiedTime() == nil || modification_instance_model.getModifiedTime().getTime() == nil
    @user_name = modification_instance_model.getUserName() unless modification_instance_model.getUserName() == nil
    @comment = modification_instance_model.getComment() unless modification_instance_model.getComment() == nil
    @email_address = modification_instance_model.getEmailAddress() unless modification_instance_model.getEmailAddress() == nil
    if modification_instance_model.getModifiedFiles() != nil
      @modified_files = []
      modification_instance_model.getModifiedFiles().each do |modified_file|
        @modified_files << ModifiedFileAPIModel.new(modified_file)
      end
    end
  end
end
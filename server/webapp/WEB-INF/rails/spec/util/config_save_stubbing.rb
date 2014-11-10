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

module ConfigSaveStubbing
  def stub_config_save(cruise_config, options, &blk)
    stub_for_config_save(cruise_config, options, proc {|update_command, node| update_command.subject(node)}, &blk)
  end

  def stub_config_save_with_subject subject, cruise_config = @cruise_config, options = {}
    stub_for_config_save(cruise_config, options, proc {|_, _| subject}) do |update_command, *_|
      Thread.current[:update_command] = update_command
    end
  end

  def stub_save_for_success cruise_config = @cruise_config, options = {}
    stub_config_save(cruise_config,options) do |update_command, *_|
      Thread.current[:update_command] = update_command
    end
  end

  def stub_save_for_validation_error cruise_config = @cruise_config, &failure
    stub_config_save(cruise_config,{}) do |_, result, node|
      failure.call(result, cruise_config, node)
    end
  end

  def from_thd key
    val = Thread.current[key]
    Thread.current[key] = nil
    val
  end

  def assert_save_arguments md5 = "1234abcd"
    from_thd(:assertion_map).should == {:md5 => md5, :user => @user, :result => @result}
  end

  def assert_update_command type, *includes
    command = from_thd(:update_command)
    command.kind_of?(type).should be_true
    command.class.included_modules.should include(*includes)
  end

  private
  def stub_for_config_save(cruise_config, options, subject_partial, &blk)
    stub_for_config_save_blah(cruise_config, options, subject_partial, proc {|update_command, node| update_command.update(node)}, &blk)
  end

  def stub_for_config_save_new_without_update(cruise_config, options, subject_partial, &blk)
    stub_for_config_save_blah(cruise_config, options, subject_partial, proc {|_, _| }, &blk)
  end

  def stub_for_config_save_blah(cruise_config, options, subject_partial, update_partial, &blk)
    assertion_map = Thread.current[:assertion_map] = {}
    @go_config_service.should_receive(:updateConfigFromUI) do |update_command, md5, user, result|
      assertion_map[:md5] = md5
      assertion_map[:user] = user
      assertion_map[:result] = result
      cloner = com.rits.cloning.Cloner.new()
      node = update_command.node(cruise_config)
      update_partial.call(update_command, node)
      subject = subject_partial.call(update_command, node)
      after = cloner.deepClone(cruise_config)
      blk.call(update_command, result, node)
      ConfigUpdateResponse.new(cruise_config, node, subject, Class.new do
        include com.thoughtworks.go.config.ConfigAwareUpdate

        def initialize after
          @after = after
        end

        def configAfter
          @after
        end
      end.new(after), options[:config_save_state])
    end
  end
end
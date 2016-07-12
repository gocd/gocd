##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

require 'spec_helper'

describe ApiV2::Config::Tasks::TaskRepresenter do
  include TaskMother

  shared_examples_for 'tasks' do

    describe :serialize do
      it 'should render task with hal representation' do
        presenter          = ApiV2::Config::Tasks::TaskRepresenter.new(existing_task)
        actual_json        = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_task_hash = task_hash
        expect(actual_json).to eq(expected_task_hash)
      end

      it 'should render task with hal representation with run_if' do
        run_if_config                            = [RunIfConfig::PASSED, RunIfConfig::FAILED, RunIfConfig::ANY].sample
        presenter                                = ApiV2::Config::Tasks::TaskRepresenter.new(with_run_if(run_if_config, existing_task))
        actual_json                              = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_task_hash                       = task_hash
        expected_task_hash[:attributes][:run_if] = [run_if_config.to_s]
        expect(actual_json).to eq(expected_task_hash)
      end

      it 'should render task with hal representation with oncancel' do
        on_cancel_task = ant_task('build.xml', 'package', 'hero/ka/directory')
        existing_task.setCancelTask(on_cancel_task)
        presenter                                   = ApiV2::Config::Tasks::TaskRepresenter.new(existing_task)
        actual_json                                 = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_task_hash                          = task_hash
        expected_task_hash[:attributes][:on_cancel] = ApiV2::Config::Tasks::OnCancelRepresenter.new(existing_task.getOnCancelConfig).to_hash(url_builder: UrlBuilder.new)
        expect(actual_json).to eq(expected_task_hash)
      end
    end

    describe :deserialize do
      it 'should convert hash to Task' do
        new_task = task_type.new

        presenter = ApiV2::Config::Tasks::TaskRepresenter.new(new_task)
        presenter.from_hash(ApiV2::Config::Tasks::TaskRepresenter.new(existing_task).to_hash(url_builder: UrlBuilder.new))
        expect(new_task).to eq(existing_task)
      end

      it 'should convert hash with run_if to Task with run_if' do
        new_task = task_type.new

        run_if_config    = [RunIfConfig::PASSED, RunIfConfig::FAILED, RunIfConfig::ANY].sample
        task_with_run_if = with_run_if(run_if_config, existing_task)

        presenter = ApiV2::Config::Tasks::TaskRepresenter.new(new_task)
        presenter.from_hash(ApiV2::Config::Tasks::TaskRepresenter.new(task_with_run_if).to_hash(url_builder: UrlBuilder.new))

        expect(new_task).to eq(task_with_run_if)
      end

      it 'should convert hash with oncancel to Task with oncancel' do
        on_cancel_task = ant_task('build.xml', 'package', 'hero/ka/directory')
        existing_task.setCancelTask(on_cancel_task)

        new_task = task_type.new

        presenter = ApiV2::Config::Tasks::TaskRepresenter.new(new_task)
        presenter.from_hash(ApiV2::Config::Tasks::TaskRepresenter.new(existing_task).to_hash(url_builder: UrlBuilder.new))

        expect(new_task).to eq(existing_task)
      end
    end
  end

  describe :exec do
    it_should_behave_like 'tasks'

    def existing_task
      @task ||= simple_exec_task_with_args_list
    end

    def task_type
      ExecTask
    end

    def task_hash
      {
        type:       'exec',
        attributes: {
          command:           'ls',
          arguments:         ['-l', '-a'],
          working_directory: 'hero/ka/directory',
          run_if:     [],
          on_cancel:  nil
        }
      }
    end

    it 'should represent errors' do
      task         = ExecTask.new()
      task.setWorkingDirectory("../outside")
      validation_context = double('ValidationContext')
      validation_context.stub(:isWithinPipelines).and_return(true)
      pipeline = PipelineConfigMother::createPipelineConfigWithStage("this_pipeline", "stage")
      validation_context.stub(:getPipeline).and_return(pipeline)
      validation_context.stub(:getStage).and_return(pipeline.first)
      validation_context.stub(:getJob).and_return(pipeline.first.getJobs.first)
      task.validateTree(validation_context)

      presenter   = ApiV2::Config::Tasks::TaskRepresenter.new(task)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(errors_hash)
      expect(errors_hash[:errors].keys.size).to eq(task.errors.size)
    end

    def errors_hash
      {
          type:       'exec',
          attributes: { run_if: [], on_cancel: nil, command: "", working_directory: "../outside"},
          errors: {
            command: ["Command cannot be empty"],
            working_directory: ["The path of the working directory for the custom command in job 'dev' in stage 'stage' of pipeline 'this_pipeline' is outside the agent sandbox."]
          }
      }
    end
  end

  describe :ant do
    it_should_behave_like 'tasks'

    def existing_task
      @task ||= ant_task('build.xml', 'package', 'hero/ka/directory')
    end

    def task_type
      AntTask
    end

    def task_hash
      {
        type:       'ant',
        attributes: {
          working_directory: 'hero/ka/directory',
          build_file:        'build.xml',
          target:            'package',
          on_cancel:         nil,
          run_if:            []
        }
      }
    end

    it 'should represent errors' do
      task         = AntTask.new()
      task.setWorkingDirectory("../outside")
      validation_context = double('ValidationContext')
      validation_context.stub(:isWithinPipelines).and_return(true)
      pipeline = PipelineConfigMother::createPipelineConfigWithStage("this_pipeline", "stage")
      validation_context.stub(:getPipeline).and_return(pipeline)
      validation_context.stub(:getStage).and_return(pipeline.first)
      validation_context.stub(:getJob).and_return(pipeline.first.getJobs.first)
      task.validateTree(validation_context)

      presenter   = ApiV2::Config::Tasks::TaskRepresenter.new(task)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(errors_hash)
      expect(errors_hash[:errors].keys.size).to eq(task.errors.size)
    end

    def errors_hash
      {
          type:       'ant',
          attributes: { run_if: [], on_cancel: nil, working_directory: "../outside", build_file: nil, target: nil},
          errors: {
            working_directory: ["Task of job 'dev' in stage 'stage' of pipeline 'this_pipeline' has path '../outside' which is outside the working directory."]
          }
      }
    end
  end

  describe :nant do
    it_should_behave_like 'tasks'

    def task_type
      NantTask
    end

    def existing_task
      @task ||=nant_task('build.xml', 'package', 'hero/ka/directory')
    end

    def task_hash
      {
        type:       'nant',
        attributes: {
          build_file:        'build.xml',
          target:            'package',
          working_directory: 'hero/ka/directory',
          nant_path:         nil,
          run_if:     [],
          on_cancel:  nil
        }
      }
    end

    it 'should represent errors' do
      task         = NantTask.new()
      task.setWorkingDirectory("../outside")
      validation_context = double('ValidationContext')
      validation_context.stub(:isWithinPipelines).and_return(true)
      pipeline = PipelineConfigMother::createPipelineConfigWithStage("this_pipeline", "stage")
      validation_context.stub(:getPipeline).and_return(pipeline)
      validation_context.stub(:getStage).and_return(pipeline.first)
      validation_context.stub(:getJob).and_return(pipeline.first.getJobs.first)
      task.validateTree(validation_context)

      presenter   = ApiV2::Config::Tasks::TaskRepresenter.new(task)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(errors_hash)
      expect(errors_hash[:errors].keys.size).to eq(task.errors.size)
    end

    def errors_hash
      {
          type:       'nant',
          attributes: { run_if: [], on_cancel: nil, working_directory: "../outside", build_file: nil, target: nil, nant_path:nil},
          errors: {
            working_directory: ["Task of job 'dev' in stage 'stage' of pipeline 'this_pipeline' has path '../outside' which is outside the working directory."]
          }
      }
    end
  end

  describe :rake do
    it_should_behave_like 'tasks'

    def existing_task
      @task ||= rake_task('rakefile', 'package', 'hero/ka/directory')
    end

    def task_type
      RakeTask
    end

    def task_hash
      {
        type:       'rake',
        attributes: {
          build_file:        'rakefile',
          target:            'package',
          working_directory: 'hero/ka/directory',
          run_if:     [],
          on_cancel:  nil
        }
      }
    end

    it 'should represent errors' do
      task         = RakeTask.new()
      task.setWorkingDirectory("../outside")
      validation_context = double('ValidationContext')
      validation_context.stub(:isWithinPipelines).and_return(true)
      pipeline = PipelineConfigMother::createPipelineConfigWithStage("this_pipeline", "stage")
      validation_context.stub(:getPipeline).and_return(pipeline)
      validation_context.stub(:getStage).and_return(pipeline.first)
      validation_context.stub(:getJob).and_return(pipeline.first.getJobs.first)
      task.validateTree(validation_context)

      presenter   = ApiV2::Config::Tasks::TaskRepresenter.new(task)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(errors_hash)
      expect(errors_hash[:errors].keys.size).to eq(task.errors.size)
    end

    def errors_hash
      {
          type:       'rake',
          attributes: {run_if: [], on_cancel: nil, working_directory: "../outside", build_file: nil, target: nil},
          errors: {
            working_directory: ["Task of job 'dev' in stage 'stage' of pipeline 'this_pipeline' has path '../outside' which is outside the working directory."]
          }

      }
    end
  end

  describe :fetch do

    it_should_behave_like 'tasks'

    def existing_task
      @task ||= fetch_task
    end

    def task_type
      FetchTask
    end

    it 'should represent errors' do
      fetch_task         = FetchTask.new(CaseInsensitiveString.new('this_pipeline'), CaseInsensitiveString.new(''), CaseInsensitiveString.new(''), "../src", "../dest")
      validation_context = double('ValidationContext')
      validation_context.stub(:isWithinPipelines).and_return(true)
      pipeline = PipelineConfigMother::createPipelineConfigWithStage("this_pipeline", "stage")
      validation_context.stub(:getPipeline).and_return(pipeline)
      validation_context.stub(:getStage).and_return(pipeline.first)
      validation_context.stub(:getJob).and_return(pipeline.first.getJobs.first)
      fetch_task.validateTree(validation_context)

      presenter   = ApiV2::Config::Tasks::TaskRepresenter.new(fetch_task)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(errors_hash)
      expect(errors_hash[:errors].keys.size).to eq(fetch_task.errors.size)
    end

    def errors_hash
      {
        type:       'fetch',
        attributes: {pipeline: "this_pipeline", stage: "", job: "", is_source_a_file: true, source: "../src", destination: "../dest", run_if: [], on_cancel: nil},
        errors:     {
          job:   ['Job is a required field.'],
          stage: ['Stage is a required field.'],
          destination: ["Task of job 'dev' in stage 'stage' of pipeline 'this_pipeline' has dest path '../dest' which is outside the working directory."],
          source: ["Task of job 'dev' in stage 'stage' of pipeline 'this_pipeline' has src path '../src' which is outside the working directory."]
        }
      }
    end

    def task_hash
      {
        type:       'fetch',
        attributes: {
          pipeline:         'pipeline',
          stage:            'stage',
          job:              'job',
          source:           'src',
          is_source_a_file: true,
          destination:      'dest',
          run_if:     [],
          on_cancel:  nil
        }
      }
    end

  end

  describe :pluggable do
    it_should_behave_like 'tasks'

    before(:each) do
      task            = TaskMother::StubTask.new
      simple_property = TaskConfigProperty.new('simple_key', 'value')
      secure_property = TaskConfigProperty.new('secure_key', 'encrypted').with(com.thoughtworks.go.plugin.api.config.Property::SECURE, true)

      task.config.add(simple_property)
      task.config.add(secure_property)
      task_preference = TaskPreference.new(task)

      PluggableTaskConfigStore.store().setPreferenceFor("curl", task_preference);
    end



    def existing_task
      @task ||= begin
        config = [
          ConfigurationProperty.new(ConfigurationKey.new('simple_key'), ConfigurationValue.new('value')),
          ConfigurationProperty.new(ConfigurationKey.new('secure_key'), EncryptedConfigurationValue.new('encrypted'))
        ]
        plugin_task('curl', config)
      end
    end

    def task_type
      PluggableTask
    end

    def task_hash
      {
        type:       'pluggable_task',
        attributes: {
          plugin_configuration: {
            id:      'curl',
            version: '1.0'
          },
          configuration:        [
                                  {
                                    key:   'simple_key',
                                    value: 'value'
                                  },
                                  {
                                    key:             'secure_key',
                                    encrypted_value: 'encrypted'
                                  }
                                ],
          on_cancel:            nil,
          run_if:               [],
        }
      }
    end
  end

  it 'should raise error when de-serializing a type that does not exist' do
    expect do
      ApiV2::Config::Tasks::TaskRepresenter.from_hash({type: :foo})
    end.to raise_error(ApiV2::UnprocessableEntity, /Invalid task type 'foo'. It has to be one of/)
  end
end

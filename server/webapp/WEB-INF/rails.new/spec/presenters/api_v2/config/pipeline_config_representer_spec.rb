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

describe ApiV2::Config::PipelineConfigRepresenter do

  describe :serialize do
    it 'renders a pipeline with hal representation' do
      presenter   = ApiV2::Config::PipelineConfigRepresenter.new(get_pipeline_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :find, :doc)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/pipelines/wunderbar')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/pipelines/:pipeline_name')
      expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#pipeline-config')
      actual_json.delete(:_links)
      expect(actual_json).to eq(pipeline_hash)
    end

    it 'should serialize pipeline with template' do
      pipeline_config = pipeline_with_template
      presenter       = ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config)
      actual_json     = presenter.to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)
      expect(actual_json).to eq(pipeline_with_template_hash)
    end

    def pipeline_with_template_hash
      {
        label_template:          "${COUNT}",
        enable_pipeline_locking: false,
        name:                    "wunderbar",
        template:                "template1",
        parameters:              [],
        environment_variables:   [],
        materials:               pipeline_with_template.materialConfigs().collect { |j| ApiV2::Config::Materials::MaterialRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
        stages:                  nil,
        tracking_tool:           nil,
        timer:                   nil
      }
    end

    def pipeline_with_template
      pipeline_config = PipelineConfig.new(CaseInsensitiveString.new("wunderbar"), "${COUNT}", nil, true, MaterialConfigsMother.defaultMaterialConfigs(), ArrayList.new)
      pipeline_config.setTemplateName(CaseInsensitiveString.new("template1"))
      pipeline_config
    end
  end

  describe :deserialise do

    it "should convert from minimal json to PipelineConfig" do
      pipeline_config = PipelineConfig.new

      ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash(pipeline_hash_basic)
      expect(pipeline_config.name.to_s).to eq("wunderbar")
      expect(pipeline_config.getParams.isEmpty).to eq(true)
      expect(pipeline_config.variables.isEmpty).to eq(true)
    end

    def pipeline_hash_basic
      {
        label_template:          "foo-1.0.${COUNT}-${svn}",
        enable_pipeline_locking: false,
        name:                    "wunderbar",
        materials:               [
                                   {
                                     type:        "svn",
                                     attributes:  {
                                       url:             "http://some/svn/url",
                                       destination:     "svnDir",
                                       check_externals: false
                                     },
                                     name:        "http___some_svn_url",
                                     auto_update: true
                                   }
                                 ],
        stages:                  [
                                   {
                                     name:                    "stage1",
                                     fetch_materials:         true,
                                     clean_working_directory: false,
                                     never_cleanup_artifacts: false,
                                     jobs:                    [
                                                                {
                                                                  name:  "defaultJob",
                                                                  tasks: [
                                                                           {
                                                                             type:       "ant",
                                                                             attributes: {
                                                                               working_dir: "working-directory",
                                                                               build_file:  "build-file",
                                                                               target:      "target"
                                                                             }
                                                                           }
                                                                         ]
                                                                }
                                                              ]
                                   }
                                 ],
      }
    end

    describe :pipeline_with_environment_varibales do
      it "should convert pipeline hash with environment variables  to PipelineConfig" do
        pipeline_config = PipelineConfig.new

        environment_variables=[
          {
            name:   'plain',
            value:  'plain',
            secure: false
          },
          {
            secure:          true,
            name:            'secure',
            encrypted_value: GoCipher.new.encrypt('confidential')
          }
        ]

        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ environment_variables: environment_variables })
        expect(pipeline_config.variables.map(&:name)).to eq(['plain', 'secure'])
      end

      it "should convert pipeline hash with empty environment variables  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ environment_variables: [] })
        expect(pipeline_config.variables.size).to eq(0)
      end
    end

    describe :pipeline_with_parmas do
      it "should convert pipeline hash with parameters to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ parameters: [{
                                                                                                 name:  "command",
                                                                                                 value: "echo"
                                                                                               }, {
                                                                                                 name:  "command",
                                                                                                 value: "sleep"
                                                                                               }] })
        expect(pipeline_config.getParams.map(&:name)).to eq(['command', 'command'])
      end

      it "should convert pipeline hash with empty parmas to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ parameters: nil })
        expect(pipeline_config.getParams.size).to eq(0)
      end
    end

    describe :pipeline_with_materials do
      it "should convert pipeline hash with materials  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ materials: [
                                                                                               {
                                                                                                 type:       'git',
                                                                                                 attributes: {
                                                                                                   url:              "http://user:password@funk.com/blank",
                                                                                                   destination:      "destination",
                                                                                                   filter:           {
                                                                                                     ignore: %w(**/*.html **/foobar/)
                                                                                                   },
                                                                                                   branch:           'branch',
                                                                                                   submodule_folder: 'sub_module_folder',
                                                                                                   name:             'AwesomeGitMaterial',
                                                                                                   auto_update:      false
                                                                                                 }
                                                                                               },
                                                                                               {
                                                                                                 type:       'svn',
                                                                                                 attributes: {
                                                                                                   url:                "url",
                                                                                                   destination:        "svnDir",
                                                                                                   filter:             {
                                                                                                     ignore: [
                                                                                                               "*.doc"
                                                                                                             ]
                                                                                                   },
                                                                                                   name:               "svn-material",
                                                                                                   auto_update:        false,
                                                                                                   check_externals:    true,
                                                                                                   username:           "user",
                                                                                                   encrypted_password: GoCipher.new.encrypt("pass")
                                                                                                 }
                                                                                               }
                                                                                             ] })
        expect(pipeline_config.materialConfigs.map(&:type)).to eq(['GitMaterial', 'SvnMaterial'])
      end

      it "should convert pipeline hash with empty materials  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ materials: nil })
        expect(pipeline_config.materialConfigs.size).to eq(0)
      end

      it 'should raise exception when passing invalid material type' do
        hash             = pipeline_hash_basic
        hash[:materials] = [{ type: 'bad-material-type', attributes: { foo: 'bar' } }]
        pipeline_config  = PipelineConfig.new

        expect do
          ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash(hash)
        end.to raise_error(ApiV2::UnprocessableEntity, /Invalid material type 'bad-material-type'. It has to be one of/)
      end

    end

    describe :pipeline_with_stages do
      it "should convert pipeline hash with stages  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        stages          =[{
                            name:                    'stage1',
                            fetch_materials:         true,
                            clean_working_directory: false,
                            never_cleanup_artifacts: false,
                            approval:                {
                              type:          'success',
                              authorization: {
                                roles: [],
                                users: []
                              }
                            },
                            environment_variables:   [
                                                       {
                                                         name:   'plain',
                                                         value:  'plain',
                                                         secure: false
                                                       },
                                                       {
                                                         secure:          true,
                                                         name:            'secure',
                                                         encrypted_value: GoCipher.new.encrypt('confidential')
                                                       }
                                                     ],
                            jobs:                    [{
                                                        name:               'some-job',
                                                        run_on_all_agents:  true,
                                                        run_instance_count: '3',
                                                        timeout:            '100',
                                                      }]
                          }]

        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ stages: stages })
        expect(pipeline_config.getStages.map(&:name).map(&:to_s)).to eq(['stage1'])
        expect(pipeline_config.getStages.first.getJobs.map(&:name).map(&:to_s)).to eq(['some-job'])
        expect(pipeline_config.getStages.first.variables.map(&:name)).to eq(['plain', 'secure'])

      end

      it "should convert pipeline hash with empty stages  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ stages: nil })
        expect(pipeline_config.getStages.size).to eq(0)
      end

    end

    describe :pipeline_with_tracking_tool do
      it "should convert pipeline hash with tracking tool  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ tracking_tool: {
          type:       "generic",
          attributes: {
            url_pattern:  "link",
            regex: "regex"
          }
        } })
        expect(pipeline_config.getTrackingTool.getLink).to eq('link')
        expect(pipeline_config.getTrackingTool.getRegex).to eq('regex')
      end

      it 'should raise exception when passing invalid tracking tool type' do
        pipeline_config = PipelineConfig.new
        expect do
          ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ tracking_tool: {
            type:       "bad-tracking-tool",
            attributes: {
              link:  "link",
              regex: "regex"
            }
          } })
        end.to raise_error(ApiV2::UnprocessableEntity, /Invalid Tracking Tool type 'bad-tracking-tool'. It has to be one of/)
      end


    end

    it "should convert from full blown document to PipelineConfig" do
      pipeline_config = PipelineConfig.new

      ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash(pipeline_hash)
      expect(pipeline_config).to eq(get_pipeline_config)
    end


    describe :pipeline_with_timer do
      it "should convert pipeline hash with timer  to PipelineConfig" do
        pipeline_config = PipelineConfig.new
        ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config).from_hash({ timer: { spec: "0 0 22 ? * MON-FRI", only_on_changes: true } })
        expect(pipeline_config.getTimer.getOnlyOnChanges).to eq(true)
      end
    end

    it "should render errors" do
      pipeline_config = PipelineConfig.new(CaseInsensitiveString.new("wunderbar"), "", "", true, nil, ArrayList.new)
      config          = BasicCruiseConfig.new(BasicPipelineConfigs.new("grp", Authorization.new, pipeline_config))

      pipeline_config.validateTree(com.thoughtworks.go.config.PipelineConfigSaveValidationContext::forChain(true, "grp", config, pipeline_config))

      presenter   = ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_hash_with_errors)
    end

    it "should render errors on nested objects" do
      pipeline_config = get_invalid_pipeline_config
      config          = BasicCruiseConfig.new(BasicPipelineConfigs.new("grp", Authorization.new, get_pipeline_config))
      pipeline_config.validateTree(com.thoughtworks.go.config.PipelineConfigSaveValidationContext::forChain(true, "grp", config, pipeline_config))

      presenter   = ApiV2::Config::PipelineConfigRepresenter.new(pipeline_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)
      expect(actual_json).to eq(expected_hash_with_nested_errors)
    end

  end

  def expected_hash_with_errors
    {
      label_template:          "",
      enable_pipeline_locking: false,
      name:                    "wunderbar",
      template:                nil,
      parameters:              [],
      environment_variables:   [],
      materials:               [],
      stages:                  nil,
      tracking_tool:           nil,
      timer:                   { spec: "", only_on_changes: true, errors: { spec: ["Invalid cron syntax: Unexpected end of expression."] } },
      errors:                  {
        materials:     ["A pipeline must have at least one material"],
        pipeline:      ["Pipeline 'wunderbar' does not have any stages configured. A pipeline must have at least one stage."],
        label_template: ["Label cannot be blank. Label should be composed of alphanumeric text, it should contain the builder number as ${COUNT}, can contain a material revision as ${<material-name>} of ${<material-name>[:<number>]}, or use params as \#{<param-name>}."]
      }
    }
  end

  def expected_hash_with_nested_errors
    {
      label_template:          "foo-1.0.${COUNT}-${svn}",
      enable_pipeline_locking: false,
      name:                    "wunderbar",
      template:                nil,
      parameters:              [
                                 {
                                   name:   nil, value: "echo",
                                   errors: {
                                     name: [
                                             "Parameter cannot have an empty name for pipeline 'wunderbar'.",
                                             "Invalid parameter name 'null'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."
                                           ]
                                   }
                                 }
                               ],
      environment_variables:   [{:secure=>false, :name=>"", :value=>"", :errors=>{:name=>["Environment Variable cannot have an empty name for pipeline 'wunderbar'."]}}],
      materials:               [
                                 {
                                   type: "svn", attributes: { url: "http://some/svn/url", destination: "svnDir", filter: nil, invert_filter:false, name: "http___some_svn_url", auto_update: true, check_externals: false, username: nil }
                                 },
                                 {
                                   type:   "git", attributes: { url: nil, destination: nil, filter: nil, invert_filter:false, name: nil, auto_update: true, branch: "master", submodule_folder: nil, shallow_clone: false },
                                   errors: { destination: ["Destination directory is required when specifying multiple scm materials"], url: ["URL cannot be blank"] }
                                 }
                               ],
      stages:                  [{ name: "stage1", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: { type: "success", authorization: { :roles => [], :users => [] } }, environment_variables: [], jobs: [] }],
      timer:                   { spec: "0 0 22 ? * MON-FRI", only_on_changes: true },
      tracking_tool:           {
        type:   "generic", attributes: { url_pattern: "", regex: "" },
        errors: {
          regex: ["Regex should be populated"],
          url_pattern:  ["Link should be populated", "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time."]

        }
      },
      errors:                  {
        label_template: ["You have defined a label template in pipeline wunderbar that refers to a material called svn, but no material with this name is defined."]
      }
    }
  end

  def get_invalid_pipeline_config
    material_configs = MaterialConfigsMother.defaultMaterialConfigs()
    git              = GitMaterialConfig.new
    git.setFolder(nil);
    material_configs.add(git);

    pipeline_config = PipelineConfig.new(CaseInsensitiveString.new("wunderbar"), "foo-1.0.${COUNT}-${svn}", "0 0 22 ? * MON-FRI", true, material_configs, ArrayList.new)
    pipeline_config.addParam(ParamConfig.new(nil, "echo"))
    pipeline_config.addEnvironmentVariable('','')
    pipeline_config.add(StageConfigMother.stageConfig("stage1"))
    pipeline_config.setTrackingTool(TrackingTool.new())
    pipeline_config
  end


  def get_pipeline_config
    material_configs = MaterialConfigsMother.defaultMaterialConfigs()
    pipeline_config  = PipelineConfig.new(CaseInsensitiveString.new("wunderbar"), "foo-1.0.${COUNT}-${svn}", "0 0 22 ? * MON-FRI", true, material_configs, ArrayList.new)
    pipeline_config.setVariables(EnvironmentVariablesConfigMother.environmentVariables())
    pipeline_config.addParam(ParamConfig.new("COMMAND", "echo"))
    pipeline_config.addParam(ParamConfig.new("WORKING_DIR", "/repo/branch"))
    pipeline_config.add(StageConfigMother.stageConfigWithEnvironmentVariable("stage1"))
    pipeline_config.setTrackingTool(TrackingTool.new("link", "regex"))
    pipeline_config
  end

  def pipeline_hash
    {
      label_template:          "foo-1.0.${COUNT}-${svn}",
      enable_pipeline_locking: false,
      name:                    "wunderbar",
      template:                nil,
      parameters:              get_pipeline_config.getParams().collect { |j| ApiV2::Config::ParamRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
      environment_variables:   get_pipeline_config.variables().collect { |j| ApiV2::Config::EnvironmentVariableRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
      materials:               get_pipeline_config.materialConfigs().collect { |j| ApiV2::Config::Materials::MaterialRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
      stages:                  get_pipeline_config.getStages().collect { |j| ApiV2::Config::StageRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
      tracking_tool:           ApiV2::Config::TrackingTool::TrackingToolRepresenter.new(get_pipeline_config.getTrackingTool).to_hash(url_builder: UrlBuilder.new),
      timer:                   ApiV2::Config::TimerRepresenter.new(get_pipeline_config.getTimer).to_hash(url_builder: UrlBuilder.new)
    }
  end

end

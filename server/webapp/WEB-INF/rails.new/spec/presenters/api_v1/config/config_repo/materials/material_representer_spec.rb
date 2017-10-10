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

require 'rails_helper'
default_branch = 'master'
describe ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter do
  shared_examples_for 'materials' do

    describe "serialize" do
      it 'should render material with hal representation' do
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material)
        actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_material_hash = material_hash
        expect(actual_json).to eq(expected_material_hash)
      end

      it 'should render errors' do
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material_with_errors)
        actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_material_hash = expected_material_hash_with_errors
        expect(actual_json).to eq(expected_material_hash)
      end
    end

    describe "deserialize" do
      it 'should convert hash to Material' do
        new_material = material_type.new
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(new_material)
        presenter.from_hash(ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material).to_hash(url_builder: UrlBuilder.new))
        expect(new_material.name).to eq(existing_material.name)
        expect(new_material).to eq(existing_material)
      end
    end

  end

  describe "git" do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.gitMaterialConfig('http://user:password@funk.com/blank')
    end

    def material_type
      GitMaterialConfig
    end

    def existing_material_with_errors
      git_config = GitMaterialConfig.new(UrlArgument.new(''), '', 'dest', true, nil, false, '', CaseInsensitiveString.new('!nV@l!d'), false)
      material_configs = MaterialConfigs.new(git_config)

      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.get(0)
    end

    it "should serialize material without name" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.prepare(GitMaterialConfig.new("http://user:password@funk.com/blank"))
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(git_material_basic_hash)
    end


    it "should serialize material with blank branch" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.prepare(GitMaterialConfig.new("http://user:password@funk.com/blank", ""))
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(git_material_basic_hash)
    end

    it "should deserialize material without name" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(GitMaterialConfig.new)
      deserialized_object = presenter.from_hash({
                                                  :type => 'git',
                                                  :attributes => {
                                                    :url => "http://user:password@funk.com/blank",
                                                    :branch => "master",
                                                    :name => nil
                                                  }
                                                })
      expected = GitMaterialConfig.new("http://user:password@funk.com/blank")
      expect(deserialized_object.name.to_s).to eq("")
      expect(deserialized_object).to eq(expected)
    end

    it "should deserialize material with blank branch" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(GitMaterialConfig.new)
      deserialized_object = presenter.from_hash({
                                                  type: 'git',
                                                  attributes: {
                                                    url: "http://user:password@funk.com/blank",
                                                    branch: "",
                                                    name: nil
                                                  }
                                                })
      expect(deserialized_object.branch.to_s).to eq(default_branch)
    end

    def material_hash
      git_material_basic_hash()
    end

    def git_material_basic_hash
      {
        type: 'git',
        attributes: {
          url: "http://user:password@funk.com/blank",
          name: nil,
          branch: "master",
          auto_update: true
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "git",
        attributes: {
          url: "",
          name: "!nV@l!d",
          branch: "master",
          auto_update: true
        },
        errors: {
          name: ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          url: ["URL cannot be blank"]
        }
      }
    end

  end

  describe "svn" do
    it_should_behave_like 'materials'

    def existing_material
      material = MaterialConfigsMother.svnMaterialConfig
      material.setFolder(nil)
      material.setCheckExternals(false)
      material
    end

    def material_type
      SvnMaterialConfig
    end

    def existing_material_with_errors
      svn_config = SvnMaterialConfig.new(UrlArgument.new(''), '', '', true, GoCipher.new, true, nil, false, '', CaseInsensitiveString.new('!nV@l!d'))
      material_configs = MaterialConfigs.new(svn_config)
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.get(0)
    end

    def material_hash
      {
        type: 'svn',
        attributes: {
          url: "url",
          name: "svn-material",
          check_externals: false,
          auto_update: false,
          username: "user",
          encrypted_password: GoCipher.new.encrypt("pass")
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "svn",
        attributes: {
          url: "",
          name: "!nV@l!d",
          check_externals: true,
          auto_update: true,
          username: ""
        },
        errors: {
          name: ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          url: ["URL cannot be blank"]
        }
      }
    end
  end

  describe "hg" do
    it_should_behave_like 'materials'

    def existing_material
      material = MaterialConfigsMother.hgMaterialConfigFull
      material.setFolder(nil)
      material
    end

    def material_type
      HgMaterialConfig
    end

    def existing_material_with_errors
      hg_config = HgMaterialConfig.new(com.thoughtworks.go.util.command::HgUrlArgument.new(''), true, nil, false, nil, CaseInsensitiveString.new('!nV@l!d'))
      material_configs = MaterialConfigs.new(hg_config);
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.get(0)
    end

    def material_hash
      {
        type: 'hg',
        attributes: {
          url: "http://user:pass@domain/path##branch",
          name: "hg-material",
          auto_update: true
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "hg",
        attributes: {
          url: "",
          name: "!nV@l!d",
          auto_update: true
        },
        errors: {
          name: ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          url: ["URL cannot be blank"]
        }
      }
    end

  end

  describe "tfs" do
    it_should_behave_like 'materials'

    def existing_material
      material = MaterialConfigsMother.tfsMaterialConfig
      material.setFilter(nil)
      material.setFolder(nil)
      material
    end

    def material_type
      TfsMaterialConfig
    end

    def existing_material_with_errors
      tfs_config = TfsMaterialConfig.new(GoCipher.new, com.thoughtworks.go.util.command::HgUrlArgument.new(''), '', '', '', '/some-path/')
      material_configs = MaterialConfigs.new(tfs_config);
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.first()
    end

    def material_hash
      {
        type: 'tfs',
        attributes: {
          url: "http://10.4.4.101:8080/tfs/Sample",
          domain: "some_domain",
          username: "loser",
          encrypted_password: com.thoughtworks.go.security.GoCipher.new.encrypt("passwd"),
          project_path: "walk_this_path",
          name: "tfs-material",
          auto_update: true
        }
      }
    end

    def expected_material_hash_with_errors
      {
        :type => "tfs",
        :attributes => {
          url: "",
          name: nil,
          domain: "",
          username: "",
          project_path: "/some-path/",
          auto_update: true
        },
        errors:
          {
            url: ["URL cannot be blank"],
            username: ["Username cannot be blank"]
          }
      }
    end

  end

  describe "p4" do
    it_should_behave_like 'materials'

    def existing_material
      material = MaterialConfigsMother.p4MaterialConfigFull
      material.setFolder(nil)
      material.setFilter(nil)
      material.setUseTickets(false)
      material
    end

    def material_type
      P4MaterialConfig
    end

    def existing_material_with_errors
      p4_config = P4MaterialConfig.new('', '', '', false, '', GoCipher.new, CaseInsensitiveString.new(''), true, nil, false, 'dest/')
      material_configs = MaterialConfigs.new(p4_config)
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.first()
    end

    def material_hash
      {
        type: 'p4',
        attributes: {
          port: "host:9876",
          username: "user",
          encrypted_password: GoCipher.new.encrypt("password"),
          view: "view",
          use_tickets: false,
          name: "p4-material",
          auto_update: true
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "p4",
        attributes: {
          name: "",
          port: "",
          username: "",
          view: "",
          auto_update: true,
          use_tickets: false
        },
        errors: {
          view: ["P4 view cannot be empty."],
          port: ["P4 port cannot be empty."]
        }
      }
    end

  end

  describe :pluggable do
    before :each do
      @go_config = BasicCruiseConfig.new

    end
    it "should represent a pluggable scm material" do
      pluggable_scm_material = MaterialConfigsMother.pluggableSCMMaterialConfig()
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.prepare(pluggable_scm_material)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(pluggable_scm_material_hash)
    end

    it "should deserialize" do
      scm= SCMMother.create("scm-id")
      @go_config.getSCMs().add(scm)

      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(PluggableSCMMaterialConfig.new)
      deserialized_object = presenter.from_hash(pluggable_scm_material_hash, {go_config: @go_config})
      expect(deserialized_object.getScmId).to eq("scm-id")
      expect(deserialized_object.getSCMConfig).to eq(scm)
    end

    it "should set scmId during deserialisation if matching package definition is not present in config" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(PluggableSCMMaterialConfig.new)

      deserialized_object = presenter.from_hash(pluggable_scm_material_hash, {go_config: @go_config})
      expect(deserialized_object.getScmId).to eq("scm-id")
      expect(deserialized_object.getSCMConfig).to eq(nil)
    end

    it "should render errors" do
      pluggable_scm_material = PluggableSCMMaterialConfig.new(CaseInsensitiveString.new(''), nil, nil, nil)
      material_configs = MaterialConfigs.new(pluggable_scm_material)
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))

      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(material_configs.first())
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expected_material_hash = expected_material_hash_with_errors
      expect(actual_json).to eq(expected_material_hash)
    end

    def pluggable_scm_material_hash
      {
        type: 'plugin',
        attributes: {
          ref: "scm-id"
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "plugin",
        attributes: {
          ref: nil
        },
        errors: {
          ref: ["Please select a SCM"]
        }
      }
    end
  end
end

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

require 'spec_helper'
default_branch = 'master'
describe ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter do
  shared_examples_for 'materials' do

    describe :serialize do
      it 'should render material with hal representation' do
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material)
        actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_material_hash = material_hash
        expect(actual_json).to eq(expected_material_hash)
      end

      it "should render errors" do
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material_with_errors)
        actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
        expected_material_hash = expected_material_hash_with_errors
        expect(actual_json).to eq(expected_material_hash)
      end
    end

    describe :deserialize do
      it 'should convert hash to Material' do
        new_material = material_type.new
        presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(new_material)
        presenter.from_hash(ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(existing_material).to_hash(url_builder: UrlBuilder.new))
        expect(new_material.autoUpdate).to eq(existing_material.autoUpdate)
        expect(new_material.name).to eq(existing_material.name)
        expect(new_material).to eq(existing_material)
      end
    end

  end

  describe :git do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.gitMaterialConfig
    end

    def material_type
      GitMaterialConfig
    end

    def existing_material_with_errors
      git_config = GitMaterialConfig.new(UrlArgument.new(''), '', '', true, nil, false, '', CaseInsensitiveString.new('!nV@l!d'), false)
      dup_git_material = GitMaterialConfig.new(UrlArgument.new(''), '', '', true, nil, false, '', CaseInsensitiveString.new('!nV@l!d'), false)
      material_configs = MaterialConfigs.new(git_config);
      material_configs.add(dup_git_material)

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
                                                  type: 'git',
                                                  attributes: {
                                                    url: "http://user:password@funk.com/blank",
                                                    branch: "master",
                                                    auto_update: true,
                                                    name: nil
                                                  }
                                                })
      expected = GitMaterialConfig.new("http://user:password@funk.com/blank")
      expect(deserialized_object.autoUpdate).to eq(expected.autoUpdate)
      expect(deserialized_object.name.to_s).to eq("")
      expect(deserialized_object).to eq(expected)
    end

    it "should deserialize material without invert_filter" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(GitMaterialConfig.new)
      deserialized_object = presenter.from_hash({
                                                  type: 'git',
                                                  attributes: {
                                                    url: "http://user:password@funk.com/blank",
                                                    branch: "master",
                                                    auto_update: true,
                                                    name: nil,
                                                    invert_filter: nil
                                                  }
                                                })
      expected = GitMaterialConfig.new("http://user:password@funk.com/blank")
      expect(deserialized_object.invertFilter).to eq(expected.invertFilter)
      expect(deserialized_object).to eq(expected)
    end

    it "should deserialize material with invert_filter" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(GitMaterialConfig.new)
      deserialized_object = presenter.from_hash({
                                                  type: 'git',
                                                  attributes: {
                                                    url: "http://user:password@funk.com/blank",
                                                    branch: "master",
                                                    auto_update: true,
                                                    name: nil,
                                                    invert_filter: true
                                                  }
                                                })
      expected = GitMaterialConfig.new("http://user:password@funk.com/blank")
      expected.setInvertFilter(true)
      expect(deserialized_object.invertFilter).to eq(expected.invertFilter)
      expect(deserialized_object).to eq(expected)
    end

    it "should deserialize material with blank branch" do
      presenter = ApiV1::Config::ConfigRepo::Materials::MaterialRepresenter.new(GitMaterialConfig.new)
      deserialized_object = presenter.from_hash({
                                                  type: 'git',
                                                  attributes: {
                                                    url: "http://user:password@funk.com/blank",
                                                    branch: "",
                                                    auto_update: true,
                                                    name: nil
                                                  }
                                                })
      expect(deserialized_object.branch.to_s).to eq(default_branch)
    end

    def material_hash
      {
        type: 'git',
        attributes: {
          url: "http://user:password@funk.com/blank",
          destination: "destination",
          filter: {
            ignore: %w(**/*.html **/foobar/)
          },
          invert_filter: false,
          branch: 'branch',
          submodule_folder: 'sub_module_folder',
          shallow_clone: true,
          name: 'AwesomeGitMaterial',
          auto_update: false
        }
      }
    end

    def git_material_basic_hash
      {
        type: 'git',
        attributes: {
          url: "http://user:password@funk.com/blank",
          destination: nil,
          filter: nil,
          invert_filter: false,
          name: nil,
          auto_update: true,
          branch: "master",
          submodule_folder: nil,
          shallow_clone: false
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "git",
        attributes: {
          url: "",
          destination: "",
          filter: nil,
          invert_filter: false,
          name: "!nV@l!d",
          auto_update: true,
          branch: "master",
          submodule_folder: "",
          shallow_clone: false
        },
        errors: {
          name: ["You have defined multiple materials called '!nV@l!d'. Material names are case-insensitive and must be unique. Note that for dependency materials the default materialName is the name of the upstream pipeline. You can override this by setting the materialName explicitly for the upstream pipeline.", "Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          destination: ["Destination directory is required when specifying multiple scm materials"],
          url: ["URL cannot be blank"]
        }
      }
    end

  end

  describe :svn do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.svnMaterialConfig
    end

    def material_type
      SvnMaterialConfig
    end

    def existing_material_with_errors
      svn_config = SvnMaterialConfig.new(UrlArgument.new(''), '', '', true, GoCipher.new, true, nil, false, '', CaseInsensitiveString.new('!nV@l!d'))
      material_configs = MaterialConfigs.new(svn_config);
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.get(0)
    end

    def material_hash
      {
        type: 'svn',
        attributes: {
          url: "url",
          destination: "svnDir",
          filter: {
            ignore: [
              "*.doc"
            ]
          },
          invert_filter: false,
          name: "svn-material",
          auto_update: false,
          check_externals: true,
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
          destination: "",
          filter: nil,
          invert_filter: false,
          name: "!nV@l!d",
          auto_update: true,
          check_externals: true,
          username: ""
        },
        errors: {
          name: ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          url: ["URL cannot be blank"]
        }
      }
    end
  end

  describe :hg do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.hgMaterialConfigFull
    end

    def material_type
      HgMaterialConfig
    end

    def existing_material_with_errors
      hg_config = HgMaterialConfig.new(com.thoughtworks.go.util.command::HgUrlArgument.new(''), true, nil, false, '/dest/', CaseInsensitiveString.new('!nV@l!d'))
      material_configs = MaterialConfigs.new(hg_config);
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.get(0)
    end

    def material_hash
      {
        type: 'hg',
        attributes: {
          url: "http://user:pass@domain/path##branch",
          destination: "dest-folder",
          filter: {
            ignore: %w(**/*.html **/foobar/)
          },
          invert_filter: false,
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
          destination: "/dest/",
          filter: nil,
          invert_filter: false,
          name: "!nV@l!d",
          auto_update: true
        },
        errors: {
          name: ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
          destination: ["Dest folder '/dest/' is not valid. It must be a sub-directory of the working folder."],
          url: ["URL cannot be blank"]
        }
      }
    end

  end

  describe :tfs do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.tfsMaterialConfig
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
          destination: "dest-folder",
          filter: {
            ignore: %w(**/*.html **/foobar/)
          },
          invert_filter: false,
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
          destination: nil,
          filter: nil,
          invert_filter: false,
          name: nil,
          auto_update: true,
          domain: "",
          username: "",
          project_path: "/some-path/"
        },
        errors:
          {
            url: ["URL cannot be blank"],
            username: ["Username cannot be blank"]
          }
      }
    end

  end

  describe :p4 do
    it_should_behave_like 'materials'

    def existing_material
      MaterialConfigsMother.p4MaterialConfigFull
    end

    def material_type
      P4MaterialConfig
    end

    def existing_material_with_errors
      p4_config = P4MaterialConfig.new('', '', '', false, '', GoCipher.new, CaseInsensitiveString.new(''), true, nil, false, '/dest/')
      material_configs = MaterialConfigs.new(p4_config);
      material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", BasicCruiseConfig.new(), PipelineConfig.new()))
      material_configs.first()
    end

    def material_hash
      {
        type: 'p4',
        attributes: {
          destination: "dest-folder",
          filter: {
            ignore: %w(**/*.html **/foobar/)
          },
          invert_filter: false,
          port: "host:9876",
          username: "user",
          encrypted_password: GoCipher.new.encrypt("password"),
          use_tickets: true,
          view: "view",
          name: "p4-material",
          auto_update: true
        }
      }
    end

    def expected_material_hash_with_errors
      {
        type: "p4",
        attributes: {
          destination: "/dest/",
          filter: nil,
          invert_filter: false,
          name: "",
          auto_update: true,
          port: "",
          username: "",
          use_tickets: false,
          view: ""
        },
        errors: {
          view: ["P4 view cannot be empty."],
          destination: ["Dest folder '/dest/' is not valid. It must be a sub-directory of the working folder."],
          port: ["P4 port cannot be empty."]
        }
      }
    end

  end
end

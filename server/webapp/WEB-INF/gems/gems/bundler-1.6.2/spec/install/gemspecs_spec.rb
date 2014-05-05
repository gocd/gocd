require 'spec_helper'

describe "bundle install" do

  describe "when a gem has a YAML gemspec" do
    before :each do
      build_repo2 do
        build_gem "yaml_spec", :gemspec => :yaml
      end
    end

    it "still installs correctly" do
      gemfile <<-G
        source "file://#{gem_repo2}"
        gem "yaml_spec"
      G
      bundle :install
      expect(err).to be_empty
    end

    it "still installs correctly when using path" do
      build_lib 'yaml_spec', :gemspec => :yaml

      install_gemfile <<-G
        gem 'yaml_spec', :path => "#{lib_path('yaml_spec-1.0')}"
      G
      expect(err).to eq("")
    end
  end

  it "should use gemspecs in the system cache when available" do
    gemfile <<-G
      source "http://localtestserver.gem"
      gem 'rack'
    G

    FileUtils.mkdir_p "#{tmp}/gems/system/specifications"
    File.open("#{tmp}/gems/system/specifications/rack-1.0.0.gemspec", 'w+') do |f|
      spec = Gem::Specification.new do |s|
        s.name = 'rack'
        s.version = '1.0.0'
        s.add_runtime_dependency 'activesupport', '2.3.2'
      end
      f.write spec.to_ruby
    end
    bundle :install, :artifice => 'endpoint_marshal_fail' # force gemspec load
    should_be_installed "activesupport 2.3.2"
  end

end

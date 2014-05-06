require 'spec_helper'
require 'bundler/psyched_yaml'

describe Bundler::YamlSyntaxError do
  it "is raised on YAML parse errors" do
    expect{ YAML.parse "{foo" }.to raise_error(Bundler::YamlSyntaxError)
  end
end

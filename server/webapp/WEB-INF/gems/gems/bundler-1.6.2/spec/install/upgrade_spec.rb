require "spec_helper"

describe "bundle install for the first time with v1.0" do
  before :each do
    in_app_root

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G
  end

  it "removes lockfiles in 0.9 YAML format" do
    File.open("Gemfile.lock", "w"){|f| YAML.dump({}, f) }
    bundle :install
    expect(File.read("Gemfile.lock")).not_to match(/^---/)
  end

  it "removes env.rb if it exists" do
    bundled_app.join(".bundle").mkdir
    bundled_app.join(".bundle/environment.rb").open("w"){|f| f.write("raise 'nooo'") }
    bundle :install
    expect(bundled_app.join(".bundle/environment.rb")).not_to exist
  end

end

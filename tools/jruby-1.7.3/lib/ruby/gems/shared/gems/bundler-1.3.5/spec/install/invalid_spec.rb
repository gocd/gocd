require "spec_helper"

describe "bundle install with deprecated features" do
  before :each do
    in_app_root
  end

  it "reports that lib is an invalid option" do
    gemfile <<-G
      gem "rack", :lib => "rack"
    G

    bundle :install
    expect(out).to match(/You passed :lib as an option for gem 'rack', but it is invalid/)
  end

end

describe "bundle install to a dead symlink" do
  before do
    in_app_root do
      `ln -s /tmp/idontexist bundle`
    end
  end

  it "reports the symlink is dead" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    bundle "install --path bundle"
    expect(out).to match(/invalid symlink/)
  end
end

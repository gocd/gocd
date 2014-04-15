require "spec_helper"

describe "bundle show" do
  before :each do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G
  end

  it "creates a Gemfile.lock if one did not exist" do
    FileUtils.rm("Gemfile.lock")

    bundle "show"

    expect(bundled_app("Gemfile.lock")).to exist
  end

  it "creates a Gemfile.lock when invoked with a gem name" do
    FileUtils.rm("Gemfile.lock")

    bundle "show rails"

    expect(bundled_app("Gemfile.lock")).to exist
  end

  it "prints path if gem exists in bundle" do
    bundle "show rails"
    expect(out).to eq(default_bundle_path('gems', 'rails-2.3.2').to_s)
  end

  it "warns if path no longer exists on disk" do
    FileUtils.rm_rf("#{system_gem_path}/gems/rails-2.3.2")

    bundle "show rails"

    expect(out).to match(/has been deleted/i)
    expect(out).to include(default_bundle_path('gems', 'rails-2.3.2').to_s)
  end

  it "prints the path to the running bundler" do
    bundle "show bundler"
    expect(out).to eq(File.expand_path('../../../', __FILE__))
  end

  it "complains if gem not in bundle" do
    bundle "show missing"
    expect(out).to match(/could not find gem 'missing'/i)
  end

  it "prints path of all gems in bundle sorted by name" do
    bundle "show --paths"

    expect(out).to include(default_bundle_path('gems', 'rake-10.0.2').to_s)
    expect(out).to include(default_bundle_path('gems', 'rails-2.3.2').to_s)

    # Gem names are the last component of their path.
    gem_list = out.split.map { |p| p.split('/').last }
    expect(gem_list).to eq(gem_list.sort)
  end
end

describe "bundle show with a git repo" do
  before :each do
    @git = build_git "foo", "1.0"
  end

  it "prints out git info" do
    install_gemfile <<-G
      gem "foo", :git => "#{lib_path('foo-1.0')}"
    G
    should_be_installed "foo 1.0"

    bundle :show
    expect(out).to include("foo (1.0 #{@git.ref_for('master', 6)}")
  end

  it "prints out branch names other than master" do
    update_git "foo", :branch => "omg" do |s|
      s.write "lib/foo.rb", "FOO = '1.0.omg'"
    end
    @revision = revision_for(lib_path("foo-1.0"))[0...6]

    install_gemfile <<-G
      gem "foo", :git => "#{lib_path('foo-1.0')}", :branch => "omg"
    G
    should_be_installed "foo 1.0.omg"

    bundle :show
    expect(out).to include("foo (1.0 #{@git.ref_for('omg', 6)}")
  end

  it "doesn't print the branch when tied to a ref" do
    sha = revision_for(lib_path("foo-1.0"))
    install_gemfile <<-G
      gem "foo", :git => "#{lib_path('foo-1.0')}", :ref => "#{sha}"
    G

    bundle :show
    expect(out).to include("foo (1.0 #{sha[0..6]})")
  end
end

require "spec_helper"

describe "bundle check" do
  it "returns success when the Gemfile is satisfied" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    bundle :check, :exitstatus => true
    expect(@exitstatus).to eq(0)
    expect(out).to eq("The Gemfile's dependencies are satisfied")
  end

  it "works with the --gemfile flag when not in the directory" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    Dir.chdir tmp
    bundle "check --gemfile bundled_app/Gemfile"
    expect(out).to eq("The Gemfile's dependencies are satisfied")
  end

  it "creates a Gemfile.lock by default if one does not exist" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    FileUtils.rm("Gemfile.lock")

    bundle "check"

    expect(bundled_app("Gemfile.lock")).to exist
  end

  it "does not create a Gemfile.lock if --dry-run was passed" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    FileUtils.rm("Gemfile.lock")

    bundle "check --dry-run"

    expect(bundled_app("Gemfile.lock")).not_to exist
  end

  it "prints a generic error if the missing gems are unresolvable" do
    system_gems ["rails-2.3.2"]

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    bundle :check
    expect(out).to include("Bundler can't satisfy your Gemfile's dependencies.")
  end

  it "prints a generic error if a Gemfile.lock does not exist and a toplevel dependency does not exist" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
    G

    bundle :check, :exitstatus => true
    expect(@exitstatus).to be > 0
    expect(out).to include("Bundler can't satisfy your Gemfile's dependencies.")
  end

  it "prints a generic message if you changed your lockfile" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem 'rails'
    G
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem 'rails_fail'
    G

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rails"
      gem "rails_fail"
    G

    bundle :check
    expect(out).to include("Bundler can't satisfy your Gemfile's dependencies.")
  end

  it "remembers --without option from install" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      group :foo do
        gem "rack"
      end
    G

    bundle "install --without foo"
    bundle "check", :exitstatus => true
    expect(@exitstatus).to eq(0)
    expect(out).to include("The Gemfile's dependencies are satisfied")
  end

  it "ensures that gems are actually installed and not just cached" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", :group => :foo
    G

    bundle "install --without foo"

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    bundle "check", :exitstatus => true
    expect(out).to include("* rack (1.0.0)")
    expect(@exitstatus).to eq(1)
  end

  it "ignores missing gems restricted to other platforms" do
    system_gems "rack-1.0.0"

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      platforms :#{not_local_tag} do
        gem "activesupport"
      end
    G

    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          activesupport (2.3.5)
          rack (1.0.0)

      PLATFORMS
        #{local}
        #{not_local}

      DEPENDENCIES
        rack
        activesupport
    G

    bundle :check
    expect(out).to eq("The Gemfile's dependencies are satisfied")
  end

  it "works with env conditionals" do
    system_gems "rack-1.0.0"

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      env :NOT_GOING_TO_BE_SET do
        gem "activesupport"
      end
    G

    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          activesupport (2.3.5)
          rack (1.0.0)

      PLATFORMS
        #{local}
        #{not_local}

      DEPENDENCIES
        rack
        activesupport
    G

    bundle :check
    expect(out).to eq("The Gemfile's dependencies are satisfied")
  end

  it "outputs an error when the default Gemfile is not found" do
    bundle :check, :exitstatus => true
    expect(@exitstatus).to eq(10)
    expect(out).to include("Could not locate Gemfile")
  end

  it "does not output fatal error message" do
    bundle :check, :exitstatus => true
    expect(@exitstatus).to eq(10)
    expect(out).not_to include("Unfortunately, a fatal error has occurred. ")
  end

  it "should not crash when called multiple times on a new machine" do
    gemfile <<-G
      gem 'rails', '3.0.0.beta3'
      gem 'paperclip', :git => 'git://github.com/thoughtbot/paperclip.git'
    G

    simulate_new_machine
    bundle "check"
    last_out = out
    3.times do |i|
      bundle :check
      expect(out).to eq(last_out)
      expect(err).to be_empty
    end
  end

  it "fails when there's no lock file and frozen is set" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "foo"
    G

    bundle "install"
    bundle "install --deployment"
    FileUtils.rm(bundled_app("Gemfile.lock"))

    bundle :check, :exitstatus => true
    expect(exitstatus).not_to eq(0)
  end

  context "--path" do
    before do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rails"
      G
      bundle "install --path vendor/bundle"

      FileUtils.rm_rf(bundled_app(".bundle"))
    end

    it "returns success" do
      bundle "check --path vendor/bundle", :exitstatus => true
      expect(@exitstatus).to eq(0)
      expect(out).to eq("The Gemfile's dependencies are satisfied")
    end

    it "should write to .bundle/config" do
      bundle "check --path vendor/bundle", :exitstatus => true
      bundle "check", :exitstatus => true
      expect(@exitstatus).to eq(0)
    end
  end

  describe "when locked" do
    before :each do
      system_gems "rack-1.0.0"
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", "1.0"
      G
    end

    it "returns success when the Gemfile is satisfied" do
      bundle :install
      bundle :check, :exitstatus => true
      expect(@exitstatus).to eq(0)
      expect(out).to eq("The Gemfile's dependencies are satisfied")
    end

    it "shows what is missing with the current Gemfile if it is not satisfied" do
      simulate_new_machine
      bundle :check
      expect(out).to match(/The following gems are missing/)
      expect(out).to include("* rack (1.0")
    end
  end
end

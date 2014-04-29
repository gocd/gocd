require "spec_helper"

describe "bundle update" do
  before :each do
    build_repo2

    install_gemfile <<-G
      source "file://#{gem_repo2}"
      gem "activesupport"
      gem "rack-obama"
    G
  end

  describe "with no arguments" do
    it "updates the entire bundle" do
      update_repo2 do
        build_gem "activesupport", "3.0"
      end

      bundle "update"
      should_be_installed "rack 1.2", "rack-obama 1.0", "activesupport 3.0"
    end

    it "doesn't delete the Gemfile.lock file if something goes wrong" do
      gemfile <<-G
        source "file://#{gem_repo2}"
        gem "activesupport"
        gem "rack-obama"
        exit!
      G
      bundle "update"
      expect(bundled_app("Gemfile.lock")).to exist
    end
  end

  describe "--quiet argument" do
    it "shows UI messages without --quiet argument" do
      bundle "update"
      expect(out).to include("Fetching source")
    end

    it "does not show UI messages with --quiet argument" do
      bundle "update --quiet"
      expect(out).not_to include("Fetching source")
    end
  end

  describe "with a top level dependency" do
    it "unlocks all child dependencies that are unrelated to other locked dependencies" do
      update_repo2 do
        build_gem "activesupport", "3.0"
      end

      bundle "update rack-obama"
      should_be_installed "rack 1.2", "rack-obama 1.0", "activesupport 2.3.5"
    end
  end

  describe "with an unknown dependency" do
    it "should inform the user" do
      bundle "update halting-problem-solver", :expect_err=>true
      expect(out).to include "Could not find gem 'halting-problem-solver'"
    end
    it "should suggest alternatives" do
      bundle "update active-support", :expect_err=>true
      expect(out).to include "Did you mean activesupport?"
    end
  end

  describe "with a child dependency" do
    it "should update the child dependency" do
      update_repo2
      bundle "update rack"
      should_be_installed "rack 1.2"
    end
  end

  describe "with --local option" do
    it "doesn't hit repo2" do
      FileUtils.rm_rf(gem_repo2)

      bundle "update --local"
      expect(out).not_to match(/Fetching source index/)
    end
  end

  describe "with --group option" do
    it "should update only specifed group gems" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "activesupport", :group => :development
        gem "rack"
      G
      update_repo2 do
        build_gem "activesupport", "3.0"
      end
      bundle "update --group development"
      should_be_installed "activesupport 3.0"
      should_not_be_installed "rack 1.2"
    end
  end
end

describe "bundle update in more complicated situations" do
  before :each do
    build_repo2
  end

  it "will eagerly unlock dependencies of a specified gem" do
    install_gemfile <<-G
      source "file://#{gem_repo2}"

      gem "thin"
      gem "rack-obama"
    G

    update_repo2 do
      build_gem "thin" , '2.0' do |s|
        s.add_dependency "rack"
      end
    end

    bundle "update thin"
    should_be_installed "thin 2.0", "rack 1.2", "rack-obama 1.0"
  end
end

describe "bundle update without a Gemfile.lock" do
  it "should not explode" do
    build_repo2

    gemfile <<-G
      source "file://#{gem_repo2}"

      gem "rack", "1.0"
    G

    bundle "update"

    should_be_installed "rack 1.0.0"
  end
end

describe "bundle update when a gem depends on a newer version of bundler" do
  before(:each) do
    build_repo2 do
      build_gem "rails", "3.0.1" do |s|
        s.add_dependency "bundler", Bundler::VERSION.succ
      end
    end

    gemfile <<-G
      source "file://#{gem_repo2}"
      gem "rails", "3.0.1"
    G
  end

  it "should not explode" do
    bundle "update"
    expect(err).to be_empty
  end

  it "should explain that bundler conflicted" do
    bundle "update"
    expect(out).not_to match(/in snapshot/i)
    expect(out).to match(/current Bundler version/i)
    expect(out).to match(/perhaps you need to update bundler/i)
  end
end

describe "bundle update" do
  it "shows the previous version of the gem when updated from rubygems source" do
    build_repo2

    install_gemfile <<-G
      source "file://#{gem_repo2}"
      gem "activesupport"
    G

    bundle "update"
    expect(out).to include("Using activesupport 2.3.5")

    update_repo2 do
      build_gem "activesupport", "3.0"
    end

    bundle "update"
    expect(out).to include("Installing activesupport 3.0 (was 2.3.5)")
  end

  it "shows error message when Gemfile.lock is not preset and gem is specified" do
    install_gemfile <<-G
      source "file://#{gem_repo2}"
      gem "activesupport"
    G

    bundle "update nonexisting", :exitstatus => true
    expect(out).to include("This Bundle hasn't been installed yet. Run `bundle install` to update and install the bundled gems.")
    expect(@exitstatus).to eq(22)
  end
end

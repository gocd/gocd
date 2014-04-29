require "spec_helper"

describe "bundle package" do
  context "with --gemfile" do
    it "finds the gemfile" do
      gemfile bundled_app("NotGemfile"), <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      bundle "package --gemfile=NotGemfile"

      ENV['BUNDLE_GEMFILE'] = "NotGemfile"
      should_be_installed "rack 1.0.0"
    end
  end

  context "with --path" do
    it "sets root directory for gems" do
      gemfile <<-D
        source "file://#{gem_repo1}"
        gem 'rack'
      D

      bundle "package --path=#{bundled_app('test')}"

      should_be_installed "rack 1.0.0"
      expect(bundled_app("test/vendor/cache/")).to exist
    end
  end
end

describe "bundle install with gem sources" do
  describe "when cached and locked" do
    it "does not hit the remote at all" do
      build_repo2
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rack"
      G

      bundle :pack
      simulate_new_machine
      FileUtils.rm_rf gem_repo2

      bundle "install --local"
      should_be_installed "rack 1.0.0"
    end

    it "does not hit the remote at all" do
      build_repo2
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rack"
      G

      bundle :pack
      simulate_new_machine
      FileUtils.rm_rf gem_repo2

      bundle "install --deployment"
      should_be_installed "rack 1.0.0"
    end

    it "does not reinstall already-installed gems" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G
      bundle :pack

      build_gem "rack", "1.0.0", :path => bundled_app('vendor/cache') do |s|
        s.write "lib/rack.rb", "raise 'omg'"
      end

      bundle :install
      expect(err).to be_empty
      should_be_installed "rack 1.0"
    end

    it "ignores cached gems for the wrong platform" do
      simulate_platform "java" do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G
        bundle :pack
      end

      simulate_new_machine

      simulate_platform "ruby" do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G
        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 RUBY")
      end
    end

    it "does not update the cache if --no-cache is passed" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G
      bundled_app("vendor/cache").mkpath
      expect(bundled_app("vendor/cache").children).to be_empty

      bundle "install --no-cache"
      expect(bundled_app("vendor/cache").children).to be_empty
    end
  end
end

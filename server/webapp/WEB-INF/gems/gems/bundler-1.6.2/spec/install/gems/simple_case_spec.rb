require "spec_helper"

describe "bundle install with gem sources" do
  describe "the simple case" do
    it "prints output and returns if no dependencies are specified" do
      gemfile <<-G
        source "file://#{gem_repo1}"
      G

      bundle :install
      expect(out).to match(/no dependencies/)
    end

    it "does not make a lockfile if the install fails" do
      install_gemfile <<-G, :expect_err => true
        raise StandardError, "FAIL"
      G

      expect(err).to match(/FAIL \(StandardError\)/)
      expect(bundled_app("Gemfile.lock")).not_to exist
    end

    it "creates a Gemfile.lock" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      expect(bundled_app('Gemfile.lock')).to exist
    end

    it "creates lock files based on the Gemfile name" do
      gemfile bundled_app('OmgFile'), <<-G
        source "file://#{gem_repo1}"
        gem "rack", "1.0"
      G

      bundle 'install --gemfile OmgFile'

      expect(bundled_app("OmgFile.lock")).to exist
    end

    it "doesn't delete the lockfile if one already exists" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      lockfile = File.read(bundled_app("Gemfile.lock"))

      install_gemfile <<-G, :expect_err => true
        raise StandardError, "FAIL"
      G

      expect(File.read(bundled_app("Gemfile.lock"))).to eq(lockfile)
    end

    it "does not touch the lockfile if nothing changed" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      expect { run '1' }.not_to change { File.mtime(bundled_app('Gemfile.lock')) }
    end

    it "fetches gems" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      expect(default_bundle_path("gems/rack-1.0.0")).to exist
      should_be_installed("rack 1.0.0")
    end

    it "fetches gems when multiple versions are specified" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack', "> 0.9", "< 1.0"
      G

      expect(default_bundle_path("gems/rack-0.9.1")).to exist
      should_be_installed("rack 0.9.1")
    end

    it "fetches gems when multiple versions are specified take 2" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack', "< 1.0", "> 0.9"
      G

      expect(default_bundle_path("gems/rack-0.9.1")).to exist
      should_be_installed("rack 0.9.1")
    end

    it "raises an appropriate error when gems are specified using symbols" do
      status = install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"
        gem :rack
      G
      expect(status).to eq(4)
    end

    it "pulls in dependencies" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rails"
      G

      should_be_installed "actionpack 2.3.2", "rails 2.3.2"
    end

    it "does the right version" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", "0.9.1"
      G

      should_be_installed "rack 0.9.1"
    end

    it "does not install the development dependency" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "with_development_dependency"
      G

      should_be_installed "with_development_dependency 1.0.0"
      should_not_be_installed "activesupport 2.3.5"
    end

    it "resolves correctly" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activemerchant"
        gem "rails"
      G

      should_be_installed "activemerchant 1.0", "activesupport 2.3.2", "actionpack 2.3.2"
    end

    it "activates gem correctly according to the resolved gems" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activesupport", "2.3.5"
      G

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activemerchant"
        gem "rails"
      G

      should_be_installed "activemerchant 1.0", "activesupport 2.3.2", "actionpack 2.3.2"
    end

    it "does not reinstall any gem that is already available locally" do
      system_gems "activesupport-2.3.2"

      build_repo2 do
        build_gem "activesupport", "2.3.2" do |s|
          s.write "lib/activesupport.rb", "ACTIVESUPPORT = 'fail'"
        end
      end

      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "activerecord", "2.3.2"
      G

      should_be_installed "activesupport 2.3.2"
    end

    it "works when the gemfile specifies gems that only exist in the system" do
      build_gem "foo", :to_system => true
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
        gem "foo"
      G

      should_be_installed "rack 1.0.0", "foo 1.0.0"
    end

    it "prioritizes local gems over remote gems" do
      build_gem 'rack', '1.0.0', :to_system => true do |s|
        s.add_dependency "activesupport", "2.3.5"
      end

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      should_be_installed "rack 1.0.0", "activesupport 2.3.5"
    end

    describe "with a gem that installs multiple platforms" do
      it "installs gems for the local platform as first choice" do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 #{Gem::Platform.local}")
      end

      it "falls back on plain ruby" do
        simulate_platform "foo-bar-baz"
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 RUBY")
      end

      it "installs gems for java" do
        simulate_platform "java"
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 JAVA")
      end

      it "installs gems for windows" do
        simulate_platform mswin

        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 MSWIN")
      end
    end

    describe "doing bundle install foo" do
      before do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
        G
      end

      it "works" do
        bundle "install --path vendor"
        should_be_installed "rack 1.0"
      end

      it "allows running bundle install --system without deleting foo" do
        bundle "install --path vendor"
        bundle "install --system"
        FileUtils.rm_rf(bundled_app("vendor"))
        should_be_installed "rack 1.0"
      end

      it "allows running bundle install --system after deleting foo" do
        bundle "install --path vendor"
        FileUtils.rm_rf(bundled_app("vendor"))
        bundle "install --system"
        should_be_installed "rack 1.0"
      end
    end

    it "finds gems in multiple sources" do
      build_repo2
      update_repo2

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        source "file://#{gem_repo2}"

        gem "activesupport", "1.2.3"
        gem "rack", "1.2"
      G

      should_be_installed "rack 1.2", "activesupport 1.2.3"
    end

    it "gives a useful error if no sources are set" do
      install_gemfile <<-G
        gem "rack"
      G

      bundle :install, :expect_err => true
      expect(out).to match(/Your Gemfile has no gem server sources/i)
    end

    it "creates a Gemfile.lock on a blank Gemfile" do
      install_gemfile <<-G
      G

      expect(File.exist?(bundled_app("Gemfile.lock"))).to eq(true)
    end

    it "gracefully handles error when rubygems server is unavailable" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        source "http://localhost:9384"

        gem 'foo'
      G

      bundle :install
      expect(out).to include("Could not fetch specs from http://localhost:9384/")
      expect(out).not_to include("file://")
    end

    it "doesn't blow up when the local .bundle/config is empty" do
      FileUtils.mkdir_p(bundled_app(".bundle"))
      FileUtils.touch(bundled_app(".bundle/config"))

      install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"

        gem 'foo'
      G
      expect(exitstatus).to eq(0)
    end

    it "doesn't blow up when the global .bundle/config is empty" do
      FileUtils.mkdir_p("#{Bundler.rubygems.user_home}/.bundle")
      FileUtils.touch("#{Bundler.rubygems.user_home}/.bundle/config")

      install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"

        gem 'foo'
      G
      expect(exitstatus).to eq(0)
    end
  end

  describe "when Bundler root contains regex chars" do
    before do
      root_dir = tmp("foo[]bar")

      FileUtils.mkdir_p(root_dir)
      in_app_root_custom(root_dir)
    end

    it "doesn't blow up" do
      build_lib "foo"
      gemfile = <<-G
        gem 'foo', :path => "#{lib_path('foo-1.0')}"
      G
      File.open('Gemfile', 'w') do |file|
        file.puts gemfile
      end

      bundle :install, :exitstatus => true

      expect(exitstatus).to eq(0)
    end
  end

  describe "when requesting a quiet install via --quiet" do
    it "should be quiet if there are no warnings" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      bundle :install, :quiet => true
      expect(out).to eq("")
    end

    it "should still display warnings" do
      gemfile <<-G
        gem 'rack'
      G

      bundle :install, :quiet => true
      expect(out).to match(/Your Gemfile has no gem server sources/)
    end
  end

end

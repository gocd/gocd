require "spec_helper"

describe "bundle install --standalone" do
  describe "with simple gems" do
    before do
      install_gemfile <<-G, :standalone => true
        source "file://#{gem_repo1}"
        gem "rails"
      G
    end

    it "still makes the gems available to normal bundler" do
      should_be_installed "actionpack 2.3.2", "rails 2.3.2"
    end

    it "generates a bundle/bundler/setup.rb" do
      expect(bundled_app("bundle/bundler/setup.rb")).to exist
    end

    it "makes the gems available without bundler" do
      ruby <<-RUBY, :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
      RUBY

      expect(out).to eq("2.3.2")
    end

    it "works on a different system" do
      FileUtils.mv(bundled_app, "#{bundled_app}2")
      Dir.chdir("#{bundled_app}2")

      ruby <<-RUBY, :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
      RUBY

      expect(out).to eq("2.3.2")
    end
  end

  describe "with a combination of gems and git repos" do
    before do
      build_git "devise", "1.0"

      install_gemfile <<-G, :standalone => true
        source "file://#{gem_repo1}"
        gem "rails"
        gem "devise", :git => "#{lib_path('devise-1.0')}"
      G
    end

    it "still makes the gems available to normal bundler" do
      should_be_installed "actionpack 2.3.2", "rails 2.3.2", "devise 1.0"
    end

    it "generates a bundle/bundler/setup.rb" do
      expect(bundled_app("bundle/bundler/setup.rb")).to exist
    end

    it "makes the gems available without bundler" do
      ruby <<-RUBY, :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "devise"
        require "actionpack"
        puts DEVISE
        puts ACTIONPACK
      RUBY

      expect(out).to eq("1.0\n2.3.2")
    end
  end

  describe "with groups" do
    before do
      build_git "devise", "1.0"

      install_gemfile <<-G, :standalone => true
        source "file://#{gem_repo1}"
        gem "rails"

        group :test do
          gem "rspec"
          gem "rack-test"
        end
      G
    end

    it "makes the gems available without bundler" do
      ruby <<-RUBY, :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        require "spec"
        require "rack/test"
        puts ACTIONPACK
        puts SPEC
        puts RACK_TEST
      RUBY

      expect(out).to eq("2.3.2\n1.2.7\n1.0")
    end

    it "allows creating a standalone file with limited groups" do
      bundle "install --standalone default"

      load_error_ruby <<-RUBY, 'spec', :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
        require "spec"
      RUBY

      expect(out).to eq("2.3.2")
      expect(err).to eq("ZOMG LOAD ERROR")
    end

    it "allows --without to limit the groups used in a standalone" do
      bundle "install --standalone --without test"

      load_error_ruby <<-RUBY, 'spec', :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
        require "spec"
      RUBY

      expect(out).to eq("2.3.2")
      expect(err).to eq("ZOMG LOAD ERROR")
    end

    it "allows --path to change the location of the standalone bundle" do
      bundle "install --standalone --path path/to/bundle"

      ruby <<-RUBY, :no_lib => true, :expect_err => false
        $:.unshift File.expand_path("path/to/bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
      RUBY

      expect(out).to eq("2.3.2")
    end

    it "allows remembered --without to limit the groups used in a standalone" do
      bundle "install --without test"
      bundle "install --standalone"

      load_error_ruby <<-RUBY, 'spec', :no_lib => true
        $:.unshift File.expand_path("bundle")
        require "bundler/setup"

        require "actionpack"
        puts ACTIONPACK
        require "spec"
      RUBY

      expect(out).to eq("2.3.2")
      expect(err).to eq("ZOMG LOAD ERROR")
    end
  end

  describe "with gemcutter's dependency API" do
    let(:source_uri) { "http://localgemserver.test" }

    describe "simple gems" do
      before do
        gemfile <<-G
          source "#{source_uri}"
          gem "rails"
        G
      end

      it "should run without errors" do
        bundle "install --standalone", :artifice => "endpoint", :exitstatus => true

        expect(@exitstatus).to eq(0)
      end

      it "still makes the gems available to normal bundler" do
        bundle "install --standalone", :artifice => "endpoint"

        should_be_installed "actionpack 2.3.2", "rails 2.3.2"
      end

      it "generates a bundle/bundler/setup.rb" do
        bundle "install --standalone", :artifice => "endpoint"

        expect(bundled_app("bundle/bundler/setup.rb")).to exist
      end

      it "makes the gems available without bundler" do
        bundle "install --standalone", :artifice => "endpoint"

        ruby <<-RUBY, :no_lib => true
          $:.unshift File.expand_path("bundle")
          require "bundler/setup"

          require "actionpack"
          puts ACTIONPACK
        RUBY

        expect(out).to eq("2.3.2")
      end

      it "works on a different system" do
        bundle "install --standalone", :artifice => "endpoint"

        FileUtils.mv(bundled_app, "#{bundled_app}2")
        Dir.chdir("#{bundled_app}2")

        ruby <<-RUBY, :no_lib => true
          $:.unshift File.expand_path("bundle")
          require "bundler/setup"

          require "actionpack"
          puts ACTIONPACK
        RUBY

        expect(out).to eq("2.3.2")
      end
    end
  end

  describe "with --binstubs" do
    before do
      install_gemfile <<-G, :standalone => true, :binstubs => true
        source "file://#{gem_repo1}"
        gem "rails"
      G
    end

    it "creates stubs that use the standalone load path" do
      Dir.chdir(bundled_app) do
        expect(`bin/rails -v`.chomp).to eql "2.3.2"
      end
    end

    it "creates stubs that can be executed from anywhere" do
      require 'tmpdir'
      Dir.chdir(Dir.tmpdir) do
        expect(`#{bundled_app}/bin/rails -v`.chomp).to eql "2.3.2"
      end
    end
  end
end

require "spec_helper"

describe "bundle install with groups" do

  describe "installing with no options" do
    before :each do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
        group :emo do
          gem "activesupport", "2.3.5"
        end
        gem "thin", :groups => [:emo]
      G
    end

    it "installs gems in the default group" do
      should_be_installed "rack 1.0.0"
    end

    it "installs gems in a group block into that group" do
      should_be_installed "activesupport 2.3.5"

      load_error_run <<-R, 'activesupport', :default
        require 'activesupport'
        puts ACTIVESUPPORT
      R

      expect(err).to eq("ZOMG LOAD ERROR")
    end

    it "installs gems with inline :groups into those groups" do
      should_be_installed "thin 1.0"

      load_error_run <<-R, 'thin', :default
        require 'thin'
        puts THIN
      R

      expect(err).to eq("ZOMG LOAD ERROR")
    end

    it "sets up everything if Bundler.setup is used with no groups" do
      out = run("require 'rack'; puts RACK")
      expect(out).to eq('1.0.0')

      out = run("require 'activesupport'; puts ACTIVESUPPORT")
      expect(out).to eq('2.3.5')

      out = run("require 'thin'; puts THIN")
      expect(out).to eq('1.0')
    end

    it "removes old groups when new groups are set up" do
      load_error_run <<-RUBY, 'thin', :emo
        Bundler.setup(:default)
        require 'thin'
        puts THIN
      RUBY

      expect(err).to eq("ZOMG LOAD ERROR")
    end

    it "sets up old groups when they have previously been removed" do
      out = run <<-RUBY, :emo
        Bundler.setup(:default)
        Bundler.setup(:default, :emo)
        require 'thin'; puts THIN
      RUBY
      expect(out).to eq('1.0')
    end
  end

  describe "installing --without" do
    describe "with gems assigned to a single group" do
      before :each do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
          group :emo do
            gem "activesupport", "2.3.5"
          end
        G
      end

      it "installs gems in the default group" do
        bundle :install, :without => "emo"
        should_be_installed "rack 1.0.0", :groups => [:default]
      end

      it "does not install gems from the excluded group" do
        bundle :install, :without => "emo"
        should_not_be_installed "activesupport 2.3.5", :groups => [:default]
      end

      it "does not install gems from the previously excluded group" do
        bundle :install, :without => "emo"
        should_not_be_installed "activesupport 2.3.5"
        bundle :install
        should_not_be_installed "activesupport 2.3.5"
      end

      it "does not say it installed gems from the excluded group" do
        bundle :install, :without => "emo"
        expect(out).not_to include("activesupport")
      end

      it "allows Bundler.setup for specific groups" do
        bundle :install, :without => "emo"
        run("require 'rack'; puts RACK", :default)
        expect(out).to eq('1.0.0')
      end

      it "does not effect the resolve" do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "activesupport"
          group :emo do
            gem "rails", "2.3.2"
          end
        G

        bundle :install, :without => "emo"
        should_be_installed "activesupport 2.3.2", :groups => [:default]
      end

      it "still works on a different machine and excludes gems" do
        bundle :install, :without => "emo"

        simulate_new_machine
        bundle :install, :without => "emo"

        should_be_installed "rack 1.0.0", :groups => [:default]
        should_not_be_installed "activesupport 2.3.5", :groups => [:default]
      end

      it "still works when BUNDLE_WITHOUT is set" do
        ENV["BUNDLE_WITHOUT"] = "emo"

        bundle :install
        expect(out).not_to include("activesupport")

        should_be_installed "rack 1.0.0", :groups => [:default]
        should_not_be_installed "activesupport 2.3.5", :groups => [:default]

        ENV["BUNDLE_WITHOUT"] = nil
      end

      it "clears without when passed an empty list" do
        bundle :install, :without => "emo"

        bundle 'install --without ""'
        should_be_installed "activesupport 2.3.5"
      end

      it "doesn't clear without when nothing is passed" do
        bundle :install, :without => "emo"

        bundle :install
        should_not_be_installed "activesupport 2.3.5"
      end
    end

    describe "with gems assigned to multiple groups" do
      before :each do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
          group :emo, :lolercoaster do
            gem "activesupport", "2.3.5"
          end
        G
      end

      it "installs gems in the default group" do
        bundle :install, :without => "emo lolercoaster"
        should_be_installed "rack 1.0.0"
      end

      it "installs the gem if any of its groups are installed" do
        bundle "install --without emo"
        should_be_installed "rack 1.0.0", "activesupport 2.3.5"
      end

      describe "with a gem defined multiple times in different groups" do
        before :each do
          gemfile <<-G
            source "file://#{gem_repo1}"
            gem "rack"

            group :emo do
              gem "activesupport", "2.3.5"
            end

            group :lolercoaster do
              gem "activesupport", "2.3.5"
            end
          G
        end

        it "installs the gem w/ option --without emo" do
          bundle "install --without emo"
          should_be_installed "activesupport 2.3.5"
        end

        it "installs the gem w/ option --without lolercoaster" do
          bundle "install --without lolercoaster"
          should_be_installed "activesupport 2.3.5"
        end

        it "does not install the gem w/ option --without emo lolercoaster" do
          bundle "install --without emo lolercoaster"
          should_not_be_installed "activesupport 2.3.5"
        end

        it "does not install the gem w/ option --without 'emo lolercoaster'" do
          bundle "install --without 'emo lolercoaster'"
          should_not_be_installed "activesupport 2.3.5"
        end
      end
    end

    describe "nesting groups" do
      before :each do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
          group :emo do
            group :lolercoaster do
              gem "activesupport", "2.3.5"
            end
          end
        G
      end

      it "installs gems in the default group" do
        bundle :install, :without => "emo lolercoaster"
        should_be_installed "rack 1.0.0"
      end

      it "installs the gem if any of its groups are installed" do
        bundle "install --without emo"
        should_be_installed "rack 1.0.0", "activesupport 2.3.5"
      end

    end
  end

  describe "when loading only the default group" do
    it "should not load all groups" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
        gem "activesupport", :groups => :development
      G

      ruby <<-R
        require "bundler"
        Bundler.setup :default
        Bundler.require :default
        puts RACK
        begin
          require "activesupport"
        rescue LoadError
          puts "no activesupport"
        end
      R

      expect(out).to include("1.0")
      expect(out).to include("no activesupport")
    end
  end


  describe "when locked and installed with --without" do
    before(:each) do
      build_repo2
      system_gems "rack-0.9.1" do
        install_gemfile <<-G, :without => :rack
          source "file://#{gem_repo2}"
          gem "rack"

          group :rack do
            gem "rack_middleware"
          end
        G
      end
    end

    it "uses the correct versions even if --without was used on the original" do
      should_be_installed "rack 0.9.1"
      should_not_be_installed "rack_middleware 1.0"
      simulate_new_machine

      bundle :install

      should_be_installed "rack 0.9.1"
      should_be_installed "rack_middleware 1.0"
    end

    it "does not hit the remote a second time" do
      FileUtils.rm_rf gem_repo2
      bundle "install --without rack"
      expect(err).to be_empty
    end
  end

end

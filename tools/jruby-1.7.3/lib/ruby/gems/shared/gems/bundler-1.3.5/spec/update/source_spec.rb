require "spec_helper"

describe "bundle update" do
  describe "git sources" do
    before :each do
      build_repo2
      @git = build_git "foo", :path => lib_path("foo") do |s|
        s.executables = "foobar"
      end

      install_gemfile <<-G
        source "file://#{gem_repo2}"
        git "#{lib_path('foo')}" do
          gem 'foo'
        end
        gem 'rack'
      G
    end

    it "updates the source" do
      update_git "foo", :path => @git.path

      bundle "update --source foo"

      in_app_root do
        run <<-RUBY
          require 'foo'
          puts "WIN" if defined?(FOO_PREV_REF)
        RUBY

        expect(out).to eq("WIN")
      end
    end

    it "unlocks gems that were originally pulled in by the source" do
      update_git "foo", "2.0", :path => @git.path

      bundle "update --source foo"
      should_be_installed "foo 2.0"
    end

    it "leaves all other gems frozen" do
      update_repo2
      update_git "foo", :path => @git.path

      bundle "update --source foo"
      should_be_installed "rack 1.0"
    end
  end

end

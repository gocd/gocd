require "spec_helper"
require 'rake'
require 'bundler/gem_helper'

describe "Bundler::GemHelper tasks" do
  context "determining gemspec" do
    it "interpolates the name when there is only one gemspec" do
      bundle 'gem test'
      app = bundled_app("test")
      helper = Bundler::GemHelper.new(app.to_s)
      expect(helper.gemspec.name).to eq('test')
    end

    it "interpolates the name for a hidden gemspec" do
      bundle 'gem test'
      app = bundled_app("test")
      FileUtils.mv app.join('test.gemspec'), app.join('.gemspec')
      helper = Bundler::GemHelper.new(app.to_s)
      expect(helper.gemspec.name).to eq('test')
    end

    it "should fail when there is no gemspec" do
      bundle 'gem test'
      app = bundled_app("test")
      FileUtils.rm(File.join(app.to_s, 'test.gemspec'))
      expect { Bundler::GemHelper.new(app.to_s) }.to raise_error(/Unable to determine name/)
    end

    it "should fail when there are two gemspecs and the name isn't specified" do
      bundle 'gem test'
      app = bundled_app("test")
      File.open(File.join(app.to_s, 'test2.gemspec'), 'w') {|f| f << ''}
      expect { Bundler::GemHelper.new(app.to_s) }.to raise_error(/Unable to determine name/)
    end

    it "handles namespaces and converting to CamelCase" do
      bundle 'gem test-foo_bar'
      lib = bundled_app('test-foo_bar').join('lib/test/foo_bar.rb').read
      expect(lib).to include("module Test")
      expect(lib).to include("module FooBar")
    end
  end

  context "gem management" do
    def mock_confirm_message(message)
      Bundler.ui.should_receive(:confirm).with(message)
    end

    def mock_build_message
      mock_confirm_message "test 0.0.1 built to pkg/test-0.0.1.gem."
    end

    before(:each) do
      bundle 'gem test'
      @app = bundled_app("test")
      @gemspec = File.read("#{@app.to_s}/test.gemspec")
      File.open("#{@app.to_s}/test.gemspec", 'w'){|f| f << @gemspec.gsub('TODO: ', '') }
      @helper = Bundler::GemHelper.new(@app.to_s)
    end

    it "uses a shell UI for output" do
      expect(Bundler.ui).to be_a(Bundler::UI::Shell)
    end

    describe 'install_tasks' do
      before(:each) do
        @saved, Rake.application = Rake.application, Rake::Application.new
      end

      after(:each) do
        Rake.application = @saved
      end

      it "defines Rake tasks" do
        names = %w[build install release]

        names.each { |name|
          expect { Rake.application[name] }.to raise_error(/Don't know how to build task/)
        }

        @helper.install

        names.each { |name|
          expect { Rake.application[name] }.not_to raise_error
          expect(Rake.application[name]).to be_instance_of Rake::Task
        }
      end

      it "provides a way to access the gemspec object" do
        @helper.install
        expect(Bundler::GemHelper.gemspec.name).to eq('test')
      end
    end

    describe 'build' do
      it "builds" do
        mock_build_message
        @helper.build_gem
        expect(bundled_app('test/pkg/test-0.0.1.gem')).to exist
      end

      it "raises an appropriate error when the build fails" do
        # break the gemspec by adding back the TODOs...
        File.open("#{@app.to_s}/test.gemspec", 'w'){|f| f << @gemspec }
        expect { @helper.build_gem }.to raise_error(/TODO/)
      end
    end

    describe 'install' do
      it "installs" do
        mock_build_message
        mock_confirm_message "test (0.0.1) installed."
        @helper.install_gem
        expect(bundled_app('test/pkg/test-0.0.1.gem')).to exist
        expect(%x{gem list}).to include("test (0.0.1)")
      end

      it "raises an appropriate error when the install fails" do
        @helper.should_receive(:build_gem) do
          # write an invalid gem file, so we can simulate install failure...
          FileUtils.mkdir_p(File.join(@app.to_s, 'pkg'))
          path = "#{@app.to_s}/pkg/test-0.0.1.gem"
          File.open(path, 'w'){|f| f << "not actually a gem"}
          path
        end
        expect { @helper.install_gem }.to raise_error
      end
    end

    describe 'release' do
      it "shouldn't push if there are unstaged files" do
        expect { @helper.release_gem }.to raise_error(/files that need to be committed/)
      end

      it "shouldn't push if there are uncommitted files" do
        %x{cd test; git add .}
        expect { @helper.release_gem }.to raise_error(/files that need to be committed/)
      end

      it 'raises an appropriate error if there is no git remote' do
        Bundler.ui.stub(:confirm => nil, :error => nil) # silence messages

        Dir.chdir(gem_repo1) {
          `git init --bare`
        }
        Dir.chdir(@app) {
          `git init`
          `git config user.email "you@example.com"`
          `git config user.name "name"`
          `git commit -a -m "initial commit"`
        }

        expect { @helper.release_gem }.to raise_error
      end

      it "releases" do
        mock_build_message
        mock_confirm_message(/Tagged v0.0.1/)
        mock_confirm_message("Pushed git commits and tags.")

        @helper.should_receive(:rubygem_push).with(bundled_app('test/pkg/test-0.0.1.gem').to_s)

        Dir.chdir(gem_repo1) {
          `git init --bare`
        }
        Dir.chdir(@app) {
          `git init`
          `git config user.email "you@example.com"`
          `git config user.name "name"`
          `git remote add origin file://#{gem_repo1}`
          `git commit -a -m "initial commit"`
          sys_exec("git push origin master", true)
          `git commit -a -m "another commit"`
        }
        @helper.release_gem
      end

      it "releases even if tag already exists" do
        mock_build_message
        mock_confirm_message("Tag v0.0.1 has already been created.")

        @helper.should_receive(:rubygem_push).with(bundled_app('test/pkg/test-0.0.1.gem').to_s)

        Dir.chdir(gem_repo1) {
          `git init --bare`
        }
        Dir.chdir(@app) {
          `git init`
          `git config user.email "you@example.com"`
          `git config user.name "name"`
          `git commit -a -m "another commit"`
          `git tag -a -m \"Version 0.0.1\" v0.0.1`
        }
        @helper.release_gem
      end

    end
  end
end

require "spec_helper"

describe "Bundler.with_env helpers" do

  shared_examples_for "Bundler.with_*_env" do
    it "should reset and restore the environment" do
      gem_path = ENV['GEM_PATH']

      Bundler.with_clean_env do
        expect(`echo $GEM_PATH`.strip).not_to eq(gem_path)
      end

      expect(ENV['GEM_PATH']).to eq(gem_path)
    end
  end

  around do |example|
    env = Bundler::ORIGINAL_ENV.dup
    Bundler::ORIGINAL_ENV['BUNDLE_PATH'] = "./Gemfile"
    example.run
    Bundler::ORIGINAL_ENV.replace env
  end

  describe "Bundler.with_clean_env" do

    it_should_behave_like "Bundler.with_*_env"

    it "should keep the original GEM_PATH even in sub processes" do
      gemfile ""
      bundle "install --path vendor/bundle"

      code = "Bundler.with_clean_env do;" +
             "  print ENV['GEM_PATH'] != '';" +
             "end"

      result = bundle "exec ruby -e #{code.inspect}"
      expect(result).to eq("true")
    end

    it "should not pass any bundler environment variables" do
      Bundler.with_clean_env do
        expect(`echo $BUNDLE_PATH`.strip).not_to eq('./Gemfile')
      end
    end

    it "should not pass RUBYOPT changes" do
      lib_path = File.expand_path('../../../lib', __FILE__)
      Bundler::ORIGINAL_ENV['RUBYOPT'] = " -I#{lib_path} -rbundler/setup"

      Bundler.with_clean_env do
        expect(`echo $RUBYOPT`.strip).not_to include '-rbundler/setup'
        expect(`echo $RUBYOPT`.strip).not_to include "-I#{lib_path}"
      end

      expect(Bundler::ORIGINAL_ENV['RUBYOPT']).to eq(" -I#{lib_path} -rbundler/setup")
    end

    it "should not change ORIGINAL_ENV" do
      expect(Bundler::ORIGINAL_ENV['BUNDLE_PATH']).to eq('./Gemfile')
    end

  end

  describe "Bundler.with_original_env" do

    it_should_behave_like "Bundler.with_*_env"

    it "should pass bundler environment variables set before Bundler was run" do
      Bundler.with_original_env do
        expect(`echo $BUNDLE_PATH`.strip).to eq('./Gemfile')
      end
    end
  end

  describe "Bundler.clean_system" do
    it "runs system inside with_clean_env" do
      Bundler.clean_system(%{echo 'if [ "$BUNDLE_PATH" = "" ]; then exit 42; else exit 1; fi' | /bin/sh})
      expect($?.exitstatus).to eq(42)
    end
  end

  describe "Bundler.clean_exec" do
    it "runs exec inside with_clean_env" do
      pid = Kernel.fork do
        Bundler.clean_exec(%{echo 'if [ "$BUNDLE_PATH" = "" ]; then exit 42; else exit 1; fi' | /bin/sh})
      end
      Process.wait(pid)
      expect($?.exitstatus).to eq(42)
    end
  end
end

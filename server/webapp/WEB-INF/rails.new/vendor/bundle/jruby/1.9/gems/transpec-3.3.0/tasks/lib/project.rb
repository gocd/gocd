# coding: utf-8

require 'transpec'
require 'fileutils'

class Project
  include FileUtils # This is Rake's one.

  BUNDLER_RETRY_COUNT = 3

  attr_reader :url, :ref, :bundler_args

  def self.base_dir
    @base_dir ||= begin
      path = base_dir_path
      FileUtils.mkdir_p(path) unless Dir.exist?(path)
      path
    end
  end

  def self.base_dir_path
    File.join(Transpec.root, 'tmp', 'test')
  end

  def initialize(url, ref = nil, bundler_args = [])
    @url = url
    @ref = ref
    @bundler_args = bundler_args
  end

  def name
    @name ||= File.basename(url, '.git')
  end

  def project_dir
    @project_dir ||= File.join(self.class.base_dir, name)
  end

  def in_project_dir
    Dir.chdir(project_dir) do
      with_clean_bundler_env do
        yield
      end
    end
  end

  def git(*args)
    in_project_dir do
      sh 'git', *args
    end
  end

  private

  def setup
    if Dir.exist?(project_dir)
      git 'checkout', '.'
      git 'checkout', ref
    else
      git_clone
      bundle_install
    end
  end

  def git_clone
    Dir.chdir(self.class.base_dir) do
      # Disabling checkout here to suppress "detached HEAD" warning.
      command = %w(git clone --no-checkout)
      command.concat(%w(--depth 1)) if shallow_clone?
      command.concat(['--branch', ref, url])
      sh command.join(' ')
    end

    git 'checkout', '--quiet', ref
  end

  def shallow_clone?
    true
  end

  def bundle_install
    in_project_dir do
      sh 'bundle', 'install', '--retry', BUNDLER_RETRY_COUNT.to_s, *bundler_args
    end
  end

  def with_clean_bundler_env
    if defined?(Bundler)
      Bundler.with_clean_env do
        # Bundler.with_clean_env cleans environment variables
        # which are set after bundler is loaded.
        prepare_env
        yield
      end
    else
      prepare_env
      yield
    end
  end

  def prepare_env
    # Disable Coveralls.
    ENV['CI'] = ENV['JENKINS_URL'] = ENV['COVERALLS_RUN_LOCALLY'] = nil
  end
end

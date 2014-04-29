require 'bundler/vendored_thor' unless defined?(Thor)
require 'bundler'

module Bundler
  class GemHelper
    include Rake::DSL if defined? Rake::DSL

    class << self
      # set when install'd.
      attr_accessor :instance

      def install_tasks(opts = {})
        new(opts[:dir], opts[:name]).install
      end

      def gemspec(&block)
        gemspec = instance.gemspec
        block.call(gemspec) if block
        gemspec
      end
    end

    attr_reader :spec_path, :base, :gemspec

    def initialize(base = nil, name = nil)
      Bundler.ui = UI::Shell.new
      @base = (base ||= SharedHelpers.pwd)
      gemspecs = name ? [File.join(base, "#{name}.gemspec")] : Dir[File.join(base, "{,*}.gemspec")]
      raise "Unable to determine name from existing gemspec. Use :name => 'gemname' in #install_tasks to manually set it." unless gemspecs.size == 1
      @spec_path = gemspecs.first
      @gemspec = Bundler.load_gemspec(@spec_path)
    end

    def install
      built_gem_path = nil

      desc "Build #{name}-#{version}.gem into the pkg directory."
      task 'build' do
        built_gem_path = build_gem
      end

      desc "Build and install #{name}-#{version}.gem into system gems."
      task 'install' => 'build' do
        install_gem(built_gem_path)
      end

      desc "Create tag #{version_tag} and build and push #{name}-#{version}.gem to Rubygems"
      task 'release' => 'build' do
        release_gem(built_gem_path)
      end

      GemHelper.instance = self
    end

    def build_gem
      file_name = nil
      sh("gem build -V '#{spec_path}'") { |out, code|
        file_name = File.basename(built_gem_path)
        FileUtils.mkdir_p(File.join(base, 'pkg'))
        FileUtils.mv(built_gem_path, 'pkg')
        Bundler.ui.confirm "#{name} #{version} built to pkg/#{file_name}."
      }
      File.join(base, 'pkg', file_name)
    end

    def install_gem(built_gem_path=nil)
      built_gem_path ||= build_gem
      out, _ = sh_with_code("gem install '#{built_gem_path}' --local")
      raise "Couldn't install gem, run `gem install #{built_gem_path}' for more detailed output" unless out[/Successfully installed/]
      Bundler.ui.confirm "#{name} (#{version}) installed."
    end

    def release_gem(built_gem_path=nil)
      guard_clean
      built_gem_path ||= build_gem
      tag_version { git_push } unless already_tagged?
      rubygem_push(built_gem_path) if gem_push?
    end

    protected
    def rubygem_push(path)
      if Pathname.new("~/.gem/credentials").expand_path.exist?
        sh("gem push '#{path}'")
        Bundler.ui.confirm "Pushed #{name} #{version} to rubygems.org."
      else
        raise "Your rubygems.org credentials aren't set. Run `gem push` to set them."
      end
    end

    def built_gem_path
      Dir[File.join(base, "#{name}-*.gem")].sort_by{|f| File.mtime(f)}.last
    end

    def git_push
      perform_git_push
      perform_git_push ' --tags'
      Bundler.ui.confirm "Pushed git commits and tags."
    end

    def perform_git_push(options = '')
      cmd = "git push #{options}"
      out, code = sh_with_code(cmd)
      raise "Couldn't git push. `#{cmd}' failed with the following output:\n\n#{out}\n" unless code == 0
    end

    def already_tagged?
      if sh('git tag').split(/\n/).include?(version_tag)
        Bundler.ui.confirm "Tag #{version_tag} has already been created."
        true
      end
    end

    def guard_clean
      clean? && committed? or raise("There are files that need to be committed first.")
    end

    def clean?
      sh_with_code("git diff --exit-code")[1] == 0
    end

    def committed?
      sh_with_code("git diff-index --quiet --cached HEAD")[1] == 0
    end

    def tag_version
      sh "git tag -a -m \"Version #{version}\" #{version_tag}"
      Bundler.ui.confirm "Tagged #{version_tag}."
      yield if block_given?
    rescue
      Bundler.ui.error "Untagging #{version_tag} due to error."
      sh_with_code "git tag -d #{version_tag}"
      raise
    end

    def version
      gemspec.version
    end

    def version_tag
      "v#{version}"
    end

    def name
      gemspec.name
    end

    def sh(cmd, &block)
      out, code = sh_with_code(cmd, &block)
      code == 0 ? out : raise(out.empty? ? "Running `#{cmd}' failed. Run this command directly for more detailed output." : out)
    end

    def sh_with_code(cmd, &block)
      cmd << " 2>&1"
      outbuf = ''
      Bundler.ui.debug(cmd)
      SharedHelpers.chdir(base) {
        outbuf = `#{cmd}`
        if $? == 0
          block.call(outbuf) if block
        end
      }
      [outbuf, $?]
    end

    def gem_push?
      ! %w{n no nil false off 0}.include?(ENV['gem_push'].to_s.downcase)
    end
  end
end

unless defined?(Bundler)
  $stderr.puts "You didn't run bundle exec did you? Try again: bundle exec rake test"
  exit 1
end
require 'fileutils'
require 'tmpdir'

class Sass::Rails::TestCase < ActiveSupport::TestCase

  class ExecutionError < StandardError
    attr_accessor :output
    def initialize(message, output = nil)
      super(message)
      self.output = output
    end
    def message
      "#{super}\nOutput was:\n#{output}"
    end
  end

  module SilentError
    attr_accessor :output
    def message
      "#{super}\nOutput was:\n#{output}"
    end
  end

  protected

  def fixture_path(path)
    File.expand_path("../../fixtures/#{path}", __FILE__)
  end

  module TestAssetPaths
    attr_accessor :assets
  end

  def sprockets_render(project, filename)
    within_rails_app(project) do
      asset_output(filename)
    end
  end

  def asset_output(filename)
    runcmd "ruby script/rails runner 'puts Rails.application.assets[#{filename.inspect}]'"
  end

  def assert_file_exists(filename)
    assert File.exists?(filename), "could not find #{filename}. PWD=#{Dir.pwd}\nDid find: #{Dir.glob(File.dirname(filename)+"/*").join(", ")}"
  end

  def assert_not_output(match)
    assert_no_match match, $last_ouput
  end

  def assert_output(match)
    assert $last_ouput.to_s =~ match, "#{match} was not found in #{$last_ouput.inspect}"
  end

  def assert_line_count(count)
    last_count = $last_ouput.lines.count
    assert last_count == count, "Wrong line count, expected: #{count} but got: #{last_count}"
  end
  # Copies a rails app fixture to a temp directory
  # and changes to that directory during the yield.
  #
  # Automatically changes back to the working directory
  # and removes the temp directory when done.
  def within_rails_app(name, without_gems = [], gem_options = $gem_options)
    sourcedir = File.expand_path("../../fixtures/#{name}", __FILE__)
    Dir.mktmpdir do |tmpdir|
      FileUtils.cp_r "#{sourcedir}/.", tmpdir
      Dir.chdir(tmpdir) do
        gem_options.each { |gem_name, options| modify_gem_entry gem_name, options }
        without_gems.each { |gem_name| remove_gem name }
        FileUtils.rm("Gemfile.lock") if File.exist?("Gemfile.lock")
        runcmd "bundle install --verbose"
        runcmd "bundle exec rake db:create --trace"
        yield tmpdir
      end
    end
  end

  def process_gemfile(gemfile = "Gemfile", &blk)
    gem_contents = File.readlines(gemfile)
    gem_contents.map!(&blk)
    gem_contents.compact!
    File.open(gemfile, "w") do |f|
      f.print(gem_contents.join(""))
    end
  end

  def modify_gem_entry(gemname, options, gemfile = "Gemfile")
    found = false
    process_gemfile(gemfile) do |line|
      if line =~ /gem *(["'])#{Regexp.escape(gemname)}\1/
        found = true
        gem_entry(gemname, options) + "\n"
      else
        line
      end
    end
    unless found
      File.open(gemfile, "a") do |f|
        f.print("\n#{gem_entry(gemname, options)}\n")
      end
    end
  end

  def gem_entry(gemname, options)
    entry = %Q{gem "#{gemname}", "~> #{options[:version]}"}
    entry += ", :path => #{options[:path].inspect}" if options[:path]
    entry
  end

  def remove_gem(gemname)
    process_gemfile(gemfile) do |line|
      line unless line =~ /gem *(["'])#{Regexp.escape(gemname)}\1/
    end
  end

  def silently
    output = StringIO.new
    $stderr, old_stderr = output, $stderr
    $stdout, old_stdout = output, $stdout
    begin
      yield
    rescue ExecutionError => e
      raise
    rescue => e
      e.extend(SilentError)
      e.output = output.string
      raise
    end
  ensure
    $stderr = old_stderr
    $stdout = old_stdout
  end

  # executes a system command
  # raises an error if it does not complete successfully
  # returns the output as a string if it does complete successfully
  def runcmd(cmd, working_directory = Dir.pwd, clean_env = true, gemfile = "Gemfile", env = {})
    # There's a bug in bundler where with_clean_env doesn't clear out the BUNDLE_GEMFILE environment setting
    # https://github.com/carlhuda/bundler/issues/1133
    env["BUNDLE_GEMFILE"] = "#{working_directory}/#{gemfile}" if clean_env
    todo = Proc.new do
      r, w = IO.pipe
      Kernel.spawn(env, cmd, :out => w , :err => w, :chdir => working_directory)
      w.close
      Process.wait
      output = r.read
      r.close
      unless $?.exitstatus == 0
        raise ExecutionError, "Command failed with exit status #{$?.exitstatus}: #{cmd}", output
      end
      $last_ouput = output
    end
    if clean_env
      Bundler.with_clean_env(&todo)
    else
      todo.call
    end
  end

end

# -*- mode: ruby; coding: us-ascii; -*-

require 'rubygems'

begin
  gem 'rake'
rescue Gem::LoadError
  warn "Using the crusty system installed rake... you probably want to upgrade"
end
require 'rake'
require 'rake/testtask'
require 'rbconfig'

begin
  require 'psych'
rescue LoadError
  # do nothing
end
require 'yaml'

require 'hoe/rake'

##
# Hoe is a simple rake/rubygems helper for project Rakefiles. It helps
# generate rubygems and includes a dynamic plug-in system allowing for
# easy extensibility. Hoe ships with plug-ins for all your usual
# project tasks including rdoc generation, testing, packaging, and
# deployment.
#
# == Using Hoe
#
# === Basics
#
# Sow generates a new project from scratch. Sow uses a simple ERB
# templating system allowing you to capture patterns common to your
# projects. Run `sow` and then see ~/.hoe_template for more info:
#
#   % sow project_name
#   ...
#   % cd project_name
#
# and have at it.
#
# === Extra Configuration Options:
#
# Hoe maintains a config file for cross-project values. The file is
# located at <tt>~/.hoerc</tt>. The file is a YAML formatted config file with
# the following settings (extended by plugins):
#
# exclude:: A regular expression of files to exclude from +check_manifest+.
#
# Run <tt>`rake config_hoe`</tt> and see ~/.hoerc for examples.
#
# == Extending Hoe
#
# Hoe can be extended via its plugin system. Hoe searches out all
# installed files matching <tt>'hoe/*.rb'</tt> and loads them. Those
# files are expected to define a module matching the file name. The
# module must define a define task method and can optionally define an
# initialize method. Both methods must be named to match the file. eg
#
#   module Hoe::Blah
#     def initialize_blah # optional
#       # ...
#     end
#
#     def define_blah_tasks
#       # ...
#     end
#   end
#
# === Hoe Plugin Loading Sequence
#
#   Hoe.spec
#     Hoe.load_plugins
#       require
#     activate_plugins
#       extend plugin_module
#       initialize_plugins
#         initialize_XXX
#       activate_plugin_deps
#         activate_XXX_deps
#     yield spec
#     post_initialize
#       define_spec # gemspec, not hoespec
#       load_plugin_tasks
#       add_dependencies

class Hoe

  include Rake::DSL if defined?(Rake::DSL)

  # duh
  VERSION = "3.7.1"

  @@plugins = [:clean, :debug, :deps, :flay, :flog, :newb, :package,
               :publish, :gemcutter, :signing, :test]

  @bad_plugins = []

  ##
  # Used to add extra flags to RUBY_FLAGS.

  RUBY_DEBUG = ENV['RUBY_DEBUG']

  default_ruby_flags = "-w -I#{%w(lib bin test .).join(File::PATH_SEPARATOR)}" +
    (RUBY_DEBUG ? " #{RUBY_DEBUG}" : '')

  ##
  # Used to specify flags to ruby [has smart default].

  RUBY_FLAGS = ENV['RUBY_FLAGS'] || default_ruby_flags

  ##
  # Default configuration values for .hoerc. Plugins should populate
  # this on load.

  DEFAULT_CONFIG = {
    "exclude" => /tmp$|CVS|\.svn|TAGS|extconf.h|\.o$|\.log$/,
  }

  ##
  # True if you're a masochistic developer. Used for building commands.

  WINDOZE = RbConfig::CONFIG['host_os'] =~ /mswin|mingw/

  ##
  # *MANDATORY*: The author(s) of the package. (can be array)
  #
  # Use the #developer method to fill in both author and email cleanly.

  attr_accessor :author

  ##
  # Optional: A description of the release's latest changes.
  # Auto-populates to the top entry of History.txt.

  attr_accessor :changes

  ##
  # Optional: A description of the project. Auto-populates from the
  # first paragraph of the DESCRIPTION section of README.txt.
  #
  # See also: Hoe#summary and Hoe.paragraphs_of.

  attr_accessor :description

  ##
  # Optional: What sections from the readme to use for
  # auto-description. Defaults to %w(description).

  attr_accessor :description_sections

  ##
  # *MANDATORY*: The author's email address(es). (can be array)
  #
  # Use the #developer method to fill in both author and email cleanly.

  attr_accessor :email

  ##
  # Optional: An array of rubygem dependencies.
  #
  #   extra_deps << ['blah', '~> 1.0']

  attr_accessor :extra_deps

  ##
  # Optional: An array of rubygem developer dependencies.

  attr_accessor :extra_dev_deps

  ##
  # Optional: Extra files you want to add to RDoc.
  #
  # .txt files are automatically included (excluding the obvious).

  attr_accessor :extra_rdoc_files

  ##
  # Optional: The filename for the project history. [default: History.txt]

  attr_accessor :history_file

  ##
  # Optional: An array containing the license(s) under which this gem is released.
  #
  # Warns and defaults to "MIT" if not set.

  attr_accessor :licenses

  ##
  # *MANDATORY*: The name of the release.
  #
  # Set via Hoe.spec.

  attr_accessor :name

  ##
  # Optional: A post-install message to be displayed when gem is installed.

  attr_accessor :post_install_message

  ##
  # Optional: The filename for the project readme. [default: README.txt]

  attr_accessor :readme_file

  ##
  # Optional: The name of the group authoring the project. [default: name.downcase]

  attr_accessor :group_name

  alias :rubyforge_name  :group_name  # Deprecated. Use #group_name.
  alias :rubyforge_name= :group_name= # Deprecated. Use #group_name=.

  ##
  # The Gem::Specification.

  attr_accessor :spec # :nodoc:

  ##
  # Optional: A hash of extra values to set in the gemspec. Value may be a proc.
  #
  #   spec_extras[:required_rubygems_version] = '>= 1.3.2'
  #
  # (tho, see #pluggable! if that's all you want to do)

  attr_accessor :spec_extras

  ##
  # Optional: A short summary of the project. Auto-populates from the
  # first sentence of the description.
  #
  # See also: Hoe#description and Hoe.paragraphs_of.

  attr_accessor :summary

  ##
  # Optional: Number of sentences from description for summary. Defaults to 1.

  attr_accessor :summary_sentences

  ##
  # Optional: An array of test file patterns [default: test/**/test_*.rb]

  attr_accessor :test_globs

  ##
  # Deprecated: Optional: The url(s) of the project. (can be array).
  # Auto-populates to a list of urls read from the beginning of
  # README.txt.
  #

  def url
    warn "NOTE: Hoe#url is deprecated, use urls. It will be removed on or after 2012-06-01."
    warn "Used from #{caller.first}"
    @url
  end

  def url=o # :nodoc:
    warn "NOTE: Hoe#url= is deprecated, use urls=. It will be removed on or after 2012-06-01."
    warn "Used from #{caller.first}"
    @url=o
  end

  ##
  # Optional: The urls of the project. This can be an array or
  # (preferably) a hash. Auto-populates to the urls read from the
  # beginning of README.txt.
  #
  # See parse_urls for more details

  attr_accessor :urls

  ##
  # *MANDATORY*: The version. Don't hardcode! use a constant in the project.

  attr_accessor :version

  ##
  # Add extra dirs to both $: and RUBY_FLAGS (for test runs and rakefile deps)

  def self.add_include_dirs(*dirs)
    dirs = dirs.flatten
    $:.unshift(*dirs)
    s = File::PATH_SEPARATOR
    RUBY_FLAGS.sub!(/-I/, "-I#{dirs.join(s)}#{s}")
  end

  ##
  # Returns plugins that could not be loaded by Hoe.load_plugins.

  def self.bad_plugins
    @bad_plugins
  end

  ##
  # Find and load all plugin files.
  #
  # It is called at the end of hoe.rb

  def self.load_plugins plugins = Hoe.plugins
    @found  ||= {}
    @loaded ||= {}
    @files  ||= Gem.find_files "hoe/*.rb"

    @files.reverse.each do |path|
      @found[File.basename(path, ".rb").intern] = path
    end

    :keep_doing_this while @found.map { |name, plugin|
      next unless plugins.include? name
      next if @loaded[name]
      begin
        warn "loading #{plugin}" if $DEBUG
        @loaded[name] = require plugin
      rescue LoadError => e
        warn "error loading #{plugin.inspect}: #{e.message}. skipping..."
      end
    }.any?

    bad_plugins = plugins - @loaded.keys
    bad_plugins.each do |bad_plugin|
      plugins.delete bad_plugin
    end

    @bad_plugins.concat bad_plugins
    @bad_plugins.uniq!

    return @loaded, @found
  end

  ##
  # Normalize a project name into the project, file, klass and test names that
  # follow Ruby package naming guidelines.
  #
  # Project names are lowercase with _ separating package parts and -
  # separating extension parts.
  #
  # File names are lowercase with _ separating pacagke parts and / separating
  # extension parts.  net-http-persistent becomes net/http/persistent.
  #
  # Klass names are CamelCase with :: separating extension parts.
  #
  # Test klass names are same as Klass with Test prepended to each part.

  def self.normalize_names project # :nodoc:
    project    = project.gsub(/([A-Z])/, '_\1').downcase.sub(/^_/, '')
    klass      = project.gsub(/(?:^|_)([a-z])/) { $1.upcase }
    klass      = klass.  gsub(/(?:^|-)([a-z])/) { "::#{$1.upcase}" }
    test_klass = klass.  gsub(/(^|::)([A-Z])/) { "#{$1}Test#{$2}" }
    file_name  = project.gsub(/-/, '/')

    return project, file_name, klass, test_klass
  end

  ##
  # Activates +plugins+.  If a plugin cannot be loaded it will be ignored.
  #
  # Plugins may also be activated through a +plugins+ array in
  # <tt>~/.hoerc</tt>.  This should only be used for plugins that aren't
  # critical to your project and plugins that you want to use on other
  # projects.

  def self.plugin *plugins
    self.plugins.concat plugins
    self.plugins.uniq!
  end

  ##
  # The list of active plugins.

  def self.plugins
    @@plugins
  end

  ##
  # Execute the Hoe DSL to define your project's Hoe specification
  # (which interally creates a gem specification). All hoe attributes
  # and methods are available within +block+. Eg:
  #
  #   Hoe.spec name do
  #     # ... project specific data ...
  #   end

  def self.spec name, &block
    Hoe.load_plugins

    spec = self.new name
    spec.activate_plugins
    spec.instance_eval(&block)
    spec.post_initialize
    spec # TODO: remove?
  end

  ##
  # Activate plugin modules and add them to the current instance.

  def activate_plugins
    with_config do |config, _|
      config_plugins = config['plugins']
      break unless config_plugins
      Hoe.plugins.concat config_plugins.map { |plugin| plugin.intern }
    end

    Hoe.load_plugins Hoe.plugins

    names = Hoe.constants.map { |s| s.to_s }
    names.reject! { |n| n =~ /^[A-Z_]+$/ }

    names.each do |name|
      next unless Hoe.plugins.include? name.downcase.intern
      warn "extend #{name}" if $DEBUG
      self.extend Hoe.const_get(name)
    end

    initialize_plugins
    activate_plugin_deps
  end

  ##
  # Run all initialize_* methods for plugins

  def initialize_plugins
    Hoe.plugins.each do |plugin|
      msg = "initialize_#{plugin}"
      warn msg if $DEBUG
      send msg if self.respond_to? msg
    end
  end

  ##
  # Run all activate_*_deps methods for plugins

  def activate_plugin_deps
    Hoe.plugins.each do |plugin|
      msg = "activate_#{plugin}_deps"
      warn msg if $DEBUG
      send msg if self.respond_to? msg
    end
  end

  ##
  #  Specify a license for your gem.
  #  Call it multiple times if you are releasing under multiple licenses.
  #
  def license name
    self.licenses << name
  end

  ##
  # Add a dependency declaration to your spec. Pass :dev to
  # +type+ for developer dependencies.

  def dependency name, version, type = :runtime
    raise "Unknown dependency type: #{type}" unless
      [:runtime, :dev, :development, :developer].include? type

    ary = if type == :runtime then
            extra_deps
          else
            extra_dev_deps
          end

    ary << [name, version]
  end

  ##
  # Add standard and user defined dependencies to the spec.

  def add_dependencies
    self.extra_deps     = normalize_deps extra_deps
    self.extra_dev_deps = normalize_deps extra_dev_deps

    case name
    when 'hoe' then
      dependency "rake", "~> 0.8"
    else
      version = VERSION.split(/\./).first(2).join(".")
      dependency "hoe", "~> #{version}", :development
    end

    seen = {}

    extra_deps.each do |dep|
      next if seen[dep.first]
      seen[dep.first] = true

      spec.add_dependency(*dep)
    end

    extra_dev_deps.each do |dep|
      next if seen[dep.first]
      seen[dep.first] = true

      spec.add_development_dependency(*dep)
    end
  end

  ##
  # Returns the proper dependency list for the thingy.

  def dependency_target
    self.name == 'hoe' ? extra_deps : extra_dev_deps
  end

  ##
  # Define the Gem::Specification.

  def define_spec
    self.spec = Gem::Specification.new do |s|
      dirs = Dir['lib']

      manifest = read_manifest

      abort [
             "Manifest is missing or couldn't be read.",
             "The Manifest is kind of a big deal.",
             "Maybe you're using a gem packaged by a linux project.",
             "It seems like they enjoy breaking other people's code."
             ].join "\n" unless manifest

      s.name                 = name
      s.version              = version if version
      s.summary              = summary
      s.email                = email
      s.homepage             = case urls
                               when Hash then
                                 urls["home"] || urls.values.first
                               when Array then
                                 urls.first
                               else
                                 raise "unknown urls format: #{urls.inspect}"
                               end
      s.rubyforge_project    = group_name
      s.description          = description
      s.files                = manifest
      s.executables          = s.files.grep(/^bin/) { |f| File.basename(f) }
      s.bindir               = "bin"
      s.require_paths        = dirs unless dirs.empty?
      s.rdoc_options         = ['--main', readme_file]
      s.post_install_message = post_install_message
      s.test_files           = Dir[*self.test_globs].uniq

      missing "Manifest.txt" if s.files.empty?

      case author
      when Array
        s.authors = author
      else
        s.author  = author
      end

      s.extra_rdoc_files += s.files.grep(/\.(txt|rdoc|md)$/)
      s.extra_rdoc_files.reject! { |f| f =~ %r%^(test|spec|vendor|template|data|tmp)/% }
      s.extra_rdoc_files += @extra_rdoc_files
    end

    check_for_version

    if licenses.empty?
      warn "Defaulting gemspec to MIT license."
      warn "Call license in hoe spec to change."
      license "MIT"
    end

    spec.licenses = licenses

    run_spec_extras
  end

  def check_for_version # :nodoc:
    unless self.version then
      version    = nil
      version_re = /VERSION += +([\"\'])([\d][\w\.]+)\1/

      spec.files.each do |file|
        next unless File.exist? file
        version = File.read_utf(file)[version_re, 2] rescue nil
        break if version
      end

      spec.version = self.version = version if version

      unless self.version then
        spec.version = self.version = "0.borked"
        warn "** Add 'VERSION = \"x.y.z\"' to your code,"
        warn "   add a version to your hoe spec,"
        warn "   or fix your Manifest.txt"
      end
    end
  end

  def run_spec_extras # :nodoc:
    # Do any extra stuff the user wants
    self.spec_extras.each do |msg, val|
      case val
      when Proc
        val.call spec.send(msg)
      else
        spec.send "#{msg}=", val
      end
    end
  end

  ##
  # Convenience method to set add to both the author and email fields.

  def developer name, email
    self.author << name
    self.email  << email
  end

  ##
  # Returns true if the gem +name+ is installed.

  def have_gem? name
    Gem::Specification.find_by_name name.to_s
  rescue Gem::LoadError
    false
  end

  ##
  # Create a newly initialized hoe spec.

  def initialize name, version = nil # :nodoc:
    self.name                 = name
    self.version              = version

    self.author               = []
    self.changes              = nil
    self.description          = nil
    self.description_sections = %w(description)
    self.email                = []
    self.extra_deps           = []
    self.extra_dev_deps       = []
    self.extra_rdoc_files     = []
    self.licenses             = []
    self.post_install_message = nil
    self.group_name           = name.downcase
    self.spec                 = nil
    self.spec_extras          = {}
    self.summary              = nil
    self.summary_sentences    = 1
    self.test_globs           = ['test/**/{test,spec}_*.rb',
                                 'test/**/*_{test,spec}.rb']

    if manifest = read_manifest then
      self.readme_file  = manifest.grep(/^README\./).first
      self.history_file = manifest.grep(/^History\./).first
    end

    self.history_file ||= Dir.glob("History.{txt,md}").first
    self.readme_file  ||= Dir.glob("README.{txt,md}").first

    abort "Hoe.new {...} removed. Switch to Hoe.spec." if block_given?
  end

  ##
  # Intuit values from the readme and history files.

  def intuit_values
    header_re = /^((?:=+|#+) .*)$/
    readme    = File.read_utf(readme_file).split(header_re)[1..-1] rescue ''

    unless readme.empty? then
      sections = Hash[*readme.map { |s|
        s =~ /^[=#]/ ? s.strip.downcase.chomp(':').split.last : s.strip
      }]
      desc     = sections.values_at(*description_sections).join("\n\n")
      summ     = desc.split(/\.\s+/).first(summary_sentences).join(". ")
      urls     = parse_urls(readme[1])

      self.urls        ||= urls
      self.description ||= desc
      self.summary     ||= summ
    else
      missing readme_file
    end

    self.changes ||= begin
                       h = File.read_utf(history_file)
                       h.split(/^(={2,}|\#{2,})/)[1..2].join.strip
                     rescue
                       missing history_file
                       ''
                     end
  end

  ##
  # Parse the urls section of the readme file. Returns a hash or an
  # array depending on the format of the section.
  #
  #     label1 :: url1
  #     label2 :: url2
  #     label3 :: url3
  #
  # vs:
  #
  #     * url1
  #     * url2
  #     * url3
  #
  # The hash format is preferred as it will be used to populate gem
  # metadata. The array format will work, but will warn that you
  # should update the readme.

  def parse_urls text

    lines = text.gsub(/^\* /, '').delete("<>").split(/\n/).grep(/\S+/)

    if lines.first =~ /::/ then
      Hash[lines.map { |line| line.split(/\s*::\s*/) }]
    else
      lines
    end
  end

  ##
  # Load activated plugins by calling their define tasks method.

  def load_plugin_tasks
    bad = []

    $plugin_max = self.class.plugins.map { |s| s.to_s.size }.max

    self.class.plugins.each do |plugin|
      warn "define: #{plugin}" if $DEBUG

      old_tasks = Rake::Task.tasks.dup

      begin
        send "define_#{plugin}_tasks"
      rescue NoMethodError
        warn "warning: couldn't activate the #{plugin} plugin, skipping"

        bad << plugin
        next
      end

      (Rake::Task.tasks - old_tasks).each do |task|
        task.plugin = plugin
      end
    end
    @@plugins -= bad
  end

  ##
  # Bitch about a file that is missing data or unparsable for intuiting values.

  def missing name
    warn "** #{name} is missing or in the wrong format for auto-intuiting."
    warn "   run `sow blah` and look at its text files"
  end

  ##
  # Normalize the dependencies.

  def normalize_deps deps
    deps = Array(deps)

    deps.each do |o|
      abort "ERROR: Add '~> x.y' to the '#{o}' dependency." if String === o
    end

    deps
  end

  ##
  # Reads a file at +path+ and spits out an array of the +paragraphs+ specified.
  #
  #   changes = p.paragraphs_of('History.txt', 0..1).join("\n\n")
  #   summary, *description = p.paragraphs_of('README.txt', 3, 3..8)

  def paragraphs_of path, *paragraphs
    File.read_utf(path).delete("\r").split(/\n\n+/).values_at(*paragraphs)
  end

  ##
  # Tell the world you're a pluggable package (ie you require rubygems 1.3.1+)
  #
  # This uses require_rubygems_version. Last one wins. Make sure you
  # account for that.

  def pluggable!
    abort "update rubygems to >= 1.3.1" unless  Gem.respond_to? :find_files
    require_rubygems_version '>= 1.3.1'
  end

  ##
  # Is a plugin activated? Used for guarding missing plugins in your
  # hoe spec:
  #
  #   Hoe.spec "blah" do
  #     if plugin? :enhancement then
  #       self.enhancement = true # or whatever...
  #     end
  #   end

  def plugin? name
    self.class.plugins.include? name
  end

  ##
  # Finalize configuration

  def post_initialize
    intuit_values
    validate_fields
    define_spec
    load_plugin_tasks
    add_dependencies
  end

  ##
  # Reads Manifest.txt and returns an Array of lines in the manifest.
  #
  # Returns nil if no manifest was found.

  def read_manifest
    File.read_utf("Manifest.txt").split(/\r?\n\r?/) rescue nil
  end

  ##
  # Declare that your gem requires a specific rubygems version. Last one wins.

  def require_rubygems_version *versions
    spec_extras[:required_rubygems_version] = versions
  end

  ##
  # Declare that your gem requires a specific ruby version. Last one wins.

  def require_ruby_version *versions
    spec_extras[:required_ruby_version] = versions
  end

  ##
  # Provide a linear degrading value from n to m over start to finis
  # dates. If not provided, start and finis will default to 1/1 and
  # 12/31 of the current year.

  def timebomb n, m, finis = nil, start = nil
    require 'time'
    finis = Time.parse(finis || "#{Time.now.year}-12-31")
    start = Time.parse(start || "#{Time.now.year}-01-01")
    rest  = (finis - Time.now)
    full  = (finis - start)

    [((n - m) * rest / full).to_i + m, m].max
  end

  ##
  # Verify that mandatory fields are set.

  def validate_fields
    %w(email author).each do |field|
      value = self.send(field)
      abort "Hoe #{field} value not set. aborting" if value.nil? or value.empty?
    end
  end

  ##
  # Loads ~/.hoerc, merges it with a .hoerc in the current pwd (if
  # any) and yields the configuration and its path

  def with_config
    config = Hoe::DEFAULT_CONFIG

    rc = File.expand_path("~/.hoerc")
    exists = File.exist? rc
    homeconfig = exists ? YAML.load_file(rc) : {}

    config = config.merge homeconfig

    localrc = File.join Dir.pwd, '.hoerc'
    exists = File.exist? localrc
    localconfig = exists ? YAML.load_file(localrc) : {}

    config = config.merge localconfig

    yield config, rc
  end
end

class File
  # Like File::read, but strips out a BOM marker if it exists.
  def self.read_utf path
    r19 = "<3".respond_to? :encoding
    opt = r19 ? "r:bom|utf-8" : "rb"

    open path, opt do |f|
      if r19 then
        f.read
      else
        f.read.sub %r/\A\xEF\xBB\xBF/, ''
      end
    end
  end
end

def Gem.bin_wrapper name # :nodoc: HACK
  File.join Gem.bindir, Gem.default_exec_format % name
end unless Gem.respond_to? :bin_wrapper

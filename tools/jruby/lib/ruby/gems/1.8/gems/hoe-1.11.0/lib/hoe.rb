# -*- ruby -*-

require 'rubygems'
require 'rake'
require 'rake/gempackagetask'
require 'rake/rdoctask'
require 'rake/testtask'
require 'rbconfig'
require 'rubyforge'
require 'yaml'

begin
  gem 'rdoc'
rescue Gem::LoadError
end

##
# hoe - a tool to help rake
#
# Hoe is a simple rake/rubygems helper for project Rakefiles. It
# generates all the usual tasks for projects including rdoc generation,
# testing, packaging, and deployment.
#
# == Using Hoe
#
# === Basics
#
# Use this as a minimal starting point:
#
#   require 'hoe'
#   require './lib/project.rb'
#
#   Hoe.new("project_name", Project::VERSION) do |p|
#     p.rubyforge_name = "rf_project"
#     p.developer("Joe Blow", "joe@example.com")
#     # add other details here
#   end
#
#   # add other tasks here
#
# === Tasks Provided:
#
# announce::           Create news email file and post to rubyforge.
# audit::              Run ZenTest against the package.
# check_extra_deps::   Install missing dependencies.
# check_manifest::     Verify the manifest.
# clean::              Clean up all the extras.
# config_hoe::         Create a fresh ~/.hoerc file.
# debug_gem::          Show information about the gem.
# default::            Run the default task(s).
# deps:email::         Print a contact list for gems dependent on this gem
# deps:fetch::         Fetch all the dependent gems of this gem into tarballs
# deps:list::          List all the dependent gems of this gem
# docs::               Build the docs HTML Files
# email::              Generate email announcement file.
# flay::               Analyze for code duplication.
# flog::               Analyze code complexity.
# gem::                Build the gem file hoe-1.9.0.gem
# generate_key::       Generate a key for signing your gems.
# install_gem::        Install the package as a gem.
# multi::              Run the test suite using multiruby.
# package::            Build all the packages
# post_blog::          Post announcement to blog.
# post_news::          Post announcement to rubyforge.
# publish_docs::       Publish RDoc to RubyForge.
# rcov::               Analyze code coverage with tests
# release::            Package and upload the release to rubyforge.
# ridocs::             Generate ri locally for testing.
# tasks::              Generate a list of tasks for doco.
# test::               Run the test suite.
# test_deps::          Show which test files fail when run alone.
#
# === Extra Configuration Options:
#
# Run +config_hoe+ to generate a new ~/.hoerc file. The file is a
# YAML formatted config file with the following settings:
#
# exclude::             A regular expression of files to exclude from
#                       +check_manifest+.
# publish_on_announce:: Run +publish_docs+ when you run +release+.
# signing_key_file::    Signs your gems with this private key.
# signing_cert_file::   Signs your gem with this certificate.
# blogs::               An array of hashes of blog settings.
#
# Run +config_hoe+ and see ~/.hoerc for examples.
#
# === Signing Gems:
#
# Run the 'generate_key' task.  This will:
#
# 1. Configure your ~/.hoerc.
# 2. Generate a signing key and certificate.
# 3. Install the private key and public certificate files into ~/.gem.
# 4. Upload the certificate to RubyForge.
#
# Hoe will now generate signed gems when the package task is run.  If you have
# multiple machines you build gems on, be sure to install your key and
# certificate on each machine.
#
# Keep your private key secret!  Keep your private key safe!
#
# To make sure your gems are signed run:
#
#   rake package; tar tf pkg/yourproject-1.2.3.gem
#
# If your gem is signed you will see:
#
#   data.tar.gz
#   data.tar.gz.sig
#   metadata.gz
#   metadata.gz.sig
#
# === Platform awareness
#
# Hoe allows bundling of pre-compiled extensions in the +package+ task.
#
# To create a package for your current platform:
#
#   rake package INLINE=1
#
# This will force Hoe analize your +Inline+ already compiled
# extensions and include them in your gem.
#
# If somehow you need to force a specific platform:
#
#   rake package INLINE=1 FORCE_PLATFORM=mswin32
#
# This will set the +Gem::Specification+ platform to the one indicated in
# +FORCE_PLATFORM+ (instead of default Gem::Platform::CURRENT)
#

class Hoe
  VERSION = '1.11.0'
  GEMURL = URI.parse 'http://gems.rubyforge.org' # for namespace :deps below

  ruby_prefix = Config::CONFIG['prefix']
  sitelibdir = Config::CONFIG['sitelibdir']

  ##
  # Configuration for the supported test frameworks for test task.

  SUPPORTED_TEST_FRAMEWORKS = {
    :testunit => "test/unit",
    :minitest => "minitest/autorun",
  }

  ##
  # Used to add extra flags to RUBY_FLAGS.

  RUBY_DEBUG = ENV['RUBY_DEBUG']

  default_ruby_flags = "-w -I#{%w(lib ext bin test).join(File::PATH_SEPARATOR)}" +
    (RUBY_DEBUG ? " #{RUBY_DEBUG}" : '')

  ##
  # Used to specify flags to ruby [has smart default].

  RUBY_FLAGS = ENV['RUBY_FLAGS'] || default_ruby_flags

  ##
  # Used to add flags to test_unit (e.g., -n test_borked).
  #
  # eg FILTER="-n test_blah"

  FILTER = ENV['FILTER'] || ENV['TESTOPTS']

  # :stopdoc:

  DLEXT = Config::CONFIG['DLEXT']

  WINDOZE = /mswin|mingw/ =~ RUBY_PLATFORM unless defined? WINDOZE

  DIFF = if WINDOZE
           'diff.exe'
         else
           if system("gdiff", __FILE__, __FILE__)
             'gdiff' # solaris and kin suck
           else
             'diff'
           end
         end unless defined? DIFF

  # :startdoc:

  ##
  # *MANDATORY*: The author(s) of the package. (can be array)

  attr_accessor :author

  ##
  # Populated automatically from the manifest. List of executables.

  attr_accessor :bin_files # :nodoc:

  ##
  # *Optional*: An array of the project's blog categories. Defaults to project name.

  attr_accessor :blog_categories

  ##
  # Optional: A description of the release's latest changes. Auto-populates.

  attr_accessor :changes

  ##
  # Optional: An array of file patterns to delete on clean.

  attr_accessor :clean_globs

  ##
  # Optional: A description of the project. Auto-populates.

  attr_accessor :description

  ##
  # Optional: What sections from the readme to use for auto-description. Defaults to %w(description).

  attr_accessor :description_sections

  ##
  # *MANDATORY*: The author's email address(es). (can be array)

  attr_accessor :email

  ##
  # Optional: An array of rubygem dependencies.

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
  # Optional: flay threshold to determine threshold failure. [default: 1200-100]

  attr_accessor :flay_threshold

  ##
  # Optional: flog threshold to determine threshold failure. [default: 1500-200]

  attr_accessor :flog_threshold

  ##
  # Optional: The filename for the project history. [default: History.txt]

  attr_accessor :history_file

  ##
  # Populated automatically from the manifest. List of library files.

  attr_accessor :lib_files # :nodoc:

  ##
  # Optional: Array of incompatible versions for multiruby filtering. Used as a regex.

  attr_accessor :multiruby_skip

  ##
  # *MANDATORY*: The name of the release.

  attr_accessor :name

  ##
  # Optional: Should package create a tarball? [default: true]

  attr_accessor :need_tar

  ##
  # Optional: Should package create a zipfile? [default: false]

  attr_accessor :need_zip

  ##
  # Optional: A post-install message to be displayed when gem is installed.

  attr_accessor :post_install_message

  ##
  # Optional: The filename for the project readme. [default: README.txt]

  attr_accessor :readme_file

  ##
  # Optional: Name of RDoc destination directory on Rubyforge. [default: +name+]

  attr_accessor :remote_rdoc_dir

  ##
  # Optional: RSpec dirs. [default: %w(spec lib)]

  attr_accessor :rspec_dirs

  ##
  # Optional: RSpec options. [default: []]

  attr_accessor :rspec_options

  ##
  # Optional: Flags for RDoc rsync. [default: "-av --delete"]

  attr_accessor :rsync_args

  ##
  # Optional: The name of the rubyforge project. [default: name.downcase]

  attr_accessor :rubyforge_name

  ##
  # The Gem::Specification.

  attr_accessor :spec # :nodoc:

  ##
  # Optional: A hash of extra values to set in the gemspec. Value may be a proc.

  attr_accessor :spec_extras

  ##
  # Optional: A short summary of the project. Auto-populates.

  attr_accessor :summary

  ##
  # Optional: Number of sentences from description for summary. Defaults to 1.

  attr_accessor :summary_sentences

  ##
  # Populated automatically from the manifest. List of tests.

  attr_accessor :test_files # :nodoc:

  ##
  # Optional: An array of test file patterns [default: test/**/test_*.rb]

  attr_accessor :test_globs

  ##
  # Optional: What test library to require [default: :testunit]

  attr_accessor :testlib

  ##
  # Optional: The url(s) of the project. (can be array). Auto-populates.

  attr_accessor :url

  ##
  # *MANDATORY*: The version. Don't hardcode! use a constant in the project.

  attr_accessor :version

  ##
  # Add extra dirs to both $: and RUBY_FLAGS (for test runs)

  def self.add_include_dirs(*dirs)
    dirs = dirs.flatten
    $:.unshift(*dirs)
    s = File::PATH_SEPARATOR
    Hoe::RUBY_FLAGS.sub!(/-I/, "-I#{dirs.join(s)}#{s}")
  end

  def self.normalize_names project
    project   = project.tr('-', '_').gsub(/([A-Z])/, '_\1').downcase.sub(/^_/, '')
    klass     = project.gsub(/(?:^|_)([a-z])/) { $1.upcase }
    file_name = project

    return project, file_name, klass
  end

  def normalize_deps deps
    Array(deps).map { |o| String === o ? [o] : o }
  end

  def missing name
    warn "** #{name} is missing or in the wrong format for auto-intuiting."
    warn "   run `sow blah` and look at its text files"
  end

  def timebomb n, m, finis = '2010-04-01', start = '2009-03-14'
    finis = Time.parse finis
    start = Time.parse start
    rest  = (finis - Time.now)
    full  = (finis - start)

    ((n - m) * rest / full).to_i + m
  end

  def initialize(name, version) # :nodoc:
    self.name    = name
    self.version = version

    # Defaults
    self.author               = []
    self.blog_categories      = [name]
    self.clean_globs          = %w(diff diff.txt email.txt ri deps .source_index
                                   *.gem **/*~ **/.*~ **/*.rbc)
    self.description_sections = %w(description)
    self.email                = []
    self.extra_deps           = []
    self.extra_dev_deps       = []
    self.extra_rdoc_files     = []
    self.flay_threshold       = timebomb 1200, 100  # 80% of average :(
    self.flog_threshold       = timebomb 1500, 1000 # 80% of average :(
    self.history_file         = "History.txt"
    self.multiruby_skip       = []
    self.need_tar             = true
    self.need_zip             = false
    self.post_install_message = nil
    self.readme_file          = "README.txt"
    self.remote_rdoc_dir      = name
    self.rspec_dirs           = %w(spec lib)
    self.rspec_options        = []
    self.rsync_args           = '-av --delete'
    self.rubyforge_name       = name.downcase
    self.spec_extras          = {}
    self.summary_sentences    = 1
    self.test_globs           = ['test/**/test_*.rb']
    self.testlib              = :testunit

    yield self if block_given?

    # Intuit values:

    readme = File.read(readme_file).split(/^(=+ .*)$/)[1..-1] rescue ''
    unless readme.empty? then
      sections = readme.map { |s|
        s =~ /^=/ ? s.strip.downcase.chomp(':').split.last : s.strip
      }
      sections = Hash[*sections]
      desc = sections.values_at(*description_sections).join("\n\n")
      summ = desc.split(/\.\s+/).first(summary_sentences).join(". ")

      self.description ||= desc
      self.summary     ||= summ
      self.url         ||= readme[1].gsub(/^\* /, '').split(/\n/).grep(/\S+/)
    else
      missing readme_file
    end

    self.changes ||= begin
                       h = File.read(history_file)
                       h.split(/^(===.*)/)[1..2].join.strip
                     rescue
                       missing history_file
                       ''
                     end

    %w(email author).each do |field|
      value = self.send(field)
      if value.nil? or value.empty? then
        if Time.now < Time.local(2008, 4, 1) then
          warn "Hoe #{field} value not set - Fix by 2008-04-01!"
          self.send "#{field}=", "doofus"
        else
          abort "Hoe #{field} value not set. aborting"
        end
      end
    end

    hoe_deps = {
      'rake' => ">= #{RAKEVERSION}",
      'rubyforge' => ">= #{::RubyForge::VERSION}",
    }

    self.extra_deps     = normalize_deps extra_deps
    self.extra_dev_deps = normalize_deps extra_dev_deps

    if name == 'hoe' then
      hoe_deps.each do |pkg, vers|
        extra_deps << [pkg, vers]
      end
    else
      extra_dev_deps << ['hoe', ">= #{VERSION}"] unless hoe_deps.has_key? name
    end

    define_tasks
  end

  def developer name, email
    self.author << name
    self.email << email
  end

  def with_config # :nodoc:
    rc = File.expand_path("~/.hoerc")
    exists = File.exist? rc
    config = exists ? YAML.load_file(rc) : {}
    yield(config, rc)
  end

  def define_tasks # :nodoc:
    default_tasks = []

    if File.directory? "test" then
      desc 'Run the test suite. Use FILTER or TESTOPTS to add flags/args.'
      task :test do
        run_tests
      end

      desc 'Run the test suite using multiruby.'
      task :multi do
        run_tests :multi
      end

      desc 'Show which test files fail when run alone.'
      task :test_deps do
        tests = Dir["test/**/test_*.rb"]  +  Dir["test/**/*_test.rb"]

        paths = ['bin', 'lib', 'test'].join(File::PATH_SEPARATOR)
        null_dev = WINDOZE ? '> NUL 2>&1' : '&> /dev/null'

        tests.each do |test|
          if not system "ruby -I#{paths} #{test} #{null_dev}" then
            puts "Dependency Issues: #{test}"
          end
        end
      end

      default_tasks << :test
    end

    if File.directory? "spec" then
      begin
        require 'spec/rake/spectask'

        desc "Run all specifications"
        Spec::Rake::SpecTask.new(:spec) do |t|
          t.libs = self.rspec_dirs
          t.spec_opts = self.rspec_options
        end
      rescue LoadError
        # do nothing
      end
      default_tasks << :spec
    end

    desc 'Run the default task(s).'
    task :default => default_tasks

    begin # take a whack at defining rcov tasks
      require 'rcov/rcovtask'

      Rcov::RcovTask.new do |t|
        pattern = ENV['PATTERN'] || 'test/test_*.rb'

        t.test_files = FileList[pattern]
        t.verbose = true
        t.rcov_opts << "--threshold 80"
        t.rcov_opts << "--no-color"
        t.rcov_opts << "-x ^/"
      end

      # this is for autotest's rcov... also used by my emacs integration
      task :rcov_info do
        pattern = ENV['PATTERN'] || "test/test_*.rb"
        ruby "-Ilib -S rcov --text-report --save coverage.info --test-unit-only #{pattern}"
      end

      # this is for my emacs rcov overlay stuff on emacswiki.
      task :rcov_overlay do
        rcov, eol = Marshal.load(File.read("coverage.info")).last[ENV["FILE"]], 1
        puts rcov[:lines].zip(rcov[:coverage]).map { |line, coverage|
          bol, eol = eol, eol + line.length
          [bol, eol, "#ffcccc"] unless coverage
        }.compact.inspect
      end
    rescue LoadError
      # skip
    end

    begin
      require 'flay'
      require 'flay_task'
      FlayTask.new :flay, self.flay_threshold
    rescue LoadError
      # skip
    end

    begin
      require 'flog'
      require 'flog_task'
      FlogTask.new :flog, self.flog_threshold
    rescue LoadError
      # skip
    end

    ############################################################
    # Packaging and Installing

    signing_key = nil
    cert_chain = []

    with_config do |config, path|
      break unless config['signing_key_file'] and config['signing_cert_file']
      key_file = File.expand_path config['signing_key_file'].to_s
      signing_key = key_file if File.exist? key_file

      cert_file = File.expand_path config['signing_cert_file'].to_s
      cert_chain << cert_file if File.exist? cert_file
    end

    self.spec = Gem::Specification.new do |s|
      s.name = name
      s.version = version
      s.summary = summary
      case author
      when Array
        s.authors = author
      else
        s.author = author
      end
      s.email = email
      s.homepage = Array(url).first
      s.rubyforge_project = rubyforge_name

      s.description = description

      extra_deps.each do |dep|
        s.add_dependency(*dep)
      end

      extra_dev_deps.each do |dep|
        s.add_development_dependency(*dep)
      end

      s.files = File.read("Manifest.txt").delete("\r").split(/\n/)
      s.executables = s.files.grep(/^bin/) { |f| File.basename(f) }

      s.bindir = "bin"
      dirs = Dir['{lib,ext}']
      s.require_paths = dirs unless dirs.empty?

      s.rdoc_options = ['--main', readme_file]

      s.extra_rdoc_files += s.files.grep(/txt$/)
      s.extra_rdoc_files.reject! { |f| f =~ %r%^(test|spec|vendor|template|data|tmp)/% }
      s.extra_rdoc_files += @extra_rdoc_files
      s.has_rdoc = true

      s.post_install_message = post_install_message

      if test ?f, "test/test_all.rb" then
        s.test_file = "test/test_all.rb"
      else
        s.test_files = Dir[*self.test_globs]
      end

      if signing_key and cert_chain then
        s.signing_key = signing_key
        s.cert_chain = cert_chain
      end

      ############################################################
      # Allow automatic inclusion of compiled extensions
      if ENV['INLINE'] then
        s.platform = ENV['FORCE_PLATFORM'] || Gem::Platform::CURRENT

        # Try collecting Inline extensions for +name+
        if defined?(Inline) then
          directory 'lib/inline'

          Inline.registered_inline_classes.each do |cls|
            name = cls.name # TODO: what about X::Y::Z?
            # name of the extension is CamelCase
            alternate_name = if name =~ /[A-Z]/ then
                               name.gsub(/([A-Z])/, '_\1').downcase.sub(/^_/, '')
                             elsif name =~ /_/ then
                               name.capitalize.gsub(/_([a-z])/) { $1.upcase }
                             end

            extensions = Dir.chdir(Inline::directory) {
              Dir["Inline_{#{name},#{alternate_name}}_*.#{DLEXT}"]
            }

            extensions.each do |ext|
              # add the inlined extension to the spec files
              s.files += ["lib/inline/#{ext}"]

              # include the file in the tasks
              file "lib/inline/#{ext}" => ["lib/inline"] do
                cp File.join(Inline::directory, ext), "lib/inline"
              end
            end
          end
        end
      end

      # Do any extra stuff the user wants
      spec_extras.each do |msg, val|
        case val
        when Proc
          val.call(s.send(msg))
        else
          s.send "#{msg}=", val
        end
      end
    end

    desc 'Show information about the gem.'
    task :debug_gem do
      puts spec.to_ruby
    end

    self.lib_files  = spec.files.grep(/^(lib|ext)/)
    self.bin_files  = spec.files.grep(/^bin/)
    self.test_files = spec.files.grep(/^test/)

    Rake::GemPackageTask.new spec do |pkg|
      pkg.need_tar = @need_tar
      pkg.need_zip = @need_zip
    end

    desc 'Install the package as a gem.'
    task :install_gem => [:clean, :package, :check_extra_deps] do
      gem = Dir['pkg/*.gem'].first
      sh "#{'sudo ' unless WINDOZE}gem install --local #{gem}"
    end

    desc 'Package and upload the release to rubyforge.'
    task :release => [:clean, :package] do |t|
      v = ENV["VERSION"] or abort "Must supply VERSION=x.y.z"
      abort "Versions don't match #{v} vs #{version}" if v != version
      pkg = "pkg/#{name}-#{version}"

      if $DEBUG then
        puts "release_id = rf.add_release #{rubyforge_name.inspect}, #{name.inspect}, #{version.inspect}, \"#{pkg}.tgz\""
        puts "rf.add_file #{rubyforge_name.inspect}, #{name.inspect}, release_id, \"#{pkg}.gem\""
      end

      rf = RubyForge.new.configure
      puts "Logging in"
      rf.login

      c = rf.userconfig
      c["release_notes"] = description if description
      c["release_changes"] = changes if changes
      c["preformatted"] = true

      files = [(@need_tar ? "#{pkg}.tgz" : nil),
               (@need_zip ? "#{pkg}.zip" : nil),
               "#{pkg}.gem"].compact

      puts "Releasing #{name} v. #{version}"
      rf.add_release rubyforge_name, name, version, *files
    end

    ############################################################
    # Doco

    Rake::RDocTask.new(:docs) do |rd|
      rd.main = readme_file
      rd.options << '-d' if (`which dot` =~ /\/dot/) unless
        ENV['NODOT'] || WINDOZE
      rd.rdoc_dir = 'doc'

      rd.rdoc_files += spec.require_paths
      rd.rdoc_files += spec.extra_rdoc_files

      title = "#{name}-#{version} Documentation"
      title = "#{rubyforge_name}'s " + title if rubyforge_name != name

      rd.options << "-t" << title
    end

    desc 'Generate ri locally for testing.'
    task :ridocs => :clean do
      sh %q{ rdoc --ri -o ri . }
    end

    desc 'Publish RDoc to RubyForge.'
    task :publish_docs => [:clean, :docs] do
      config = YAML.load(File.read(File.expand_path("~/.rubyforge/user-config.yml")))
      host = "#{config["username"]}@rubyforge.org"

      remote_dir = "/var/www/gforge-projects/#{rubyforge_name}/#{remote_rdoc_dir}"
      local_dir = 'doc'

      sh %{rsync #{rsync_args} #{local_dir}/ #{host}:#{remote_dir}}
    end

    # no doco for this one
    task :publish_on_announce do
      with_config do |config, _|
        Rake::Task['publish_docs'].invoke if config["publish_on_announce"]
      end
    end

    ############################################################
    # Dependencies:

    namespace :deps do
      require 'zlib' # HACK for rubygems 1.3.0
      require 'rubygems/remote_fetcher'

      @@index = nil

      def self.get_source_index
        return @@index if @@index

        dump = unless File.exist? '.source_index' then
                 url = GEMURL + "Marshal.#{Gem.marshal_version}.Z"
                 dump = Gem::RemoteFetcher.fetcher.fetch_path url
                 dump = Gem.inflate dump
                 open '.source_index', 'wb' do |io| io.write dump end
                 dump
               else
                 open '.source_index', 'rb' do |io| io.read end
               end

        @@index = Marshal.load dump
      end

      def self.get_latest_gems
        @@cache ||= get_source_index.latest_specs
      end

      def self.get_gems_by_name
        @@by_name ||= Hash[*get_latest_gems.map { |gem|
                             [gem.name, gem, gem.full_name, gem]
                           }.flatten]
      end

      def self.dependent_upon name
        get_latest_gems.find_all { |gem|
          gem.dependencies.any? { |dep| dep.name == name }
        }
      end


      desc "List all the dependent gems of this gem"
      task :list do
        gems = self.get_gems_by_name
        gem  = gems[self.name]

        abort "Couldn't find gem: #{self.name}" unless gem

        deps = self.dependent_upon self.name
        max  = deps.map { |s| s.full_name.size }.max

        puts "  dependents:"
        unless deps.empty? then
          deps.sort_by { |spec| spec.full_name }.each do |spec|
            vers = spec.dependencies.find {|s| s.name == name }.requirement_list
            puts "    %-*s - %s" % [max, spec.full_name, vers.join(", ")]
          end
        else
          puts "    none"
        end
      end

      desc "Print a contact list for gems dependent on this gem"
      task :email do
        gems = self.get_gems_by_name
        gem  = gems[self.name]

        abort "Couldn't find gem: #{self.name}" unless gem

        deps = self.dependent_upon self.name

        email = deps.map { |s| s.email }.flatten.sort.uniq
        email = email.map { |s| s.split(/,\s*/) }.flatten.sort.uniq

        email.map! { |s| # don't you people realize how easy this is?
          s.gsub(/ at | _at_ |\s*(atmark|@nospam@|-at?-|@at?@|<at?>|\[at?\]|\(at?\))\s*/i, '@').gsub(/\s*(dot|\[d(ot)?\]|\.dot\.)\s*/i, '.').gsub(/\s+com$/, '.com')
        }

        bad, good = email.partition { |e| e !~ /^[\w.+-]+\@[\w.+-]+$/ }

        warn "Rejecting #{bad.size} email. I couldn't unmunge them." unless
          bad.empty?

        puts good.join(", ")
      end

      desc "Fetch all the dependent gems of this gem into tarballs"
      task :fetch do
        gems = self.get_gems_by_name
        gem  = gems[self.name]
        deps = self.dependent_upon self.name

        mkdir "deps" unless File.directory? "deps"
        Dir.chdir "deps" do
          begin
            deps.sort_by { |spec| spec.full_name }.each do |spec|
              full_name = spec.full_name
              tgz_name  = "#{full_name}.tgz"
              gem_name  = "#{full_name}.gem"

              next if File.exist? tgz_name
              FileUtils.rm_rf [full_name, gem_name]

              begin
                warn "downloading #{full_name}"
                Gem::RemoteFetcher.fetcher.download(spec, GEMURL, Dir.pwd)
                FileUtils.mv "cache/#{gem_name}", '.'
              rescue Gem::RemoteFetcher::FetchError
                warn "  failed"
                next
              end

              warn "converting #{gem_name} to tarball"

              system "gem unpack #{gem_name} 2> /dev/null"
              system "gem spec -l #{gem_name} > #{full_name}/gemspec.rb"
              system "tar zmcf #{tgz_name} #{full_name}"
              FileUtils.rm_rf [full_name, gem_name, "cache"]
            end
          ensure
            FileUtils.rm_rf "cache"
          end
        end
      end
    end

    ############################################################
    # Misc/Maintenance:

    desc 'Run ZenTest against the package.'
    task :audit do
      libs = %w(lib test ext).join(File::PATH_SEPARATOR)
      sh "zentest -I=#{libs} #{spec.files.grep(/^(lib|test)/).join(' ')}"
    end

    task :clobber_rcov # in case rcov didn't load

    desc 'Clean up all the extras.'
    task :clean => [ :clobber_docs, :clobber_package, :clobber_rcov ] do
      clean_globs.each do |pattern|
        files = Dir[pattern]
        rm_rf files, :verbose => true unless files.empty?
      end
    end

    desc 'Install missing dependencies.'
    task :check_extra_deps do
      # extra_deps = [["rubyforge", ">= 1.0.0"], ["rake", ">= 0.8.1"]]
      extra_deps.each do |dep|
        begin
          gem(*dep)
        rescue Gem::LoadError
          dep_gem, dep_version = dep
          dep_version = "--version '#{dep_version}'" if dep_version
          sh "#{'sudo ' unless WINDOZE}gem install #{dep_gem} #{dep_version}"
        end
      end
    end

    desc 'Create a fresh ~/.hoerc file.'
    task :config_hoe do
      with_config do |config, path|
        default_config = {
          "exclude" => /tmp$|CVS|\.svn/,
          "publish_on_announce" => false,
          "signing_key_file" => "~/.gem/gem-private_key.pem",
          "signing_cert_file" => "~/.gem/gem-public_cert.pem",
          "blogs" => [ {
                         "user" => "user",
                         "url" => "url",
                         "extra_headers" => {
                           "mt_convert_breaks" => "markdown"
                         },
                         "blog_id" => "blog_id",
                         "password"=>"password",
                       } ],
        }
        File.open(path, "w") do |f|
          YAML.dump(default_config.merge(config), f)
        end

        editor = ENV['EDITOR'] || 'vi'
        system "#{editor} #{path}" if ENV['SHOW_EDITOR'] != 'no'
      end
    end

    desc 'Generate email announcement file.'
    task :email do
      require 'rubyforge'
      subject, title, body, urls = announcement

      File.open("email.txt", "w") do |mail|
        mail.puts "Subject: [ANN] #{subject}"
        mail.puts
        mail.puts title
        mail.puts
        mail.puts urls
        mail.puts
        mail.puts body
        mail.puts
        mail.puts urls
      end
      puts "Created email.txt"
    end

    desc 'Post announcement to blog.'
    task :post_blog do
      require 'xmlrpc/client'

      with_config do |config, path|
        break unless config['blogs']

        subject, title, body, urls = announcement
        body += "\n\n#{urls}"

        config['blogs'].each do |site|
          server = XMLRPC::Client.new2(site['url'])
          content = site['extra_headers'].merge(:title => title,
                                                :description => body,
                                                :categories => blog_categories)

          result = server.call('metaWeblog.newPost',
                               site['blog_id'],
                               site['user'],
                               site['password'],
                               content,
                               true)
        end
      end
    end

    desc 'Post announcement to rubyforge.'
    task :post_news do
      require 'rubyforge'
      subject, title, body, urls = announcement

      rf = RubyForge.new.configure
      rf.login
      rf.post_news(rubyforge_name, subject, "#{title}\n\n#{body}")
      puts "Posted to rubyforge"
    end

    desc 'Create news email file and post to rubyforge.'
    task :announce => [:email, :post_news, :post_blog, :publish_on_announce ]

    desc 'Verify the manifest.'
    task :check_manifest => :clean do
      f = "Manifest.tmp"
      require 'find'
      files = []
      with_config do |config, _|
        exclusions = config["exclude"]
        abort "exclude entry missing from .hoerc. Aborting." if exclusions.nil?
        Find.find '.' do |path|
          next unless File.file? path
          next if path =~ exclusions
          files << path[2..-1]
        end
        files = files.sort.join "\n"
        File.open f, 'w' do |fp| fp.puts files end
        system "#{DIFF} -du Manifest.txt #{f}"
        rm f
      end
    end

    desc 'Generate a key for signing your gems.'
    task :generate_key do
      email = Array(spec.email)
      abort "No email in your gemspec" if email.nil? or email.empty?

      key_file = with_config { |config, _| config['signing_key_file'] }
      cert_file = with_config { |config, _| config['signing_cert_file'] }

      if key_file.nil? or cert_file.nil? then
        ENV['SHOW_EDITOR'] ||= 'no'
        Rake::Task['config_hoe'].invoke

        key_file = with_config { |config, _| config['signing_key_file'] }
        cert_file = with_config { |config, _| config['signing_cert_file'] }
      end

      key_file = File.expand_path key_file
      cert_file = File.expand_path cert_file

      unless File.exist? key_file or File.exist? cert_file then
        warn "NOTICE: using #{email.first} for certificate" if email.size > 1

        sh "gem cert --build #{email.first}"
        mv "gem-private_key.pem", key_file, :verbose => true
        mv "gem-public_cert.pem", cert_file, :verbose => true

        puts "Installed key and certificate."

        rf = RubyForge.new.configure
        rf.login

        cert_package = "#{rubyforge_name}-certificates"

        begin
          rf.lookup 'package', cert_package
        rescue
          rf.create_package rubyforge_name, cert_package
        end

        begin
          rf.lookup('release', cert_package)['certificates']
          rf.add_file rubyforge_name, cert_package, 'certificates', cert_file
        rescue
          rf.create_package rubyforge_name, cert_package
          rf.add_release rubyforge_name, cert_package, 'certificates', cert_file
        end

        puts "Uploaded certificate to release \"certificates\" in package #{cert_package}"
      else
        puts "Keys already exist: #{key_file} and #{cert_file}"
      end
    end
  end # end define

  def run_tests(multi=false) # :nodoc:
    framework = SUPPORTED_TEST_FRAMEWORKS[testlib]
    raise "unsupported test framework #{testlib}" unless framework

    tests = ["rubygems", framework] +
      test_globs.map { |g| Dir.glob(g) }.flatten
    tests.map! {|f| %(require "#{f}")}

    cmd = "#{RUBY_FLAGS} -e '#{tests.join("; ")}' #{FILTER}"

    if multi then
      ENV['EXCLUDED_VERSIONS'] = multiruby_skip.join ":"
      cmd = "-S multiruby #{cmd}"
    end

    ruby cmd
  end

  def announcement # :nodoc:
    changes = self.changes.rdoc_to_markdown
    subject = "#{name} #{version} Released"
    title   = "#{name} version #{version} has been released!"
    body    = "#{description}\n\nChanges:\n\n#{changes}".rdoc_to_markdown
    urls    = Array(url).map { |s| "* <#{s.strip.rdoc_to_markdown}>" }.join("\n")

    return subject, title, body, urls
  end

  ##
  # Reads a file at +path+ and spits out an array of the +paragraphs+ specified.
  #
  #   changes = p.paragraphs_of('History.txt', 0..1).join("\n\n")
  #   summary, *description = p.paragraphs_of('README.txt', 3, 3..8)

  def paragraphs_of(path, *paragraphs)
    File.read(path).delete("\r").split(/\n\n+/).values_at(*paragraphs)
  end
end

# :enddoc:

class ::Rake::SshDirPublisher # :nodoc:
  attr_reader :host, :remote_dir, :local_dir
end

class String
  def rdoc_to_markdown
    self.gsub(/^mailto:/, '').gsub(/^(=+)/) { "#" * $1.size }
  end
end

if $0 == __FILE__ then
  out = `rake -T | egrep -v "redocs|repackage|clobber|trunk"`
  if ARGV.empty? then
    # # default::        Run the default tasks.
    puts out.gsub(/(\s*)\#/, '::\1').gsub(/^rake /, '# ')
  else
    # * default        - Run the default tasks.
    puts out.gsub(/\#/, '-').gsub(/^rake /, '* ')
  end
end

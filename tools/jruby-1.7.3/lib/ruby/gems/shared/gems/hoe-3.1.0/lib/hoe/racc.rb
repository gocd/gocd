##
# Racc plugin for hoe.
#
# === Tasks Provided:
#
# lexer            :: Generate lexers for all .rex files in your Manifest.txt.
# parser           :: Generate parsers for all .y files in your Manifest.txt.
# .y   -> .rb rule :: Generate a parser using racc.
# .rex -> .rb rule :: Generate a lexer using rexical.

module Hoe::Racc

  ##
  # Optional: Defines what tasks need to generate parsers/lexers first.
  #
  # Defaults to [:multi, :test, :check_manifest]
  #
  # If you have extra tasks that require your parser/lexer to be
  # built, add their names here in your hoe spec. eg:
  #
  #    racc_tasks << :debug

  attr_accessor :racc_tasks

  ##
  # Optional: Defines what flags to use for racc. default: "-v -l"

  attr_accessor :racc_flags

  ##
  # Optional: Defines what flags to use for rex. default: "--independent"

  attr_accessor :rex_flags

  ##
  # Initialize variables for racc plugin.

  def initialize_racc
    self.racc_tasks = [:multi, :test, :check_manifest]

    # -v = verbose
    # -l = no-line-convert (they don't ever line up anyhow)
    self.racc_flags ||= "-v -l"
    self.rex_flags  ||= "--independent"
  end

  def activate_racc_deps
    dependency 'racc', '~> 1.4.6', :development
  end

  ##
  # Define tasks for racc plugin

  def define_racc_tasks
    racc_files   = self.spec.files.find_all { |f| f =~ /\.y$/ }
    rex_files    = self.spec.files.find_all { |f| f =~ /\.rex$/ }

    parser_files = racc_files.map { |f| f.sub(/\.y$/, ".rb") }
    lexer_files  = rex_files.map  { |f| f.sub(/\.rex$/, ".rb") }

    self.clean_globs += parser_files
    self.clean_globs += lexer_files

    rule ".rb" => ".y" do |t|
      begin
        sh "racc #{racc_flags} -o #{t.name} #{t.source}"
      rescue
        abort "need racc, sudo gem install racc"
      end
    end

    rule ".rb" => ".rex" do |t|
      begin
        sh "rex #{rex_flags} -o #{t.name} #{t.source}"
      rescue
        abort "need rexical, sudo gem install rexical"
      end
    end

    task :isolate # stub task

    desc "build the parser" unless parser_files.empty?
    task :parser => :isolate

    desc "build the lexer" unless lexer_files.empty?
    task :lexer  => :isolate

    task :parser => parser_files
    task :lexer  => lexer_files

    racc_tasks.each do |t|
      task t => [:parser, :lexer]
    end
  end
end

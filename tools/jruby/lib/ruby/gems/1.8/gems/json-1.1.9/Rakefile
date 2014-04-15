begin
  require 'rake/gempackagetask'
  require 'rake/extensiontask'
rescue LoadError
end
require 'rake/clean'
CLOBBER.include Dir['benchmarks/data/*.{dat,log}']

require 'rbconfig'
include Config

MAKE = ENV['MAKE'] || %w[gmake make].find { |c| system(c, '-v') }
PKG_NAME          = 'json'
PKG_VERSION       = File.read('VERSION').chomp
PKG_FILES         = FileList["**/*"].exclude(/CVS|pkg|tmp|coverage|Makefile/).exclude(/\.(so|bundle|o|#{CONFIG['DLEXT']})$/)
EXT_ROOT_DIR      = 'ext/json/ext'
EXT_PARSER_DIR    = "#{EXT_ROOT_DIR}/parser"
EXT_PARSER_DL     = "#{EXT_ROOT_DIR}/parser.#{CONFIG['DLEXT']}"
EXT_PARSER_SRC    = "#{EXT_PARSER_DIR}/parser.c"
PKG_FILES << EXT_PARSER_SRC
EXT_GENERATOR_DIR = "#{EXT_ROOT_DIR}/generator"
EXT_GENERATOR_DL  = "#{EXT_ROOT_DIR}/generator.#{CONFIG['DLEXT']}"
EXT_GENERATOR_SRC = "#{EXT_GENERATOR_DIR}/generator.c"
RAGEL_CODEGEN     = %w[rlcodegen rlgen-cd ragel].find { |c| system(c, '-v') }
RAGEL_DOTGEN      = %w[rlgen-dot rlgen-cd ragel].find { |c| system(c, '-v') }
RAGEL_PATH        = "#{EXT_PARSER_DIR}/parser.rl"
CLEAN.include FileList['diagrams/*.*'], 'doc', 'coverage', 'tmp',
  FileList["ext/**/{Makefile,mkmf.log}"],
  FileList["{ext,lib}/**/*.{so,bundle,#{CONFIG['DLEXT']},o,obj,pdb,lib,manifest,exp,def}"]

desc "Installing library (pure)"
task :install_pure => :version do
  ruby 'install.rb'
end

task :install_ext_really do
  sitearchdir = CONFIG["sitearchdir"]
  cd 'ext' do
    for file in Dir["json/ext/*.#{CONFIG['DLEXT']}"]
      d = File.join(sitearchdir, file)
      mkdir_p File.dirname(d)
      install(file, d)
    end
  end
end

desc "Installing library (extension)"
task :install_ext => [ :compile_ext, :install_pure, :install_ext_really ]

desc "Installing library (extension)"
task :install => :install_ext

desc "Compiling extension"
task :compile_ext => [ EXT_PARSER_DL, EXT_GENERATOR_DL ]

file EXT_PARSER_DL => EXT_PARSER_SRC do
  cd EXT_PARSER_DIR do
    ruby 'extconf.rb'
    system MAKE
  end
  cp "#{EXT_PARSER_DIR}/parser.#{CONFIG['DLEXT']}", EXT_ROOT_DIR
end

file EXT_GENERATOR_DL => EXT_GENERATOR_SRC do
  cd EXT_GENERATOR_DIR do
    ruby 'extconf.rb'
    system MAKE
  end
  cp "#{EXT_GENERATOR_DIR}/generator.#{CONFIG['DLEXT']}", EXT_ROOT_DIR
end

desc "Generate parser with ragel"
task :ragel => EXT_PARSER_SRC

task :ragel_clean do
  rm_rf EXT_PARSER_SRC
end

file EXT_PARSER_SRC => RAGEL_PATH do
  cd EXT_PARSER_DIR do
    if RAGEL_CODEGEN == 'ragel'
      system "ragel parser.rl -G2 -o parser.c"
    else
      system "ragel -x parser.rl | #{RAGEL_CODEGEN} -G2"
    end
  end
end

desc "Generate diagrams of ragel parser (ps)"
task :ragel_dot_ps do
  root = 'diagrams'
  specs = []
  File.new(RAGEL_PATH).grep(/^\s*machine\s*(\S+);\s*$/) { specs << $1 }
  for s in specs 
    if RAGEL_DOTGEN == 'ragel'
      system "ragel #{RAGEL_PATH} -S#{s} -p -V | dot -Tps -o#{root}/#{s}.ps"
    else
      system "ragel -x #{RAGEL_PATH} -S#{s} | #{RAGEL_DOTGEN} -p|dot -Tps -o#{root}/#{s}.ps"
    end
  end
end

desc "Generate diagrams of ragel parser (png)"
task :ragel_dot_png do
  root = 'diagrams'
  specs = []
  File.new(RAGEL_PATH).grep(/^\s*machine\s*(\S+);\s*$/) { specs << $1 }
  for s in specs 
    if RAGEL_DOTGEN == 'ragel'
      system "ragel #{RAGEL_PATH} -S#{s} -p -V | dot -Tpng -o#{root}/#{s}.png"
    else
      system "ragel -x #{RAGEL_PATH} -S#{s} | #{RAGEL_DOTGEN} -p|dot -Tpng -o#{root}/#{s}.png"
    end
  end
end

desc "Generate diagrams of ragel parser"
task :ragel_dot => [ :ragel_dot_png, :ragel_dot_ps ]

desc "Testing library (pure ruby)"
task :test_pure => :clean do
  ENV['JSON'] = 'pure'
  ENV['RUBYOPT'] = "-Iext:lib #{ENV['RUBYOPT']}"
  system "testrb #{Dir['tests/*.rb'] * ' '}"
end

desc "Testing library (extension)"
task :test_ext => :compile_ext do
  ENV['JSON'] = 'ext'
  ENV['RUBYOPT'] = "-Iext:lib #{ENV['RUBYOPT']}"
  system "testrb #{Dir['tests/*.rb'] * ' '}"
end

desc "Testing library (pure ruby and extension)"
task :test => [ :test_pure, :test_ext ]

desc "Benchmarking parser"
task :benchmark_parser do
  ENV['RUBYOPT'] = "-Ilib:ext #{ENV['RUBYOPT']}"
  ruby 'benchmarks/parser_benchmark.rb'
end

desc "Benchmarking generator"
task :benchmark_generator do
  ENV['RUBYOPT'] = "-Ilib:ext #{ENV['RUBYOPT']}"
  ruby 'benchmarks/generator_benchmark.rb'
end

desc "Benchmarking library"
task :benchmark => [ :benchmark_parser, :benchmark_generator ]

desc "Create RDOC documentation"
task :doc => [ :version, EXT_PARSER_SRC ] do
  system "rdoc -o doc -m doc-main.txt doc-main.txt lib/json.rb #{FileList['lib/json/**/*.rb']} #{EXT_PARSER_SRC} #{EXT_GENERATOR_SRC}"
end

if defined?(Gem) and defined?(Rake::GemPackageTask) and defined?(Rake::ExtensionTask)
  spec_pure = Gem::Specification.new do |s|
    s.name = 'json_pure'
    s.version = PKG_VERSION
    s.summary = "A JSON implementation in Ruby"
    s.description = "This is a JSON implementation in pure Ruby."

    s.files = PKG_FILES

    s.require_path = 'lib'

    s.bindir = "bin"
    s.executables = [ "edit_json.rb", "prettify_json.rb" ]
    s.default_executable = "edit_json.rb"

    s.has_rdoc = true
    s.extra_rdoc_files << 'doc-main.txt'
    s.rdoc_options <<
      '--title' <<  'JSON -- A JSON implemention' << '--main' << 'doc-main.txt'
    s.test_files.concat Dir['tests/*.rb']

    s.author = "Florian Frank"
    s.email = "flori@ping.de"
    s.homepage = "http://json.rubyforge.org"
    s.rubyforge_project = "json"
  end

  Rake::GemPackageTask.new(spec_pure) do |pkg|
      pkg.need_tar = true
      pkg.package_files += PKG_FILES
  end

  spec_ext = Gem::Specification.new do |s|
    s.name = 'json'
    s.version = PKG_VERSION
    s.summary = "A JSON implementation as a Ruby extension"
    s.description = "This is a JSON implementation as a Ruby extension in C."

    s.files = PKG_FILES

    s.extensions = FileList['ext/**/extconf.rb']

    s.require_path = EXT_ROOT_DIR
    s.require_paths << 'ext'
    s.require_paths << 'lib'

    s.bindir = "bin"
    s.executables = [ "edit_json.rb", "prettify_json.rb" ]
    s.default_executable = "edit_json.rb"

    s.has_rdoc = true
    s.extra_rdoc_files << 'doc-main.txt'
    s.rdoc_options <<
      '--title' <<  'JSON -- A JSON implemention' << '--main' << 'doc-main.txt'
    s.test_files.concat Dir['tests/*.rb']

    s.author = "Florian Frank"
    s.email = "flori@ping.de"
    s.homepage = "http://json.rubyforge.org"
    s.rubyforge_project = "json"
  end

  Rake::GemPackageTask.new(spec_ext) do |pkg|
    pkg.need_tar      = true
    pkg.package_files = PKG_FILES
  end

  Rake::ExtensionTask.new do |ext|
    ext.name            = 'parser'
    ext.gem_spec        = spec_ext
    ext.cross_compile   = true
    ext.cross_platform  = 'i386-mswin32'
    ext.ext_dir         = 'ext/json/ext/parser'
    ext.lib_dir         = 'lib/json/ext'
  end
    
  Rake::ExtensionTask.new do |ext|
    ext.name            = 'generator'
    ext.gem_spec        = spec_ext
    ext.cross_compile   = true
    ext.cross_platform  = 'i386-mswin32'
    ext.ext_dir         = 'ext/json/ext/generator'
    ext.lib_dir         = 'lib/json/ext'
  end
end

desc m = "Writing version information for #{PKG_VERSION}"
task :version do
  puts m
  File.open(File.join('lib', 'json', 'version.rb'), 'w') do |v|
    v.puts <<EOT
module JSON
  # JSON version
  VERSION         = '#{PKG_VERSION}'
  VERSION_ARRAY   = VERSION.split(/\\./).map { |x| x.to_i } # :nodoc:
  VERSION_MAJOR   = VERSION_ARRAY[0] # :nodoc:
  VERSION_MINOR   = VERSION_ARRAY[1] # :nodoc:
  VERSION_BUILD   = VERSION_ARRAY[2] # :nodoc:
end
EOT
  end
end

desc "Build all gems and archives for a new release."
task :release => [ :clean, :version, :cross, :native, :gem ] do
  system "#$0 clean native gem"
  system "#$0 clean package"
end

desc "Compile in the the source directory"
task :default => [ :version, :compile_ext ]

require File.dirname(__FILE__) + '/lib/ruby-debug-ide/version'
require "date"

# ------- Default Package ----------
RUBY_DEBUG_IDE_VERSION = Debugger::IDE_VERSION unless defined? RUBY_DEBUG_IDE_VERSION

unless defined? FILES
  FILES = ['CHANGES',
  'ChangeLog.md',
  'ChangeLog.archive',
  'MIT-LICENSE',
  'Rakefile',
  'ext/mkrf_conf.rb',
  'Gemfile',
  'ruby-debug-ide.gemspec'
  ]
  FILES.push(*Dir['bin/*'])
  FILES.push(*Dir['lib/**/*'])
  #  'test/**/*',
end

Gem::Specification.new do |spec|
  spec.name = "ruby-debug-ide"

  spec.homepage = "https://github.com/ruby-debug/ruby-debug-ide"
  spec.summary = "IDE interface for ruby-debug."
  spec.description = <<-EOF
An interface which glues ruby-debug to IDEs like Eclipse (RDT), NetBeans and RubyMine.
EOF

  spec.version = RUBY_DEBUG_IDE_VERSION

  spec.author = "Markus Barchfeld, Martin Krauskopf, Mark Moseley, JetBrains RubyMine Team"
  spec.email = "rubymine-feedback@jetbrains.com"
  spec.platform = Gem::Platform::RUBY
  spec.require_path = "lib"
  spec.bindir = "bin"
  spec.executables = ["rdebug-ide"]
  spec.files = FILES

  spec.extensions << "ext/mkrf_conf.rb" unless ENV['NO_EXT']
  spec.add_dependency("rake", ">= 0.8.1")

  spec.required_ruby_version = '>= 1.8.2'
  spec.date = DateTime.now
  spec.rubyforge_project = 'debug-commons'

  # rdoc
  spec.has_rdoc = false
end

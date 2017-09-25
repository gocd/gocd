# coding: utf-8
$LOAD_PATH.unshift File.expand_path('../lib', __FILE__)
require 'rake/version'

Gem::Specification.new do |s|
  s.name = "rake".freeze
  s.version = Rake::VERSION
  s.date = "2016-06-12"
  s.authors = ["Hiroshi SHIBATA".freeze, "Eric Hodel".freeze, "Jim Weirich".freeze]
  s.email = ["hsbt@ruby-lang.org".freeze, "drbrain@segment7.net".freeze, "".freeze]

  s.summary = "Rake is a Make-like program implemented in Ruby".freeze
  s.description = "Rake is a Make-like program implemented in Ruby. Tasks and dependencies are\nspecified in standard Ruby syntax.\n\nRake has the following features:\n\n* Rakefiles (rake's version of Makefiles) are completely defined in\n  standard Ruby syntax.  No XML files to edit.  No quirky Makefile\n  syntax to worry about (is that a tab or a space?)\n\n* Users can specify tasks with prerequisites.\n\n* Rake supports rule patterns to synthesize implicit tasks.\n\n* Flexible FileLists that act like arrays but know about manipulating\n  file names and paths.\n\n* A library of prepackaged tasks to make building rakefiles easier. For example,\n  tasks for building tarballs and publishing to FTP or SSH sites.  (Formerly\n  tasks for building RDoc and Gems were included in rake but they're now\n  available in RDoc and RubyGems respectively.)\n\n* Supports parallel execution of tasks.".freeze
  s.homepage = "https://github.com/ruby/rake".freeze
  s.licenses = ["MIT".freeze]

  s.files = `git ls-files -z`.split("\x0").reject { |f| f.match(%r{^(test|spec|features)/}) }
  s.bindir = "exe"
  s.executables = s.files.grep(%r{^exe/}) { |f| File.basename(f) }
  s.require_paths = ["lib".freeze]

  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.1".freeze
  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.2".freeze)
  s.rdoc_options = ["--main".freeze, "README.rdoc".freeze]

  s.add_development_dependency(%q<bundler>.freeze, ["~> 1.11"])
  s.add_development_dependency(%q<minitest>.freeze, ["~> 5.8"])
  s.add_development_dependency(%q<rdoc>.freeze, ["~> 4.0"])
end

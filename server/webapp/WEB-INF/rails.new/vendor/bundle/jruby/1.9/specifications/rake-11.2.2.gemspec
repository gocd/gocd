# -*- encoding: utf-8 -*-
# stub: rake 11.2.2 ruby lib

Gem::Specification.new do |s|
  s.name = "rake"
  s.version = "11.2.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.2") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Hiroshi SHIBATA", "Eric Hodel", "Jim Weirich"]
  s.bindir = "exe"
  s.date = "2016-06-12"
  s.description = "Rake is a Make-like program implemented in Ruby. Tasks and dependencies are\nspecified in standard Ruby syntax.\n\nRake has the following features:\n\n* Rakefiles (rake's version of Makefiles) are completely defined in\n  standard Ruby syntax.  No XML files to edit.  No quirky Makefile\n  syntax to worry about (is that a tab or a space?)\n\n* Users can specify tasks with prerequisites.\n\n* Rake supports rule patterns to synthesize implicit tasks.\n\n* Flexible FileLists that act like arrays but know about manipulating\n  file names and paths.\n\n* A library of prepackaged tasks to make building rakefiles easier. For example,\n  tasks for building tarballs and publishing to FTP or SSH sites.  (Formerly\n  tasks for building RDoc and Gems were included in rake but they're now\n  available in RDoc and RubyGems respectively.)\n\n* Supports parallel execution of tasks."
  s.email = ["hsbt@ruby-lang.org", "drbrain@segment7.net", ""]
  s.executables = ["rake"]
  s.files = ["exe/rake"]
  s.homepage = "https://github.com/ruby/rake"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--main", "README.rdoc"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.4.8"
  s.summary = "Rake is a Make-like program implemented in Ruby"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.11"])
      s.add_development_dependency(%q<minitest>, ["~> 5.8"])
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.11"])
      s.add_dependency(%q<minitest>, ["~> 5.8"])
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.11"])
    s.add_dependency(%q<minitest>, ["~> 5.8"])
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
  end
end

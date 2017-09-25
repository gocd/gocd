# -*- encoding: utf-8 -*-
# stub: ruby-debug-base 0.10.6 java lib

Gem::Specification.new do |s|
  s.name = "ruby-debug-base".freeze
  s.version = "0.10.6"
  s.platform = "java".freeze

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Kent Sibilev".freeze]
  s.date = "2015-08-27"
  s.description = "ruby-debug is a fast implementation of the standard Ruby debugger debug.rb.\nIt is implemented by utilizing a new Ruby C API hook. The core component\nprovides support that front-ends can build on. It provides breakpoint\nhandling, bindings for stack frames among other things.\n".freeze
  s.email = "ksibilev@yahoo.com".freeze
  s.extra_rdoc_files = ["README".freeze, "ext/ruby_debug.c".freeze]
  s.files = ["README".freeze, "ext/ruby_debug.c".freeze]
  s.homepage = "https://github.com/ruby-debug/".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Fast Ruby debugger - core component".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>.freeze, [">= 0"])
      s.add_development_dependency(%q<rdoc>.freeze, [">= 0"])
      s.add_development_dependency(%q<rake-compiler>.freeze, ["~> 0.8.1"])
    else
      s.add_dependency(%q<rake>.freeze, [">= 0"])
      s.add_dependency(%q<rdoc>.freeze, [">= 0"])
      s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.8.1"])
    end
  else
    s.add_dependency(%q<rake>.freeze, [">= 0"])
    s.add_dependency(%q<rdoc>.freeze, [">= 0"])
    s.add_dependency(%q<rake-compiler>.freeze, ["~> 0.8.1"])
  end
end

# -*- encoding: utf-8 -*-
# stub: ruby-debug-base 0.10.5.rc10 java lib

Gem::Specification.new do |s|
  s.name = "ruby-debug-base"
  s.version = "0.10.5.rc10"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new("> 1.3.1") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Kent Sibilev"]
  s.date = "2014-10-02"
  s.description = "ruby-debug is a fast implementation of the standard Ruby debugger debug.rb.\nIt is implemented by utilizing a new Ruby C API hook. The core component\nprovides support that front-ends can build on. It provides breakpoint\nhandling, bindings for stack frames among other things.\n"
  s.email = "ksibilev@yahoo.com"
  s.extra_rdoc_files = ["README", "ext/ruby_debug.c"]
  s.files = ["README", "ext/ruby_debug.c"]
  s.homepage = "https://github.com/ruby-debug/"
  s.rubygems_version = "2.4.8"
  s.summary = "Fast Ruby debugger - core component"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<rdoc>, [">= 0"])
      s.add_development_dependency(%q<rake-compiler>, ["~> 0.8.1"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<rdoc>, [">= 0"])
      s.add_dependency(%q<rake-compiler>, ["~> 0.8.1"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<rdoc>, [">= 0"])
    s.add_dependency(%q<rake-compiler>, ["~> 0.8.1"])
  end
end

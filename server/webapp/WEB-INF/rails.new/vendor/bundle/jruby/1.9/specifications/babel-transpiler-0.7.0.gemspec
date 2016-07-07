# -*- encoding: utf-8 -*-
# stub: babel-transpiler 0.7.0 ruby lib

Gem::Specification.new do |s|
  s.name = "babel-transpiler"
  s.version = "0.7.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Joshua Peek"]
  s.date = "2015-04-03"
  s.description = "    Ruby Babel is a bridge to the JS Babel transpiler.\n"
  s.email = "josh@joshpeek.com"
  s.homepage = "https://github.com/babel/ruby-babel-transpiler"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Ruby Babel JS Compiler"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<babel-source>, ["< 6", ">= 4.0"])
      s.add_runtime_dependency(%q<execjs>, ["~> 2.0"])
      s.add_development_dependency(%q<minitest>, ["~> 5.5"])
    else
      s.add_dependency(%q<babel-source>, ["< 6", ">= 4.0"])
      s.add_dependency(%q<execjs>, ["~> 2.0"])
      s.add_dependency(%q<minitest>, ["~> 5.5"])
    end
  else
    s.add_dependency(%q<babel-source>, ["< 6", ">= 4.0"])
    s.add_dependency(%q<execjs>, ["~> 2.0"])
    s.add_dependency(%q<minitest>, ["~> 5.5"])
  end
end

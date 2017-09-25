# -*- encoding: utf-8 -*-
# stub: babel-transpiler 0.7.0 ruby lib

Gem::Specification.new do |s|
  s.name = "babel-transpiler".freeze
  s.version = "0.7.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Joshua Peek".freeze]
  s.date = "2015-04-03"
  s.description = "    Ruby Babel is a bridge to the JS Babel transpiler.\n".freeze
  s.email = "josh@joshpeek.com".freeze
  s.homepage = "https://github.com/babel/ruby-babel-transpiler".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Ruby Babel JS Compiler".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<babel-source>.freeze, ["< 6", ">= 4.0"])
      s.add_runtime_dependency(%q<execjs>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<minitest>.freeze, ["~> 5.5"])
    else
      s.add_dependency(%q<babel-source>.freeze, ["< 6", ">= 4.0"])
      s.add_dependency(%q<execjs>.freeze, ["~> 2.0"])
      s.add_dependency(%q<minitest>.freeze, ["~> 5.5"])
    end
  else
    s.add_dependency(%q<babel-source>.freeze, ["< 6", ">= 4.0"])
    s.add_dependency(%q<execjs>.freeze, ["~> 2.0"])
    s.add_dependency(%q<minitest>.freeze, ["~> 5.5"])
  end
end

# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "bouncy-castle-java"
  s.version = "1.5.0147"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Hiroshi Nakamura"]
  s.date = "2013-03-12"
  s.description = "Gem redistribution of \"Legion of the Bouncy Castle Java cryptography APIs\" jars at http://www.bouncycastle.org/java.html"
  s.email = "nahi@ruby-lang.org"
  s.homepage = "http://github.com/jruby/jruby/tree/master/gems/bouncy-castle-java/"
  s.require_paths = ["lib"]
  s.rubyforge_project = "jruby-extras"
  s.rubygems_version = "1.8.24"
  s.summary = "Gem redistribution of Bouncy Castle jars"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

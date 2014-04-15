# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "jruby-pageant"
  s.version = "1.1.1"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Art\u{16b}ras \u{160}lajus"]
  s.date = "2012-06-11"
  s.description = "This is a convenience gem packaging required JNA/JSCH jars."
  s.email = ["arturas.slajus@gmail.com"]
  s.homepage = "http://github.com/arturaz/jruby-pageant"
  s.require_paths = ["lib-java"]
  s.rubygems_version = "1.8.24"
  s.summary = "jruby-pageant allows Pageant access on JRuby + Windows"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

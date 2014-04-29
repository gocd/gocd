# -*- encoding: utf-8 -*-
# stub: jruby-openssl 0.8.10 ruby lib/shared

Gem::Specification.new do |s|
  s.name = "jruby-openssl"
  s.version = "0.8.10"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Ola Bini", "JRuby contributors"]
  s.date = "2013-09-22"
  s.description = "JRuby-OpenSSL is an add-on gem for JRuby that emulates the Ruby OpenSSL native library."
  s.email = "ola.bini@gmail.com"
  s.homepage = "https://github.com/jruby/jruby"
  s.require_paths = ["lib/shared"]
  s.rubyforge_project = "jruby/jruby"
  s.rubygems_version = "2.1.9"
  s.summary = "OpenSSL add-on for JRuby"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_development_dependency(%q<rake>, ["~> 10.1"])
      s.add_development_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    else
      s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
      s.add_dependency(%q<rake>, ["~> 10.1"])
      s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
    end
  else
    s.add_dependency(%q<bouncy-castle-java>, [">= 1.5.0147"])
    s.add_dependency(%q<rake>, ["~> 10.1"])
    s.add_dependency(%q<ruby-maven>, ["~> 3.1.0.0.0"])
  end
end

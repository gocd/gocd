# -*- encoding: utf-8 -*-
# stub: therubyrhino 2.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "therubyrhino"
  s.version = "2.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Charles Lowell"]
  s.date = "2014-07-26"
  s.description = "Call javascript code and manipulate javascript objects from ruby. Call ruby code and manipulate ruby objects from javascript."
  s.email = "cowboyd@thefrontside.net"
  s.extra_rdoc_files = ["README.md"]
  s.files = ["README.md"]
  s.homepage = "http://github.com/cowboyd/therubyrhino"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "therubyrhino"
  s.rubygems_version = "2.1.9"
  s.summary = "Embed the Rhino JavaScript interpreter into JRuby"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<therubyrhino_jar>, [">= 1.7.3"])
      s.add_development_dependency(%q<rspec>, ["~> 2.14.1"])
      s.add_development_dependency(%q<mocha>, ["~> 0.13.3"])
    else
      s.add_dependency(%q<therubyrhino_jar>, [">= 1.7.3"])
      s.add_dependency(%q<rspec>, ["~> 2.14.1"])
      s.add_dependency(%q<mocha>, ["~> 0.13.3"])
    end
  else
    s.add_dependency(%q<therubyrhino_jar>, [">= 1.7.3"])
    s.add_dependency(%q<rspec>, ["~> 2.14.1"])
    s.add_dependency(%q<mocha>, ["~> 0.13.3"])
  end
end

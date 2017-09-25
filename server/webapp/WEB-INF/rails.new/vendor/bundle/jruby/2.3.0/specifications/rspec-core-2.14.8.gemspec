# -*- encoding: utf-8 -*-
# stub: rspec-core 2.14.8 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-core".freeze
  s.version = "2.14.8"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Steven Baker".freeze, "David Chelimsky".freeze, "Chad Humphries".freeze]
  s.bindir = "exe".freeze
  s.date = "2014-02-28"
  s.description = "BDD for Ruby. RSpec runner and example groups.".freeze
  s.email = "rspec-users@rubyforge.org".freeze
  s.executables = ["autospec".freeze, "rspec".freeze]
  s.files = ["exe/autospec".freeze, "exe/rspec".freeze]
  s.homepage = "http://github.com/rspec/rspec-core".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--charset=UTF-8".freeze]
  s.rubyforge_project = "rspec".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "rspec-core-2.14.8".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>.freeze, ["~> 10.0.0"])
      s.add_development_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
      s.add_development_dependency(%q<aruba>.freeze, ["~> 0.5"])
      s.add_development_dependency(%q<ZenTest>.freeze, ["~> 4.6"])
      s.add_development_dependency(%q<nokogiri>.freeze, ["= 1.5.2"])
      s.add_development_dependency(%q<syntax>.freeze, ["= 1.0.0"])
      s.add_development_dependency(%q<mocha>.freeze, ["~> 0.13.0"])
      s.add_development_dependency(%q<rr>.freeze, ["~> 1.0.4"])
      s.add_development_dependency(%q<flexmock>.freeze, ["~> 0.9.0"])
    else
      s.add_dependency(%q<rake>.freeze, ["~> 10.0.0"])
      s.add_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
      s.add_dependency(%q<aruba>.freeze, ["~> 0.5"])
      s.add_dependency(%q<ZenTest>.freeze, ["~> 4.6"])
      s.add_dependency(%q<nokogiri>.freeze, ["= 1.5.2"])
      s.add_dependency(%q<syntax>.freeze, ["= 1.0.0"])
      s.add_dependency(%q<mocha>.freeze, ["~> 0.13.0"])
      s.add_dependency(%q<rr>.freeze, ["~> 1.0.4"])
      s.add_dependency(%q<flexmock>.freeze, ["~> 0.9.0"])
    end
  else
    s.add_dependency(%q<rake>.freeze, ["~> 10.0.0"])
    s.add_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
    s.add_dependency(%q<aruba>.freeze, ["~> 0.5"])
    s.add_dependency(%q<ZenTest>.freeze, ["~> 4.6"])
    s.add_dependency(%q<nokogiri>.freeze, ["= 1.5.2"])
    s.add_dependency(%q<syntax>.freeze, ["= 1.0.0"])
    s.add_dependency(%q<mocha>.freeze, ["~> 0.13.0"])
    s.add_dependency(%q<rr>.freeze, ["~> 1.0.4"])
    s.add_dependency(%q<flexmock>.freeze, ["~> 0.9.0"])
  end
end

# -*- encoding: utf-8 -*-
# stub: rspec-expectations 2.14.5 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-expectations".freeze
  s.version = "2.14.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Steven Baker".freeze, "David Chelimsky".freeze]
  s.date = "2014-02-01"
  s.description = "rspec expectations (should[_not] and matchers)".freeze
  s.email = "rspec-users@rubyforge.org".freeze
  s.homepage = "http://github.com/rspec/rspec-expectations".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--charset=UTF-8".freeze]
  s.rubyforge_project = "rspec".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "rspec-expectations-2.14.5".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<diff-lcs>.freeze, ["< 2.0", ">= 1.1.3"])
      s.add_development_dependency(%q<rake>.freeze, ["~> 10.0.0"])
      s.add_development_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
      s.add_development_dependency(%q<aruba>.freeze, ["~> 0.5"])
    else
      s.add_dependency(%q<diff-lcs>.freeze, ["< 2.0", ">= 1.1.3"])
      s.add_dependency(%q<rake>.freeze, ["~> 10.0.0"])
      s.add_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
      s.add_dependency(%q<aruba>.freeze, ["~> 0.5"])
    end
  else
    s.add_dependency(%q<diff-lcs>.freeze, ["< 2.0", ">= 1.1.3"])
    s.add_dependency(%q<rake>.freeze, ["~> 10.0.0"])
    s.add_dependency(%q<cucumber>.freeze, ["~> 1.1.9"])
    s.add_dependency(%q<aruba>.freeze, ["~> 0.5"])
  end
end

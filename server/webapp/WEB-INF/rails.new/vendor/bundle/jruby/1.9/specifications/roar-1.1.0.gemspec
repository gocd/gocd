# -*- encoding: utf-8 -*-
# stub: roar 1.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "roar"
  s.version = "1.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Nick Sutterer"]
  s.date = "2017-01-12"
  s.description = "Object-oriented representers help you defining nested REST API documents which can then be rendered and parsed using one and the same concept."
  s.email = ["apotonick@gmail.com"]
  s.homepage = "http://trailblazer.to/gems/roar"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Parse and render REST API documents using representers."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<representable>, ["~> 3.0.0"])
      s.add_development_dependency(%q<rake>, [">= 0.10.1"])
      s.add_development_dependency(%q<test_xml>, ["= 0.1.6"])
      s.add_development_dependency(%q<minitest>, [">= 5.10"])
      s.add_development_dependency(%q<sinatra>, [">= 2.0.0"])
      s.add_development_dependency(%q<sinatra-contrib>, [">= 2.0.0"])
      s.add_development_dependency(%q<virtus>, [">= 1.0.0"])
      s.add_development_dependency(%q<faraday>, [">= 0"])
      s.add_development_dependency(%q<multi_json>, [">= 0"])
    else
      s.add_dependency(%q<representable>, ["~> 3.0.0"])
      s.add_dependency(%q<rake>, [">= 0.10.1"])
      s.add_dependency(%q<test_xml>, ["= 0.1.6"])
      s.add_dependency(%q<minitest>, [">= 5.10"])
      s.add_dependency(%q<sinatra>, [">= 2.0.0"])
      s.add_dependency(%q<sinatra-contrib>, [">= 2.0.0"])
      s.add_dependency(%q<virtus>, [">= 1.0.0"])
      s.add_dependency(%q<faraday>, [">= 0"])
      s.add_dependency(%q<multi_json>, [">= 0"])
    end
  else
    s.add_dependency(%q<representable>, ["~> 3.0.0"])
    s.add_dependency(%q<rake>, [">= 0.10.1"])
    s.add_dependency(%q<test_xml>, ["= 0.1.6"])
    s.add_dependency(%q<minitest>, [">= 5.10"])
    s.add_dependency(%q<sinatra>, [">= 2.0.0"])
    s.add_dependency(%q<sinatra-contrib>, [">= 2.0.0"])
    s.add_dependency(%q<virtus>, [">= 1.0.0"])
    s.add_dependency(%q<faraday>, [">= 0"])
    s.add_dependency(%q<multi_json>, [">= 0"])
  end
end

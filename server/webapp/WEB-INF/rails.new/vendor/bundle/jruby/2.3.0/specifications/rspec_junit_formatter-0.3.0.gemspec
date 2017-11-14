# -*- encoding: utf-8 -*-
# stub: rspec_junit_formatter 0.3.0 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec_junit_formatter".freeze
  s.version = "0.3.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 2.0.0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Samuel Cochran".freeze]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDKDCCAhCgAwIBAgIBBDANBgkqhkiG9w0BAQUFADA6MQ0wCwYDVQQDDARzajI2\nMRQwEgYKCZImiZPyLGQBGRYEc2oyNjETMBEGCgmSJomT8ixkARkWA2NvbTAeFw0x\nNjA3MjYxMTIxMjZaFw0xNzA3MjYxMTIxMjZaMDoxDTALBgNVBAMMBHNqMjYxFDAS\nBgoJkiaJk/IsZAEZFgRzajI2MRMwEQYKCZImiZPyLGQBGRYDY29tMIIBIjANBgkq\nhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsr60Eo/ttCk8GMTMFiPr3GoYMIMFvLak\nxSmTk9YGCB6UiEePB4THSSA5w6IPyeaCF/nWkDp3/BAam0eZMWG1IzYQB23TqIM0\n1xzcNRvFsn0aQoQ00k+sj+G83j3T5OOV5OZIlu8xAChMkQmiPd1NXc6uFv+Iacz7\nkj+CMsI9YUFdNoU09QY0b+u+Rb6wDYdpyvN60YC30h0h1MeYbvYZJx/iZK4XY5zu\n4O/FL2ChjL2CPCpLZW55ShYyrzphWJwLOJe+FJ/ZBl6YXwrzQM9HKnt4titSNvyU\nKzE3L63A3PZvExzLrN9u09kuWLLJfXB2sGOlw3n9t72rJiuBr3/OQQIDAQABozkw\nNzAJBgNVHRMEAjAAMAsGA1UdDwQEAwIEsDAdBgNVHQ4EFgQU99dfRjEKFyczTeIz\nm3ZsDWrNC80wDQYJKoZIhvcNAQEFBQADggEBAH//VnNBPm1b6MCuqaLLkulf2rqq\n7P8OY517doHl3uDKBeAvG0ufJ1Hk3yDCaoSnkP+JlpF3bPA9VFO/omKEMOLcEKEC\n9X6kFHhGo2jTRBAwaJ3OXvsm5bi5ZE0oivVh/QXpNOkLM+hqozLjquXrZ8la5KL8\njv7OM8j53TJA004uwxjuVK+nEqiril93acOe2tKdDedd0Qb0KrObFYY/en6OV92h\nbn56+Lu22lsUMYD62lUEdAPyYn1/JLPfq47UEuV1p5gy/f++aTxknsAYE/z/bfmO\nEkIxoeESVQ0Kqhp754GWYv3Sfmtyk8UbGo5XCZHzvC6h2G7mplPzh6o+Edw=\n-----END CERTIFICATE-----\n".freeze]
  s.date = "2017-07-05"
  s.description = "RSpec results that your continuous integration service can read.".freeze
  s.email = "sj26@sj26.com".freeze
  s.homepage = "http://github.com/sj26/rspec_junit_formatter".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 2.0.0".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "RSpec JUnit XML formatter".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rspec-core>.freeze, ["!= 2.12.0", "< 4", ">= 2"])
      s.add_development_dependency(%q<nokogiri>.freeze, ["~> 1.6"])
    else
      s.add_dependency(%q<rspec-core>.freeze, ["!= 2.12.0", "< 4", ">= 2"])
      s.add_dependency(%q<nokogiri>.freeze, ["~> 1.6"])
    end
  else
    s.add_dependency(%q<rspec-core>.freeze, ["!= 2.12.0", "< 4", ">= 2"])
    s.add_dependency(%q<nokogiri>.freeze, ["~> 1.6"])
  end
end

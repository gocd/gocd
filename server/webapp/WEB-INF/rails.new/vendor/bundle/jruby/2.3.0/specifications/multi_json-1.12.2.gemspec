# -*- encoding: utf-8 -*-
# stub: multi_json 1.12.2 ruby lib

Gem::Specification.new do |s|
  s.name = "multi_json".freeze
  s.version = "1.12.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.5".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Michael Bleigh".freeze, "Josh Kalderimis".freeze, "Erik Michaels-Ober".freeze, "Pavel Pravosud".freeze]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDcDCCAligAwIBAgIBATANBgkqhkiG9w0BAQUFADA/MQ4wDAYDVQQDDAVwYXZl\nbDEYMBYGCgmSJomT8ixkARkWCHByYXZvc3VkMRMwEQYKCZImiZPyLGQBGRYDY29t\nMB4XDTE3MDkwNTA0MjAxNVoXDTE4MDkwNTA0MjAxNVowPzEOMAwGA1UEAwwFcGF2\nZWwxGDAWBgoJkiaJk/IsZAEZFghwcmF2b3N1ZDETMBEGCgmSJomT8ixkARkWA2Nv\nbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMBI9gFjVGizTKJZmmim\n8zHrJmSu7NGlL7HBWRhV7e/Qp2qdKtiG7TGGi9HRc6s6LmmJ7/v1EuPDsRCY1hnO\n6aeiJKF02f9w/o1HSK1qnSvqzvXbMYmREbJygw5EQVSPfmtGhyMHwiq4DfwK+PnW\ndgNJYYIyf/FtWw+plgZapTZ3bj0EKswkLvFy12UyLhrLRu38JAezHfaeNs6Zf+Q1\nYgyAiPsbLeUjP+2k6QnFMM2gOJRZZq8ZDI5yFAr7+Q2AHqzb5I+BacYnODWuadXx\nqTbeYrgJS/20L+zoStoBpFlfSv8/BVu2ZwybxyU91WAFC5gnnNDymV6rKlza9gUJ\n4lkCAwEAAaN3MHUwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAwHQYDVR0OBBYEFGjY\n4LmBKPWO2jdS5KM1uNTR/xd8MB0GA1UdEQQWMBSBEnBhdmVsQHByYXZvc3VkLmNv\nbTAdBgNVHRIEFjAUgRJwYXZlbEBwcmF2b3N1ZC5jb20wDQYJKoZIhvcNAQEFBQAD\nggEBAB5ZWFOjLnT6Klcr4UFvss5oW1Wtg0BdRCS5V7v8ZvnJ3mfD7rw+J3c7ylpT\n9F4c4JLfJsg+FFbkDSETB1LfviGKPkpfjL3Uy1z6nWOko62XpFUiBydB9hnq0Fll\npDshFjNeJVp6BgWXjVs9m8h1hRvGsy0WxEF/IQ+V5Dw7hvi+qg5K6eMoXvpv3OJh\nIo9NF6vsT7YYaUl+zobpp+tMYsAST801YnAa2RSSlsRKEMtsDFtC9oTQZhAhGqHb\nlnsOhB8m73/1c9sJkVBZl4lGniy6lnB4BIr+2+hTXDK5AjQJC1uwKG1etzwBuCkz\n8dtH5+dv1G5BCpbYE2uIVLsl+LY=\n-----END CERTIFICATE-----\n".freeze]
  s.date = "2017-09-05"
  s.description = "A common interface to multiple JSON libraries, including Oj, Yajl, the JSON gem (with C-extensions), the pure-Ruby JSON gem, NSJSONSerialization, gson.rb, JrJackson, and OkJson.".freeze
  s.email = ["michael@intridea.com".freeze, "josh.kalderimis@gmail.com".freeze, "sferik@gmail.com".freeze, "pavel@pravosud.com".freeze]
  s.homepage = "http://github.com/intridea/multi_json".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "A common interface to multiple JSON libraries.".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>.freeze, ["~> 1.0"])
    else
      s.add_dependency(%q<bundler>.freeze, ["~> 1.0"])
    end
  else
    s.add_dependency(%q<bundler>.freeze, ["~> 1.0"])
  end
end

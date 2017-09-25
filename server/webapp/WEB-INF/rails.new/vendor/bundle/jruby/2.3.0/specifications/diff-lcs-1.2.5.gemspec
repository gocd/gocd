# -*- encoding: utf-8 -*-
# stub: diff-lcs 1.2.5 ruby lib

Gem::Specification.new do |s|
  s.name = "diff-lcs".freeze
  s.version = "1.2.5"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Austin Ziegler".freeze]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDNjCCAh6gAwIBAgIBADANBgkqhkiG9w0BAQUFADBBMQ8wDQYDVQQDDAZhdXN0\naW4xGTAXBgoJkiaJk/IsZAEZFglydWJ5Zm9yZ2UxEzARBgoJkiaJk/IsZAEZFgNv\ncmcwHhcNMTMwMjA0MDMzMzI3WhcNMTQwMjA0MDMzMzI3WjBBMQ8wDQYDVQQDDAZh\ndXN0aW4xGTAXBgoJkiaJk/IsZAEZFglydWJ5Zm9yZ2UxEzARBgoJkiaJk/IsZAEZ\nFgNvcmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC2mPNf4L37GhKI\nSPCYsvYWXA2/R9u5+pyUnbJ2R1o2CiRq2ZA/AIzY6N3hGnsgoWnh5RzvgTN1Lt08\nDNIrsIG2VDYk/JVt6f9J6zZ8EQHbznWa3cWYoCFaaICdk7jV1n/42hg70jEDYXl9\ngDOl0k6JmyF/rtfFu/OIkFGWeFYIuFHvRuLyUbw66+QDTOzKb3t8o55Ihgy1GVwT\ni6pkDs8LhZWXdOD+921l2Z1NZGZa9KNbJIg6vtgYKU98jQ5qr9iY3ikBAspHrFas\nK6USvGgAg8fCD5YiotBEvCBMYtfqmfrhpdU2p+gvTgeLW1Kaevwqd7ngQmFUrFG1\neUJSURv5AgMBAAGjOTA3MAkGA1UdEwQCMAAwHQYDVR0OBBYEFAtJKMp6YYNqlgR3\n9TiZLWqvLagSMAsGA1UdDwQEAwIEsDANBgkqhkiG9w0BAQUFAAOCAQEApTPkvDm8\n7gJlUT4FfumXPvtuqP67LxUtGE8syvR0A4As+0P/wylLJFUOsGTTdZYtThhxCSJG\n+7KG2FfIcH4Zz2d97arZGAzBoi8iPht2/UtSl1fCcUI5vmJa1MiXZT2oqdW7Wydq\nrAZcBPlrYYuiwtGI0yqIOgBfXSZCWWsJsuyTKELep6mCLgz0YZUfmvKr8W/Ab3ax\nDuLzH92LSRjZJyjyAUpw/Vc2rM4giiP5jtByrb1Y1dGnQhHTMHf1GfucWm7Nw/V9\ntwEPVw8+0f88JQucxOTmTF1NbLFpiRwQUZ1zoZbNg2e7mShc/eexnVLWKFKxRoP6\nKPj3WoD+spB8fA==\n-----END CERTIFICATE-----\n".freeze]
  s.date = "2013-11-08"
  s.description = "Diff::LCS computes the difference between two Enumerable sequences using the\nMcIlroy-Hunt longest common subsequence (LCS) algorithm. It includes utilities\nto create a simple HTML diff output format and a standard diff-like tool.\n\nThis is release 1.2.4, fixing a bug introduced after diff-lcs 1.1.3 that did\nnot properly prune common sequences at the beginning of a comparison set.\nThanks to Paul Kunysch for fixing this issue.\n\nCoincident with the release of diff-lcs 1.2.3, we reported an issue with\nRubinius in 1.9 mode\n({rubinius/rubinius#2268}[https://github.com/rubinius/rubinius/issues/2268]).\nWe are happy to report that this issue has been resolved.".freeze
  s.email = ["austin@rubyforge.org".freeze]
  s.executables = ["htmldiff".freeze, "ldiff".freeze]
  s.extra_rdoc_files = ["Contributing.rdoc".freeze, "History.rdoc".freeze, "License.rdoc".freeze, "Manifest.txt".freeze, "README.rdoc".freeze, "docs/COPYING.txt".freeze, "docs/artistic.txt".freeze]
  s.files = ["Contributing.rdoc".freeze, "History.rdoc".freeze, "License.rdoc".freeze, "Manifest.txt".freeze, "README.rdoc".freeze, "bin/htmldiff".freeze, "bin/ldiff".freeze, "docs/COPYING.txt".freeze, "docs/artistic.txt".freeze]
  s.homepage = "http://diff-lcs.rubyforge.org/".freeze
  s.licenses = ["MIT".freeze, "Perl Artistic v2".freeze, "GNU GPL v2".freeze]
  s.rdoc_options = ["--main".freeze, "README.rdoc".freeze]
  s.rubyforge_project = "diff-lcs".freeze
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Diff::LCS computes the difference between two Enumerable sequences using the McIlroy-Hunt longest common subsequence (LCS) algorithm".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rubyforge>.freeze, [">= 2.0.4"])
      s.add_development_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_development_dependency(%q<hoe-bundler>.freeze, ["~> 1.2"])
      s.add_development_dependency(%q<hoe-doofus>.freeze, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-gemspec2>.freeze, ["~> 1.1"])
      s.add_development_dependency(%q<hoe-git>.freeze, ["~> 1.5"])
      s.add_development_dependency(%q<hoe-rubygems>.freeze, ["~> 1.0"])
      s.add_development_dependency(%q<hoe-travis>.freeze, ["~> 1.2"])
      s.add_development_dependency(%q<rake>.freeze, ["~> 10.0"])
      s.add_development_dependency(%q<rspec>.freeze, ["~> 2.0"])
      s.add_development_dependency(%q<hoe>.freeze, ["~> 3.7"])
    else
      s.add_dependency(%q<rubyforge>.freeze, [">= 2.0.4"])
      s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
      s.add_dependency(%q<hoe-bundler>.freeze, ["~> 1.2"])
      s.add_dependency(%q<hoe-doofus>.freeze, ["~> 1.0"])
      s.add_dependency(%q<hoe-gemspec2>.freeze, ["~> 1.1"])
      s.add_dependency(%q<hoe-git>.freeze, ["~> 1.5"])
      s.add_dependency(%q<hoe-rubygems>.freeze, ["~> 1.0"])
      s.add_dependency(%q<hoe-travis>.freeze, ["~> 1.2"])
      s.add_dependency(%q<rake>.freeze, ["~> 10.0"])
      s.add_dependency(%q<rspec>.freeze, ["~> 2.0"])
      s.add_dependency(%q<hoe>.freeze, ["~> 3.7"])
    end
  else
    s.add_dependency(%q<rubyforge>.freeze, [">= 2.0.4"])
    s.add_dependency(%q<rdoc>.freeze, ["~> 4.0"])
    s.add_dependency(%q<hoe-bundler>.freeze, ["~> 1.2"])
    s.add_dependency(%q<hoe-doofus>.freeze, ["~> 1.0"])
    s.add_dependency(%q<hoe-gemspec2>.freeze, ["~> 1.1"])
    s.add_dependency(%q<hoe-git>.freeze, ["~> 1.5"])
    s.add_dependency(%q<hoe-rubygems>.freeze, ["~> 1.0"])
    s.add_dependency(%q<hoe-travis>.freeze, ["~> 1.2"])
    s.add_dependency(%q<rake>.freeze, ["~> 10.0"])
    s.add_dependency(%q<rspec>.freeze, ["~> 2.0"])
    s.add_dependency(%q<hoe>.freeze, ["~> 3.7"])
  end
end

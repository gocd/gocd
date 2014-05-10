# -*- encoding: utf-8 -*-
# stub: xpath 2.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "xpath"
  s.version = "2.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jonas Nicklas"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDhTCCAm2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBEMRYwFAYDVQQDDA1qb25h\ncy5uaWNrbGFzMRUwEwYKCZImiZPyLGQBGRYFZ21haWwxEzARBgoJkiaJk/IsZAEZ\nFgNjb20wHhcNMTMwMzE2MDEzMTExWhcNMTQwMzE2MDEzMTExWjBEMRYwFAYDVQQD\nDA1qb25hcy5uaWNrbGFzMRUwEwYKCZImiZPyLGQBGRYFZ21haWwxEzARBgoJkiaJ\nk/IsZAEZFgNjb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC+B9mx\n0TVkvXIN31JC4s4Ni6mchJL/98h+euMIENt5LoxceYlijP0nGdYHAMoA2CUl7E+J\n/Km3W1ziSC0Mr3YIqCwp4fygP1woyRKRYmoyBRZdW5JiPAQzYxXaib0+BCB+JhSm\nRev2+BaHJuRRKWq+S3bzZ61U6RMVp/tAfeInXOQdvm3ycyhOGWx6XpEW2ON6oRDl\nmBuAM8b9hjpOAzFXh3OLXBC9zq5Z5pl2frlfAVB2hKztwiEiNkGjx/qBXixvo7du\nMx+O296WpLuuWwrOaIsR3Q37aNvLBvieh74D9QJjcwR3Mr0RRQLbXxNGsSQa6gY4\nl9j0EtUVSDZB54H5AgMBAAGjgYEwfzAJBgNVHRMEAjAAMAsGA1UdDwQEAwIEsDAd\nBgNVHQ4EFgQUf8GtjwnaRtSqgvJDzCc3Ta7LHMswIgYDVR0RBBswGYEXam9uYXMu\nbmlja2xhc0BnbWFpbC5jb20wIgYDVR0SBBswGYEXam9uYXMubmlja2xhc0BnbWFp\nbC5jb20wDQYJKoZIhvcNAQEFBQADggEBAGlsnpcev4fu+FDAygbObfc73pltKnQF\nEwy88+HgJ4AhjwHER7OWDVMwh6vHkmEep1rhLnNWGztD/ym5iFk4FGIz1D5J7TqQ\nk4v2w+Oje3YW/pSAGgAVPGCCrlqiw4hKZUzPob8NWhuS4tKDPWMGBQx3ZSRyqILc\n/WQ/FmnbVa9xRUo+RWiP9TpzKQJiR3BP9OpVrRx13m0euFxzTi6ikJzC45Q3Dq5V\niQxUmCCRsJVcsSFORJLdWNVqzBOWZRnqrLDunzouCbcqziNWk8KqjZCr5itRi319\n/2kZgwc32FHlCdCBXkhElEHT/bGCFk2+ajnmWRtqZpz/poTgEehiIUY=\n-----END CERTIFICATE-----\n"]
  s.date = "2013-04-09"
  s.description = "XPath is a Ruby DSL for generating XPath expressions"
  s.email = ["jonas.nicklas@gmail.com"]
  s.extra_rdoc_files = ["README.md"]
  s.files = ["README.md"]
  s.homepage = "http://github.com/jnicklas/xpath"
  s.rdoc_options = ["--main", "README.md"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "xpath"
  s.rubygems_version = "2.1.9"
  s.summary = "Generate XPath expressions from Ruby"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<nokogiri>, ["~> 1.3"])
      s.add_development_dependency(%q<rspec>, ["~> 2.0"])
      s.add_development_dependency(%q<yard>, [">= 0.5.8"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<nokogiri>, ["~> 1.3"])
      s.add_dependency(%q<rspec>, ["~> 2.0"])
      s.add_dependency(%q<yard>, [">= 0.5.8"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<nokogiri>, ["~> 1.3"])
    s.add_dependency(%q<rspec>, ["~> 2.0"])
    s.add_dependency(%q<yard>, [">= 0.5.8"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end

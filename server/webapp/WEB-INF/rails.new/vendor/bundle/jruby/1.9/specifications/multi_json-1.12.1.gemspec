# -*- encoding: utf-8 -*-
# stub: multi_json 1.12.1 ruby lib

Gem::Specification.new do |s|
  s.name = "multi_json"
  s.version = "1.12.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.5") if s.respond_to? :required_rubygems_version=
  s.authors = ["Michael Bleigh", "Josh Kalderimis", "Erik Michaels-Ober", "Pavel Pravosud"]
  s.cert_chain = ["-----BEGIN CERTIFICATE-----\nMIIDcDCCAligAwIBAgIBATANBgkqhkiG9w0BAQUFADA/MQ4wDAYDVQQDDAVwYXZl\nbDEYMBYGCgmSJomT8ixkARkWCHByYXZvc3VkMRMwEQYKCZImiZPyLGQBGRYDY29t\nMB4XDTE2MDQyNDIyMDk1MVoXDTE3MDQyNDIyMDk1MVowPzEOMAwGA1UEAwwFcGF2\nZWwxGDAWBgoJkiaJk/IsZAEZFghwcmF2b3N1ZDETMBEGCgmSJomT8ixkARkWA2Nv\nbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK+YCSpSUOeZvxOyp0Zm\nDhlQ9Kc8ZxgaB3ekCS6lp7hV+eE6nZ84j4RLEqhfx0Vffx+yCmSx0lWum6eY9aOy\nrr+uCtiSiL+HR7t6KHqQ5myXwIvT7B+SqMYw8223fMFZMUit73PfTaMlIon+EsZB\n9TWzVU7MSRIHLr8P92/kExOuDhVcqFgmz+pWLeZjCk7r0JI0vxacFEK+ONjXThHk\nW1IRwy8qaFNiUdnIfTRgZV45T/PHzuLttdkgySTDQkZp198t9Y0m0eEDhpPjHNlr\nKoXtqUIqk1lmgsKKrOj4vsSX004v869GT45C4qR4/Oa2OyUsWiPf8N3GCYDBnK9C\nRDcCAwEAAaN3MHUwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAwHQYDVR0OBBYEFKm/\njUdmc0kO/erio7IwB4zhYGmxMB0GA1UdEQQWMBSBEnBhdmVsQHByYXZvc3VkLmNv\nbTAdBgNVHRIEFjAUgRJwYXZlbEBwcmF2b3N1ZC5jb20wDQYJKoZIhvcNAQEFBQAD\nggEBAGZprwh9PfxTaukluduGO2NWJpI5NC7A/OpoVFrtLTlMKDeoPvCgmNdSejS3\n6CyH8P3SI3OEkymRnLtQiJeQ//WDb7QPPQDPG0ZuxAylc35ITz7jTPAFC41AoTWM\neSDWXP6yq0Gi6vlcvyIoBrvfFRPsg/gGhUp5DYKDLYzaEjNE30bME9fwDvlab7XR\nv4so5Zmmcof+9apAoaXDtj7HijhJWJcia8GWN5ycuDX38qMcpSU9/PF84s567W6e\nDe8xFEGqLG8vclcTv7gGjDJH5FJTXuwLg41wc8p4ONXEBgLiaC7+S/DVDXWpYxuB\nakI17ua4eRKTFNvBtzP1802SP1k=\n-----END CERTIFICATE-----\n"]
  s.date = "2016-05-18"
  s.description = "A common interface to multiple JSON libraries, including Oj, Yajl, the JSON gem (with C-extensions), the pure-Ruby JSON gem, NSJSONSerialization, gson.rb, JrJackson, and OkJson."
  s.email = ["michael@intridea.com", "josh.kalderimis@gmail.com", "sferik@gmail.com", "pavel@pravosud.com"]
  s.homepage = "http://github.com/intridea/multi_json"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "A common interface to multiple JSON libraries."

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.0"])
  end
end

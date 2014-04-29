# -*- encoding: utf-8 -*-
# stub: krypt 0.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "krypt"
  s.version = "0.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Hiroshi Nakamura, Martin Bosslet"]
  s.date = "2013-02-27"
  s.description = "krypt provides a unified framework for Ruby cryptography by offering a platform- and library-independent provider mechanism."
  s.email = "Martin.Bosslet@gmail.com"
  s.extra_rdoc_files = ["README.md"]
  s.files = ["lib/krypt/x509/certificate.rb", "lib/krypt/x509/common.rb", "lib/krypt/x509/crl.rb", "lib/krypt/codec/base_codec.rb", "lib/krypt/codec/base64.rb", "lib/krypt/codec/hex.rb", "lib/krypt/pkcs5/pbkdf2.rb", "lib/krypt/provider.rb", "lib/krypt/x509.rb", "lib/krypt/asn1/template.rb", "lib/krypt/asn1/common.rb", "lib/krypt/hmac.rb", "lib/krypt/pkcs5.rb", "lib/krypt/digest.rb", "lib/krypt/asn1.rb", "lib/krypt/codec.rb", "lib/krypt.rb", "lib/krypt_missing.rb", "spec/krypt/codec/hex_decoder_spec.rb", "spec/krypt/codec/hex_mixed_spec.rb", "spec/krypt/codec/hex_encoder_spec.rb", "spec/krypt/codec/base64_encoder_spec.rb", "spec/krypt/codec/base64_mixed_spec.rb", "spec/krypt/codec/identity_shared.rb", "spec/krypt/codec/base64_decoder_spec.rb", "spec/krypt/pkcs5/pbkdf2_spec.rb", "spec/krypt/hmac/hmac_spec.rb", "spec/krypt/provider/provider_spec.rb", "spec/krypt-core/digest/digest_spec.rb", "spec/krypt-core/MEMO.txt", "spec/krypt-core/hex/hex_spec.rb", "spec/krypt-core/asn1/asn1_pem_spec.rb", "spec/krypt-core/asn1/asn1_data_spec.rb", "spec/krypt-core/asn1/asn1_generalized_time_spec.rb", "spec/krypt-core/asn1/asn1_parser_spec.rb", "spec/krypt-core/asn1/asn1_integer_spec.rb", "spec/krypt-core/asn1/asn1_set_spec.rb", "spec/krypt-core/asn1/asn1_enumerated_spec.rb", "spec/krypt-core/asn1/asn1_octet_string_spec.rb", "spec/krypt-core/asn1/asn1_bit_string_spec.rb", "spec/krypt-core/asn1/asn1_constants_spec.rb", "spec/krypt-core/asn1/asn1_utc_time_spec.rb", "spec/krypt-core/asn1/asn1_null_spec.rb", "spec/krypt-core/asn1/asn1_sequence_spec.rb", "spec/krypt-core/asn1/asn1_boolean_spec.rb", "spec/krypt-core/asn1/asn1_object_id_spec.rb", "spec/krypt-core/asn1/asn1_utf8_string_spec.rb", "spec/krypt-core/asn1/resources.rb", "spec/krypt-core/asn1/asn1_end_of_contents_spec.rb", "spec/krypt-core/base64/base64_spec.rb", "spec/krypt-core/pem/pem_decode_spec.rb", "spec/krypt-core/template/template_choice_parse_spec.rb", "spec/krypt-core/template/template_seq_parse_spec.rb", "spec/krypt-core/template/template_seq_of_parse_spec.rb", "spec/krypt-core/template/template_dsl_spec.rb", "spec/krypt-core/resources.rb", "spec/resources.rb", "spec/res/certificate.pem", "spec/res/ca-bundle.crt", "spec/res/multiple_certs.pem", "spec/res/certificate.cer", "test/test_krypt_parser.rb", "test/scratch.rb", "test/helper.rb", "test/test_krypt_asn1.rb", "test/resources.rb", "test/res/certificate.cer", "LICENSE", "README.md"]
  s.homepage = "https://github.com/krypt/krypt"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.1.3"
  s.summary = "Platform- and library-independent cryptography for Ruby"
  s.test_files = ["test/test_krypt_parser.rb", "test/test_krypt_asn1.rb"]

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<krypt-core>, ["= 0.0.1"])
    else
      s.add_dependency(%q<krypt-core>, ["= 0.0.1"])
    end
  else
    s.add_dependency(%q<krypt-core>, ["= 0.0.1"])
  end
end

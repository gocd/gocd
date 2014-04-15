module Krypt
  module ASN1

    class DirectoryString
      include Template::Choice

      asn1_t61_string
      asn1_ia5_string
      asn1_printable_string
      asn1_universal_string
      asn1_utf8_string
      asn1_bmp_string
    end

    class DistinguishedName
      include Template::SequenceOf

      class AttributeTypeAndValue
        include Template::Sequence

        asn1_object_id :type
        asn1_template :value, DirectoryString
      end

      class RelativeDistinguishedName
        include Template::SetOf

        asn1_type AttributeTypeAndValue
      end

      asn1_type RelativeDistinguishedName
    end

    class GeneralName
      include Template::Choice

      class OtherName
        include Template::Sequence

        asn1_object_id :type
        asn1_any :value, tag: 0, tagging: :EXPLICIT
      end

      class EDIPartyName
        include Template::Sequence

        asn1_template :name_assigner, DirectoryString, tag: 0, tagging: :IMPLICIT, optional: true
        asn1_template :party_name,    DirectoryString, tag: 1, tagging: :IMPLICIT
      end

      asn1_template     OtherName,         tag: 0, tagging: :IMPLICIT
      asn1_ia5_string                      tag: 1, tagging: :IMPLICIT
      asn1_ia5_string                      tag: 2, tagging: :IMPLICIT
      asn1_any                             tag: 3, tagging: :IMPLICIT 
      asn1_template     DistinguishedName, tag: 4, tagging: :EXPLICIT
      asn1_template     EDIPartyName,      tag: 5, tagging: :IMPLICIT
      asn1_ia5_string                      tag: 6, tagging: :IMPLICIT
      asn1_octet_string                    tag: 7, tagging: :IMPLICIT
      asn1_object_id                       tag: 8, tagging: :IMPLICIT
    end

    class AlgorithmIdentifier
      include Template::Sequence

      asn1_object_id :algorithm
      asn1_any :params, optional: true

      def self.algorithm_null_params(name)
        AlgorithmIdentifier.new do |alg|
          alg.algorithm = name
          alg.params = Krypt::ASN1::Null.new
        end
      end
      class << self; private :algorithm_null_params; end

      MD5        = algorithm_null_params('1.2.840.113549.2.5')
      RIPEMD160  = algorithm_null_params('1.3.36.3.2.1')
      SHA1       = algorithm_null_params('1.3.14.3.2.26')
      SHA224     = algorithm_null_params('2.16.840.1.101.3.4.2.4')
      SHA256     = algorithm_null_params('2.16.840.1.101.3.4.2.1')
      SHA384     = algorithm_null_params('2.16.840.1.101.3.4.2.2')
      SHA512     = algorithm_null_params('2.16.840.1.101.3.4.2.3')

      RSA        = algorithm_null_params('1.2.840.113549.1.1.1')

      RSA_MD5    = algorithm_null_params('1.2.840.113549.1.1.4')
      RSA_SHA1   = algorithm_null_params('1.2.840.113549.1.1.5')
      RSA_SHA224 = algorithm_null_params('1.2.840.113549.1.1.14')
      RSA_SHA256 = algorithm_null_params('1.2.840.113549.1.1.11')
      RSA_SHA384 = algorithm_null_params('1.2.840.113549.1.1.12')
      RSA_SHA512 = algorithm_null_params('1.2.840.113549.1.1.13')
    end

  end
end


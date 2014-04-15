module Krypt
  module X509

    class Extension
      include ASN1::Template::Sequence

      asn1_object_id :id
      asn1_boolean :critical, default: false
      asn1_octet_string :value
    end

    class Attribute
      include ASN1::Template::Sequence

      asn1_object_id :type
      asn1_set_of :value, ASN1::ASN1Data
    end

    class IssuerSerialNumber
      include Krypt::ASN1::Template::Sequence

      asn1_template :issuer, ASN1::DistinguishedName
      asn1_integer :serial
    end

    class Time
      include ASN1::Template::Choice

      asn1_utc_time
      asn1_generalized_time
    end

    class Validity
      include ASN1::Template::Sequence

      asn1_template :not_before, X509::Time
      asn1_template :not_after, X509::Time
    end

  end
end

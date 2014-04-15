module Krypt
  module X509

    class CRL
      include ASN1::Template::Sequence

      class RevokedCertificates
        include ASN1::Template::Sequence

        asn1_integer :serial_number
        asn1_template :revocation_date, X509::Time
        asn1_template :crl_entry_extensions, X509::Extension
      end

      class TBSCertList
        include ASN1::Template::Sequence

        asn1_integer :version, default: 1
        asn1_template :signature_algorithm, ASN1::AlgorithmIdentifier
        asn1_template :issuer, ASN1::DistinguishedName
        asn1_template :this_update, X509::Time
        asn1_template :next_update, X509::Time
        asn1_sequence_of :revoked_certificates, RevokedCertificates, optional: true
        asn1_template :extensions, X509::Extension, tag: 0, tagging: :EXPLICIT, optional: true
      end

      asn1_template :tbs_cert_list, TBSCertList
      asn1_template :signature_algorithm, ASN1::AlgorithmIdentifier
      asn1_bit_string :signature
    end

  end
end

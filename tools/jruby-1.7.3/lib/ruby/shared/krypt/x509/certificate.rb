module Krypt
  module X509

    class Certificate
      include ASN1::Template::Sequence

      class SubjectPublicKeyInfo
        include ASN1::Template::Sequence

        asn1_template :algorithm, ASN1::AlgorithmIdentifier
        asn1_bit_string :subject_pkey
      end

      class TBSCertificate
        include ASN1::Template::Sequence

        asn1_integer :version, tag: 0, tagging: :EXPLICIT, default: 0
        asn1_integer :serial
        asn1_template :algorithm, ASN1::AlgorithmIdentifier
        asn1_template :issuer, ASN1::DistinguishedName
        asn1_template :validity, X509::Validity
        asn1_template :subject, ASN1::DistinguishedName
        asn1_template :subject_pkey, SubjectPublicKeyInfo
        asn1_bit_string :issuer_id, tag: 1, tagging: :IMPLICIT, optional: true
        asn1_bit_string :subject_id, tag: 2, tagging: :IMPLICIT, optional: true
        asn1_sequence_of :extensions, X509::Extension, tag: 3, tagging: :EXPLICIT, optional: true
      end

      asn1_template :tbs_cert, TBSCertificate
      asn1_template :algorithm, ASN1::AlgorithmIdentifier
      asn1_bit_string :signature
    end

  end
end


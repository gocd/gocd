module PKCS7Test
  class TestJavaPKCS7 < Test::Unit::TestCase
    def test_is_signed
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      assert p7.signed?
      assert !p7.encrypted?
      assert !p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert !p7.data?
      assert !p7.digest?
    end

    def test_is_encrypted
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert !p7.signed?
      assert p7.encrypted?
      assert !p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert !p7.data?
      assert !p7.digest?
    end

    def test_is_enveloped
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert !p7.signed?
      assert !p7.encrypted?
      assert p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert !p7.data?
      assert !p7.digest?
    end

    def test_is_signed_and_enveloped
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      assert !p7.signed?
      assert !p7.encrypted?
      assert !p7.enveloped?
      assert p7.signed_and_enveloped?
      assert !p7.data?
      assert !p7.digest?
    end

    def test_is_data
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert !p7.signed?
      assert !p7.encrypted?
      assert !p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert p7.data?
      assert !p7.digest?
    end

    def test_is_digest
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert !p7.signed?
      assert !p7.encrypted?
      assert !p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert !p7.data?
      assert p7.digest?
    end

    def test_set_detached
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed

      sign = Signed.new
      p7.sign = sign
      
      test_p7 = PKCS7.new
      test_p7.type = ASN1Registry::NID_pkcs7_data 
      test_p7.data = ASN1::OctetString.new("foo".to_java_bytes)
      sign.contents = test_p7
      
      p7.detached = 2
      assert_equal 1, p7.get_detached
      assert_equal nil, test_p7.get_data
    end

    def test_set_not_detached
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed

      sign = Signed.new
      p7.sign = sign
      
      test_p7 = PKCS7.new
      test_p7.type = ASN1Registry::NID_pkcs7_data 
      data = ASN1::OctetString.new("foo".to_java_bytes)
      test_p7.data = data
      sign.contents = test_p7
      
      p7.detached = 0
      assert_equal 0, p7.get_detached
      assert_equal data, test_p7.get_data
    end

    def test_is_detached
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed

      sign = Signed.new
      p7.sign = sign
      
      test_p7 = PKCS7.new
      test_p7.type = ASN1Registry::NID_pkcs7_data 
      data = ASN1::OctetString.new("foo".to_java_bytes)
      test_p7.data = data
      sign.contents = test_p7
      
      p7.detached = 1
      assert p7.detached?
    end

    def test_is_detached_with_wrong_type
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      
      assert !p7.detached?
    end
    
    def _test_encrypt_generates_enveloped_PKCS7_object
      p7 = PKCS7.encrypt([], "".to_java_bytes, nil, 0)
      assert !p7.signed?
      assert !p7.encrypted?
      assert p7.enveloped?
      assert !p7.signed_and_enveloped?
      assert !p7.data?
      assert !p7.digest?
    end
    
    def test_set_type_throws_exception_on_wrong_argument
      assert_raises NativeException do 
        # 42 is a value that is not one of the valid NID's for type
        PKCS7.new.type = 42
      end
    end
    
    def test_set_type_signed
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed

      assert p7.signed?
      assert_equal 1, p7.get_sign.version

      assert_nil p7.get_data
      assert_nil p7.get_enveloped
      assert_nil p7.get_signed_and_enveloped
      assert_nil p7.get_digest
      assert_nil p7.get_encrypted
      assert_nil p7.get_other
    end

    def test_set_type_data
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data

      assert p7.data?
      assert_equal ASN1::OctetString.new("".to_java_bytes), p7.get_data

      assert_nil p7.get_sign
      assert_nil p7.get_enveloped
      assert_nil p7.get_signed_and_enveloped
      assert_nil p7.get_digest
      assert_nil p7.get_encrypted
      assert_nil p7.get_other
    end

    def test_set_type_signed_and_enveloped
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped

      assert p7.signed_and_enveloped?
      assert_equal 1, p7.get_signed_and_enveloped.version
      assert_equal ASN1Registry::NID_pkcs7_data, p7.get_signed_and_enveloped.enc_data.content_type

      assert_nil p7.get_sign
      assert_nil p7.get_enveloped
      assert_nil p7.get_data
      assert_nil p7.get_digest
      assert_nil p7.get_encrypted
      assert_nil p7.get_other
    end

    def test_set_type_enveloped
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped

      assert p7.enveloped?
      assert_equal 0, p7.get_enveloped.version
      assert_equal ASN1Registry::NID_pkcs7_data, p7.get_enveloped.enc_data.content_type

      assert_nil p7.get_sign
      assert_nil p7.get_signed_and_enveloped
      assert_nil p7.get_data
      assert_nil p7.get_digest
      assert_nil p7.get_encrypted
      assert_nil p7.get_other
    end

    def test_set_type_encrypted
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted

      assert p7.encrypted?
      assert_equal 0, p7.get_encrypted.version
      assert_equal ASN1Registry::NID_pkcs7_data, p7.get_encrypted.enc_data.content_type

      assert_nil p7.get_sign
      assert_nil p7.get_signed_and_enveloped
      assert_nil p7.get_data
      assert_nil p7.get_digest
      assert_nil p7.get_enveloped
      assert_nil p7.get_other
    end

    def test_set_type_digest
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest

      assert p7.digest?
      assert_equal 0, p7.get_digest.version

      assert_nil p7.get_sign
      assert_nil p7.get_signed_and_enveloped
      assert_nil p7.get_data
      assert_nil p7.get_encrypted
      assert_nil p7.get_enveloped
      assert_nil p7.get_other
    end
    
    def test_set_cipher_on_non_enveloped_object
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      
      assert_raises NativeException do 
        p7.cipher = nil
      end
      
      p7.type = ASN1Registry::NID_pkcs7_encrypted

      assert_raises NativeException do 
        p7.cipher = nil
      end

      p7.type = ASN1Registry::NID_pkcs7_data

      assert_raises NativeException do 
        p7.cipher = nil
      end

      p7.type = ASN1Registry::NID_pkcs7_signed

      assert_raises NativeException do 
        p7.cipher = nil
      end
    end
    
    def test_set_cipher_on_enveloped_object
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped

      cipher = javax.crypto.Cipher.getInstance("RSA")
      
      p7.cipher = cipher
      
      assert_equal cipher, p7.get_enveloped.enc_data.cipher
    end

    
    def test_set_cipher_on_signedAndEnveloped_object
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped

      cipher = javax.crypto.Cipher.getInstance("RSA")
      
      p7.cipher = cipher
      
      assert_equal cipher, p7.get_signed_and_enveloped.enc_data.cipher
    end
    
    def test_add_recipient_info_to_something_that_cant_have_recipients
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      assert_raises NativeException do 
        p7.add_recipient(X509Cert)
      end

      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.add_recipient(X509Cert)
      end
      
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.add_recipient(X509Cert)
      end
      
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert_raises NativeException do 
        p7.add_recipient(X509Cert)
      end
    end

    def test_add_recipient_info_to_enveloped_should_add_that_to_stack
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      
      ri = p7.add_recipient(X509Cert)
      
      assert_equal 1, p7.get_enveloped.recipient_info.size
      assert_equal ri, p7.get_enveloped.recipient_info.iterator.next
    end


    def test_add_recipient_info_to_signedAndEnveloped_should_add_that_to_stack
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      
      ri = p7.add_recipient(X509Cert)
      
      assert_equal 1, p7.get_signed_and_enveloped.recipient_info.size
      assert_equal ri, p7.get_signed_and_enveloped.recipient_info.iterator.next
    end
    
    def test_add_signer_to_something_that_cant_have_signers
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_raises NativeException do 
        p7.add_signer(SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil))
      end

      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.add_signer(SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil))
      end
      
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.add_signer(SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil))
      end
      
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert_raises NativeException do 
        p7.add_signer(SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil))
      end
    end

    def test_add_signer_to_signed_should_add_that_to_stack
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      
      si = SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil)
      p7.add_signer(si)
      
      assert_equal 1, p7.get_sign.signer_info.size
      assert_equal si, p7.get_sign.signer_info.iterator.next
    end


    def test_add_signer_to_signedAndEnveloped_should_add_that_to_stack
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      
      si = SignerInfoWithPkey.new(nil, nil, nil, nil, nil, nil, nil)
      p7.add_signer(si)
      
      assert_equal 1, p7.get_signed_and_enveloped.signer_info.size
      assert_equal si, p7.get_signed_and_enveloped.signer_info.iterator.next
    end

    def create_signer_info_with_algo(algo)
      md5 = AlgorithmIdentifier.new(ASN1Registry.nid2obj(4))
      SignerInfoWithPkey.new(DERInteger.new(BigInteger::ONE), 
                     IssuerAndSerialNumber.new(X509Name.new("C=SE"), DERInteger.new(BigInteger::ONE)), 
                     algo, 
                     DERSet.new, 
                     md5, 
                     DEROctetString.new([].to_java(:byte)), 
                     DERSet.new)
    end
    
    def test_add_signer_to_signed_with_new_algo_should_add_that_algo_to_the_algo_list
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed

      # YES, these numbers are correct. Don't change them. They are OpenSSL internal NIDs
      md5 = AlgorithmIdentifier.new(ASN1Registry.nid2obj(4))
      md4 = AlgorithmIdentifier.new(ASN1Registry.nid2obj(5))
      
      si = create_signer_info_with_algo(md5)
      p7.add_signer(si)

      assert_equal md5, p7.get_sign.md_algs.iterator.next
      assert_equal 1, p7.get_sign.md_algs.size

      si = create_signer_info_with_algo(md5)
      p7.add_signer(si)

      assert_equal md5, p7.get_sign.md_algs.iterator.next
      assert_equal 1, p7.get_sign.md_algs.size

      si = create_signer_info_with_algo(md4)
      p7.add_signer(si)

      assert_equal 2, p7.get_sign.md_algs.size
      assert p7.get_sign.md_algs.contains(md4)
      assert p7.get_sign.md_algs.contains(md5)
    end


    def test_add_signer_to_signedAndEnveloped_with_new_algo_should_add_that_algo_to_the_algo_list
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      
      # YES, these numbers are correct. Don't change them. They are OpenSSL internal NIDs
      md5 = AlgorithmIdentifier.new(ASN1Registry.nid2obj(4))
      md4 = AlgorithmIdentifier.new(ASN1Registry.nid2obj(5))

      si = create_signer_info_with_algo(md5)
      p7.add_signer(si)

      assert_equal md5, p7.get_signed_and_enveloped.md_algs.iterator.next
      assert_equal 1, p7.get_signed_and_enveloped.md_algs.size

      si = create_signer_info_with_algo(md5)
      p7.add_signer(si)

      assert_equal md5, p7.get_signed_and_enveloped.md_algs.iterator.next
      assert_equal 1, p7.get_signed_and_enveloped.md_algs.size

      si = create_signer_info_with_algo(md4)
      p7.add_signer(si)

      assert_equal 2, p7.get_signed_and_enveloped.md_algs.size
      assert p7.get_signed_and_enveloped.md_algs.contains(md4)
      assert p7.get_signed_and_enveloped.md_algs.contains(md5)
    end
    
    def test_set_content_on_data_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.setContent(PKCS7.new)
      end
    end

    def test_set_content_on_enveloped_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_raises NativeException do 
        p7.setContent(PKCS7.new)
      end
    end

    def test_set_content_on_signedAndEnveloped_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      assert_raises NativeException do 
        p7.setContent(PKCS7.new)
      end
    end

    def test_set_content_on_encrypted_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.setContent(PKCS7.new)
      end
    end

    def test_set_content_on_signed_sets_the_content
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      p7new = PKCS7.new
      p7.setContent(p7new)
      
      assert_equal p7new, p7.get_sign.contents
    end

    def test_set_content_on_digest_sets_the_content
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      p7new = PKCS7.new
      p7.setContent(p7new)
      
      assert_equal p7new, p7.get_digest.contents
    end
    
    def test_get_signer_info_on_digest_returns_null
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert_nil p7.signer_info
    end

    def test_get_signer_info_on_data_returns_null
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_nil p7.signer_info
    end

    def test_get_signer_info_on_encrypted_returns_null
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_nil p7.signer_info
    end

    def test_get_signer_info_on_enveloped_returns_null
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_nil p7.signer_info
    end

    def test_get_signer_info_on_signed_returns_signer_info
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      assert_equal p7.get_sign.signer_info.object_id, p7.signer_info.object_id
    end

    def test_get_signer_info_on_signedAndEnveloped_returns_signer_info
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      assert_equal p7.get_signed_and_enveloped.signer_info.object_id, p7.signer_info.object_id
    end
    
    def test_content_new_on_data_raises_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.content_new(ASN1Registry::NID_pkcs7_data)
      end
    end

    def test_content_new_on_encrypted_raises_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.content_new(ASN1Registry::NID_pkcs7_data)
      end
    end

    def test_content_new_on_enveloped_raises_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_raises NativeException do 
        p7.content_new(ASN1Registry::NID_pkcs7_data)
      end
    end

    def test_content_new_on_signedAndEnveloped_raises_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      assert_raises NativeException do 
        p7.content_new(ASN1Registry::NID_pkcs7_data)
      end
    end
    
    def test_content_new_on_digest_creates_new_content
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      p7.content_new(ASN1Registry::NID_pkcs7_signedAndEnveloped)
      assert p7.get_digest.contents.signed_and_enveloped?
      
      p7.content_new(ASN1Registry::NID_pkcs7_encrypted)
      assert p7.get_digest.contents.encrypted?
    end

    def test_content_new_on_signed_creates_new_content
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      p7.content_new(ASN1Registry::NID_pkcs7_signedAndEnveloped)
      assert p7.get_sign.contents.signed_and_enveloped?
      
      p7.content_new(ASN1Registry::NID_pkcs7_encrypted)
      assert p7.get_sign.contents.encrypted?
    end

    
    def test_add_certificate_on_data_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.add_certificate(X509Cert)
      end
    end

    def test_add_certificate_on_enveloped_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_raises NativeException do 
        p7.add_certificate(X509Cert)
      end
    end

    def test_add_certificate_on_encrypted_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.add_certificate(X509Cert)
      end
    end

    def test_add_certificate_on_digest_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert_raises NativeException do 
        p7.add_certificate(X509Cert)
      end
    end

    def test_add_certificate_on_signed_adds_the_certificate
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      p7.add_certificate(X509Cert)
      assert_equal 1, p7.get_sign.cert.size
      assert_equal X509Cert, p7.get_sign.cert.iterator.next
    end

    def test_add_certificate_on_signedAndEnveloped_adds_the_certificate
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      p7.add_certificate(X509Cert)
      assert_equal 1, p7.get_signed_and_enveloped.cert.size
      assert_equal X509Cert, p7.get_signed_and_enveloped.cert.get(0)
    end

    def test_add_crl_on_data_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_data
      assert_raises NativeException do 
        p7.add_crl(X509CRL)
      end
    end

    def test_add_crl_on_enveloped_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_enveloped
      assert_raises NativeException do 
        p7.add_crl(X509CRL)
      end
    end

    def test_add_crl_on_encrypted_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_encrypted
      assert_raises NativeException do 
        p7.add_crl(X509CRL)
      end
    end

    def test_add_crl_on_digest_throws_exception
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_digest
      assert_raises NativeException do 
        p7.add_crl(X509CRL)
      end
    end

    def test_add_crl_on_signed_adds_the_crl
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signed
      p7.add_crl(X509CRL)
      assert_equal 1, p7.get_sign.crl.size
      assert_equal X509CRL, p7.get_sign.crl.iterator.next
    end

    def test_add_crl_on_signedAndEnveloped_adds_the_crl
      p7 = PKCS7.new
      p7.type = ASN1Registry::NID_pkcs7_signedAndEnveloped
      p7.add_crl(X509CRL)
      assert_equal 1, p7.get_signed_and_enveloped.crl.size
      assert_equal X509CRL, p7.get_signed_and_enveloped.crl.get(0)
    end
    
    EXISTING_PKCS7_DEF = "0\202\002 \006\t*\206H\206\367\r\001\a\003\240\202\002\0210\202\002\r\002\001\0001\202\001\2700\201\331\002\001\0000B0=1\0230\021\006\n\t\222&\211\223\362,d\001\031\026\003org1\0310\027\006\n\t\222&\211\223\362,d\001\031\026\truby-lang1\v0\t\006\003U\004\003\f\002CA\002\001\0020\r\006\t*\206H\206\367\r\001\001\001\005\000\004\201\200\213kF\330\030\362\237\363$\311\351\207\271+_\310sr\344\233N\200\233)\272\226\343\003\224OOf\372 \r\301{\206\367\241\270\006\240\254\3179F\232\231Q\232\225\347\373\233\032\375\360\035o\371\275p\306\v5Z)\263\037\302|\307\300\327\a\375\023G'Ax\313\346\261\254\227K\026\364\242\337\367\362rk\276\023\217m\326\343F\366I1\263\nLuNf\234\203\261\300\030\232Q\277\231\f0\030\001\332\021\0030\201\331\002\001\0000B0=1\0230\021\006\n\t\222&\211\223\362,d\001\031\026\003org1\0310\027\006\n\t\222&\211\223\362,d\001\031\026\truby-lang1\v0\t\006\003U\004\003\f\002CA\002\001\0030\r\006\t*\206H\206\367\r\001\001\001\005\000\004\201\200\215\223\3428\2440]\0278\016\230,\315\023Tg\325`\376~\353\304\020\243N{\326H\003\005\361q\224OI\310\2324-\341?\355&r\215\233\361\245jF\255R\271\203D\304v\325\265\243\321$\bSh\031i\eS\240\227\362\221\364\232\035\202\f?x\031\223D\004ZHD\355'g\243\037\236mJ\323\210\347\274m\324-\351\332\353#A\273\002\"h\aM\202\347\236\265\aI$@\240bt=<\212\2370L\006\t*\206H\206\367\r\001\a\0010\035\006\t`\206H\001e\003\004\001\002\004\020L?\325\372\\\360\366\372\237|W\333nnI\255\200 \253\234\252\263\006\335\037\320\350{s\352r\337\304\305\216\223k\003\376f\027_\201\035#*\002yM\334"

    EXISTING_PKCS7_1 = PKCS7::from_asn1(ASN1InputStream.new(EXISTING_PKCS7_DEF.to_java_bytes).read_object)
    
    def test_encrypt_integration_test
      certs = [X509Cert]
      cipher = Cipher.get_instance("AES", BCP.new)
      data = "aaaaa\nbbbbb\nccccc\n".to_java_bytes
      PKCS7::encrypt(certs, data, cipher, PKCS7::BINARY)
#       puts
#       puts PKCS7::encrypt(certs, data, cipher, PKCS7::BINARY)
#       puts 
#       puts EXISTING_PKCS7_1
    end
    
    EXISTING_PKCS7_PEM = <<PKCS7STR
-----BEGIN PKCS7-----
MIICIAYJKoZIhvcNAQcDoIICETCCAg0CAQAxggG4MIHZAgEAMEIwPTETMBEGCgmS
JomT8ixkARkWA29yZzEZMBcGCgmSJomT8ixkARkWCXJ1YnktbGFuZzELMAkGA1UE
AwwCQ0ECAQIwDQYJKoZIhvcNAQEBBQAEgYCPGMV4KS/8amYA2xeIjj9qLseJf7dl
BtSDp+YAU3y1JnW7XufBCKxYw7eCuhWWA/mrxijr+wdsFDvSalM6nPX2P2NiVMWP
a7mzErZ4WrzkKIuGczYPYPJetwBYuhik3ya4ygYygoYssVRAITOSsEKpfqHAPmI+
AUJkqmCdGpQu9TCB2QIBADBCMD0xEzARBgoJkiaJk/IsZAEZFgNvcmcxGTAXBgoJ
kiaJk/IsZAEZFglydWJ5LWxhbmcxCzAJBgNVBAMMAkNBAgEDMA0GCSqGSIb3DQEB
AQUABIGAPaBX0KM3S+2jcrQrncu1jrvm1PUXlUvMfFIG2oBfPkMhiqCBvkOct1Ve
ws1hxvGtsqyjAUn02Yx1+gQJhTN4JZZHNqkfi0TwN32nlwLxclKcrbF9bvtMiVHx
V3LrSygblxxJsBf8reoV4yTJRa3w98bEoDhjUwjfy5xTml2cAn4wTAYJKoZIhvcN
AQcBMB0GCWCGSAFlAwQBAgQQath+2gUo4ntkKl8FO1LLhoAg58j0Jn/OfWG3rNRH
kTtUQfnBFk/UGbTZgExHILaGz8Y=
-----END PKCS7-----
PKCS7STR
    
    PKCS7_PEM_CONTENTS = "\347\310\364&\177\316}a\267\254\324G\221;TA\371\301\026O\324\031\264\331\200LG \266\206\317\306" 

    PKCS7_PEM_FIRST_KEY = "\217\030\305x)/\374jf\000\333\027\210\216?j.\307\211\177\267e\006\324\203\247\346\000S|\265&u\273^\347\301\b\254X\303\267\202\272\025\226\003\371\253\306(\353\373\al\024;\322jS:\234\365\366?cbT\305\217k\271\263\022\266xZ\274\344(\213\206s6\017`\362^\267\000X\272\030\244\337&\270\312\0062\202\206,\261T@!3\222\260B\251~\241\300>b>\001Bd\252`\235\032\224.\365"

    PKCS7_PEM_SECOND_KEY = "=\240W\320\2437K\355\243r\264+\235\313\265\216\273\346\324\365\027\225K\314|R\006\332\200_>C!\212\240\201\276C\234\267U^\302\315a\306\361\255\262\254\243\001I\364\331\214u\372\004\t\2053x%\226G6\251\037\213D\3607}\247\227\002\361rR\234\255\261}n\373L\211Q\361Wr\353K(\e\227\034I\260\027\374\255\352\025\343$\311E\255\360\367\306\304\2408cS\b\337\313\234S\232]\234\002~"
    
    def test_PEM_read_pkcs7_bio
      bio = BIO::mem_buf(EXISTING_PKCS7_PEM.to_java_bytes)
      p7 = PKCS7.read_pem(bio)

      assert_equal ASN1Registry::NID_pkcs7_enveloped, p7.type
      env = p7.get_enveloped
      assert_equal 0, env.version
      enc_data = env.enc_data
      assert_equal ASN1Registry::NID_pkcs7_data, enc_data.content_type
      assert_equal ASN1Registry::NID_aes_128_cbc, ASN1Registry::obj2nid(enc_data.algorithm.get_object_id)
      assert_equal PKCS7_PEM_CONTENTS, String.from_java_bytes(enc_data.enc_data.octets)
      
      ris = env.recipient_info
      assert_equal 2, ris.size
      
      first = second = nil
      tmp = ris.iterator.next

      if tmp.issuer_and_serial.certificate_serial_number.value == 2
        first = tmp
        iter = ris.iterator
        iter.next
        second = iter.next
      else 
        second = tmp
        iter = ris.iterator
        iter.next
        first = iter.next
      end
      
      assert_equal 0, first.version
      assert_equal 0, second.version
      
      assert_equal "DC=org,DC=ruby-lang,CN=CA", first.issuer_and_serial.name.to_s
      assert_equal "DC=org,DC=ruby-lang,CN=CA", second.issuer_and_serial.name.to_s
      
      assert_equal ASN1Registry::NID_rsaEncryption, ASN1Registry::obj2nid(first.key_enc_algor.get_object_id)
      assert_equal ASN1Registry::NID_rsaEncryption, ASN1Registry::obj2nid(second.key_enc_algor.get_object_id)

      assert_equal PKCS7_PEM_FIRST_KEY, String.from_java_bytes(first.enc_key.octets)
      assert_equal PKCS7_PEM_SECOND_KEY, String.from_java_bytes(second.enc_key.octets)
    end
  end
end


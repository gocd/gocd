module PKCS7Test
  class TestJavaSMIME < Test::Unit::TestCase
    def test_read_pkcs7_should_raise_error_when_parsing_headers_fails
      bio = BIO.new
      mime = Mime.new
      mime.stubs(:parseHeaders).returns(nil)

      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_MIME_PARSE_ERROR, e.cause.get_reason
      end
    end

    def test_read_pkcs7_should_raise_error_when_content_type_is_not_there
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(nil)

      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_NO_CONTENT_TYPE, e.cause.get_reason
      end


      
      
      mime = Mime.new
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(MimeHeader.new("content-type", nil))

      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_NO_CONTENT_TYPE, e.cause.get_reason
      end
    end
    
    def test_read_pkcs7_should_set_the_second_arguments_contents_to_null_if_its_there
      mime = Mime.new
      mime.stubs(:parseHeaders).raises("getOutOfJailForFree")
      
      bio2 = BIO.new
      arr = [bio2].to_java BIO
      
      begin
        SMIME.new(mime).readPKCS7(nil, arr)
      rescue
      end

      assert_nil arr[0]


      arr = [bio2, bio2].to_java BIO
      begin
        SMIME.new(mime).readPKCS7(nil, arr)
      rescue
      end

      assert_nil arr[0]
      assert_equal bio2, arr[1]
    end
    
    def test_read_pkcs7_should_call_methods_on_mime
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(MimeHeader.new("content-type", "application/pkcs7-mime"))

      begin
        SMIME.new(mime).readPKCS7(bio, nil)
      rescue java.lang.UnsupportedOperationException
        # This error is expected, since the bio used is not a real one
      end
    end

    def test_read_pkcs7_throws_correct_exception_if_wrong_content_type
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(MimeHeader.new("content-type", "foo"))

      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_INVALID_MIME_TYPE, e.cause.get_reason
        assert_equal "type: foo", e.cause.error_data
      end
    end
    
    def test_read_pkcs7_with_multipart_should_fail_if_no_boundary_found
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      hdr = MimeHeader.new("content-type", "multipart/signed")
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(hdr)

      mime.expects(:findParam).with(hdr, "boundary").returns(nil)
      
      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_NO_MULTIPART_BOUNDARY, e.cause.get_reason
      end
    end
    
    def test_read_pkcs7_with_multipart_should_fail_if_null_boundary_value
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      hdr = MimeHeader.new("content-type", "multipart/signed")
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(hdr)

      mime.expects(:findParam).with(hdr, "boundary").returns(MimeParam.new("boundary", nil))
      
      begin
        SMIME.new(mime).readPKCS7(bio, nil)
        assert false
      rescue PKCS7Exception => e
        assert_equal PKCS7::F_SMIME_READ_PKCS7, e.cause.get_method
        assert_equal PKCS7::R_NO_MULTIPART_BOUNDARY, e.cause.get_reason
      end
    end

    # TODO: redo this test to be an integration test
    def _test_read_pkcs7_happy_path_without_multipart
      bio = BIO.new
      mime = Mime.new

      headers = ArrayList.new
      mime.expects(:parseHeaders).with(bio).returns(headers)
      mime.expects(:findHeader).with(headers, "content-type").returns(MimeHeader.new("content-type", "application/pkcs7-mime"))

      SMIME.new(mime).readPKCS7(bio, nil)
    end
    
    def test_read_pkcs7_happy_path_multipart
      bio = BIO::from_string(MultipartSignedString)
      mime = Mime::DEFAULT
      p7 = SMIME.new(mime).readPKCS7(bio, nil)
    end

    def test_read_pkcs7_happy_path_without_multipart_enveloped
      bio = BIO::from_string(MimeEnvelopedString)
      mime = Mime::DEFAULT
      p7 = SMIME.new(mime).readPKCS7(bio, nil)
    end

    def test_read_pkcs7_happy_path_without_multipart_signed
      bio = BIO::from_string(MimeSignedString)
      mime = Mime::DEFAULT
      p7 = SMIME.new(mime).readPKCS7(bio, nil)
    end
  end
end

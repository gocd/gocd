module PKCS7Test
  class TestJavaAttribute < Test::Unit::TestCase
    def test_attributes
      val = ASN1::OctetString.new("foo".to_java_bytes)
      val2 = ASN1::OctetString.new("bar".to_java_bytes)
      attr = Attribute.create(123, 444, val)
      assert_raise NoMethodError do 
        attr.type = 12
      end
      assert_raise NoMethodError do 
        attr.value = val2
      end

      assert_equal 123, attr.type
      assert_equal val, attr.set.get(0)

      attr2 = Attribute.create(123, 444, val)
      
      assert_equal attr, attr2
      
      assert_not_equal Attribute.create(124, 444, val), attr
      assert_not_equal Attribute.create(123, 444, val2), attr
    end
  end
end

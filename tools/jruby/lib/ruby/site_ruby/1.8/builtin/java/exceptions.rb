class Java::JavaLang::Exception
  def self.===(rhs)
    (NativeException == rhs.class) && (java_class.assignable_from?(rhs.cause.java_class))
  end
end

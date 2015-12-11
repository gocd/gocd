class Object
  def to_bool
    if self.blank? || self =~ /^false$/i
      return false
    end

    !!(self =~ /^true$/i)
  end
end

class TrueClass
  def to_bool
    self
  end
end

class FalseClass
  def to_bool
    self
  end
end

module java::lang::Runnable
  def to_proc
    proc { self.run }
  end
end


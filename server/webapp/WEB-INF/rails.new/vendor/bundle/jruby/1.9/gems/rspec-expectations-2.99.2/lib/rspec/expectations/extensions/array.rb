# @private
class Array
  unless public_instance_methods.map {|m| m.to_s}.include?('none?')
    # Supports +none?+ on early patch levels of Ruby 1.8.6
    def none?(&block)
      !any?(&block)
    end
  end
end

module Requirejs
  module Rails
    class ViewProxy
      include ActionView::Context

      if ::Rails::VERSION::MAJOR >= 4
        include ActionView::Helpers::AssetUrlHelper
        include ActionView::Helpers::TagHelper
      else
        include ActionView::Helpers::AssetTagHelper
      end
    end
  end
end

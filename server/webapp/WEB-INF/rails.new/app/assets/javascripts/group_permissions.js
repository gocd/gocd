GroupPermissions = function() {

    var onState, offState, disabledState;

    function setHiddenField(element) {
        var checkBox = jQuery(element);
        var hiddenField = checkBox.siblings("input[type='hidden']");
        if (checkBox.is(":checked")) {
            hiddenField.val(onState)
        } else {
            hiddenField.val(offState)
        }
    }

    init.prototype.bindFromHiddenField = function () {
        jQuery("input:[type='checkbox']").each(function() {
            var checkBox = jQuery(this);
            var hiddenField = checkBox.siblings("input[type='hidden']");
            if (hiddenField.val() == onState) {
                checkBox.attr('checked', true);
            } else {
                if (hiddenField.val() == offState) {
                    checkBox.attr('checked', false);
                } else {
                    if (hiddenField.val() == disabledState) {
                        checkBox.attr('checked', true);
                        checkBox.attr('disabled', "disabled");
                    }
                }
            }
        });
    }

    init.prototype.bindHandlers = function() {
        jQuery("input:[type='checkbox']").click(function() {
            setHiddenField(this);
        });

        jQuery("input:[type='checkbox'][name='admin']").click(function() {
               var adminCheckBox = jQuery(this);
               jQuery(this).parent().siblings("td").each(function() {
                   var checkBox = jQuery(this).find("input[type='checkbox']");
                   var viewCheckBox = jQuery(this).find("input[type='checkbox'][name='view']");
                   var operateCheckBox = jQuery(this).find("input[type='checkbox'][name='operate']");
                   if (checkBox.length == 1) {
                       checkBox = jQuery(checkBox);
                       if (adminCheckBox.is(":checked")) {
                           checkBox.attr('checked', true);
                           checkBox.attr('disabled', "disabled");
                       } else {
                           jQuery(operateCheckBox).attr('checked', false);
                           jQuery(viewCheckBox).attr('checked', true);
                           checkBox.removeAttr('disabled');
                       }
                   }
               });
           });

    }

    function init(on, off, disabled) {
        onState = on;
        offState = off;
        disabledState = disabled;
    }

    return init;
}();
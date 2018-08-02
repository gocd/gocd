package com.thoughtworks.go.apiv1.adminsconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminsRepresenter {
    public static void toJSON(OutputWriter jsonWriter, AdminsConfig admin) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Admins.DOC)
                        .addLink("self", Routes.Admins.BASE));
        jsonWriter.addChildList("roles", rolesAsString(admin.getRoles()));
        jsonWriter.addChildList("users", userAsString(admin.getUsers()));
        if (admin.hasErrors()) {
            jsonWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.singletonMap("SystemAdmin", "system_admin"))
                    .toJSON(errorWriter, admin));
        }
    }

    public static AdminsConfig fromJSON(JsonReader jsonReader) {
        AdminsConfig adminsConfig = new AdminsConfig();

        jsonReader.readArrayIfPresent("users", users -> {
            users.forEach(user -> adminsConfig.add(new AdminUser(new CaseInsensitiveString(user.getAsString()))));
        });

        jsonReader.readArrayIfPresent("roles", roles -> {
            roles.forEach(role -> adminsConfig.add(new AdminRole(new CaseInsensitiveString(role.getAsString()))));
        });

        return adminsConfig;
    }

    private static List<String> rolesAsString(List<AdminRole> roles) {
        return roles.stream().map(role -> role.getName().toString()).collect(Collectors.toList());
    }

    private static List<String> userAsString(List<AdminUser> users) {
        return users.stream().map(user -> user.getName().toString()).collect(Collectors.toList());
    }
}

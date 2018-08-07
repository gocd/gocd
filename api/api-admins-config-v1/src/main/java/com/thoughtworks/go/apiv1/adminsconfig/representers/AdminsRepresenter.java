package com.thoughtworks.go.apiv1.adminsconfig.representers;

import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.spark.Routes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminsRepresenter {
    public static void toJSON(OutputWriter jsonWriter, AdminsConfig admin) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Admins.DOC)
                        .addLink("self", Routes.Admins.BASE));


        if (admin instanceof AdminsConfig) {
            jsonWriter.addChildList("roles", rolesAsString(admin.getRoles()));
            jsonWriter.addChildList("users" , userAsString(admin.getUsers()));
        }
    }

    public static AdminsConfig fromJSON(JsonReader jsonReader) {
        List<Admin> admins =  new ArrayList();
        jsonReader.readArrayIfPresent("users", users -> {
            users.forEach(user -> admins.add(new AdminUser(new CaseInsensitiveString(user.getAsString()))));
        });
        jsonReader.readArrayIfPresent("roles", roles -> {
            roles.forEach(role -> admins.add(new AdminRole(new CaseInsensitiveString(role.getAsString()))));
        });
        AdminsConfig adminsConfig = new AdminsConfig(admins);

        return adminsConfig;
    }

    private static List<String> rolesAsString(List<AdminRole> roles) {
        return roles.stream().map(role -> role.getName().toString()).collect(Collectors.toList());
    }

    private static List<String> userAsString(List<AdminUser> users) {
        return users.stream().map(user -> user.getName().toString()).collect(Collectors.toList());
    }
}

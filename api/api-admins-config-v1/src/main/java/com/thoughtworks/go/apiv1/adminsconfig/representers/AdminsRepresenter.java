package com.thoughtworks.go.apiv1.adminsconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.spark.Routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminsRepresenter {
    public static void toJSON(OutputWriter jsonWriter, AdminsConfig admin) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Admins.DOC)
                        .addLink("self", Routes.Admins.BASE));


        if (admin instanceof AdminsConfig) {
            jsonWriter.addChildList("roles", rolesAsString(admin.getRoles()));
            jsonWriter.addChildList("users", userAsString(admin.getUsers()));
        }
    }

    public static AdminsConfig fromJSON(JsonReader jsonReader, String method, AdminsConfig config) {
        AdminsConfig adminsConfig;
        if (method.equalsIgnoreCase("PUT")) {
            adminsConfig = getAdminsForPutRequest(jsonReader);
        } else {
            adminsConfig = getAdminsForPatchRequest(jsonReader,config);
        }

        return adminsConfig;
    }

    private static AdminsConfig getAdminsForPatchRequest(JsonReader jsonReader, AdminsConfig config) {
        List<Admin> admins = new ArrayList();
        List<AdminUser> adminsUsersAdd = new ArrayList();
        List<AdminUser> adminsUsersRemove = new ArrayList();
        List<AdminRole> rolesAdd = new ArrayList();
        List<AdminRole> rolesRemove = new ArrayList();

        Optional<JsonReader> usersArray = jsonReader.optJsonObject("users");
        usersArray.get().readArrayIfPresent("add", users -> {
            users.forEach(user -> adminsUsersAdd.add(new AdminUser(new CaseInsensitiveString(user.getAsString()))));
        });
        usersArray.get().readArrayIfPresent("remove", users -> {
            users.forEach(user -> adminsUsersRemove.add(new AdminUser(new CaseInsensitiveString(user.getAsString()))));
        });

        Optional<JsonReader> rolesArray = jsonReader.optJsonObject("roles");
        rolesArray.get().readArrayIfPresent("add", roles -> {
            roles.forEach(role -> rolesAdd.add(new AdminRole(new CaseInsensitiveString(role.getAsString()))));
        });
        rolesArray.get().readArrayIfPresent("remove", roles -> {
            roles.forEach(role -> rolesRemove.add(new AdminRole(new CaseInsensitiveString(role.getAsString()))));
        });
        for(AdminUser user : config.getUsers()){
            if(!adminsUsersRemove.contains(user)){
                admins.add(user);
            }
        }
        for(AdminUser user : adminsUsersAdd){
            if(!admins.contains(user)){
                admins.add(user);
            }
        }
        for(AdminRole role : config.getRoles()){
            if(!rolesRemove.contains(role)){
                admins.add(role);
            }
        }
        for(AdminRole role : rolesAdd){
            if(!admins.contains(role)){
                admins.add(role);
            }
        }
        AdminsConfig adminsConfig = new AdminsConfig(admins);
        return adminsConfig;
    }

    private static AdminsConfig getAdminsForPutRequest(JsonReader jsonReader) {
        List<Admin> admins = new ArrayList();

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

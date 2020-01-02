/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.apiv3.users;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv3.users.model.UserToRepresent;
import com.thoughtworks.go.apiv3.users.representers.BulkDeletionFailureResultRepresenter;
import com.thoughtworks.go.apiv3.users.representers.UserRepresenter;
import com.thoughtworks.go.apiv3.users.representers.UsersRepresenter;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.RoleConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.TriState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityAlreadyExists;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseRenameOfEntityIsNotSupported;
import static spark.Spark.*;

@Component
public class UsersControllerV3 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private UserService userService;
    private SecurityService securityService;
    private RoleConfigService roleConfigService;

    @Autowired
    public UsersControllerV3(ApiAuthenticationHelper apiAuthenticationHelper,
                             UserService userService,
                             SecurityService securityService,
                             RoleConfigService roleConfigService) {
        super(ApiVersion.v3);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.userService = userService;
        this.securityService = securityService;
        this.roleConfigService = roleConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Users.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", this.mimeType, this::index);
            post("", this.mimeType, this::create);
            delete("", this.mimeType, this::bulkDelete);

            get(Routes.Users.USER_NAME, this.mimeType, this::show);
            patch(Routes.Users.USER_NAME, this.mimeType, this::patchUser);
            delete(Routes.Users.USER_NAME, this.mimeType, this::deleteUser);
            patch(Routes.Users.USER_STATE, this.mimeType, this::bulkUpdateUsersState);
        });
    }

    public String index(Request req, Response res) throws Exception {
        Collection<User> allUsers = userService.allUsers();
        HashMap<Username, RolesConfig> usersToRolesMap = roleConfigService.getRolesForUser(allUsers.stream().map(User::getUsername).collect(Collectors.toCollection(ArrayList::new)));
        List<UserToRepresent> users = allUsers.stream().map((User user) -> getUserToRepresent(user, usersToRolesMap)).collect(Collectors.toList());
        return writerForTopLevelObject(req, res, writer -> UsersRepresenter.toJSON(writer, users));
    }

    public String show(Request req, Response res) throws Exception {
        String loginName = req.params("login_name");
        User user = userService.findUserByName(loginName);

        if (user.equals(new NullUser())) {
            throw new RecordNotFoundException(EntityType.User, loginName);
        }

        UserToRepresent toRepresent = getUserToRepresent(user, roleConfigService.getRolesForUser(Collections.singletonList(user.getUsername())));
        return writerForTopLevelObject(req, res, writer -> UserRepresenter.toJSON(writer, toRepresent));
    }

    public String create(Request req, Response res) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        User user = buildUserEntityFromRequestBody(req, false);

        if (!userService.findUserByName(user.getName()).equals(new NullUser())) {
            throw haltBecauseEntityAlreadyExists(jsonWriter(user, new HashMap<>()), "User", user.getName());
        }

        return saveUserAndRenderResult(req, res, result, user, user, user.getName());
    }

    public String patchUser(Request req, Response res) throws Exception {
        String username = req.params("login_name");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        User userFromRequest = buildUserEntityFromRequestBody(req, true);

        User existingUser = userService.findUserByName(username);
        if (existingUser.equals(new NullUser())) {
            throw new RecordNotFoundException(EntityType.User, username);
        }

        if (isRenameAttempted(req, userFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("User");
        }

        return saveUserAndRenderResult(req, res, result, existingUser, userFromRequest, username);
    }

    public String deleteUser(Request req, Response res) throws Exception {
        String username = req.params("login_name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.deleteUser(username, currentUsernameString(), result);
        return renderHTTPOperationResult(result, req, res);
    }

    public String bulkUpdateUsersState(Request req, Response res) throws Exception {
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        List<String> users = jsonReader.readStringArrayIfPresent("users").orElse(Collections.emptyList());
        boolean shouldEnable = jsonReader.readJsonObject("operations").getBoolean("enable");

        userService.bulkEnableDisableUsers(users, shouldEnable, result);

        if (!result.isSuccessful()) {
            res.status(result.httpCode());
            return writerForTopLevelObject(req, res, outputWriter -> BulkDeletionFailureResultRepresenter.toJSON(outputWriter, result));
        }

        return renderHTTPOperationResult(result, req, res);
    }

    public String bulkDelete(Request req, Response res) throws Exception {
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        List<String> users = jsonReader.readStringArrayIfPresent("users").orElse(Collections.emptyList());

        userService.deleteUsers(users, currentUsernameString(), result);

        if (!result.isSuccessful()) {
            res.status(result.httpCode());
            return writerForTopLevelObject(req, res, outputWriter -> BulkDeletionFailureResultRepresenter.toJSON(outputWriter, result));
        }

        return renderHTTPOperationResult(result, req, res);
    }

    private String saveUserAndRenderResult(Request req, Response res, HttpLocalizedOperationResult result, User userToOperate, User userFromRequest, String username) throws IOException {
        userService.save(userToOperate, TriState.from(userFromRequest.isEnabled()), TriState.from(userFromRequest.isEmailMe()), userFromRequest.getEmail(), userFromRequest.getMatcher(), result);
        boolean isSaved = result.isSuccessful();
        if (isSaved) {
            return writerForTopLevelObject(req, res, writer -> UserRepresenter.toJSON(writer, getUserToRepresent(userService.findUserByName(username), roleConfigService.getRolesForUser(Collections.singletonList(new Username(username))))));
        } else {
            return renderHTTPOperationResult(result, req, res);
        }
    }

    private User buildUserEntityFromRequestBody(Request req, boolean isLoginNameOptional) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return UserRepresenter.fromJSON(jsonReader, isLoginNameOptional);
    }

    private boolean isRenameAttempted(Request req, User user) {
        if (user.getName() == null) {
            return false;
        }

        return !req.params("login_name").equalsIgnoreCase(user.getName());
    }

    private UserToRepresent getUserToRepresent(User user, HashMap<Username, RolesConfig> userToRolesMap) {
        return UserToRepresent.from(user, securityService.isUserAdmin(user.getUsername()), userToRolesMap.get(user.getUsername()));
    }

    public Consumer<OutputWriter> jsonWriter(User user, HashMap<Username, RolesConfig> userToRolesMap) {
        return writer -> UserRepresenter.toJSON(writer, getUserToRepresent(user, userToRolesMap));
    }
}

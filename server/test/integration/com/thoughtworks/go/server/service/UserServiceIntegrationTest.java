/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.UserRoleMatcherMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.oauth.OauthAuthorization;
import com.thoughtworks.go.server.domain.oauth.OauthClient;
import com.thoughtworks.go.server.domain.oauth.OauthToken;
import com.thoughtworks.go.server.persistence.OauthRepository;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TriState;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-rest-servlet.xml"
})
public class UserServiceIntegrationTest {
    @Autowired private UserDao userDao;
    @Autowired private UserService userService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private Localizer localizer;
    @Autowired private GoCache goCache;
    @Autowired private OauthRepository repo;

    private HibernateTemplate template;

    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper(ConfigFileFixture.XML_WITH_ENTERPRISE_LICENSE_FOR_TWO_USERS);
    private Username ROOT = new Username(new CaseInsensitiveString("root"));


    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingCruiseConfigDao(goConfigDao);
        template = repo.getHibernateTemplate();
        goCache.clear();
    }

    @After
    public void teardown() throws Exception {
        configFileHelper.onTearDown();
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Test
    public void shouldSaveUser() throws ValidationException {
        User user = new User("name1", new String[]{"regx"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        User savedUser = userDao.findUser("name1");
        assertThat(savedUser, is(user));
        assertThat(userService.load(savedUser.getId()), is(user));
    }

    @Test
    public void shouldUpdateWhenUserAlreadyExist() throws ValidationException {
        addUser(new User("name1", new String[]{"regx"}, "user@mail.com", true));
        User updatedUser = userService.findUserByName("name1");
        updatedUser.setEmail("user2@mail.com");
        updatedUser.setMatcher("regx2");

        userService.saveOrUpdate(updatedUser);

        User user = userDao.findUser("name1");
        assertThat(user, is(updatedUser));
        assertThat(user.getId(), is(updatedUser.getId()));
    }

    @Test
    public void addUserIfDoesNotExist_shouldAddUserIfDoesNotExist() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(getAuthUser("new_user"), "credentials", new GrantedAuthority[]{GoAuthority.ROLE_USER.asAuthority()});
        assertThat(userDao.findUser("new_user"), isANullUser());
        userService.addUserIfDoesNotExist(UserHelper.getUserName(auth));
        User loadedUser = userDao.findUser("new_user");
        assertThat(loadedUser, is(new User("new_user", "new_user", "")));
        assertThat(loadedUser, not(isANullUser()));
    }

    @Test
    public void addUserIfDoesNotExist_shouldNotAddUserIfExists() throws Exception {
        User user = new User("old_user");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(getAuthUser("old_user"), "credentials", new GrantedAuthority[]{GoAuthority.ROLE_USER.asAuthority()});
        addUser(user);
        userService.addUserIfDoesNotExist(UserHelper.getUserName(auth));
    }

    @Test
    public void addUserIfDoesNotExist_shouldNotAddUserIfAnonymous() throws Exception {
        userService.addUserIfDoesNotExist(Username.ANONYMOUS);
        assertThat(userDao.findUser(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername())), isANullUser());
        assertThat(userDao.findUser(Username.ANONYMOUS.getDisplayName()), isANullUser());
    }

    private org.springframework.security.userdetails.User getAuthUser(String userName) {
        return new org.springframework.security.userdetails.User(userName, "pass", true, true, true, true, new GrantedAuthority[]{GoAuthority.ROLE_USER.asAuthority()});
    }

    @Test
    public void shouldvalidateUser() throws Exception {
        try {
            userService.validate(new User("username", new String[]{"committer"}, "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldNotSaveUserWhenValidationFailed() throws Exception {
        try {
            userService.saveOrUpdate(new User("username", new String[]{"committer"}, "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException e) {
            assertThat(userService.findUserByName("username"), is(instanceOf(NullUser.class)));
        }
    }

    @Test
    public void shouldAddNotificationFilterForExistingUser() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName("jez");
        assertThat(user.getNotificationFilters().size(), is(1));
        assertThat(user.getNotificationFilters(), hasItem(filter));
    }

    @Test
    public void shouldRemoveNotificationFilterForUser() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUser(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName(user.getName());
        assertThat(user.getNotificationFilters().size(), is(1));
        long deletedNotificationId = user.getNotificationFilters().get(0).getId();
        userService.removeNotificationFilter(user.getId(), deletedNotificationId);
        assertThat(userService.findUserByName(user.getName()).getNotificationFilters().size(), is(0));
    }

    @Test
    public void shouldNotAddDuplicateNotificationFilter() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Fixed, false);
        addUserWithNotificationFilter(user, filter);
        user = userDao.findUser(user.getName());

        try {
            userService.addNotificationFilter(user.getId(), filter);
            fail("shouldNotAddDuplicateNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("already exist"));
        }
    }

    @Test
    public void shouldNotAddUnnecessaryNotificationFilter() throws ValidationException {
        User user = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        userService.addNotificationFilter(user.getId(), new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));

        try {
            userService.addNotificationFilter(user.getId(), new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
            fail("shouldNotAddUnnecessaryNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("already exist"));
        }
    }

    @Test
    public void shouldLoadUsersWhoSubscribedNotificationOnStage() {
        User tom = new User("tom", new String[]{"tom"}, "tom@mail.com", true);
        addUserWithNotificationFilter(tom, new NotificationFilter("p1", "s1", StageEvent.Breaks, true));

        User jez = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUserWithNotificationFilter(jez,
                new NotificationFilter("cruise", "dev", StageEvent.All, false),
                new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("cruise", "dev"));
        assertThat(users.size(), is(1));
        assertThat(users.get(0), is(jez));
        assertThat(users.get(0).getNotificationFilters().size(), is(2));
    }

    @Test
    public void shouldLoadAuthorizedUser() throws Exception {
        givingJezViewPermissionToMingle();

        User tom = new User("tom", new String[]{"tom"}, "tom@mail.com", true);
        User jez = new User("jez", new String[]{"jez"}, "user@mail.com", true);
        addUserWithNotificationFilter(jez,
                new NotificationFilter("mingle", "dev", StageEvent.All, false));
        addUserWithNotificationFilter(tom,
                new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("mingle", "dev"));
        assertThat(users.size(), is(1));
        assertThat(users.get(0), is(jez));
        assertThat(users.get(0).getNotificationFilters().size(), is(1));
    }

    @Test
    public void shouldCreateANewUser() throws Exception {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.LDAP);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(foo), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(localizer), is("User 'fooUser' successfully added."));
    }

    @Test
    public void shouldReturnErrorWhenTryingToAddAnonymousUser() throws Exception {
        UserSearchModel anonymous = new UserSearchModel(new User(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()), "Mr. Anonymous", "anon@cruise.com"), UserSourceType.LDAP);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(anonymous), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Failed to add user. Username 'anonymous' is not permitted."));
    }

    @Test
    public void shouldReturnErrorWhenUserAlreadyExists() throws Exception {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.LDAP);
        addUser(foo.getUser());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(foo), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Failed to add user. User 'fooUser' already exists."));
    }

    @Test
    public void create_shouldReturnErrorWhenNoUsersSelected() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(new ArrayList<UserSearchModel>(), result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("No users selected."));
    }

    @Test
    public void disableUsers_shouldAlsoExpireOauthTokens() throws Exception {
        addUser(new User("user_one"));
        addUser(new User("user_two"));

        generateOauthTokenFor("user_one");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.disable(Arrays.asList("user_one"), result);

        assertThat(result.isSuccessful(), is(true));
        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat("user should be disabled", models.get(0).isEnabled(), is(false));
        assertThat("user should be enabled", models.get(1).isEnabled(), is(true));

        assertThat(template.find("from OauthAuthorization").size(), is(0));
        assertThat(template.find("from OauthToken").size(), is(0));
    }

    private void generateOauthTokenFor(String userId) {
        OauthClient mingle = new OauthClient("mingle09", "client_id", "client_secret", "http://some-tracking-tool");
        template.save(mingle);
        OauthAuthorization authorization = new OauthAuthorization(userId, mingle, "code", 332333);
        template.save(authorization);
        OauthToken oauthToken = new OauthToken(userId, mingle, "access-token", "refresh-token", 23324324);
        template.save(oauthToken);
    }

    @Test
    public void shouldEnableUsers() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        User user1 = new User("user_one");
        user1.disable();
        addUser(user1);

        createDisabledUser("user_two");

        userService.enable(Arrays.asList("user_one"), new HttpLocalizedOperationResult());

        assertThat(result.isSuccessful(), is(true));
        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat("user should be enabled", models.get(0).isEnabled(), is(true));
        assertThat("user should be disabled", models.get(1).isEnabled(), is(false));
    }

    @Test
    public void shouldKnowEnabledAndDisbaledUsersCount() throws Exception {
        addUser(new User("user_one"));
        addUser(new User("user_three"));

        createDisabledUser("user_two");

        assertThat(userService.enabledUserCount(), is(2));
        assertThat(userService.disabledUserCount(), is(1));
    }

    @Test
    public void shouldReturnErrorMessageWhenUserValidationsFail() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User invalidUser = new User("fooUser", "Foo User", "invalidEmail");
        UserSearchModel searchModel = new UserSearchModel(invalidUser, UserSourceType.LDAP);

        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Failed to add user. Validations failed. Invalid email address."));
    }

    @Test
    public void shouldDeleteAllUsers() throws Exception {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(foo), result);

        assertThat(userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.ASC).size(), is(1));

        userService.deleteAll();

        assertThat(userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.ASC).size(), is(0));
    }

    @Test
    public void shouldReturnErrorMessageWhenTheLastAdminIsBeingDisabled() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        configFileHelper.turnOnSecurity();
        configFileHelper.addAdmins("Jake", "Pavan", "Yogi");

        userService.create(users("Jake", "Pavan", "Shilpa", "Yogi"), new HttpLocalizedOperationResult());

        userService.disable(Arrays.asList("Yogi"), result);
        assertThat(result.isSuccessful(), is(true));

        userService.disable(Arrays.asList("Pavan", "Jake"), result);//disable remaining admins

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.message(localizer), is("Did not disable any of the selected users. Ensure that all configured admins are not being disabled."));
    }

    @Test
    public void modifyRoles_shouldAddUserToExistingRole() throws Exception {
        configFileHelper.addRole(new Role(new CaseInsensitiveString("dev")));
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(true));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void modifyRoles_shouldNotAddUserToExistingRoleIfAlreadyAMember() throws Exception {
        addUser(new User("user-1"));
        // first time
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // second time
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(true));
    }

    @Test
    public void modifyRoles_shouldCreateRoleAndAddUserIfRoleDoesntExist() throws Exception {
        addUser(new User("user-1"));
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(true));
    }

    @Test
    public void modifyRoles_shouldNotCreateRoleIfItHasInvalidCharacters() throws Exception {
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection(".dev+", TriStateSelection.Action.add)), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer),containsString("Failed to add role. Reason - "));
    }

    @Test
    public void modifyRoles_shouldRemoveUserFromRole() throws Exception {
        addUser(new User("user-1"));
        // add it first
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // now remove it
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.remove)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(false));
    }

    @Test
    public void modifyRoles_shouldNotModifyRolesWhenActionIsNoChange() throws Exception {
        addUser(new User("user-1"));
        // add it first
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        // no change
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.nochange)), new HttpLocalizedOperationResult());
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(true));
    }

    @Test
    public void modifyRoles_shouldNotModifyRolesForAUserThatDoesNotExistInDb() throws Exception {
        assertThat(userDao.findUser("user-1"), is(instanceOf(NullUser.class)));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), result);

        assertThat(userDao.findUser("user-1"), is(instanceOf(NullUser.class)));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), containsString("User 'user-1' does not exist in the database."));
    }

    @Test
    public void shouldModifyRolesAndAdminPrivilegeAtTheSameTime() throws Exception {
        configFileHelper.addRole(new Role(new CaseInsensitiveString("dev")));
        addUser(new User("user-1"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user-1"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("dev")).hasMember(new CaseInsensitiveString("user-1")), is(true));
        assertThat(cruiseConfig.server().security().adminsConfig().hasUser(new CaseInsensitiveString("user-1"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldAddAdminPrivilegeToMultipleUsers() throws Exception {
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add), new ArrayList<TriStateSelection>(), result);
        CruiseConfig cruiseConfig = goConfigDao.load();
        final AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldRemoveUserLevelAdminPrivilegeFromMultipleUsers_withoutModifingRoleLevelPrvileges() throws Exception {
        configFileHelper.addAdmins("user", "boozer");
        configFileHelper.addRole(new Role(new CaseInsensitiveString("mastersOfTheWorld"), new RoleUser(new CaseInsensitiveString("loser")), new RoleUser(new CaseInsensitiveString("boozer"))));
        configFileHelper.addAdminRoles("mastersOfTheWorld");
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));

        CruiseConfig cruiseConfig = goConfigDao.load();
        SecurityConfig securityConfig = cruiseConfig.server().security();
        AdminsConfig adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove), new ArrayList<TriStateSelection>(), result);

        cruiseConfig = goConfigDao.load();
        securityConfig = cruiseConfig.server().security();
        adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        final SecurityService.UserRoleMatcherImpl groupMatcher = new SecurityService.UserRoleMatcherImpl(securityConfig);
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), groupMatcher), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), groupMatcher), is(true));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), groupMatcher), is(true));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldNotModifyAdminPrivilegesWhen_NoChange_requested() throws Exception {
        configFileHelper.addAdmins("user", "boozer");
        configFileHelper.addRole(new Role(new CaseInsensitiveString("mastersOfTheWorld"), new RoleUser(new CaseInsensitiveString("loser")), new RoleUser(new CaseInsensitiveString("boozer"))));
        configFileHelper.addAdminRoles("mastersOfTheWorld");
        addUser(new User("user"));
        addUser(new User("loser"));
        addUser(new User("boozer"));


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("user", "boozer"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), new ArrayList<TriStateSelection>(), result);

        final CruiseConfig cruiseConfig = goConfigDao.load();
        final SecurityConfig securityConfig = cruiseConfig.server().security();
        final AdminsConfig adminsConfig = securityConfig.adminsConfig();
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("user"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("loser"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
        assertThat(adminsConfig.hasUser(new CaseInsensitiveString("boozer"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void getRoleSelection() throws Exception {
        configFileHelper.addRole(new Role(new CaseInsensitiveString("dev")));
        configFileHelper.addRole(new Role(new CaseInsensitiveString("boy")));
        configFileHelper.addRole(new Role(new CaseInsensitiveString("girl")));
        configFileHelper.addRole(new Role(new CaseInsensitiveString("none")));
        addUser(new User("yogi"));
        addUser(new User("shilpa"));
        addUser(new User("pavan"));
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("yogi", "shilpa"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("dev", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("shilpa"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("girl", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("yogi"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("boy", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        userService.modifyRolesAndUserAdminPrivileges(Arrays.asList("pavan"), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange), Arrays.asList(new TriStateSelection("none", TriStateSelection.Action.add)), new HttpLocalizedOperationResult());
        List<TriStateSelection> selections = userService.getAdminAndRoleSelections(Arrays.asList("yogi", "shilpa")).getRoleSelections();
        assertThat(selections.size(), is(4));
        assertRoleSelection(selections.get(0), "boy", TriStateSelection.Action.nochange);
        assertRoleSelection(selections.get(1), "dev", TriStateSelection.Action.add);
        assertRoleSelection(selections.get(2), "girl", TriStateSelection.Action.nochange);
        assertRoleSelection(selections.get(3), "none", TriStateSelection.Action.remove);
    }

    @Test
    public void shouldGetAdminSelectionWithCorrectState() throws Exception {
        configFileHelper.addAdmins("foo", "quux");
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo", "bar")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo", "quux")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("baz", "bar")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove)));
    }

    @Test
    public void shouldDisableAdminSelectionWhenUserIsMemberOfAdminRole() throws Exception {
        configFileHelper.addRole(new Role(new CaseInsensitiveString("foo-grp"), new RoleUser(new CaseInsensitiveString("foo")), new RoleUser(new CaseInsensitiveString("foo-one"))));
        configFileHelper.addRole(new Role(new CaseInsensitiveString("quux-grp"), new RoleUser(new CaseInsensitiveString("quux"))));
        configFileHelper.addRole(new Role(new CaseInsensitiveString("bar-grp"), new RoleUser(new CaseInsensitiveString("bar")), new RoleUser(new CaseInsensitiveString("bar-one"))));
        configFileHelper.addAdminRoles("foo-grp", "quux-grp");

        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add, false)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo", "bar")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.nochange, false)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("bar", "baz")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("baz")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.remove)));
        assertThat(userService.getAdminAndRoleSelections(Arrays.asList("foo", "quux")).getAdminSelection(), is(new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add, false)));
    }

    @Test
    public void shouldUpdateEnabledStateToFalse() throws Exception {
        User user = new User("user-1");
        user.enable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.FALSE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.isEnabled(), is(false));
    }

    @Test
    public void shouldUpdateEnabledStateToTrue() throws Exception {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.TRUE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.isEnabled(), is(true));
    }

    @Test
    public void shouldNotUpdateEnabledStateWhenAskedToBeLeftUnset() throws Exception {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEnabled(), is(false));
    }

    @Test
    public void updateShouldUpdateEmailMeStateToTrue() throws Exception {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(false);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.TRUE, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.isEmailMe(), is(true));
    }


    @Test
    public void updateShouldUpdateEmailMeStateToFalse() throws Exception {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.FALSE, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.isEmailMe(), is(false));;
    }

    @Test
    public void updateShouldUpdateEmailMeStateWHenAskedToBeLeftUnset() throws Exception {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEmailMe(), is(true));
    }

    @Test
    public void updateShouldUpdateEmail() throws Exception {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, "foo@example.com", null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getEmail(), is("foo@example.com"));

        result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.TRUE, TriState.UNSET, "", null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getEmail(), is(""));
    }

    @Test
    public void updateShouldNotUpdateEmailWhenNull() throws Exception {
        User user = new User("user-1");
        user.setEmail("foo@example.com");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getEmail(), is("foo@example.com"));
    }

    @Test
    public void updateShouldUpdateMatcher() throws Exception {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, "foo,bar", result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getMatcher(), is("foo,bar"));

        result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, "", result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getMatcher(), is(""));
    }

    @Test
    public void updateShouldNotUpdateMatcherWhenNull() throws Exception {
        User user = new User("user-1");
        user.setMatcher("foo,bar");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.update(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(user.getMatcher(), is("foo,bar"));
    }

    private void assertRoleSelection(TriStateSelection selection, String roleName, TriStateSelection.Action action) {
        assertThat(selection.getValue(), is(roleName));
        assertThat(selection.getAction(), is(action));
    }

    private void createDisabledUser(String username) {
        User user = new User(username);
        user.disable();
        addUser(user);
    }

    private List<UserSearchModel> users(String... usernames) {
        List<UserSearchModel> models = new ArrayList<UserSearchModel>();
        for (String username : usernames) {
            models.add(new UserSearchModel(new User(username, username, "foo@cruise.com")));
        }
        return models;
    }

    private void givingJezViewPermissionToMingle() throws Exception {
        configFileHelper.turnOnSecurity();
        configFileHelper.addPipeline("mingle", "dev");
        configFileHelper.setViewPermissionForGroup("defaultGroup", "jez");
        configFileHelper.addSecurityWithAdminConfig();
    }

    private void addUser(User user) {
        userDao.saveOrUpdate(user);
    }

    private void addUserWithNotificationFilter(User user, NotificationFilter... filters) {
        for (NotificationFilter filter : filters) {
            user.addNotificationFilter(filter);
        }
        addUser(user);
    }

    private Matcher<? super User> isANullUser() {
        return is(CoreMatchers.<Object>instanceOf(NullUser.class));
    }
}

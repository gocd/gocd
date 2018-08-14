package com.thoughtworks.go.apiv1.adminsconfig.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

class AdminsConfigRepresenterTest {
    @Test
    void shouldSerializeToJSON() {
        AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),new AdminRole(new CaseInsensitiveString("xyz")))

        def actualJson = toObjectString({ AdminsConfigRepresenter.toJSON(it, config) })

        final LinkedHashMap<String, Object> expected = ["_links": ["doc": ["href": "https://api.gocd.org/#system_admins"], "self": ["href": "http://test.host/go/api/admin/security/system_admins"]], "roles": ["xyz"], "users": ["admin"]]
        assertThatJson(actualJson).isEqualTo(expected)
    }

    @Test
    void shouldSerializeToJSONWithErrors() {
        AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),new AdminRole(new CaseInsensitiveString("xyz")))

        config.addError("users", "User name cannot be blank")
        config.addError("users", "User name cannot be blank")
        config.addError("roles", "Role does not exist")

        def actualJson = toObjectString({ AdminsConfigRepresenter.toJSON(it, config) })

        final LinkedHashMap<String, Object> expected = ["_links":["doc":["href":"https://api.gocd.org/#system_admins"],"self":["href":"http://test.host/go/api/admin/security/system_admins"]],"roles":["xyz"],"users":["admin"],"errors":["roles":["Role does not exist"],"users":["User name cannot be blank"]]]
        assertThatJson(actualJson).isEqualTo(expected);
    }

    @Test
    void shouldDeSerializeAdminsConfigFromJSON() {
        def requestJSON = [
                'roles': ['qa', 'dev'],
                'users': ['user1', 'user2']
        ]

        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(requestJSON);
        AdminsConfig adminsConfig = AdminsConfigRepresenter.fromJSON(jsonReader);

        def expected = new AdminsConfig(
          new AdminUser(new CaseInsensitiveString("user1")),
          new AdminUser(new CaseInsensitiveString("user2")),
          new AdminRole(new CaseInsensitiveString("qa")),
          new AdminRole(new CaseInsensitiveString("dev")),
        )
        assertThat(adminsConfig, is( expected))
    }
}

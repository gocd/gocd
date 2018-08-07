package com.thoughtworks.go.apiv1.adminsconfig.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.config.Admin
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AdminsRepresenterTest {
    private final LinkedHashMap<String, Object> admins = ["_links": ["doc": ["href": "https://api.gocd.org/#admins"], "self": ["href": "http://test.host/go/api/admin/security/admins"]], "roles": ["xyz"], "users": ["admin"]]

    private final LinkedHashMap<String, Object> finalAdminsAfterUpdate = ["_links": ["doc": ["href": "https://api.gocd.org/#admins"], "self": ["href": "http://test.host/go/api/admin/security/admins"]], "roles": ["xyz", "Dev", "est"], "users": ["admin", "Linux", "Firefox"]]

    private final LinkedHashMap<String, Object> finalAdminsAfterReplace = ["_links": ["doc": ["href": "https://api.gocd.org/#admins"], "self": ["href": "http://test.host/go/api/admin/security/admins"]], "roles": ["Dev", "est"], "users": ["Linux", "Firefox", "Chrome"]]

    @Test
    void shouldGenerateJSON() {
        List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
        Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
        admins.add(admin)
        Admin admin2 = new AdminRole(new CaseInsensitiveString("xyz"))
        admins.add(admin2)

        AdminsConfig config = new AdminsConfig(admins)
        def actualJson = toObjectString({ AdminsRepresenter.toJSON(it, config) })

        assertThatJson(actualJson).isEqualTo(this.admins)
    }

    @Test
    void shouldGenerateJSONForUpdate() {
        List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
        Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
        admins.add(admin)
        Admin admin2 = new AdminRole(new CaseInsensitiveString("xyz"))
        admins.add(admin2)

        AdminsConfig config = new AdminsConfig(admins)
        def body = [
                'roles': ['add': ['Dev', 'est'], 'remove': ['Production']],
                'users': ['add': ['Linux', 'Firefox'], 'remove': ['Chrome']]
        ]
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(body);
        String protocol = "PATCH";
        def adminsConfig = AdminsRepresenter.fromJSON(jsonReader, protocol, config);
        def actualJson = toObjectString({ AdminsRepresenter.toJSON(it, adminsConfig) })
        assertThatJson(actualJson).isEqualTo(this.finalAdminsAfterUpdate)
    }

    @Test
    void shouldGenerateJSONForReplace() {
        List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
        Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
        admins.add(admin)
        Admin admin2 = new AdminRole(new CaseInsensitiveString("xyz"))
        admins.add(admin2)

        AdminsConfig config = new AdminsConfig(admins)
        def body = [
                'roles': ['Dev', 'est'],
                'users': ['Linux', 'Firefox', 'Chrome']
        ]
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(body);
        String protocol = "PUT";
        def adminsConfig = AdminsRepresenter.fromJSON(jsonReader, protocol, config);
        def actualJson = toObjectString({ AdminsRepresenter.toJSON(it, adminsConfig) })
        assertThatJson(actualJson).isEqualTo(this.finalAdminsAfterReplace)
    }
}

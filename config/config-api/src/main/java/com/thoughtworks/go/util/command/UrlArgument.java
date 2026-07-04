/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.config.ConfigAttributeValue;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static com.thoughtworks.go.util.command.UrlUserInfo.urlCannotHaveUserInfo;

@ConfigAttributeValue(fieldName = "url")
public class UrlArgument extends CommandArgument {
    private static final Pattern URL_DUMB_VALIDATION_REGEX = Pattern.compile("^[a-zA-Z0-9/#].*");

    protected String url;

    public UrlArgument(String url) {
        bombIfNull(url, "Url cannot be null.");
        this.url = url;
    }

    @Override
    public String originalArgument() {
        return url;
    }

    @Override
    public String forDisplay() {
        return modifyUserInfo(sanitized(), UrlArgument::redactUserInfo);
    }

    static String modifyUserInfo(String url, UserInfoRedactor userInfoRedactor) {
        // Short-circuit parsing of URLs without any userinfo.
        if (urlCannotHaveUserInfo(url)) {
            return url;
        }

        try {
            URI uri = new URI(url);
            if (uri.getUserInfo() != null) {
                return new URI(uri.getScheme(), userInfoRedactor.redact(uri.getScheme(), uri.getUserInfo()), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment())
                    .toString();
            }
            return url;
        } catch (URISyntaxException ignore) {
            return url;
        }
    }

    interface UserInfoRedactor {
        String redact(String scheme, String userInfo);
    }

    private static String redactUserInfo(String scheme, String userInfo) {
        UrlUserInfo urlUserInfo = new UrlUserInfo(userInfo);
        return urlUserInfo.isPossiblyToken() && ("ssh".equals(scheme) || "svn+ssh".equals(scheme))
            ? userInfo
            : urlUserInfo.maskedUserInfo();
    }

    @Override
    public String forCommandLine() {
        return this.url;
    }

    String sanitized() {
        return this.url;
    }

    public static UrlArgument create(String url) {
        return new UrlArgument(url);
    }

    @Override
    public @NotNull Redactable redactFrom(@NotNull Redactable toRedact) {
        if (toRedact.isBlank() || urlCannotHaveUserInfo(url)) {
            return toRedact;
        }

        try {
            final URI uri = new URI(url);
            if (uri.getUserInfo() != null) {
                UrlUserInfo urlUserInfo = new UrlUserInfo(uri.getUserInfo());
                return toRedact.next(toRedact.value().replace(uri.getUserInfo(), urlUserInfo.maskedUserInfo()));
            }
        } catch (URISyntaxException ignore) {
            // Ignore as url is not according to URI specs
        }

        return toRedact;
    }

    @Override
    public boolean equal(CommandArgument that) {
        //BUG #3276 - on Windows svn info includes a password in svn+ssh
        if (url.startsWith("svn+ssh")) {
            return this.originalArgument().equals(that.originalArgument());
        }
        return cleanPath(this).equals(cleanPath(that));
    }

    private String cleanPath(CommandArgument commandArgument) {
        String path = commandArgument.originalArgument();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String withoutCredentials() {
        if (urlCannotHaveUserInfo(url)) {
            return url;
        }

        try {
            return new URIBuilder(this.sanitized()).setUserInfo(null).build().toString();
        } catch (URISyntaxException ignore) {
            return url;
        }
    }

    public boolean isValidURLOrLocalPath() {
        return URL_DUMB_VALIDATION_REGEX.matcher(url).matches();
    }
}

/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.skmcl.session;

import com.sk89q.skmcl.util.HttpRequest;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.sk89q.skmcl.util.SharedLocale._;

/**
 * Implements the Yggdrasil authentication scheme.
 */
@ToString(exclude = "password")
public class YggdrasilSession implements Session {

    private static final URL AUTH_URL =
            HttpRequest.url("https://authserver.mojang.com/authenticate");
    private static final URL REFRESH_URL =
            HttpRequest.url("https://authserver.mojang.com/refresh");

    private final String id;
    @Getter @Setter
    private List<Identity> identities;
    @Getter @Setter
    private String password;
    @Getter @Setter
    private String clientToken;
    @Getter @Setter
    private String accessToken;

    /**
     * Construct a login session using the given login ID.
     *
     * @param id email address or username
     */
    public YggdrasilSession(@NonNull String id) {
        this.id = id;
    }

    @Override
    public boolean isValid() {
        return accessToken != null;
    }

    public YggdrasilSession verify() throws AuthenticationException, InterruptedException, IOException {
        if (password != null && !password.isEmpty()) {
            authenticate();
        } else if (accessToken != null && clientToken != null) {
            refresh();
        } else {
            throw new AuthenticationException("Missing password/details", _("errors.incompleteCredentials"));
        }

        return this;
    }

    private void authenticate() throws IOException, InterruptedException, AuthenticationException {
        Object payload = new AuthenticatePayload(id, password);

        HttpRequest request = HttpRequest
                .post(AUTH_URL)
                .bodyJson(payload)
                .execute();

        if (request.getResponseCode() != 200) {
            ErrorResponse error = request.returnContent().asJson(ErrorResponse.class);
            throw new AuthenticationException(error.getErrorMessage(), error.getErrorMessage());
        } else {
            AuthenticateResponse response =
                    request.returnContent().asJson(AuthenticateResponse.class);
            accessToken = response.getAccessToken();
            clientToken = response.getClientToken();
            identities = response.getAvailableProfiles();
            for (Identity identity : identities) {
                identity.setAccessToken(accessToken);
                identity.setClientToken(clientToken);
            }
        }
    }

    private void refresh() throws IOException, InterruptedException, AuthenticationException {
        Object payload = new RefreshPayload(accessToken, clientToken);

        HttpRequest request = HttpRequest
                .post(REFRESH_URL)
                .bodyJson(payload)
                .execute();

        if (request.getResponseCode() != 200) {
            ErrorResponse error = request.returnContent().asJson(ErrorResponse.class);
            throw new AuthenticationException(error.getErrorMessage(), error.getErrorMessage());
        } else {
            AuthenticateResponse response =
                    request.returnContent().asJson(AuthenticateResponse.class);
            accessToken = response.getAccessToken();
            clientToken = response.getClientToken();
            identities = response.getAvailableProfiles();
            for (Identity identity : identities) {
                identity.setAccessToken(accessToken);
                identity.setClientToken(clientToken);
            }
        }
    }

    @Data
    private static class Agent {
        private final String name = "Minecraft";
        private final int version = 1;
    }

    @Data
    private static class AuthenticatePayload {
        private final Agent agent = new Agent();
        private final String username;
        private final String password;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AuthenticateResponse {
        private String accessToken;
        private String clientToken;
        private List<Identity> availableProfiles;
        private Identity selectedProfile;
    }

    @Data
    private static class RefreshPayload {
        private final String accessToken;
        private final String clientToken;
        private Identity selectedProfile;
    }

    @Data
    private static class ErrorResponse {
        private String error;
        private String errorMessage;
        private String cause;
    }

}

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

import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.skmcl.util.HttpRequest;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URL;

/**
 * Implements the Yggdrasil authentication scheme.
 */
public class YggdrasilSession implements Session {

    private final String id;
    @Getter @NonNull
    private String username;
    @Getter
    private String password;
    @Getter @Setter @NonNull
    private URL url;
    @Getter
    private String accessToken;
    @Getter
    private String sessionId;

    /**
     * Construct a login session using the given login ID, which could be an email
     * address (newer/migrated accounts) or a username.
     *
     * @param id email address or username
     * @param password the password
     */
    public YggdrasilSession(@NonNull String id, @NonNull String password) {
        this.id = id;
        this.username = id;
        this.password = password;
        this.url = HttpRequest.url("https://authserver.mojang.com/authenticate");
    }

    @Override
    public boolean isValid() {
        return sessionId != null;
    }

    @Override
    public Boolean call() throws Exception {
        HttpRequest request = null;
        Object payload = new AuthenticatePayload(id, password);

        try {
            request = HttpRequest
                    .post(url)
                    .bodyJson(payload)
                    .execute();

            if (request.getResponseCode() != 200) {
                ErrorResponse error = request.returnContent().asJson(ErrorResponse.class);
                throw new AuthenticationException(error.getErrorMessage());
            } else {
                AuthenticateResponse response =
                        request.returnContent().asJson(AuthenticateResponse.class);
                accessToken = response.getAccessToken();
                sessionId = response.getClientToken();
                return true;
            }
        } finally {
            LauncherUtils.close(request);
        }
    }

    @Data
    private static class Agent {
        private final String name = "SKMCLauncher";
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
    }

    @Data
    private static class ErrorResponse {
        private String error;
        private String errorMessage;
        private String cause;
    }

}

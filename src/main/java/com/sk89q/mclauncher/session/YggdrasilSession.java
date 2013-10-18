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

package com.sk89q.mclauncher.session;

import com.sk89q.mclauncher.util.HttpRequest;
import com.sk89q.mclauncher.util.LauncherUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implements the newer Yggdrasil authentication scheme used since the 1.6 update.
 * <p/>
 * <p>The new protocol uses JSON for payloads and can return an access token to be used
 * in place of the password.</p>
 */
public class YggdrasilSession implements MinecraftSession {

    private final String id;
    private URL url;
    private String accessToken;
    private String clientToken;

    /**
     * Construct a login session using the given login ID.
     * <p/>
     * <p>The ID is an email address for migrated and newer accounts, whereas it is a
     * username for older non-migrated accounts (as of 2013).</p>
     *
     * @param id id
     */
    public YggdrasilSession(String id) {
        this.id = id;

        try {
            url = new URL("https://authserver.mojang.com/authenticate");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected error when setting URL");
        }
    }

    /**
     * Get the URL used to authenticate.
     *
     * @return the URL
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Set the URL used to authenticate.
     *
     * @param url the URL
     */
    public void setUrl(URL url) {
        if (url == null) {
            throw new NullPointerException();
        }
        this.url = url;
    }

    @Override
    public void login(String password) throws IOException, LoginException {
        HttpRequest request = null;
        Object payload = new AuthenticatePayload(id, password);

        try {
            request = HttpRequest
                    .post(url)
                    .bodyJson(payload)
                    .execute();

            if (request.getResponseCode() != 200) {
                throw new LoginException(
                        request.asJson(ErrorResponse.class).getErrorMessage());
            } else {
                AuthenticateResponse response = request.asJson(AuthenticateResponse.class);
                accessToken = response.getAccessToken();
                clientToken = response.getClientToken();
            }
        } finally {
            LauncherUtils.close(request);
        }
    }

    @Override
    public boolean isValid() {
        return clientToken != null;
    }

    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public String getSessionId() {
        return clientToken;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * User agent to identify the originator of requests.
     */
    private static class Agent {
        private final String name = "SKMCLauncher";
        private final int version = 1;
    }

    /**
     * Request payload sent to authenticate.
     */
    private static class AuthenticatePayload {
        private final Agent agent = new Agent();
        private final String username;
        private final String password;

        public AuthenticatePayload(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Resposne payload for authentication success.
     */
    private static class AuthenticateResponse {
        private String accessToken;
        private String clientToken;

        public String getAccessToken() {
            return accessToken;
        }

        public String getClientToken() {
            return clientToken;
        }
    }

    /**
     * Response payload for errors.
     */
    private static class ErrorResponse {
        private String error;
        private String errorMessage;
        private String cause;

        public String getError() {
            return error;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * The cause of the error.
         *
         * @return may be null or an empty string
         */
        public String getCause() {
            return cause;
        }
    }

}

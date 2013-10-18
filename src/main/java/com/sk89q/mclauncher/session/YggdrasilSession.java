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

import com.google.gson.Gson;
import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.security.X509KeyRing;
import com.sk89q.mclauncher.util.LauncherUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Implements the newer Yggdrasil authentication scheme used since the
 * Minecraft 1.6 update.
 */
public class YggdrasilSession implements MinecraftSession {

    private static final int READ_TIMEOUT = 1000 * 60 * 10;
    private final Gson gson = new Gson();

    private String username;
    private String accessToken;
    private String clientToken;

    /**
     * Construct the session.
     *
     * @param username username
     */
    public YggdrasilSession(String username) {
        this.username = username;
    }

    @Override
    public void login(String password) throws IOException, LoginException {
        HttpsURLConnection conn = null;
        URL url = new URL("https://authserver.mojang.com/authenticate");
        String payload = gson.toJson(new AuthenticatePayload(username, password));

        // Get trust chain
        TrustManager[] trustManagers = new TrustManager[] {
                Launcher.getInstance().getKeyRing().getKeyStore(
                        X509KeyRing.Ring.MINECRAFT_LOGIN)};

        try {
            conn = (HttpsURLConnection) url.openConnection();
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagers, null);
            conn.setSSLSocketFactory(ctx.getSocketFactory());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length",
                    Integer.toString(payload.getBytes().length));
            conn.setRequestProperty("Content-Language", "en-US");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setReadTimeout(READ_TIMEOUT);

            conn.connect();

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(payload);
            out.flush();
            out.close();

            InputStream is = conn.getResponseCode() == 200 ?
                    conn.getInputStream() : conn.getErrorStream();
            String result = LauncherUtils.toString(is, "UTF-8");

            if (conn.getResponseCode() != 200) {
                ErrorResponse error = gson.fromJson(result, ErrorResponse.class);
                throw new LoginException(error.getErrorMessage());
            } else {
                AuthenticateResponse response = gson.fromJson(result, AuthenticateResponse.class);
                accessToken = response.getAccessToken();
                clientToken = response.getClientToken();
            }
        } catch (KeyManagementException e) {
            throw new LoginException("Failed to process PKI keys: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new LoginException("Failed to initiate TLS: " + e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
            conn = null;
        }
    }

    @Override
    public boolean isValid() {
        return clientToken != null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getSessionId() {
        return clientToken;
    }
    
    public String getAccessToken() {
        return accessToken;
    }

    private static class Agent {
        private final String name = "SKMCLauncher";
        private final int version = 1;
    }

    private static class AuthenticatePayload {
        private final Agent agent = new Agent();
        private final String username;
        private final String password;

        public AuthenticatePayload(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static class AuthenticateResponse {
        private String accessToken;
        private String clientToken;

        private String getAccessToken() {
            return accessToken;
        }

        private String getClientToken() {
            return clientToken;
        }
    }

    private static class ErrorResponse {
        private String error;
        private String errorMessage;
        private String cause;

        private String getError() {
            return error;
        }

        private String getErrorMessage() {
            return errorMessage;
        }

        private String getCause() {
            return cause;
        }
    }

}

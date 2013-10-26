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

package com.sk89q.skmcl.minecraft.model;

import com.sk89q.skmcl.util.Environment;
import com.sk89q.skmcl.util.HttpRequest;
import com.sk89q.skmcl.util.Platform;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Data
public class Library {

    public static final String BASE_URL =
            "https://s3.amazonaws.com/Minecraft.Download/libraries/";

    private String name;
    private transient String group;
    private transient String artifact;
    private transient String version;
    private Map<String, String> natives;
    private Extract extract;
    private List<Rule> rules;

    public void setName(String name) {
        this.name = name;

        if (name != null) {
            String[] parts = name.split(":");
            this.group = parts[0];
            this.artifact = parts[1];
            this.version = parts[2];
        } else {
            this.group = null;
            this.artifact = null;
            this.version = null;
        }
    }

    public boolean matches(Environment environment) {
        boolean allow = false;

        if (getRules() != null) {
            for (Rule rule : getRules()) {
                if (rule.matches(environment)) {
                    allow = rule.getAction() == Action.ALLOW;
                }
            }
        } else {
            allow = true;
        }

        return allow;
    }

    @JsonIgnore
    public String getGroup() {
        return group;
    }

    @JsonIgnore
    public String getArtifact() {
        return artifact;
    }

    @JsonIgnore
    public String getVersion() {
        return version;
    }

    public String getNativeString(Platform platform) {
        if (getNatives() != null) {
            switch (platform) {
                case LINUX:
                    return getNatives().get("linux");
                case WINDOWS:
                    return getNatives().get("windows");
                case MAC_OS_X:
                    return getNatives().get("osx");
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    public String getFilename(Environment environment) {
        String nativeString = getNativeString(environment.getPlatform());
        if (nativeString != null) {
            return String.format("%s-%s-%s.jar",
                    getArtifact(), getVersion(), nativeString);
        }

        return String.format("%s-%s.jar", getArtifact(), getVersion());
    }

    public String getPath(Environment environment) {
        StringBuilder builder = new StringBuilder();
        builder.append(getGroup().replace('.', '/'));
        builder.append("/");
        builder.append(getArtifact());
        builder.append("/");
        builder.append(getVersion());
        builder.append("/");
        builder.append(getFilename(environment));
        return builder.toString();
    }

    public URL getUrl(Environment environment) {
        StringBuilder builder = new StringBuilder();
        builder.append(BASE_URL);
        builder.append(getPath(environment));
        return HttpRequest.url(builder.toString());
    }

    @Data
    public static class Rule {
        private Action action;
        private OS os;

        public boolean matches(Environment environment) {
            if (getOs() == null) {
                return true;
            } else {
                return getOs().matches(environment);
            }
        }
    }

    @Data
    public static class OS {
        private Platform platform;
        private Pattern version;

        @JsonProperty("name")
        @JsonDeserialize(using = PlatformSerializer.class)
        public Platform getPlatform() {
            return platform;
        }

        public boolean matches(Environment environment) {
            return getPlatform().equals(environment.getPlatform()) &&
                    getVersion().matcher(environment.getPlatformVersion()).matches();
        }
    }

    @Data
    public static class Extract {
        private List<String> exclude;
    }

    private enum Action {
        ALLOW,
        DISALLOW;

        @JsonCreator
        public static Action fromJson(String text) {
            return valueOf(text.toUpperCase());
        }
    }

}

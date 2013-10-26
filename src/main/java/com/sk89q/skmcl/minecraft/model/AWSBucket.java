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

import com.sk89q.skmcl.util.HttpRequest;
import lombok.Data;

import javax.xml.bind.annotation.*;
import java.net.URL;
import java.util.ArrayList;

@XmlRootElement(name = "ListBucketResult",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class AWSBucket {

    @XmlElement(name = "IsTruncated",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
    private boolean isTruncated;

    @XmlElement(name = "Contents",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
    private ArrayList<Item> contents;

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class Item {
        @XmlElement(name = "Key",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        private String key;

        @XmlElement(name = "ETag",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        private String etag;

        @XmlElement(name = "Size",
                namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
        private int size;

        @XmlTransient
        public boolean isDirectory() {
            return getKey().endsWith("/");
        }

        public URL getUrl(URL baseUrl) {
            return HttpRequest.url(baseUrl.toExternalForm() + getKey());
        }
    }

}

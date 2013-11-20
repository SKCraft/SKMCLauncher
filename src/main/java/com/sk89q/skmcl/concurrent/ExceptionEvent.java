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

package com.sk89q.skmcl.concurrent;

import lombok.Getter;

import java.util.EventObject;

public class ExceptionEvent extends EventObject {

    @Getter
    private final Throwable throwable;

    /**
     * Constructs an exception event.
     *
     * @param source The object on which the Event initially occurred.
     * @param throwable the error
     * @throws IllegalArgumentException if source is null
     */
    public ExceptionEvent(Object source, Throwable throwable) {
        super(source);
        this.throwable = throwable;
    }

}

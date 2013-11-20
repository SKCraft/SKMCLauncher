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

public abstract class AbstractWorker<V> extends WorkUnit implements Worker<V> {

    @Override
    public V call() throws Exception {
        run();
        return null;
    }

    protected void run() throws Exception {
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s{progress=%.2f%%, status=%s}",
                getClass().toString(), getProgress() * 100, getLocalizedStatus());
    }

}

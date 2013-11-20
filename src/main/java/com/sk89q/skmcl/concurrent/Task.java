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

import lombok.Delegate;
import lombok.NonNull;

import java.util.Observer;
import java.util.concurrent.Future;

public class Task<V> implements Future<V>, ProgressObservable {

    @Delegate
    private Future<V> future;
    private final Worker<V> worker;

    public Task(@NonNull Worker<V> worker) {
        this.worker = worker;
    }

    public void setFuture(Future<V> future) {
        this.future = future;
    }

    @Override
    public double getProgress() {
        return worker.getProgress();
    }

    @Override
    public String getLocalizedTitle() {
        return worker.getLocalizedTitle();
    }

    @Override
    public String getLocalizedStatus() {
        return worker.getLocalizedStatus();
    }

    @Override
    public boolean shouldConfirmInterrupt() {
        return worker.shouldConfirmInterrupt();
    }

    @Override
    public void addObserver(Observer o) {
        worker.addObserver(o);
    }

    @Override
    public void deleteObserver(Observer o) {
        worker.deleteObserver(o);
    }

}

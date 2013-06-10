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

package com.sk89q.mclauncher.util;

public class TaskWorker extends Thread {
    
    private Task task;
    
    public TaskWorker() {
        super();
    }
    
    public TaskWorker(Task task) {
        super(task);
        this.task = task;
    }
    
    public Task getTask() {
        return task;
    }
    
    @Override
    public void run() {
        try {
            task.run();
        } finally {
        }
    }

}

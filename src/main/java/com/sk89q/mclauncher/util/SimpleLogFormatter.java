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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class SimpleLogFormatter extends Formatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append("[")
            .append(record.getLevel().getLocalizedName())
            .append("] ")
            .append(formatMessage(record))
            .append(LINE_SEPARATOR);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception e) {
            }
        }

        return sb.toString();
    }
    
    public static void setAsFormatter() {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            handler.setFormatter(new SimpleLogFormatter());
        }
    }
    
}
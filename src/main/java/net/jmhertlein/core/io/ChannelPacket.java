/*
 * Copyright (C) 2013 Joshua Michael Hertlein <jmhertlein@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.jmhertlein.core.io;

import java.io.Serializable;

/**
 * A packet carrying arbitrary data on a specific channel.
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class ChannelPacket implements Serializable {
    private final Object data;
    private final int channel;

    public ChannelPacket(Object data, int channel) {
        this.data = data;
        this.channel = channel;
    }

    public Object getData() {
        return data;
    }

    public int getChannel() {
        return channel;
    }
}

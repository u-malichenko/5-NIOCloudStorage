package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.ListSignalByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

public class ClientAuthSender {
    /**
     * метод отправки данных авторизации
     */
    public static void sendAuthInfo(Channel channel, String login, String password) {
        byte[] loginBytes = login.getBytes();
        byte[] passwordBytes = password.getBytes();
        int bufLength = 1+4 + loginBytes.length + 4 + passwordBytes.length;
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(bufLength);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeInt(login.length());
        buf.writeBytes(loginBytes);
        buf.writeInt(password.length());
        buf.writeBytes(passwordBytes);
        channel.writeAndFlush(buf);
    }
}
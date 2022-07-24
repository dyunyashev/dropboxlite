package ru.myorg.client.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class JacksonEncoder extends MessageToByteEncoder<Message> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        out.writeBytes(mapper.writeValueAsBytes(msg));
    }
}

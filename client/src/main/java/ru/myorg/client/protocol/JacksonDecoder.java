package ru.myorg.client.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.InputStream;
import java.util.List;

public class JacksonDecoder extends ByteToMessageDecoder {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        InputStream byteBufInputStream = new ByteBufInputStream(in);
        Message message = mapper.readValue(byteBufInputStream, Message.class);
        out.add(message);
    }
}

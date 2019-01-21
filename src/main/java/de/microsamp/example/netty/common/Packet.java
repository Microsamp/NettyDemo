package de.microsamp.example.netty.common;

import io.netty.buffer.ByteBuf;

public interface Packet {

	void encode(ByteBuf buffer);

	void decode(ByteBuf buffer);

}

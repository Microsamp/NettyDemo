package de.microsamp.example.netty;

import de.microsamp.example.netty.common.Packet;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;

import java.nio.charset.StandardCharsets;

public class TestPacket implements Packet {

	private static final String MESSAGE = "Hey!";

	@Override
	public void encode(ByteBuf buffer) {
		buffer.writeInt(MESSAGE.length());
		buffer.writeCharSequence(MESSAGE, StandardCharsets.UTF_8);
	}

	@Override
	public void decode(ByteBuf buffer) {
		Assert.assertArrayEquals(MESSAGE.toCharArray(), ((String) buffer.readCharSequence(buffer.readInt(), StandardCharsets.UTF_8)).toCharArray());
	}

}

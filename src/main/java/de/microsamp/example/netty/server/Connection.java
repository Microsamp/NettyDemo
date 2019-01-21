package de.microsamp.example.netty.server;

import de.microsamp.example.netty.common.Packet;
import de.microsamp.example.netty.common.PacketRegistry;
import de.microsamp.example.netty.exception.NotConnectedException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Connection {

	private final Channel channel;

	public void sendPacket(Packet packet) {
		if(this.channel == null) throw new NotConnectedException();
		ByteBuf buffer = Unpooled.directBuffer();
		buffer.writeInt(PacketRegistry.getInstance().getPacketId(packet.getClass()));
		packet.encode(buffer);
		this.channel.writeAndFlush(buffer);
	}

	public void handle(Packet packet) {
		//TODO: Handle
	}

}

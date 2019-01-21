package de.microsamp.example.netty;

import de.microsamp.example.netty.client.Client;
import de.microsamp.example.netty.common.PacketRegistry;
import de.microsamp.example.netty.server.Server;
import org.junit.Test;

public class ServerAndClientTest {

	@Test
	public void testServerAndClient() {
		PacketRegistry.getInstance().register(TestPacket.class, 0x01);

		Server server = new Server("127.0.0.1", 1337, 1);
		server.startAsync();

		Client client = new Client("127.0.0.1", 1337);
		client.startAsync();

		client.sendPacket(new TestPacket());

		client.shutdown();
		server.shutdown();

	}

}

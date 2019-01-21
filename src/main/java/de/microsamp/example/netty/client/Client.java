package de.microsamp.example.netty.client;

import de.microsamp.example.netty.common.Packet;
import de.microsamp.example.netty.common.PacketRegistry;
import de.microsamp.example.netty.exception.NotConnectedException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class Client {

	private final String host;
	private final int port;

	private EventLoopGroup workerGroup;
	private final Object lock = new Object();

	private Channel channel;

	@Setter private Consumer<Throwable> exceptionHandler = (exception) -> {
		exception.printStackTrace();
	};

	/**
	 * Starts Netty in an async thread, but waits sync until Netty established the connection.
	 */
	public void startAsync() {
		new Thread(() -> {
			this.start();
		}, "netty-master").start();

		synchronized (this.lock) {
			try {
				this.lock.wait();
			} catch (InterruptedException ex) {
				exceptionHandler.accept(ex);
			}
		}
	}

	public void start() {
		boolean epoll = Epoll.isAvailable();
		this.workerGroup = epoll ?
				new EpollEventLoopGroup() :
				new NioEventLoopGroup();

		try {
			Bootstrap bootstrap = new Bootstrap()
					.group(this.workerGroup)
					.channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
					.remoteAddress(new InetSocketAddress(this.host, this.port));

			bootstrap.handler(new ChannelInitializer<SocketChannel>() {

				protected void initChannel(SocketChannel socketChannel) throws Exception {
					socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

						@Override
						public void channelActive(ChannelHandlerContext ctx) throws Exception {
							Client.this.channel = ctx.channel();
							synchronized (Client.this.lock) {
								Client.this.lock.notifyAll();
							}
						}

						@Override
						public void channelInactive(ChannelHandlerContext ctx) throws Exception {
							Client.this.channel = null;
						}

						@Override
						public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
							if (!(msg instanceof ByteBuf)) return; // we don't want to handle this, maybe it's supposed for another handler?

							ByteBuf buffer = (ByteBuf) msg;
							Packet packet = PacketRegistry.getInstance().newInstance(buffer.readInt());
							System.out.println("Received packet: "+packet);
						}

						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							Client.this.exceptionHandler.accept(cause);
							ctx.close();
						}

					});
				}

			});

			ChannelFuture channelFuture = bootstrap.connect().sync();
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException ex) {
			this.exceptionHandler.accept(ex);
		} finally {
			this.shutdown();
		}
	}

	/**
	 * Closes the connection synchronously.
	 */
	public void shutdown() {
		try {
			this.workerGroup.shutdownGracefully().sync();
		} catch (InterruptedException ex) {
			this.exceptionHandler.accept(ex);
		}
	}

	/**
	 * Sends the given packet as binary encoded data including the packet id to the server.
	 *
	 * @param packet The packet object holding your data
	 */
	public void sendPacket(Packet packet) {
		if(this.channel == null) throw new NotConnectedException();
		ByteBuf buffer = Unpooled.directBuffer();
		buffer.writeInt(PacketRegistry.getInstance().getPacketId(packet.getClass()));
		packet.encode(buffer);
		this.channel.writeAndFlush(buffer);
	}

}
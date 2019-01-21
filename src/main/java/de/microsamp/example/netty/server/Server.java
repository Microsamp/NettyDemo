package de.microsamp.example.netty.server;

import de.microsamp.example.netty.common.PacketRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Example for a simple netty server based on TCP and Netty 4.x .
 *
 * @author Microsamp | Steve
 */
@RequiredArgsConstructor
public class Server {

	private final String host;
	private final int port, threads;

	private EventLoopGroup workerGroup;
	private final Object lock = new Object();

	private final Map<Channel, Connection> channelToConnection = new ConcurrentHashMap<>();

	@Setter private Consumer<Throwable> exceptionHandler = (exception) -> {
		exception.printStackTrace();
	};

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
				new EpollEventLoopGroup(this.threads) :
				new NioEventLoopGroup(this.threads);

		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap()
					.group(this.workerGroup)
					.channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
					.localAddress(new InetSocketAddress(this.host, this.port));

			serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

				protected void initChannel(SocketChannel socketChannel) throws Exception {
					socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {

						@Override
						public void channelActive(ChannelHandlerContext ctx) throws Exception {
							Server.this.channelToConnection.put(ctx.channel(), new Connection(ctx.channel()));
						}

						@Override
						public void channelInactive(ChannelHandlerContext ctx) throws Exception {
							Server.this.channelToConnection.remove(ctx.channel());
						}

						@Override
						public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
							if (!(msg instanceof ByteBuf)) return; // we don't want to handle this, maybe it's supposed for another handler?

							ByteBuf buffer = (ByteBuf) msg;
							Server.this.channelToConnection.get(ctx.channel()).handle(PacketRegistry.getInstance().newInstance(buffer.readInt()));
						}

						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							Server.this.exceptionHandler.accept(cause);
							ctx.close();
						}

					});
				}

			});

			ChannelFuture channelFuture = serverBootstrap.bind().sync();

			synchronized (this.lock) {
				this.lock.notifyAll();
			}

			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException ex) {
			this.exceptionHandler.accept(ex);
		} finally {
			this.shutdown();
		}
	}

	/**
	 * Closes all connections synchronously.
	 */
	public void shutdown() {
		try {
			this.workerGroup.shutdownGracefully().sync();
		} catch (InterruptedException ex) {
			this.exceptionHandler.accept(ex);
		}
	}

}

package de.microsamp.example.netty.common;

import lombok.Getter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketRegistry {

	@Getter private static final PacketRegistry instance = new PacketRegistry();

	private final Map<Class<? extends Packet>, Integer> classToPacketId = new ConcurrentHashMap<>();
	private final Map<Integer, Class<? extends Packet>> packetIdToClass = new ConcurrentHashMap<>();

	public void register(Class<? extends Packet> clazz, Integer packetId) {
		if(packetId < 0) throw new IllegalArgumentException("The packet's id cannot be lower than zero.");
		if(clazz == null) throw new IllegalArgumentException("The class cannot be null");

		this.classToPacketId.put(clazz, packetId);
		this.packetIdToClass.put(packetId, clazz);
	}

	public void unregister(Class<? extends Packet> clazz) {
		if(clazz == null) throw new IllegalArgumentException("The class cannot be null");
		this.packetIdToClass.remove(this.classToPacketId.remove(clazz));
	}

	public void unregister(Integer packetId) {
		if(packetId < 0) throw new IllegalArgumentException("The packet's id cannot be lower than zero.");
		this.classToPacketId.remove(this.packetIdToClass.remove(packetId));
	}

	/**
	 * Gets the corresponding packet id of the given class. <br>
	 * If the given class is unregistered, the error code -404 will be returned.
	 *
	 * @param clazz The corresponding packet class of the wanted packetId
	 * @return The packet id (id >= 0) or an error code (-404 = not found).
	 */
	public int getPacketId(Class<? extends Packet> clazz) {
		Integer packetId = this.classToPacketId.get(clazz);
		if(packetId != null) return packetId;
		return -404;
	}

	/**
	 * Gets the corresponding packet class of the given packet id. <br>
	 * If the given id is unregistered/unknown, null will be returned.
	 *
	 * @param packetId The corresponding packet id of the wanted class
	 * @return The packet class or null if there is no class for this id.
	 */
	public Class<? extends Packet> getPacketClass(int packetId) {
		return this.packetIdToClass.get(packetId);
	}

	public <T> T newInstance(int packetId) throws InstantiationException, IllegalAccessException {
		return (T) this.newInstance(this.getPacketClass(packetId));
	}

	public <T> T newInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
		return clazz.newInstance();
	}

}

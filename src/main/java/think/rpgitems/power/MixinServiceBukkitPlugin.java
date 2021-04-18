package think.rpgitems.power;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;
import think.rpgitems.RPGItems;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class MixinServiceBukkitPlugin implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
	private final ReEntranceLock lock;
	private final ThreadLocal<byte[]> loadBuffer = new ThreadLocal<>();
	private final ClassLoader pluginClassLoader;
	public static final int BUFFER_SIZE = 1 << 12;

	private byte[] getOrCreateBuffer() {
		byte[] buffer = loadBuffer.get();
		if (buffer == null) {
			loadBuffer.set(new byte[BUFFER_SIZE]);
			buffer = loadBuffer.get();
		}
		return buffer;
	}

	private byte[] readFully(InputStream stream) {
		try {
			byte[] buffer = getOrCreateBuffer();

			int read;
			int totalLength = 0;
			while ((read = stream.read(buffer, totalLength, buffer.length - totalLength)) != -1) {
				totalLength += read;

				// Extend our buffer
				if (totalLength >= buffer.length - 1) {
					byte[] newBuffer = new byte[buffer.length + BUFFER_SIZE];
					System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
					buffer = newBuffer;
				}
			}

			final byte[] result = new byte[totalLength];
			System.arraycopy(buffer, 0, result, 0, totalLength);
			return result;
		} catch (Throwable t) {
			return new byte[0];
		}
	}


	private static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	public MixinServiceBukkitPlugin() {
		lock = new ReEntranceLock(1);
		pluginClassLoader = RPGItems.plugin.getClass().getClassLoader();
	}

	private static final String[] RESERVED_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

	private static final String getResourceName(String name){
		if (name.indexOf('.') == -1) {
			for (final String reservedName : RESERVED_NAMES) {
				if (name.toUpperCase(Locale.ENGLISH).startsWith(reservedName)) {
					return getResourceName("_" + name);
				}
			}
		}
		return name.replace('.', '/').concat(".class");
	}

	public byte[] getClassBytes(String name, String transformedName) throws IOException {
		try {
			return getClassBytes(name, true);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
		InputStream classStream = null;
		try {
			final String resourcePath = getResourceName(name);
			final URL classResource = pluginClassLoader.getResource(resourcePath);

			if (classResource == null) {
				return null;
			}
			classStream = classResource.openStream();

			final byte[] data = readFully(classStream);
			return data;
		} finally {
			closeSilently(classStream);
		}
	}

	@Override
	public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
		return getClassNode(name, true);
	}

	@Override
	public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
		ClassReader reader = new ClassReader(getClassBytes(name, runTransformers));
		ClassNode node = new ClassNode();
		reader.accept(node, 0);
		return node;
	}

	@Override
	public URL[] getClassPath() {
		// Mixin 0.7.x only uses getClassPath() to find itself; we implement CodeSource correctly,
		// so this is unnecessary.
		return new URL[0];
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		return pluginClassLoader.loadClass(name);
	}

	@Override
	public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, pluginClassLoader);
	}

	@Override
	public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, pluginClassLoader);
	}

	@Override
	public String getName() {
		return "RPGItems";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void prepare() {

	}

	@Override
	public MixinEnvironment.Phase getInitialPhase() {
		return MixinEnvironment.Phase.PREINIT;
	}

	@Override
	public void init() {
	}

	@Override
	public void beginPhase() {

	}

	@Override
	public void checkEnv(Object bootSource) {

	}

	@Override
	public ReEntranceLock getReEntranceLock() {
		return lock;
	}

	@Override
	public IClassProvider getClassProvider() {
		return this;
	}

	@Override
	public IClassBytecodeProvider getBytecodeProvider() {
		return this;
	}

	@Override
	public ITransformerProvider getTransformerProvider() {
		return this;
	}

	@Override
	public IClassTracker getClassTracker() {
		return this;
	}

	@Override
	public IMixinAuditTrail getAuditTrail() {
		return null;
	}

	@Override
	public Collection<String> getPlatformAgents() {
		return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
	}

	@Override
	public IContainerHandle getPrimaryContainer() {
		try {
			return new ContainerHandleURI(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<IContainerHandle> getMixinContainers() {
		return Collections.emptyList();
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return pluginClassLoader.getResourceAsStream(name);
	}

	@Override
	public void registerInvalidClass(String className) {

	}

	@Override
	public boolean isClassLoaded(String className) {
		try {
			Field classesField = pluginClassLoader.getClass().getDeclaredField("classes");
			classesField.setAccessible(true);
			Map<String, Class<?>> classes = (Map<String, Class<?>>) classesField.get(pluginClassLoader);
			return classes.containsKey(className);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getClassRestrictions(String className) {
		return "";
	}

	@Override
	public Collection<ITransformer> getTransformers() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ITransformer> getDelegatedTransformers() {
		return Collections.emptyList();
	}

	@Override
	public void addTransformerExclusion(String name) {

	}

	@Override
	public String getSideName() {
		return "SERVER";
	}

	@Override
	public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
		return MixinEnvironment.CompatibilityLevel.JAVA_8;
	}

	@Override
	public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
		return MixinEnvironment.CompatibilityLevel.JAVA_11;
	}
}

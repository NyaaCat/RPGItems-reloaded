package think.rpgitems.power;

// A stripped version from guava 6f22af40 as ClassPath contained in guava 21 suffers from https://github.com/google/guava/issues/2152
/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import cat.nyaa.nyaacore.NyaaCoreLoader;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.google.common.reflect.Reflection;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;
import static java.util.logging.Level.WARNING;

public final class ClassPathUtils {

    /**
     * Separator for the Class-Path manifest attribute value in jar files.
     */
    private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR =
            Splitter.on(" ").omitEmptyStrings();
    private static final String CLASS_FILE_NAME_EXTENSION = ".class";
    private final ImmutableSet<ResourceInfo> resources;

    private ClassPathUtils(ImmutableSet<ResourceInfo> resources) {
        this.resources = resources;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T>[] scanSubclasses(File file, ClassLoader classLoader, String pack, Class<T> clazz) {
        try {
            Set<ClassPathUtils.ClassInfo> classInfos = from(file, classLoader).getAllClasses();
            return (Class<? extends T>[]) loadClassesInPackage(pack, classInfos)
                                                  .filter(c -> c != null && clazz.isAssignableFrom(c))
                                                  .toArray(Class<?>[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?>[] scanClassesWithAnnotations(File file, ClassLoader classLoader, String pack, Class<? extends Annotation> annotation) {
        try {
            Set<ClassPathUtils.ClassInfo> classInfos = from(file, classLoader).getAllClasses();
            return loadClassesInPackage(pack, classInfos)
                           .filter(c -> c != null && c.getAnnotation(annotation) != null)
                           .toArray(Class<?>[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Class<? extends T>[] scanSubclasses(Plugin plugin, String pack, Class<T> clazz) {
        try {
            return scanSubclasses(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()), plugin.getClass().getClassLoader(), pack, clazz);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?>[] scanClassesWithAnnotations(Plugin plugin, String pack, Class<? extends Annotation> annotation) {
        try {
            return scanClassesWithAnnotations(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()), plugin.getClass().getClassLoader(), pack, annotation);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<? extends Class<?>> loadClassesInPackage(String pack, Set<ClassInfo> classInfos) {
        return classInfos
                       .stream()
                       .filter(c -> pack == null || c.getPackageName().startsWith(pack))
                       .map(ClassInfo::load);
    }

    /**
     * Returns a {@code ClassPathUtils} representing all classes and resources loadable from {@code
     * classloader} and its ancestor class loaders.
     *
     * <p><b>Warning:</b> {@code ClassPathUtils} can find classes and resources only from:
     *
     * <ul>
     * <li>{@link URLClassLoader} instances' {@code file:} URLs
     * <li>the {@linkplain ClassLoader#getSystemClassLoader() system class loader}. To search the
     * system class loader even when it is not a {@link URLClassLoader} (as in Java 9), {@code
     * ClassPathUtils} searches the files from the {@code java.class.path} system property.
     * </ul>
     *
     * @throws IOException if the attempt to read class path resources (jar files or directories)
     *                     failed.
     */
    public static ClassPathUtils from(File file, ClassLoader classloader) throws IOException {
        DefaultScanner scanner = new DefaultScanner();
        scanner.scan(file, classloader);
        return new ClassPathUtils(scanner.getResources());
    }

    private static Logger getLogger() {
        return NyaaCoreLoader.getInstance().getLogger();
    }

    @VisibleForTesting
    static String getClassName(String filename) {
        int classNameEnd = filename.length() - CLASS_FILE_NAME_EXTENSION.length();
        return filename.substring(0, classNameEnd).replace('/', '.');
    }

    static File toFile(URL url) {
        checkArgument(url.getProtocol().equals("file"));
        try {
            return new File(url.toURI()); // Accepts escaped characters like %20.
        } catch (URISyntaxException e) { // URL.toURI() doesn't escape chars.
            return new File(url.getPath()); // Accepts non-escaped chars like space.
        }
    }

    /**
     * Returns all classes loadable from the current class path.
     *
     * @since 16.0
     */
    public Set<ClassInfo> getAllClasses() {
        return resources.stream().filter(ClassInfo.class::isInstance).map(ClassInfo.class::cast).collect(Collectors.toSet());
    }

    /**
     * Represents a class path resource that can be either a class file or any other resource file
     * loadable from the class path.
     *
     * @since 14.0
     */
    @Beta
    public static class ResourceInfo {
        final ClassLoader loader;
        private final String resourceName;

        ResourceInfo(String resourceName, ClassLoader loader) {
            this.resourceName = checkNotNull(resourceName);
            this.loader = checkNotNull(loader);
        }

        static ResourceInfo of(String resourceName, ClassLoader loader) {
            if (resourceName.endsWith(CLASS_FILE_NAME_EXTENSION)) {
                return new ClassInfo(resourceName, loader);
            } else {
                return new ResourceInfo(resourceName, loader);
            }
        }

        @Override
        public int hashCode() {
            return resourceName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ResourceInfo) {
                ResourceInfo that = (ResourceInfo) obj;
                return resourceName.equals(that.resourceName) && loader == that.loader;
            }
            return false;
        }

        // Do not change this arbitrarily. We rely on it for sorting ResourceInfo.
        @Override
        public String toString() {
            return resourceName;
        }
    }

    /**
     * Represents a class that can be loaded through {@link #load}.
     *
     * @since 14.0
     */
    @Beta
    public static final class ClassInfo extends ResourceInfo {
        private final String className;

        ClassInfo(String resourceName, ClassLoader loader) {
            super(resourceName, loader);
            this.className = getClassName(resourceName);
        }

        /**
         * Returns the package name of the class, without attempting to load the class.
         *
         * <p>Behaves identically to {@link Package#getName()} but does not require the class (or
         * package) to be loaded.
         */
        public String getPackageName() {
            return Reflection.getPackageName(className);
        }

        /**
         * Returns the fully qualified name of the class.
         *
         * <p>Behaves identically to {@link Class#getName()} but does not require the class to be
         * loaded.
         */
        public String getName() {
            return className;
        }

        /**
         * Loads (but doesn't link or initialize) the class.
         *
         * @throws LinkageError when there were errors in loading classes that this class depends on.
         *                      For example, {@link NoClassDefFoundError}.
         */
        @SuppressWarnings("unchecked")
        public Class<?> load() {
            try {
                byte[] classBytes = getClassBytes(className);
                classBytes = RPGItems.transformer.transformClassBytes(className, className, classBytes);
                getLogger().warning("");
                getLogger().warning("");
                getLogger().warning("");
                getLogger().warning(className);
                getLogger().warning(getResourceName(className));
                // getLogger().warning("");
                // getLogger().warning(Base64.getEncoder().encodeToString(classBytes));
                // getLogger().warning("");

                Field jarField = loader.getClass().getDeclaredField("jar");
                jarField.setAccessible(true);
                JarFile jar = (JarFile) jarField.get(loader);

                Field fileField = loader.getClass().getDeclaredField("file");
                fileField.setAccessible(true);
                File file = (File) fileField.get(loader);


                Field classesField = loader.getClass().getDeclaredField("classes");
                classesField.setAccessible(true);
                Map<String, Class<?>> classes = (Map<String, Class<?>>) classesField.get(loader);
                if (classes.containsKey(className)) {
                    getLogger().severe("containsKey");
                    getLogger().severe("");
                    getLogger().severe("");
                    getLogger().severe("");
                    return classes.get(className);
                }
                getLogger().warning("defineClass");
                getLogger().warning("");
                getLogger().warning("");
                getLogger().warning("");
                Method defineClass = loader.getClass().getSuperclass().getSuperclass().getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
                defineClass.setAccessible(true);
                Class<?> result = (Class<?>) defineClass.invoke(loader, className, classBytes, 0, Objects.requireNonNull(classBytes).length, new CodeSource(file.toURI().toURL(), jar.getJarEntry(getResourceName(className)).getCodeSigners()));
                classes.put(className, result);
                return result;
            } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                // Shouldn't happen, since the class name is read from the class path.
                throw new IllegalStateException(e);
            }
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

        public byte[] getClassBytes(String name) throws IOException {
            InputStream classStream = null;
            try {
                final String resourcePath = getResourceName(name);
                final URL classResource = loader.getResource(resourcePath);

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

        private final ThreadLocal<byte[]> loadBuffer = new ThreadLocal<>();
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

        @Override
        public String toString() {
            return className;
        }
    }

    /**
     * Abstract class that scans through the class path represented by a {@link ClassLoader} and calls
     * {@link #scanDirectory} and {@link #scanJarFile} for directories and jar files on the class path
     * respectively.
     */
    abstract static class Scanner {

        // We only scan each file once independent of the classloader that resource might be associated
        // with.
        private final Set<File> scannedUris = Sets.newHashSet();

        /**
         * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
         * to <a
         * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
         * File Specification</a>. If {@code manifest} is null, it means the jar file has no manifest,
         * and an empty set will be returned.
         */
        static ImmutableSet<File> getClassPathFromManifest(
                File jarFile, Manifest manifest) {
            if (manifest == null) {
                return ImmutableSet.of();
            }
            ImmutableSet.Builder<File> builder = ImmutableSet.builder();
            String classpathAttribute =
                    manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
            if (classpathAttribute != null) {
                for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
                    URL url;
                    try {
                        url = getClassPathEntry(jarFile, path);
                    } catch (MalformedURLException e) {
                        // Ignore bad entry
                        getLogger().warning("Invalid Class-Path entry: " + path);
                        continue;
                    }
                    if (url.getProtocol().equals("file")) {
                        builder.add(toFile(url));
                    }
                }
            }
            return builder.build();
        }

        static ImmutableMap<File, ClassLoader> getClassPathEntries(ClassLoader classloader) {
            LinkedHashMap<File, ClassLoader> entries = Maps.newLinkedHashMap();
            // Search parent first, since it's the order ClassLoader#loadClass() uses.
            ClassLoader parent = classloader.getParent();
            if (parent != null) {
                entries.putAll(getClassPathEntries(parent));
            }
            for (URL url : getClassLoaderUrls(classloader)) {
                if (url.getProtocol().equals("file")) {
                    File file = toFile(url);
                    if (!entries.containsKey(file)) {
                        entries.put(file, classloader);
                    }
                }
            }
            return ImmutableMap.copyOf(entries);
        }

        private static ImmutableList<URL> getClassLoaderUrls(ClassLoader classloader) {
            if (classloader instanceof URLClassLoader) {
                return ImmutableList.copyOf(((URLClassLoader) classloader).getURLs());
            }
            if (classloader.equals(ClassLoader.getSystemClassLoader())) {
                return parseJavaClassPath();
            }
            return ImmutableList.of();
        }

        /**
         * Returns the URLs in the class path specified by the {@code java.class.path} {@linkplain
         * System#getProperty system property}.
         */
        static ImmutableList<URL> parseJavaClassPath() {
            ImmutableList.Builder<URL> urls = ImmutableList.builder();
            for (String entry : Splitter.on(Objects.requireNonNull(PATH_SEPARATOR.value())).split(Objects.requireNonNull(JAVA_CLASS_PATH.value()))) {
                try {
                    try {
                        urls.add(new File(entry).toURI().toURL());
                    } catch (SecurityException e) { // File.toURI checks to see if the file is a directory
                        urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
                    }
                } catch (MalformedURLException e) {
                    getLogger().log(WARNING, "malformed classpath entry: " + entry, e);
                }
            }
            return urls.build();
        }

        /**
         * Returns the absolute uri of the Class-Path entry value as specified in <a
         * href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">JAR
         * File Specification</a>. Even though the specification only talks about relative urls,
         * absolute urls are actually supported too (for example, in Maven surefire plugin).
         */
        static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
            return new URL(jarFile.toURI().toURL(), path);
        }

        /**
         * Called when a directory is scanned for resource files.
         */
        protected abstract void scanDirectory(ClassLoader loader, File directory) throws IOException;

        /**
         * Called when a jar file is scanned for resource entries.
         */
        protected abstract void scanJarFile(ClassLoader loader, JarFile file) throws IOException;

        final void scan(File file, ClassLoader classloader) throws IOException {
            if (scannedUris.add(file.getCanonicalFile())) {
                scanFrom(file, classloader);
            }
        }

        private void scanFrom(File file, ClassLoader classloader) throws IOException {
            try {
                if (!file.exists()) {
                    return;
                }
            } catch (SecurityException e) {
                getLogger().warning("Cannot access " + file + ": " + e);
                // TODO(emcmanus): consider whether to log other failure cases too.
                return;
            }
            if (file.isDirectory()) {
                scanDirectory(classloader, file);
            } else {
                scanJar(file, classloader);
            }
        }

        private void scanJar(File file, ClassLoader classloader) throws IOException {
            JarFile jarFile;
            try {
                jarFile = new JarFile(file);
            } catch (IOException e) {
                // Not a jar file
                return;
            }
            try {
                for (File path : getClassPathFromManifest(file, jarFile.getManifest())) {
                    scan(path, classloader);
                }
                scanJarFile(classloader, jarFile);
            } finally {
                try {
                    jarFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static final class DefaultScanner extends Scanner {
        private final SetMultimap<ClassLoader, String> resources =
                MultimapBuilder.hashKeys().linkedHashSetValues().build();

        ImmutableSet<ResourceInfo> getResources() {
            ImmutableSet.Builder<ResourceInfo> builder = ImmutableSet.builder();
            for (Map.Entry<ClassLoader, String> entry : resources.entries()) {
                builder.add(ResourceInfo.of(entry.getValue(), entry.getKey()));
            }
            return builder.build();
        }

        @Override
        protected void scanJarFile(ClassLoader classloader, JarFile file) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
                    continue;
                }
                resources.get(classloader).add(entry.getName());
            }
        }

        @Override
        protected void scanDirectory(ClassLoader classloader, File directory) throws IOException {
            Set<File> currentPath = new HashSet<>();
            currentPath.add(directory.getCanonicalFile());
            scanDirectory(directory, classloader, "", currentPath);
        }

        /**
         * Recursively scan the given directory, adding resources for each file encountered. Symlinks
         * which have already been traversed in the current tree path will be skipped to eliminate
         * cycles; otherwise symlinks are traversed.
         *
         * @param directory     the root of the directory to scan
         * @param classloader   the classloader that includes resources found in {@code directory}
         * @param packagePrefix resource path prefix inside {@code classloader} for any files found
         *                      under {@code directory}
         * @param currentPath   canonical files already visited in the current directory tree path, for
         *                      cycle elimination
         */
        private void scanDirectory(
                File directory, ClassLoader classloader, String packagePrefix, Set<File> currentPath)
                throws IOException {
            File[] files = directory.listFiles();
            if (files == null) {
                getLogger().warning("Cannot read directory " + directory);
                // IO error, just skip the directory
                return;
            }
            for (File f : files) {
                String name = f.getName();
                if (f.isDirectory()) {
                    File deref = f.getCanonicalFile();
                    if (currentPath.add(deref)) {
                        scanDirectory(deref, classloader, packagePrefix + name + "/", currentPath);
                        currentPath.remove(deref);
                    }
                } else {
                    String resourceName = packagePrefix + name;
                    if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
                        resources.get(classloader).add(resourceName);
                    }
                }
            }
        }
    }
}
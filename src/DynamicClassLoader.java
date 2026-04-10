import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DynamicClassLoader is a utility class that loads classes from a specified directory.
 * It can be used to load classes dynamically at runtime, allowing for greater flexibility
 * in applications that require loading of external classes or plugins.
 */
public class DynamicClassLoader {

    private final List<Class<?>> loadedClasses = new ArrayList<>();

    /**
     * Constructor for DynamicClassLoader.
     * @param rootDirectoryPath The path to the directory containing class files.
     * @throws IllegalArgumentException if the provided path is not a valid directory.
     */
    public DynamicClassLoader(String rootDirectoryPath) {
        // Validate the directory path
        File rootDir = new File(rootDirectoryPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid folder: " + rootDirectoryPath);
        }

        // Convert the directory to a URL
        URL[] url = new URL[1];
        try {
            url[0] = rootDir.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid directory: " + rootDirectoryPath, e);
        }

        // Collect all class files in the directory and its subdirectories
        List<File> classFiles = new ArrayList<>();
        collectClassFiles(rootDir, classFiles);

        // Load the classes using a URLClassLoader, ensuring to close it properly
        // to avoid memory leaks.
        try (URLClassLoader loader = new URLClassLoader(url)) {
            // Load each class file
            for (File classFile : classFiles) {
                String className = getClassName(rootDir, classFile);
                try {
                    Class<?> clazz = loader.loadClass(className);
                    loadedClasses.add(clazz);
                } catch (ClassNotFoundException e) {
                    System.err.println("Failed to load: " + className);
                } catch (LinkageError e) {
                    // Skip classes whose file path implies a different binary name (or otherwise cannot be linked).
                    System.err.println("Skipping invalid class entry: " + className + " (" + e.getMessage() + ")");
                }
            }
        }
        catch (java.io.IOException e) {
            throw new RuntimeException("Failed to close class loader", e);
        }
    }

    /**
     * Recursively collects all class files in the specified directory.
     * @param dir The directory to search for class files.
     * @param classFiles The list to store found class files.
     */
    private void collectClassFiles(File dir, List<File> classFiles) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                collectClassFiles(file, classFiles);
            } else if (file.getName().endsWith(".class")) {
                classFiles.add(file);
            }
        }
    }

    /**
     * Converts a class file path to a class name.
     * @param rootDir The root directory where the classes are loaded from.
     * @param classFile The class file to convert.
     * @return The fully qualified class name.
     */
    private String getClassName(File rootDir, File classFile) {
        String relativePath = classFile.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
        return relativePath.replace(File.separatorChar, '.').replaceAll("\\.class$", "");
    }

    /**
     * Returns a list of all loaded classes.
     * @return A list of loaded classes.
     */
    public List<Class<?>> getAllClasses() {
        return new ArrayList<>(loadedClasses);
    }

    /**
     * Returns a list of subclasses of the specified parent type.
     * @param parentType The parent class to find subclasses of.
     * @param <T> The type of the parent class.
     * @return A list of subclasses of the specified parent type.
     */
    public <T> List<Class<? extends T>> getSubtypesOf(Class<T> parentType) {
        List<Class<? extends T>> results = new ArrayList<>();
        for (Class<?> clazz : loadedClasses) {
            if (parentType.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                @SuppressWarnings("unchecked")
                Class<? extends T> casted = (Class<? extends T>) clazz;
                results.add(casted);
            }
        }
        return results;
    }
}
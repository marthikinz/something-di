package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class ClassLoaderUtil {
	private static List<Class<?>> getClassList(Class<?> mainClass)
			throws IOException, ClassNotFoundException {
		StringTokenizer tok = getPackage(mainClass);

		List<Class<?>> classes = new ArrayList<Class<?>>();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		while (tok.hasMoreElements()) {
			String packageName = tok.nextToken();
			String path = packageName.replace('.', '/');
			Enumeration<URL> resources = classLoader.getResources(path);
			List<File> dirs = new ArrayList<File>();
			while (resources.hasMoreElements()) {
				URL resource = (URL) resources.nextElement();
				dirs.add(new File(resource.getFile()));
			}
			for (File directory : dirs) {
				classes.addAll(findClasses(directory, packageName));
			}
			
		}
		return classes;
	}

	public static Class<?>[] getClasses(Class<?> mainClass)
			throws ClassNotFoundException, IOException {
		return getClassList(mainClass).toArray(new Class[getClassList(mainClass).size()]);
	}

	public static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
				classes.add(Class.forName(className));
			}
		}
		return classes;
	}
	
	public static StringTokenizer getPackage(Class<?> mainClass) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = mainClass.getPackage().getName();
		StringTokenizer tok = new StringTokenizer(path, ".");
		path = tok.nextToken();
		Enumeration<URL> resources = classLoader.getResources(path);
		URL resource = (URL) resources.nextElement();
		File file = new File(resource.getFile());
		InputStream is = new FileInputStream(file.getParent() + "/setting.properties");
		Properties p = System.getProperties();
		p.load(is);
		return new StringTokenizer(p.get("package").toString(),",");
				
		
	}
}

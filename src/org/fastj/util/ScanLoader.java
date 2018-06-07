package org.fastj.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.fastj.log.LogUtil;

public class ScanLoader {
	
	private String packageName;
	private String pathPattern;
	private String namePattern;
	private Set<Class<?>> classes = new HashSet<>();
	private ClassFilter filter;
	
	public static ScanLoader ins(String packageName){
		ScanLoader sl = new ScanLoader();
		sl.packageName = packageName;
		sl.pathPattern = packageName.replace(".", "/");
		return sl;
	}
	
	public ScanLoader filter(Class<?> superClass, Class<? extends Annotation> anno) {
		filter = new ClassFilter(superClass, anno);
		return this;
	}

	public Set<Class<?>> scan() {
		if (packageName == null || packageName.trim().isEmpty()) {
			packageName = "";
		}

		if (packageName.length() > 0 && packageName.charAt(packageName.length() - 1) != '.') {
			packageName = packageName + ".";
		}

		final Set<String> packagePaths = getPackagePaths(packageName);
		for (String classPath : packagePaths) {
			try {
				classPath = URLDecoder.decode(classPath, Charset.defaultCharset().name());
			} catch (UnsupportedEncodingException e) {
			}
			search(classPath, packageName, filter);
		}

		return classes;
	}

	private void search(String path, String packageName, Filter<Class<?>> classFilter) {
		int index = path.lastIndexOf(".jar!");
		if (index != -1) {
			path = path.substring(0, index + ".jar".length());
			path = path.replace("file:", "");
			searchJar(new File(path), packageName, classFilter);
		} else {
			processFile(path, new File(path), packageName, classFilter);
		}
	}

	private void searchJar(File file, String packageName, Filter<Class<?>> classFilter) {
		try {
			for (JarEntry entry : Collections.list(new JarFile(file).entries())) {
				if (entry.getName().endsWith(".class")) {
					final String className = entry.getName().replace("/", ".").replace(".class", "");
					checkClass(className, packageName, classes, classFilter);
				}
			}
		} catch (Exception ex) {
			LogUtil.error("Search jar fail: {}", ex.getMessage());
		}
	}

	private void processFile(String classPath, File file, String packageName, Filter<Class<?>> classFilter) {
		if (file.isDirectory()) {
			searchSubs(classPath, file, packageName, classFilter);
		} else if (file.getName().endsWith(".class")) {
			processClass(classPath, file, packageName, classFilter);
		} else if (file.getName().endsWith(".jar")) {
			searchJar(file, packageName, classFilter);
		}
	}

	private void searchSubs(String classPath, File directory, String packageName, Filter<Class<?>> classFilter) {
		File[] fs = directory.listFiles(fileFilter);
		if (fs != null)
		for (File file : fs) {
			processFile(classPath, file, packageName, classFilter);
		}
	}

	private void processClass(String classPath, File file, String packageName, Filter<Class<?>> classFilter) {
		if (!classPath.endsWith(File.separator)) {
			classPath += File.separator;
		}

		String path = file.getAbsolutePath();
		if (isEmpty(packageName)) {
			path = removePrefix(path, classPath);
		}
		final String filePathWithDot = path.replace(File.separator, ".");

		int subIndex = -1;
		if ((subIndex = filePathWithDot.indexOf(packageName)) != -1) {
			final int endIndex = filePathWithDot.lastIndexOf(".class");

			final String className = filePathWithDot.substring(subIndex, endIndex);
			checkClass(className, packageName, classes, classFilter);
		}
	}

	private void checkClass(String className, String packageName, Set<Class<?>> classes, Filter<Class<?>> classFilter) {
		if (className.startsWith(packageName)) {
			try {
				final Class<?> clazz = Class.forName(className, false, getClassLoader());
				if (classFilter == null || classFilter.accept(clazz)) {
					classes.add(clazz);
				}
			} catch (Throwable ex) {
				// Pass Load Error.
			}
		}
	}

	public String removePrefix(String str, String prefix) {
		if (isEmpty(str) || isEmpty(prefix)) {
			return str;
		}

		if (str.startsWith(prefix)) {
			return str.substring(prefix.length());
		}
		return str;
	}

	public boolean isEmpty(CharSequence str) {
		return str == null || str.length() == 0;
	}

	public Set<String> getPackagePaths(String packageName) {
		String packagePath = packageName.replace(".", "/");
		Enumeration<URL> resources;
		try {
			resources = getClassLoader().getResources(packagePath);
		} catch (IOException e) {
			throw new RuntimeException("Get package resources fail");
		}
		Set<String> paths = new HashSet<String>();
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			File f = new File(url.getFile());
			try {
				paths.add(f.getCanonicalPath());
			} catch (IOException e) {
			}
		}
		return paths;
	}

	public ClassLoader getClassLoader() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = ScanLoader.class.getClassLoader();
			if (null == classLoader) {
				classLoader = ClassLoader.getSystemClassLoader();
			}
		}
		return classLoader;
	}
	
	private final boolean matchPackage(String path)
	{
		String kpar[] = pathPattern.split("/");
		String rpar[] = path.split("/");
		
		if (kpar.length > rpar.length) return false;
		
		for (int i = 0;i < kpar.length; i++)
		{
			String o1 = kpar[i];
			String o2 = rpar[i];
			
			if (o1.equals(o2) || o1.startsWith("{")) continue;
			return false;
		}
		
		return true;
	}

	private FileFilter fileFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			boolean pass = true;
			if (pathname.getName().endsWith(".class")) {
				if (namePattern != null) {
					String cn = pathname.getName().split("\\.")[0];
					pass = pass && cn.matches(namePattern);
				}
			}
			
			return pass || pathname.isDirectory() || pathname.getName().endsWith(".jar");
		}
	};
	
	public String namePattern() {
		return namePattern;
	}

	public ScanLoader namePattern(String namePattern) {
		this.namePattern = namePattern;
		return this;
	}

	public static interface Filter<T> {
		boolean accept(T t);
	}

	public static class ClassFilter implements Filter<Class<?>> {

		private Class<?> superc;
		private Class<? extends Annotation> anno;

		public ClassFilter(Class<?> superc, Class<? extends Annotation> anno) {
			this.anno = anno;
			this.superc = superc;
		}

		public boolean accept(Class<?> clazz) {
			boolean pass = true;
			
			if (superc != null) {
				pass = pass && superc.isAssignableFrom(clazz) && !superc.equals(clazz);
			}

			if (anno != null) {
				pass = pass && clazz.isAnnotationPresent(anno);
			}

			return pass;
		}
	}
}

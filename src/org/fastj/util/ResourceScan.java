package org.fastj.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
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

public class ResourceScan {

	private String path;
	private String namePattern;
	private Set<URI> classes = new HashSet<>();
	private FilenameFilter filter;

	public static ResourceScan ins(String path) {
		ResourceScan sl = new ResourceScan();
		sl.path = path;
		return sl;
	}

	public ResourceScan filter(String namePattern) {
		filter = nameFilter;
		return this;
	}

	public Set<URI> scan() {
		final Set<String> packagePaths = getPackagePaths();
		for (String fPath : packagePaths) {
			try {
				fPath = URLDecoder.decode(fPath, Charset.defaultCharset().name());
			} catch (UnsupportedEncodingException e) {
			}
			search(fPath);
		}

		return classes;
	}

	private void search(String path) {
		int index = path.lastIndexOf(".jar!");
		if (index != -1) {
			path = path.substring(0, index + ".jar".length());
			path = path.replace("file:", "");
			searchJar(new File(path));
		} else {
			processFile(new File(path));
		}
	}

	private void searchJar(File file) {
		try (JarFile jar = new JarFile(file)){
			for (JarEntry entry : Collections.list(jar.entries())) {
				if (filter.accept(null, entry.getName())) {
				}
			}
		} catch (Exception ex) {
			LogUtil.error("Search jar fail: {}", ex.getMessage());
		}
	}

	private void processFile(File file) {
		if (file.isDirectory()) {
			searchSubs(file);
		} else if (file.getName().endsWith(".jar")) {
			searchJar(file);
		} else {
			if (filter.accept(null, file.getName())) {
				classes.add(file.toURI());
			}
		}
	}

	private void searchSubs(File directory) {
		File[] fs = directory.listFiles(fileFilter);
		if (fs != null)
			for (File file : fs) {
				processFile(file);
			}
	}

	public boolean isEmpty(CharSequence str) {
		return str == null || str.length() == 0;
	}

	private Set<String> getPackagePaths() {
		Enumeration<URL> resources;
		try {
			resources = getClassLoader().getResources(path);
		} catch (IOException e) {
			throw new RuntimeException("Get resources fail");
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

	private FileFilter fileFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			boolean pass = true;
			if (pathname.isFile()) {
				pass = filter.accept(null, pathname.getName());
			}

			return pass || pathname.isDirectory() || pathname.getName().endsWith(".jar");
		}
	};

	private FilenameFilter nameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return namePattern == null || name.endsWith(namePattern) || name.matches(namePattern);
		}
	};

	public String namePattern() {
		return namePattern;
	}

	public ResourceScan namePattern(String namePattern) {
		this.namePattern = namePattern;
		return this;
	}

}

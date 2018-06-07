package org.fastj.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SysUtil {

	public static ClassLoader initClassLoader(File... libs) {

		if (libs == null)
			return SysUtil.class.getClassLoader();

		List<File> jars = new ArrayList<>(31);
		for (File lib : libs) {
			search(jars, lib, ".jar");
		}
		List<URL> urls = new ArrayList<>(jars.size());
		for (File f : jars) {
			try {
				urls.add(f.toURI().toURL());
			} catch (MalformedURLException e) {
			}
		}

		URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[urls.size()]), SysUtil.class.getClassLoader());
		return ucl;
	}

	public static List<File> search(File dir, String suffix) {
		List<File> rlt = new ArrayList<>(31);
		search(rlt, dir, suffix);
		return rlt;
	}

	public static void search(List<File> collection, File dir, String suffix) {

		if (dir == null || !dir.exists()) {
			return;
		}

		File[] fs = dir.listFiles();
		if (fs == null) {
			return;
		}

		for (File f : fs) {
			if (f.isDirectory()) {
				search(collection, f, suffix);
			} else {
				if (f.getName().endsWith(suffix)) {
					collection.add(f);
				}
			}
		}
	}

}

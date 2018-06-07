package org.fastj.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileLoggor extends Loggor {

	private boolean buffered = false;
	private File logfile = null;
	private long maxSize = 20 * 1024 * 1024;

	public FileLoggor(String file, long maxSize) throws IOException {
		this(file, maxSize, true);
	}

	public FileLoggor(String file, long maxSize, boolean buff) throws IOException {
		this.buffered = buff;
		logfile = new File(file);
		if (logfile.getParentFile() != null && !logfile.getParentFile().exists()) {
			logfile.getParentFile().mkdirs();
		}
		if (maxSize > 1024 * 1024) {
			this.maxSize = maxSize;
		}
		ps = getPS();
	}

	public void close() {
		ps.close();
		ps = null;
	}

	@Override
	protected void postProcess() {
		if (logfile.length() > maxSize) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File dir = logfile.getParentFile();
			String name = logfile.getName();
			String nname = name + "." + sdf.format(new Date());
			logfile.renameTo(new File(dir, nname));
			logfile = new File(dir, name);
			try {
				ps = getPS();
			} catch (IOException e) {
				e.printStackTrace();
				ps = null;
			}
		}
	}

	private PrintStream getPS() throws IOException {
		return buffered ? new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile)), true) : new PrintStream(new FileOutputStream(logfile), true);
	}
}

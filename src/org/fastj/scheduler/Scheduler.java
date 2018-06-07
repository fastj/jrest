package org.fastj.scheduler;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fastj.log.LogUtil;

public class Scheduler {
	
	private static ExecutorService execService = Executors.newCachedThreadPool();
	private static HashMap<String, JobHolder> jobholds = new HashMap<>();
	private static JobTask task = new JobTask();
	
	static {
		Timer t = new Timer(true);
		Calendar c = Calendar.getInstance();
		int m = c.get(Calendar.MINUTE);
		c.set(Calendar.MINUTE, m + 1);
		c.set(Calendar.SECOND, 0);
		t.scheduleAtFixedRate(task, c.getTime(), 60000);
		LogUtil.trace("Scheduler[M] started.");
	}
	
	public static void registJob(String key, Runnable job, Invoker invoker) {
		JobHolder jh = new JobHolder();
		jh.key = key;
		jh.invoker = invoker;
		jh.job = job;
		task.jobs.add(jh);
		jobholds.put(key, jh);
	}
	
	public static void remove(String key) {
		JobHolder jh = jobholds.remove(key);
		if (jh != null) {
			jh.invoker.off();
		}
	}
	
	public static void execute(Runnable r) {
		execService.execute(new Runnable() {
			public void run() {
				try {
					r.run();
				} catch (Throwable e) {
					LogUtil.error("Scheduler[M] Run task exception", e);
				}
			}
		});
	}
	
	static class JobHolder {
		String key;
		Runnable job;
		Invoker invoker;
	}
	
	static class JobTask extends TimerTask {
		
		LinkedList<JobHolder> jobs = new LinkedList<>();
		
		public void addJob(JobHolder job) {
			synchronized (jobs) {
				jobs.add(job);
			}
		}
		
		public void run() {
			Calendar c = Calendar.getInstance();
			int h = c.get(Calendar.HOUR_OF_DAY);
			int m = c.get(Calendar.MINUTE);
			int tp = h * 60 + m;
			synchronized (jobs) {
				jobs.removeIf(job -> job.invoker.isOff());
				jobs.forEach(holder -> {
					if (holder.invoker.take(tp)) {
						execService.execute(new Runnable() {
							public void run() {
								try {
									holder.job.run();
								} catch (Throwable e) {
									LogUtil.error("Task[{}] exception", e, holder.key);
								}
							}
						});
					}
				});
			}
			
		}
		
	}
}

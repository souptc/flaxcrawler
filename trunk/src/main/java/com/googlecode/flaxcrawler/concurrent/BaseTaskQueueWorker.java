package com.googlecode.flaxcrawler.concurrent;

import org.apache.log4j.Logger;

/**
 * Base task queue worker class. Better inherit from this class neither from the interface.
 */
public abstract class BaseTaskQueueWorker implements TaskQueueWorker {

    private final static int DEFAULT_STOP_TIMEOUT = 10000;
    private Logger log = Logger.getLogger(this.getClass());
    private TaskQueue taskQueue;
    private Thread workerThread;
    private boolean started;
    private final Object syncRoot = new Object();

    public BaseTaskQueueWorker() {
        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                doWorkLoop();
            }
        });

        workerThread.setDaemon(true);
    }

    @Override
    public void setTaskQueue(TaskQueue taskQueue) {
        log.info(workerThread.getName() + " set task queue");
        this.taskQueue = taskQueue;
    }

    @Override
    public void start() {
        synchronized (syncRoot) {
            if (!started) {
                started = true;
                log.info(workerThread.getName() + " is started");
                workerThread.start();
            }
        }
    }

    @Override
    public void stop() {
        synchronized (syncRoot) {
            try {
                started = false;
                log.info(workerThread.getName() + " is joined, waiting until it stops");
                workerThread.join(DEFAULT_STOP_TIMEOUT);
                if (workerThread.isAlive()) {
                    log.warn(workerThread.getName() + " has not stopped for " + DEFAULT_STOP_TIMEOUT + " ms. Interrupting thread.");
                    workerThread.interrupt();
                    workerThread.join();
                }
                log.info(workerThread.getName() + " is stopped");
            } catch (InterruptedException ex) {
                // Ignoring
            }
        }
    }

    @Override
    public void join() {
        try {
            workerThread.join();
        } catch (InterruptedException ex) {
            // Ignoring
        }
    }

    /**
     * Implement this method in your workers
     * @param task
     * @throws Exception
     */
    public abstract void doWork(Task task) throws Exception;

    /**
     * Main worker loop
     */
    private void doWorkLoop() {
        log.info(workerThread.getName() + " start do work loop");
        Task task = null;

        while (started && getTaskQueue().isStarted()) {
            try {
                task = getTaskQueue().dequeue();
                if (task != null) {
                    doWork(task);
                }
            } catch (Exception ex) {
                TaskQueueException e = new TaskQueueException("Exception in doWork", task, ex);
                doWorkHandleException(e);
            }
        }

        log.info(workerThread.getName() + " finished its work");
    }

    /**
     * Handles exception in do work. Doing nothing by default
     * @param ex
     */
    protected void doWorkHandleException(Exception ex) {
        log.error("Error in doWork", ex);
    }

    /**
     * Returns logger
     * @return
     */
    protected Logger getLogger() {
        return log;
    }

    /**
     * @return the taskQueue
     */
    protected TaskQueue getTaskQueue() {
        return taskQueue;
    }
}
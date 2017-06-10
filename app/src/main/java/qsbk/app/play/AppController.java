package qsbk.app.play;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

import qsbk.app.play.model.WorkerThread;

public class AppController extends Application {

    private static AppController mInstance;

    private WorkerThread mWorkerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        // init it in the function of onCreate in ur Application
        Utils.init(getApplicationContext());
    }

    public synchronized void initWorkerThread() {
        if (mWorkerThread == null) {
            mWorkerThread = new WorkerThread(getApplicationContext());
            mWorkerThread.start();

            mWorkerThread.waitForReady();
        }
    }

    public synchronized WorkerThread getWorkerThread() {
        return mWorkerThread;
    }

    public synchronized void deInitWorkerThread() {
        mWorkerThread.exit();
        try {
            mWorkerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mWorkerThread = null;
    }

    public static AppController instance() {
        return mInstance;
    }
}

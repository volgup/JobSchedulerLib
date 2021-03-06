package com.volgup.jobschedulerlib.work;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.volgup.jobschedulerlib.Job;
import com.volgup.jobschedulerlib.JobManager;
import com.volgup.jobschedulerlib.JobProxy;
import com.volgup.jobschedulerlib.JobRequest;
import com.volgup.jobschedulerlib.util.JobCat;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * @author VolgUpTech
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PlatformWorker extends Worker {

    private static final JobCat CAT = new JobCat("PlatformWorker");

    public PlatformWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final int jobId = getJobId();
        if (jobId < 0) {
            return Result.failure();
        }

        try {
            JobProxy.Common common = new JobProxy.Common(getApplicationContext(), CAT, jobId);

            JobRequest request = common.getPendingRequest(true, true);
            if (request == null) {
                return Result.failure();
            }

            Bundle transientBundle = null;
            if (request.isTransient()) {
                transientBundle = TransientBundleHolder.getBundle(jobId);
                if (transientBundle == null) {
                    CAT.d("Transient bundle is gone for request %s", request);
                    return Result.failure();
                }
            }

            Job.Result result = common.executeJobRequest(request, transientBundle);
            if (Job.Result.SUCCESS == result) {
                return Result.success();
            } else {
                return Result.failure();
            }
        } finally {
            TransientBundleHolder.cleanUpBundle(jobId);
        }
    }

    @Override
    public void onStopped() {
        int jobId = getJobId();
        Job job = JobManager.create(getApplicationContext()).getJob(jobId);

        if (job != null) {
            job.cancel();
            CAT.d("Called onStopped for %s", job);
        } else {
            CAT.d("Called onStopped, job %d not found", jobId);
        }
    }

    private int getJobId() {
        return JobProxyWorkManager.getJobIdFromTags(getTags());
    }
}

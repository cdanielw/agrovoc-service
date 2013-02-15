package agrovoc.adapter.cron

import agrovoc.port.cron.AgrovocTermPollingJob
import org.fao.foris.cron.Cron
import org.fao.foris.cron.Job
import org.fao.foris.cron.JobMonitor
import org.fao.foris.cron.SimpleCron

/**
 * @author Daniel Wiell
 */
class AgrovocTermCron {
    String cronExpression = '0 0 * * * ?'
    private final Job job
    private Cron cron

    AgrovocTermCron(AgrovocTermPollingJob job) {
        this.job = new JobAdapter(job)
    }

    void start() {
        cron = new SimpleCron('Agrovoc update check', job, cronExpression)
        cron.start()
    }

    void stop() {
        cron.shutdown()
        cron.awaitTermination()
    }

    private static class JobAdapter implements Job {
        private final AgrovocTermPollingJob job

        JobAdapter(AgrovocTermPollingJob job) {
            this.job = job
        }

        void run(JobMonitor monitor) {
            job.pollForChanges()
        }
    }
}

package run;

import java.util.TimerTask;

class KillTask extends TimerTask {
    Process process;

    public KillTask(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        process.destroy();
    }
}

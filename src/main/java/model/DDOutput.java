package model;

import java.util.List;

/**
 * @author lsn
 * @date 2023/2/15 4:53 PM
 */
public class DDOutput {
    int hunk;
    int loop;
    long time;
    List<HunkEntity> cc;

    public DDOutput(int hunk, int loop, long time, List<HunkEntity> cc) {
        this.hunk = hunk;
        this.loop = loop;
        this.time = time;
        this.cc = cc;
    }

    public int getHunk() {
        return hunk;
    }

    public void setHunk(int hunk) {
        this.hunk = hunk;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }

    public long getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public List<HunkEntity> getCc() {
        return cc;
    }

    public void setCc(List<HunkEntity> cc) {
        this.cc = cc;
    }

}

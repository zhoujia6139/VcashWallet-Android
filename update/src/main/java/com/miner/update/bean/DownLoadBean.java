package com.miner.update.bean;

/**
 * 下载的信息
 */
public class DownLoadBean {
    public long downId;
    public String localUrl;
    public String url;
    public int compeleteSize;
    public int totalSize;
    public int status;

    public DownLoadBean(long downId, String localUrl, String url, int compeleteSize, int totalSize, int status) {
        this.downId = downId;
        this.localUrl = localUrl;
        this.url = url;
        this.compeleteSize = compeleteSize;
        this.totalSize = totalSize;
        this.status = status;
    }

    public int getProgress(){
        return (int) (((float)compeleteSize/totalSize)*100);
    }

    @Override
    public String toString() {
        return "HyenaDownBean{" +
                "downId=" + downId +
                ", localUrl='" + localUrl + '\'' +
                ", url='" + url + '\'' +
                ", compeleteSize=" + compeleteSize +
                ", totalSize=" + totalSize +
                ", status=" + status +
                '}';
    }
}

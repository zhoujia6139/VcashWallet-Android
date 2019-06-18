package com.miner.update;

import android.content.Context;
import android.content.Intent;

/**
 * Author:YJQ
 * Time:2018/4/27  16:53
 * Description:升级管理(入口)
 */
public class UpdateManager {

    private UpdateManager(){
    }

    public static UpdateManager getInstance() {
        return UpdateManagerHolder.updateManager;
    }

    /**
     * 静态内部类单例
     */
    private static class UpdateManagerHolder {
        static final UpdateManager updateManager = new UpdateManager();
    }

    public DownLoadBuilder createDownLoadBuilder(){
        return new DownLoadBuilder();
    }

}
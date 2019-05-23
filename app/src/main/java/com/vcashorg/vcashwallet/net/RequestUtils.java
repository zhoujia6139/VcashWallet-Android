package com.vcashorg.vcashwallet.net;

import com.vcashorg.vcashwallet.bean.Demo;

public class RequestUtils {

    public static void getDemo(CommonObserver<Demo> observer){
        RetrofitUtils.getApiUrl().demo()
                .compose(RxHelper.<Demo>io2main())
                .subscribe(observer);
    }
}

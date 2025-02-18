package com.vcashorg.vcashwallet.widget.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.Result;
import com.mylhyl.zxing.scanner.ScannerView;
import com.mylhyl.zxing.scanner.common.Scanner;
import com.mylhyl.zxing.scanner.decode.QRDecode;
import com.vcashorg.vcashwallet.R;

import java.util.List;

/**
 * 扫一扫
 */
public class ScannerActivity extends DeCodeActivity {

    public static final String EXTRA_LASER_LINE_MODE = "extra_laser_line_mode";
    public static final String EXTRA_SCAN_MODE = "extra_scan_mode";
    public static final String EXTRA_SHOW_THUMBNAIL = "EXTRA_SHOW_THUMBNAIL";
    public static final String EXTRA_SCAN_FULL_SCREEN = "EXTRA_SCAN_FULL_SCREEN";
    public static final String EXTRA_HIDE_LASER_FRAME = "EXTRA_HIDE_LASER_FRAME";

    public static final int EXTRA_LASER_LINE_MODE_0 = 0;
    public static final int EXTRA_LASER_LINE_MODE_1 = 1;
    public static final int EXTRA_LASER_LINE_MODE_2 = 2;

    public static final int EXTRA_SCAN_MODE_0 = 0;
    public static final int EXTRA_SCAN_MODE_1 = 1;
    public static final int EXTRA_SCAN_MODE_2 = 2;

    public static final int APPLY_READ_EXTERNAL_STORAGE = 0x111;

    private ScannerView mScannerView;
    private Result mLastResult;


    @Override
    protected int provideContentViewId() {
        return R.layout.activity_scanner;
    }

    @Override
    public void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mScannerView = (ScannerView) findViewById(R.id.scanner_view);
        mScannerView.setOnScannerCompletionListener(this);

        int laserMode = EXTRA_LASER_LINE_MODE_0;
        int scanMode = EXTRA_SCAN_MODE_0;

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            laserMode = extras.getInt(EXTRA_LASER_LINE_MODE);
            scanMode = extras.getInt(EXTRA_SCAN_MODE);
            showThumbnail = extras.getBoolean(EXTRA_SHOW_THUMBNAIL);

            //全屏识别
            mScannerView.isScanFullScreen(extras.getBoolean(EXTRA_SCAN_FULL_SCREEN));
            //隐藏扫描框
            mScannerView.isHideLaserFrame(extras.getBoolean(EXTRA_HIDE_LASER_FRAME));
        }

        // mScannerView.setMediaResId(R.raw.beep);//设置扫描成功的声音
        mScannerView.setDrawText("", true);
        // mScannerView.setDrawTextColor(Color.RED);

        if (scanMode == 1) {
            //二维码
            mScannerView.setScanMode(Scanner.ScanMode.QR_CODE_MODE);
        } else if (scanMode == 2) {
            //一维码
            mScannerView.setScanMode(Scanner.ScanMode.PRODUCT_MODE);
        }

        //显示扫描成功后的缩略图
        mScannerView.isShowResThumbnail(showThumbnail);

//        mScannerView.isScanInvert(true);//扫描反色二维码
//        mScannerView.setCameraFacing(CameraFacing.FRONT);
//        mScannerView.setLaserMoveSpeed(1);//速度

//        mScannerView.setLaserFrameTopMargin(100);//扫描框与屏幕上方距离
//        mScannerView.setLaserFrameSize(400, 400);//扫描框大小
//        mScannerView.setLaserFrameCornerLength(25);//设置4角长度
//        mScannerView.setLaserLineHeight(5);//设置扫描线高度
//        mScannerView.setLaserFrameCornerWidth(5);

        switch (laserMode) {
            case EXTRA_LASER_LINE_MODE_0:
                mScannerView.setLaserLineResId(R.mipmap.wx_scan_line);//线图
                break;
            case EXTRA_LASER_LINE_MODE_1:
                mScannerView.setLaserGridLineResId(R.mipmap.zfb_grid_scan_line);//网格图
                mScannerView.setLaserFrameBoundColor(0xFF26CEFF);//支付宝颜色
                break;
            case EXTRA_LASER_LINE_MODE_2:
                mScannerView.setLaserColor(Color.RED);
                break;
        }
    }

    @Override
    protected void onResume() {
        mScannerView.onResume();
        resetStatusView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mScannerView.onPause();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mLastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void restartPreviewAfterDelay(long delayMS) {
        mScannerView.restartPreviewAfterDelay(delayMS);
        resetStatusView();
    }

    private void resetStatusView() {
        mLastResult = null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && resultCode == Activity.RESULT_OK) {
            if (requestCode == PickPictureTotalActivity.REQUEST_CODE_SELECT_PICTURE) {
                String picturePath = data.getStringExtra(PickPictureTotalActivity
                        .EXTRA_PICTURE_PATH);
                QRDecode.decodeQR(picturePath, this);
            }
        }
    }

    public static void gotoActivity(Activity activity, boolean isBackResult
            , int laserMode, int scanMode, boolean showThumbnail, boolean isScanFullScreen
            , boolean isHideLaserFrame) {

        Intent intent = new Intent(activity,ScannerActivity.class);
        intent.putExtra(BasicScannerActivity.EXTRA_RETURN_SCANNER_RESULT, isBackResult)
                .putExtra(ScannerActivity.EXTRA_LASER_LINE_MODE, laserMode)
                .putExtra(ScannerActivity.EXTRA_SCAN_MODE, scanMode)
                .putExtra(ScannerActivity.EXTRA_SHOW_THUMBNAIL, showThumbnail)
                .putExtra(ScannerActivity.EXTRA_SCAN_FULL_SCREEN, isScanFullScreen)
                .putExtra(ScannerActivity.EXTRA_HIDE_LASER_FRAME, isHideLaserFrame);
        activity.startActivityForResult(intent,BasicScannerActivity.REQUEST_CODE_SCANNER);

//
//        activity.startActivityForResult(new Intent(Scanner.Scan.ACTION)
//                        .putExtra(BasicScannerActivity.EXTRA_RETURN_SCANNER_RESULT, isBackResult)
//                        .putExtra(ScannerActivity.EXTRA_LASER_LINE_MODE, laserMode)
//                        .putExtra(ScannerActivity.EXTRA_SCAN_MODE, scanMode)
//                        .putExtra(ScannerActivity.EXTRA_SHOW_THUMBNAIL, showThumbnail)
//                        .putExtra(ScannerActivity.EXTRA_SCAN_FULL_SCREEN, isScanFullScreen)
//                        .putExtra(ScannerActivity.EXTRA_HIDE_LASER_FRAME, isHideLaserFrame)
//                , BasicScannerActivity.REQUEST_CODE_SCANNER);
    }
}

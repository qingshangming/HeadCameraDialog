package com.fastaoe.weight_headcameradialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by jinjin on 16/2/18.
 */
public class UpdateUserHeadDialog extends Dialog implements View.OnClickListener {

    private Button btn_photograph;
    private Button btn_gallery;
    private Button btn_cancle;

    private Activity mContext;
    private ImageView mImageView;

    private Uri photoUri;
    private File picFile;

    private Uri tempUri;
    private File tempPicFile;


    private static final String FILE_NAME = "UserHead.png";

    private static final String FILE_TEMP_NAME = "TempHead.png";
    // 拍照
    private final int PIC_FROM_CAMERA = 1;
    // 本地图片
    private final int PIC_FROM＿LOCALPHOTO = 0;

    public UpdateUserHeadDialog(Context context, ImageView iv) {
        super(context, R.style.recommend_dialog);
        this.mContext = (Activity) context;
        this.mImageView = iv;
        setContentView(R.layout.dialog_account_choose_head);

        initView();
    }

    private void initView() {
        btn_photograph = (Button) findViewById(R.id.btn_photograph);
        btn_gallery = (Button) findViewById(R.id.btn_gallery);
        btn_cancle = (Button) findViewById(R.id.btn_cancle);

        setCanceledOnTouchOutside(false);

        btn_cancle.setOnClickListener(this);
        btn_photograph.setOnClickListener(this);
        btn_gallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_photograph:
                getFromPhoto(PIC_FROM_CAMERA);// 从照相机获取
                break;

            case R.id.btn_gallery:
                getFromPhoto(PIC_FROM＿LOCALPHOTO);// 从相册中去获取
                break;

            case R.id.btn_cancle:
                dismiss();
                break;
        }
    }

    /**
     * 根据不同方式选择图片设置ImageView
     *
     * @param type 0为本地相册选择，1为拍照
     */
    private void getFromPhoto(int type) {
        try {
            //创建图片文件路径
            createFile();

            if (type == PIC_FROM＿LOCALPHOTO) {
                Intent intent = getCropImageIntent();
                mContext.startActivityForResult(intent, PIC_FROM＿LOCALPHOTO);
            } else {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                mContext.startActivityForResult(cameraIntent, PIC_FROM_CAMERA);
            }

        } catch (Exception e) {
            Log.i("HandlerPicError", "处理图片出现错误");
        }
    }

    private void createFile() throws IOException {
        File pictureFileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/head");
        if (!pictureFileDir.exists()) {
            pictureFileDir.mkdirs();
        }
        picFile = new File(pictureFileDir, FILE_NAME);
        picFile.delete();
        tempPicFile = new File(pictureFileDir,FILE_TEMP_NAME);
        tempPicFile.delete();
        if (!picFile.exists()) {
            picFile.createNewFile();
        }
        if (!tempPicFile.exists()) {
            tempPicFile.createNewFile();
        }
        photoUri = Uri.fromFile(picFile);
        tempUri = Uri.fromFile(tempPicFile);
    }


    /**
     * 调用图片剪辑程序
     */
    public Intent getCropImageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        setIntentParams(intent);
        return intent;
    }

    /**
     * 启动裁剪
     */
    private void cropImageUriByTakePhoto() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        setIntentParams(intent);
        mContext.startActivityForResult(intent, PIC_FROM＿LOCALPHOTO);
    }

    /**
     * 设置公用参数
     */
    private void setIntentParams(Intent intent) {
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);// 裁剪比例
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 600);// 输出大小
        intent.putExtra("outputY", 600);
        intent.putExtra("noFaceDetection", true); // 关闭人脸识别
        intent.putExtra("scale", true);// 缩放
        intent.putExtra("scaleUpIfNeeded", true);// 去黑边
        intent.putExtra("return-data", false);// 不返回缩略图
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);//输出文件
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());//输出格式
    }

    /**
     * 给activity的onActivityResult调用
     *
     * @param requestCode
     */
    public void activityResult(int requestCode) {
        switch (requestCode) {
            case PIC_FROM_CAMERA:
                try {
                    if ( picFile.length() > 0) {
                        cropImageUriByTakePhoto();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PIC_FROM＿LOCALPHOTO:
                try {
                    if ( tempPicFile.length() > 0) {
                        Bitmap bitmap = decodeUriAsBitmap(tempUri);
//                        Bitmap bitmap = compressImageFromFile(tempPicFile.getPath());
//                        Bitmap temp = ratio(bitmap, 300, 300);
                        //将剪辑好的图片设置到头像上
                        mImageView.setImageBitmap(bitmap);
                        //在这边设置文件的上传
//                        QiniuUploadUtils.getInstance().upload(temp, FILE_NAME);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public Bitmap ratio(Bitmap image, float pixelW, float pixelH) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, os);
        if( os.toByteArray().length / 1024>1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            os.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 30, os);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        float hh = pixelH;// 设置高度为240f时，可以明显看到图片缩小了
        float ww = pixelW;// 设置宽度为120f，可以明显看到图片缩小了
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        is = new ByteArrayInputStream(os.toByteArray());
        bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        return bitmap;
    }
}

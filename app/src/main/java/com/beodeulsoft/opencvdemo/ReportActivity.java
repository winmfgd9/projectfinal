package com.beodeulsoft.opencvdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private EditText t1, t2, t3, t4;
    private Button browse, upload, identifyButton;
    private ImageView img;
    private Bitmap bitmap;
    private String encodeImageString;
    private static final String url = "http://winmfgd9.cafe24.com/fileupload2.php";
    private TessBaseAPI tessBaseAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        img = findViewById(R.id.img);
        upload = findViewById(R.id.upload);
        browse = findViewById(R.id.browse);
        identifyButton = findViewById(R.id.identifyButton);
        t1 = findViewById(R.id.t1);
        t2 = findViewById(R.id.t2);
        t3 = findViewById(R.id.t3);
        t4 = findViewById(R.id.t4);

        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dexter.withActivity(ReportActivity.this)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType("image/*");
                                startActivityForResult(Intent.createChooser(intent, "Browse Image"), 1);
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                // 권한 거부 처리
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploaddatatodb();
            }
        });

        identifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap != null) {
                    String recognizedText = recognizeLicensePlate(bitmap);
                    t2.setText(recognizedText);
                }
            }
        });
        initTessBaseAPI();
    }

    // Tesseract OCR 초기화 메서드
    private void initTessBaseAPI() {
        tessBaseAPI = new TessBaseAPI();
        String dataPath = getFilesDir() + "/tesseract/";
        File dir = new File(dataPath + "tessdata/");
        if (!dir.exists()) dir.mkdirs();
        copyTessDataFiles("tessdata");
        tessBaseAPI.init(dataPath, "kor");
    }

    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // `path` 내의 파일을 스트림으로 읽음
                InputStream in = getAssets().open(path + "/" + fileName);
                // 내부 저장소의 해당 경로로 출력 스트림 생성
                OutputStream out = new FileOutputStream(getFilesDir() + "/" + fileName);

                // 파일 복사
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                out.close();
                in.close();
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Unable to copy files to tessdata " + e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri filepath = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(filepath);
                bitmap = BitmapFactory.decodeStream(inputStream);
                img.setImageBitmap(bitmap);
                encodeBitmapImage(bitmap);
            } catch (Exception ex) {
                // 예외 처리
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploaddatatodb() {
        final String Class = t1.getText().toString().trim();
        final String License = t2.getText().toString().trim();
        final String Point = t3.getText().toString().trim();
        final String Day = t4.getText().toString().trim();

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                t1.setText("");
                t2.setText("");
                img.setImageResource(R.drawable.ic_launcher_foreground);
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                map.put("t1", Class);
                map.put("t2", License);
                map.put("t3", Point);
                map.put("t4", Day);
                map.put("upload", encodeImageString);
                return map;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }



    private void encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytesofimage = byteArrayOutputStream.toByteArray();
        encodeImageString = Base64.encodeToString(bytesofimage, Base64.DEFAULT);
    }

    private String recognizeLicensePlate(Bitmap image) {
        String recognizedText = "";
        Mat src = new Mat();
        Utils.bitmapToMat(image, src);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        // 이진화
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);

        // 모폴로지 연산 (노이즈 제거)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat morph = new Mat();
        Imgproc.morphologyEx(binary, morph, Imgproc.MORPH_CLOSE, kernel);

        // 경계 검출
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(morph, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getFilesDir() + "/tesseract/", "kor");  // Tesseract OCR 초기화

        double maxConfidence = 0.0;

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, contour2f, approxDistance, true);

            MatOfPoint points = new MatOfPoint(contour2f.toArray());
            Rect rect = Imgproc.boundingRect(points);

            int MIN_WIDTH = 480;
            int MAX_WIDTH = 640;
            int MIN_HEIGHT = 480;
            int MAX_HEIGHT = 640;

            if (rect.width > MIN_WIDTH && rect.width < MAX_WIDTH && rect.height > MIN_HEIGHT && rect.height < MAX_HEIGHT) {
                Mat numberPlateRegion = gray.submat(rect);
                Bitmap numberPlateBitmap = toBitmap(numberPlateRegion);

                tessBaseAPI.setImage(numberPlateBitmap);
                String currentText = tessBaseAPI.getUTF8Text();
                double currentConfidence = tessBaseAPI.meanConfidence();

                if (isValidLicensePlateFormat(currentText) && currentConfidence > maxConfidence) {
                    recognizedText = currentText;
                    maxConfidence = currentConfidence;
                }
            }
        }

        tessBaseAPI.end();
        return recognizedText;
    }

    private boolean isValidLicensePlateFormat(String text) {
        // 한국 번호판 형식에 맞는 정규식
        return text.matches("[가-힣]{2} \\d{2,3}[가-힣]{1} \\d{4}") || text.matches("\\d{2,3}[가-힣]{1} \\d{4}");
    }

    private Bitmap toBitmap(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

}

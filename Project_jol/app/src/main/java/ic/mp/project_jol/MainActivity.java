package ic.mp.project_jol;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //권한
    private GpsTracker gpsTracker;

    //GPS 및 퍼미션 코드
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    // 권한 설정
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN};

    //JSon
    JSONObject json = null;

    //UI설정
    ImageView todayimage;
    TextView YYMMEE, pmtext, temtext, humitext, pmstate;
    ImageButton iblight, ibout, ibin, ibfuntion;

    //블루투스
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket = null; //블루투스 소켓
    private OutputStream outputStream = null; //블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; //블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; //문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; //수신된 문자열 저장 버퍼
    private int readBufferPosition; //버퍼  내 문자 저장 위치
    String[] array = {"0"}; //수신된 문자열을 쪼개서 저장할 배열

    //페어링 기기수 확인
    int pairedDeviceCount;

    //귀가설정
    int gul = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("HomeIoT");

        settingLayout();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServicesSetting();
        } else {
            checkRunTimePermission();
        }

        gpsTracker = new GpsTracker(MainActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        Log.d("gps", "위도 : " + latitude);
        Log.d("gps", "경도 : " + longitude);


        //위도 경도 눈금변환
        //transLocalPoint = new TransLocalPoint();
        //TransLocalPoint.LatXLngY tmp = transLocalPoint.convertGRID_GPS(TO_GRID, latitude, longitude);

        long mNow = System.currentTimeMillis();
        Date mReDate = new Date(mNow);
        SimpleDateFormat mFormatYDM = new SimpleDateFormat("yyyyMMdd");
        String formatYDM = mFormatYDM.format(mReDate);
        SimpleDateFormat mFormatTime = new SimpleDateFormat("HH00");
        String formatTime = String.valueOf(Integer.parseInt(mFormatTime.format(mReDate)) - 100);

        String Api_Key = "255120f4d47d1e4a85c1ea1f0ae94f73";
        String nx = String.format("%.0f", latitude);
        String ny = String.format("%.0f", longitude);

        String url = "https://api.openweathermap.org/data/2.5/weather?" +
                "lat=" + nx +
                "&lon=" + ny +
                "&appid=" + Api_Key +
                "&lang=kr";
        Log.d("url", url);

//        String service_key = "YzLrZt5Gi5wpYzeyYxTJT1qraziIl9iLsmcqyp1sX060qrGJ5k4uSvwz8ZKbCCiY31cXhj5dMaKSS2QJeyPfHA%3D%3D";
//        String num_of_rows = "1";
//        String page_no = "1";
//        String date_type = "JSON";
//        String base_date = formatYDM;
//        String base_time = formatTime;
//        String nx = String.format("%.0f", latitude);
//        String ny = String.format("%.0f", longitude);
//
//        String url = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst?" +
//                "serviceKey=" + service_key +
//                "&numOfRows=" + num_of_rows +
//                "&pageNo=" + page_no +
//                "&dataType=" + date_type +
//                "&base_date=" + base_date +
//                "&base_time=" + base_time +
//                "&nx=" + nx + "&ny=" + ny;
//        Log.d("url", url);

        NetworkTask networkTask = new NetworkTask(url, null);
        networkTask.execute();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //블루투스 어댑터를 디폴트 어댑터로 설정

        //진동
        //NotificationSomethings();

        //조명 기능
        iblight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //디바이스를 선택하기 위한 대화상자 생성
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("조명 제어 목록");
                //페어링 된 각각의 디바이스의 이름과 주소를 저장
                List<String> list = new ArrayList<>();
                //모든 디바이스의 이름을 리스트에 추가
                list.add("거실");
                list.add("안방");
                list.add("서재");
                list.add("부엌");
                list.add("화장실");


                //list를 Charsequence 배열로 변경
                final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
                list.toArray(new CharSequence[list.size()]);

                //해당 항목을 눌렀을 때 호출되는 이벤트 리스너
                builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //해당 디바이스와 연결하는 함수 호출
                        switch (charSequences[which].toString()) {
                            case "거실":
                                androidx.appcompat.app.AlertDialog.Builder builder1 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder1.setTitle("거실 조명");
                                builder1.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String A = "A";
                                        byte[] bytes = A.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "A입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "A입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "A입력 실패");
                                        }

                                    }
                                });

                                builder1.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String a = "a";
                                        byte[] bytes = a.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "a입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "a입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "a입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder1.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog1 = builder1.create();
                                alertDialog1.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog1.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog1.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog1.show();
                                break;
                            case "안방":
                                androidx.appcompat.app.AlertDialog.Builder builder2 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder2.setTitle("안방 조명");
                                builder2.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String B = "B";
                                        byte[] bytes = B.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "B입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "B입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "B입력 실패");
                                        }

                                    }
                                });

                                builder2.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String b = "b";
                                        byte[] bytes = b.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "b입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "b입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "b입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder2.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog2 = builder2.create();
                                alertDialog2.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog2.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog2.show();
                                break;
                            case "서재":
                                androidx.appcompat.app.AlertDialog.Builder builder3 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder3.setTitle("서재 조명");
                                builder3.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String C = "C";
                                        byte[] bytes = C.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "C입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "C입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "C입력 실패");
                                        }

                                    }
                                });

                                builder3.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String c = "c";
                                        byte[] bytes = c.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "c입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "c입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "c입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder3.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog3 = builder3.create();
                                alertDialog3.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog3.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog3.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog3.show();
                                break;
                            case "부엌":
                                androidx.appcompat.app.AlertDialog.Builder builder4 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder4.setTitle("부엌 조명");
                                builder4.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String D = "D";
                                        byte[] bytes = D.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "D입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "D입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "D입력 실패");
                                        }

                                    }
                                });

                                builder4.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String d = "d";
                                        byte[] bytes = d.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "d입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "d입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "d입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder4.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog4 = builder4.create();
                                alertDialog4.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog4.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog4.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog4.show();
                                break;
                            case "화장실":
                                androidx.appcompat.app.AlertDialog.Builder builder5 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder5.setTitle("화장실 조명");
                                builder5.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String E = "E";
                                        byte[] bytes = E.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "E입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "E입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "E입력 실패");
                                        }

                                    }
                                });

                                builder5.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String e = "e";
                                        byte[] bytes = e.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "e입력");
                                        } catch (IOException l) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "e입력 실패");
                                        } catch (NullPointerException l) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "e입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder5.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog5 = builder5.create();
                                alertDialog5.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog5.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog5.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog5.show();
                                break;
                            default:
                                break;
                        }
                    }
                });

                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });


                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                builder.setCancelable(false);
                //다이얼로그 생성
                androidx.appcompat.app.AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                    }
                });
                alertDialog.show();
            }
        });

        //편의 기능
        ibfuntion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //디바이스를 선택하기 위한 대화상자 생성
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                builder.setTitle("편의 기능 목록");
                //페어링 된 각각의 디바이스의 이름과 주소를 저장
                List<String> list = new ArrayList<>();
                //모든 디바이스의 이름을 리스트에 추가
                list.add("창문");
                list.add("선풍기");
                list.add("환풍기");

                //list를 Charsequence 배열로 변경
                final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
                list.toArray(new CharSequence[list.size()]);

                //해당 항목을 눌렀을 때 호출되는 이벤트 리스너
                builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //해당 디바이스와 연결하는 함수 호출
                        switch (charSequences[which].toString()) {
                            case "창문":
                                androidx.appcompat.app.AlertDialog.Builder builder1 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder1.setTitle("창문");
                                builder1.setNeutralButton("열림", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String F = "F";
                                        byte[] bytes = F.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "F입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "F입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "F입력 실패");
                                        }

                                    }
                                });

                                builder1.setNegativeButton("닫힘", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String f = "f";
                                        byte[] bytes = f.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "f입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "f입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "f입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder1.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog1 = builder1.create();
                                alertDialog1.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog1.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog1.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog1.show();
                                break;
                            case "선풍기":
                                androidx.appcompat.app.AlertDialog.Builder builder2 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder2.setTitle("선풍기");
                                builder2.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String G = "G";
                                        byte[] bytes = G.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "G입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "G입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "G입력 실패");
                                        }

                                    }
                                });

                                builder2.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String g = "g";
                                        byte[] bytes = g.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "g입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "g입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "g입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder2.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog2 = builder2.create();
                                alertDialog2.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog2.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog2.show();
                                break;
                            case "환풍기":
                                androidx.appcompat.app.AlertDialog.Builder builder3 = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                                builder3.setTitle("환풍기");
                                builder3.setNeutralButton("켜기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String H = "H";
                                        byte[] bytes = H.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "H입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "H입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "H입력 실패");
                                        }

                                    }
                                });

                                builder3.setNegativeButton("끄기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String h = "h";
                                        byte[] bytes = h.getBytes();           //converts entered String into bytes
                                        try {
                                            outputStream.write(bytes);
                                            Log.d("led", "h입력");
                                        } catch (IOException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "h입력 실패");
                                        } catch (NullPointerException e) {
                                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                                            Log.d("led", "h입력 실패");
                                        }
                                    }
                                });
                                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                                builder3.setCancelable(false);
                                //다이얼로그 생성
                                androidx.appcompat.app.AlertDialog alertDialog3 = builder3.create();
                                alertDialog3.setOnShowListener( new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface arg0) {
                                        alertDialog3.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                                        alertDialog3.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                                    }
                                });
                                alertDialog3.show();
                                break;
                            default:
                                break;
                        }
                    }
                });

                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });


                //뒤로가기 버튼 누를때 창이 안닫히도록 설정
                builder.setCancelable(false);
                //다이얼로그 생성
                androidx.appcompat.app.AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                    }
                });
                alertDialog.show();
            }
        });

        ibout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String j = "j";
                byte[] bytes = j.getBytes();           //converts entered String into bytes
                try {
                    outputStream.write(bytes);
                    Log.d("led", "j입력");
                    Toast.makeText(MainActivity.this, "외출 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                    Log.d("led", "j입력 실패");
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                    Log.d("led", "j입력 실패");
                }
            }
        });

        ibin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (gul) {
                    case 1:
                        String J = "J";
                        byte[] bytes1 = J.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes1);
                            Log.d("led", "1번, J입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "J입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "J입력 실패");
                        }
                        break;
                    case 2:
                        String L = "L";
                        byte[] bytes2 = L.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes2);
                            Log.d("led", "2번, L입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "L입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "L입력 실패");
                        }
                        break;
                    case 3:
                        String M = "M";
                        byte[] bytes3 = M.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes3);
                            Log.d("led", "3번, M입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "M입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "M입력 실패");
                        }
                        break;
                    case 4:
                        String K = "K";
                        byte[] bytes4 = K.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes4);
                            Log.d("led", "4번, K입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "K입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "K입력 실패");
                        }
                        break;
                    case 5:
                        String Q = "Q";
                        byte[] bytes5 = Q.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes5);
                            Log.d("led", "5번, Q입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "Q입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "Q입력 실패");
                        }
                        break;
                    case 6:
                        String N = "N";
                        byte[] bytes6 = N.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes6);
                            Log.d("led", "6번, N입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "N입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "N입력 실패");
                        }
                        break;
                    case 7:
                        String O = "O";
                        byte[] bytes7 = O.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes7);
                            Log.d("led", "7번, O입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "O입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "O입력 실패");
                        }
                        break;
                    case 8:
                        String P = "P";
                        byte[] bytes8 = P.getBytes();           //converts entered String into bytes
                        try {
                            outputStream.write(bytes8);
                            Log.d("led", "8번, P입력");
                            Toast.makeText(MainActivity.this, "귀가 모드를 실행합니다", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "P입력 실패");
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "블루투스 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                            Log.d("led", "P입력 실패");
                        }
                        break;
                    default:

                        break;
                }

            }
        });


    }

    // 레이아웃 설정정
    public void settingLayout() {
        todayimage = findViewById(R.id.todayimage);
        YYMMEE = findViewById(R.id.YYMMEE);
        pmtext = findViewById(R.id.pmtext);
        temtext = findViewById(R.id.temtext);
        iblight = findViewById(R.id.iblight);
        humitext = findViewById(R.id.humiditytext);
        ibout = findViewById(R.id.ibOUT);
        ibin = findViewById(R.id.ibIN);
        ibfuntion = findViewById(R.id.ibfuntion);
        pmstate = findViewById(R.id.pmstate);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu1:

                startBluetooth();
                break;

            case R.id.menu2:
                Intent intent = new Intent(MainActivity.this, CheckActivity.class);
                intent.putExtra("data", gul);
                startActivityForResult(intent, 1);
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    void checkRunTimePermission() {
        //런타임 퍼미션 처리
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int hasBluetoothPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
            //퍼미션 성공시
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                Toast.makeText(MainActivity.this, "위치 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private void showDialogForLocationServicesSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기위해서는 위치 서비스가 필요합니다. \n " + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton(" 취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                //데이터 받기
                gul = data.getIntExtra("result", 0);

                try {
                    Log.d("신호", "신호값 : " + gul);
                } catch (RuntimeException e) {
                    Log.d("신호", "오류 신호값 : " + gul);
                }

            }
        }

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("GPS", "onAcitivtyResult 쥐퓌에수 활성화");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public class NetworkTask extends AsyncTask<Void, Void, String> {
        private String url;
        private ContentValues values;
        TextView todaytext = findViewById(R.id.todaytext);

        public NetworkTask(String url, ContentValues values) {
            this.url = url;
            this.values = values;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... params) {
            String result;
            // 요청 결과를 저장할 변수.
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values);
            // 해당 URL로 부터 결과물을 얻어온다.
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.

            try {
                json = new JSONObject(s);

                JSONObject jObject = new JSONObject(s);
                // 배열을 가져옵니다.
                JSONArray jArray = jObject.getJSONArray("weather");
                Log.d("json", "성공욤 ㅋ " + jArray);

                // 배열의 모든 아이템을 출력합니다.
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject obj = jArray.getJSONObject(i);
                    String id = obj.getString("id");
                    String main = obj.getString("main");
                    String description = obj.getString("description");
                    String icon = obj.getString("icon");
                    Log.d("json", "아이디 " + id);
                    Log.d("json", "메인날씨 " + main);
                    Log.d("json", "날씨상세 " + description);
                    Log.d("json", "아이콘 " + icon);

                    switch (main) {
                        case "Clear":
                            todayimage.setImageResource(R.drawable.clear);
                            break;
                        case "Clouds":
                            todayimage.setImageResource(R.drawable.cloud);
                            break;
                        case "Snow":
                            todayimage.setImageResource(R.drawable.snow);
                            break;
                        case "Rain":
                            todayimage.setImageResource(R.drawable.rain);
                            break;
                        case "Drizzle":
                            todayimage.setImageResource(R.drawable.rain);
                            break;
                        case "Thunderstorm":
                            todayimage.setImageResource(R.drawable.thunder);
                            break;
                        default:
                            break;
                    }

                    Date currentTime = Calendar.getInstance().getTime();
                    String date_text = new SimpleDateFormat("MM월 dd일 EE요일", Locale.getDefault()).format(currentTime);
                    Log.d("today", date_text);
                    YYMMEE.setText(date_text);

                    todaytext.setText(description);
                }


            } catch (JSONException e) {
                Log.d("json", "실패요실패실패요실패실패요실패실패요실패" + s);
                e.printStackTrace();
            }


        }
    }

    public void startBluetooth() {
        if (bluetoothAdapter == null) { //기기가 블루투스를 지원하지 않을때
            Toast.makeText(getApplicationContext(), "Bluetooth 미지원 기기입니다.", Toast.LENGTH_SHORT).show();
            //처리코드 작성
        } else { // 기기가 블루투스를 지원할 때
            if (bluetoothAdapter.isEnabled()) { // 기기의 블루투스 기능이 켜져있을 경우
                Log.d("blue", "블투지원");
                selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출
            } else { // 기기의 블루투스 기능이 꺼져있을 경우
                // 블루투스를 활성화 하기 위한 대화상자 출력
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // 선택 값이 onActivityResult함수에서 콜백
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivityForResult(intent, REQUEST_ENABLE_BT);
                selectBluetoothDevice();
            }

        }
    }

    public void selectBluetoothDevice() {
        //이미 페어링 되어있는 블루투스 기기를 탐색
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        devices = bluetoothAdapter.getBondedDevices();
        //페어링 된 디바이스 크기 저장
        pairedDeviceCount = devices.size();
        //페어링 된 장치가 없는 경우
        if (pairedDeviceCount == 0) {
            Log.d("blue", "페어링된기기없음");
            //페어링 하기 위한 함수 호출
            Toast.makeText(getApplicationContext(), "먼저 Bluetooth 설정에 들어가 페어링을 진행해 주세요.", Toast.LENGTH_SHORT).show();
        }
        //페어링 되어있는 장치가 있는 경우
        else {
            //디바이스를 선택하기 위한 대화상자 생성
            Log.d("blue", "페어링된기기있음");
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("페어링 된 블루투스 디바이스 목록");
            //페어링 된 각각의 디바이스의 이름과 주소를 저장
            List<String> list = new ArrayList<>();
            //모든 디바이스의 이름을 리스트에 추가
            for (BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }

            //list를 Charsequence 배열로 변경
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);

            //해당 항목을 눌렀을 때 호출되는 이벤트 리스너
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //해당 디바이스와 연결하는 함수 호출
                    connectDevice(charSequences[which].toString());
                }
            });

            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            //뒤로가기 버튼 누를때 창이 안닫히도록 설정
            builder.setCancelable(false);
            //다이얼로그 생성
            androidx.appcompat.app.AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface arg0) {
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.argb(250,118,209,255));
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.argb(250,118,209,255));
                }
            });
            alertDialog.show();
        }

    }

    public void connectDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //페어링 된 디바이스 모두 탐색
        for (BluetoothDevice tempDevice : devices) {
            //사용자가 선택한 이름과 같은 디바이스로 설정하고 반복문 종료
            if (deviceName.equals(tempDevice.getName())) {
                bluetoothDevice = tempDevice;
                break;
            }

        }
        //UUID생성
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        //Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            Toast.makeText(getApplicationContext(), bluetoothDevice.getName() + " 연결 완료!", Toast.LENGTH_SHORT).show();
            receiveData();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), " 연결 실패!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void receiveData() {
        final Handler handler = new Handler();

        //데이터 수신을 위한 버퍼 생성
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        //데이터 수신을 위한 쓰레드 생성
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //데이터 수신 확인
                        int byteAvailable = inputStream.available();
                        //데이터 수신 된 경우
                        if (byteAvailable > 0) {
                            //입력 스트림에서 바이트 단위로 읽어옴
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            //입력 스트림 바이트를 한 바이트씩 읽어옴
                            for (int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                //개행문자를 기준으로 받음 (한줄)
                                if (tempByte == '\n') {
                                    //readBuffer 배열을 encodeBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    //인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //여기서 센서값을 받을 예정!
                                            array = text.split(",");
                                            try {
                                                float dust = Float.parseFloat(array[0]);
                                                int gas = Integer.parseInt(array[1]);
                                                int humi = Integer.parseInt(array[2]);
                                                int tem = Integer.parseInt(array[3]);
                                                int choong = Integer.parseInt(array[4]);
                                                Log.d("수신값", array[0] + "," + array[1] + "," + array[2] + "," + array[3] + "," + array[4] + " 수신성공");

                                                if (dust >= 151) {
                                                    pmstate.setText("매우나쁨");
                                                } else if (dust >= 81) {
                                                    pmstate.setText("나쁨");
                                                } else if (dust >= 31) {
                                                    pmstate.setText("보통");
                                                } else {
                                                    pmstate.setText("매우좋음");
                                                }

                                                pmtext.setText(String.format("%.0f", dust) + "ug/㎥");
                                                //temtext.setText(String.valueOf(tem)+"℃");
                                                //humitext.setText(String.valueOf(humi)+"%");
                                                temtext.setText(String.valueOf(tem) + "℃");
                                                humitext.setText(String.valueOf(humi) + "%");


//                                                pmtext.setText(String.valueOf(dust)+"ug/㎥");
//                                                temtext.setText(String.valueOf(tem)+"℃");
//                                                humitext.setText(String.valueOf(humi)+"%");
                                            } catch (Exception e) {
                                                Log.d("수신값", array[0] + "," + array[1] + "," + array[2] + "," + array[3] + "," + array[4] + " 수신실패");
                                                Log.d("수신값", e.getMessage());
                                            }
                                        }
                                    });
                                } // 개행문자가 아닐경우
                                else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                    }
                }
                try {
                    //1초 마다 받아옴
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        workerThread.start();
    }

    public void NotificationSomethings() {

        NotificationManager mNotificationManager;
        //notification manager 생성
        mNotificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // 기기(device)의 SDK 버전 확인 ( SDK 26 버전 이상인지 - VERSION_CODES.O = 26)
        if (android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.O) {
            //Channel 정의 생성자( construct 이용 )
            NotificationChannel notificationChannel = new NotificationChannel("primary_notification_channel"
                    , "Test Notification", mNotificationManager.IMPORTANCE_HIGH);
            //Channel에 대한 기본 설정
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Mascot");
            //Manager을 이용하여 Channel 생성
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, "primary_notification_channel")
                .setContentTitle("테스트")
                .setContentText("테스트 내용")
                .setSmallIcon(R.drawable.smarthome);

        // Manager를 통해 notification 디바이스로 전달
        mNotificationManager.notify(0, notifyBuilder.build());

        // 진동 설정 1초
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vib.vibrate(1000);

    }

}
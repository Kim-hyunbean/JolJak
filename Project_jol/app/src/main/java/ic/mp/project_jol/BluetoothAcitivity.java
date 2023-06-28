package ic.mp.project_jol;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothAcitivity extends AppCompatActivity {

    TextView textStatus;
    Button btnParied, btnSearch, btnSend;
    ListView listView;


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

    int pairedDeviceCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insetting);
        //settingLayout();
        checkRunTimePermission();

        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //블루투스 어댑터를 디폴트 어댑터로 설정

        btnParied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                        if (ActivityCompat.checkSelfPermission(BluetoothAcitivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        startActivityForResult(intent, REQUEST_ENABLE_BT);
                        selectBluetoothDevice();
                    }

                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String a = "a";
                byte[] bytes = a.getBytes();           //converts entered String into bytes
                try {
                    outputStream.write(bytes);
                    Log.d("blue", "A입력");
                } catch (IOException e) {
                    Log.d("blue", "A입력 실패");
                }
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String b = "b";
                byte[] bytes = b.getBytes();           //converts entered String into bytes
                try {
                    outputStream.write(bytes);
                    Log.d("blue", "B입력");
                } catch (IOException e) {
                    Log.d("blue", "B입력 실패");
                }
            }
        });

    }

    void checkRunTimePermission() {
        //런타임 퍼미션 처리
        int hasbluetoothPermission = ContextCompat.checkSelfPermission(BluetoothAcitivity.this, Manifest.permission.BLUETOOTH);
        int hasBlueToothConnectPermission = ContextCompat.checkSelfPermission(BluetoothAcitivity.this, Manifest.permission.BLUETOOTH_CONNECT);

        ActivityCompat.requestPermissions(BluetoothAcitivity.this, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
        }, 1);

    }

    public void selectBluetoothDevice() {
        //이미 페어링 되어있는 블루투스 기기를 탐색
        if (ActivityCompat.checkSelfPermission(BluetoothAcitivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("페어링 된 블루투스 디바이스 목록");
            //페어링 된 각각의 디바이스의 이름과 주소를 저장
            List<String> list = new ArrayList<>();
            //모든 디바이스의 이름을 리스트에 추가
            for (BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }
            list.add("취소");

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
            //뒤로가기 버튼 누를때 창이 안닫히도록 설정
            builder.setCancelable(false);
            //다이얼로그 생성
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

    }

    public void connectDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(BluetoothAcitivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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
        Toast.makeText(getApplicationContext(), bluetoothDevice.getName() + " 연결 완료!", Toast.LENGTH_SHORT).show();
        //UUID생성
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        //Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            receiveData();
        } catch (IOException e) {
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





//    public void settingLayout() {
//        textStatus = findViewById(R.id.text_status);
//        btnParied = findViewById(R.id.btn_paired);
//        btnSearch = findViewById(R.id.btn_search);
//        btnSend = findViewById(R.id.btn_send);
//        listView = findViewById(R.id.listview);
//    }
}

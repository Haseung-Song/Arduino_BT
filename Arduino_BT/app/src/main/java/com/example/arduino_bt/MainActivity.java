package com.example.arduino_bt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // UI 전역 변수 선언
    TextView mTvBluetoothStatus;
    TextView mTvReceiveData;
    TextView mTvSendData;
    Button mBtnBluetoothOn;
    Button mBtnBluetoothOff;
    Button mBtnConnect;
    Button mBtnSearch;
    Button mBtnSendData;
    ListView mList_vBtItem;

    // 블루투스 연결 및 검색에 필요한 객체 선언
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevice;
    List<BluetoothDevice> mExtraDevice;
    ArrayAdapter<String> mBtAryAdapter;
    ArrayList<String> ListPairedDevice;

    // 블루투스 통신 및 전송에 필요한 객체 선언
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    // 스레드 및 핸들러 객체 선언
    ConnectedThread mThreadConnectedBt;
    Handler mBluetoothHandler;

    // 블루투스 시리얼 통신에 필요한 UUID 상수 값 선언 (스마트폰 - 아두이노 간 통신)
    final static UUID BT_UUID1 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // 블루투스 시리얼 통신에 필요한 UUID 상수 값 선언 (스마트폰 - 스마트폰 간 통신)
    final static UUID BT_UUID2 = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66");

    // 블루투스 request code 상수 선언
    final static int BT_REQUEST_ENABLE = 1;
    final static int AC_REQUEST_ENABLE = 2;
    final static int BT_MESSAGE_READ = 3;
    final static int BT_CONNECTING_STATUS = 5;

    // boolean 변수 값 반전시키기
    private boolean isSearching;

    // 위치 서비스 활성화에 필요한 객체 선언
    LocationManager locationManager;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 바인딩
        mTvBluetoothStatus = (TextView) findViewById(R.id.tvBluetoothStatus);
        mTvReceiveData = (TextView) findViewById(R.id.tvReceiveData);
        mTvSendData = (EditText) findViewById(R.id.tvSendData);
        mBtnBluetoothOn = (Button) findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = (Button) findViewById(R.id.btnBluetoothOff);
        mBtnConnect = (Button) findViewById(R.id.btnConnect);
        mBtnSearch = (Button) findViewById(R.id.btnSearch);
        mBtnSendData = (Button) findViewById(R.id.btnSendData);
        mList_vBtItem = (ListView) findViewById(R.id.list_vBT_item);

        // 해당 장치가 블루투스 기능을 지원하는지 알아오는 메서드
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 블루투스 ON : mBtnBluetoothOn 버튼 이벤트 처리
        mBtnBluetoothOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();

            }
        });

        // 블루투스 OFF : mBtnBluetoothOff 버튼 이벤트 처리
        mBtnBluetoothOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();

            }
        });
        // 연결하기 : mBtnConnect 버튼 이벤트 처리
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPairedDevice();

            }
        });

        // App 위치 권한 허용 코드 => (단, App 처음 실행 시에 한 번만 추가)
        String[] Permission_List = {Manifest.permission.ACCESS_FINE_LOCATION};

        ActivityCompat.requestPermissions(MainActivity.this, Permission_List, AC_REQUEST_ENABLE);

        // 검색하기 : mBtnSearch 버튼 이벤트 처리
        mBtnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 검색하기 버튼을 클릭할 때마다 메서드를 번갈아가면서 실행합니다.
                isSearching = !isSearching;
                if (isSearching) {
                    // 위치 서비스 활성화창 들어가기
                    checkLocationService();

                } else {
                    // 주변의 블루투스 기기 검색하기
                    listSearchDevice();

                }
            }
        });

        // 전송하기 : mBtnSendData 버튼 이벤트 처리
        mBtnSendData.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                // 스레드 통신을 이용하여 전송하기 버튼 클릭 시 보낼 텍스트 메시지를 입력합니다.
                if (mThreadConnectedBt != null) {
                    mThreadConnectedBt.write(mTvSendData.getText().toString());
                    mTvSendData.setText("");

                }
            }
        });

        // 정확히는 모르지만 소켓 연결 성공 시 획득한 obtainMessage(를) 변수 readMessage(에) 저장 후
        // 소켓 연결에 성공하면 mTvReceiveData(에) readMessage 메시지를 전달하는 방식으로 예상합니다.
        mBluetoothHandler = new Handler(msg -> {
            if (msg.what == BT_MESSAGE_READ) {
                String readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                mTvReceiveData.setText("Receive msg : " + readMessage);

            }
            return true;

        });
        // 리스트뷰의 아이템 목록(= 페어링 된 블루투스 기기)중 하나를 클릭할 때 나타나는 이벤트 처리
        mList_vBtItem.setOnItemClickListener(new myOnItemClickListener());

    }

    // 블루투스 ON 버튼을 클릭할 때 나타나는 이벤트
    private void bluetoothOn() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth.",
                    Toast.LENGTH_SHORT).show();

        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BT_REQUEST_ENABLE); // requestCode == 1

        } else {
            mBluetoothAdapter.isEnabled();
            mTvBluetoothStatus.setText("블루투스 활성화");
            Toast.makeText(getApplicationContext(), "블루투스는 이미 활성화가 되었습니다.",
                    Toast.LENGTH_SHORT).show();

        }
    }

    // 블루투스 OFF 버튼 클릭 시에 나타나는 이벤트
    private void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            mTvBluetoothStatus.setText("블루투스 비활성화");
            Toast.makeText(getApplicationContext(), "사용자가 블루투스를 비활성화 합니다.",
                    Toast.LENGTH_SHORT).show();

        } else {
            mTvBluetoothStatus.setText("블루투스 비활성화");
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 상태입니다.",
                    Toast.LENGTH_SHORT).show();

        }
    }

    /*
    // < ArrayAdapter(와) ArrayList(의) 차이점 >
    // 리스트뷰로부터 어레이리스트가 데이터(블루투스 기기 이름 및 주소)를 저장하며
    // 리스트뷰로부터 어레이어댑터는 저장된 데이터를 화면에 뿌려주는 역할을 한다.
    */

    // 연결하기 버튼을 클릭함으로써 기존 디바이스에 이미 존재하는
    // 페어링 된 블루투스 기기 목록들을 나열하는 블루투스 이벤트
    private void listPairedDevice() {
        Divide_Case_by(); // mBluetoothAdapter
        // 만약 페어링 된 블루투스 기기가 하나라도 있으면
        if (mPairedDevice != null && mPairedDevice.size() > 0) {
            // Android OS의 일부인 내장 XML 레이아웃 문서에 대한 참조 객체 선언 => 페어링 된 블루투스 기기 리스트 나열 가능!
            mBtAryAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
            // 리스트뷰의 아이템 목록을 블루투스어레이어댑터에 적용시킨다.
            mList_vBtItem.setAdapter(mBtAryAdapter);
            // ArrayList 객체 생성 및 초기화 1
            ListPairedDevice = new ArrayList<>();
            // ArrayList 객체 생성 및 초기화 2
            mExtraDevice = new ArrayList<>();

            for (BluetoothDevice device : mPairedDevice) {
                // ArrayList에 리스트뷰 주변의 블루투스 기기 주소를 저장한다.
                ListPairedDevice.add(device.getAddress());
                // ArrayAdapter에 리스트뷰 주변의 블루투스 기기 이름을 저장하고 화면에 뿌려준다.
                mBtAryAdapter.add(device.getName());
//              mBtAryAdapter.add(device.getName() + "\n" + device.getAddress());
                // 리스트뷰에 저장된 주변의 블루투스 기기 데이터를 최신화한다.
                mBtAryAdapter.notifyDataSetChanged();

            }
        }
    }

    // 블루투스어댑터의 활성화 여부에 따라 사건을 나눈다.
    private void Divide_Case_by() {
        if (mBluetoothAdapter.isEnabled()) {
            // 페어링 된 블루투스 기기의 리스트 가져오기
            mPairedDevice = mBluetoothAdapter.getBondedDevices();
            mTvBluetoothStatus.setText("페어링 기기를 불러왔습니다.");

        } else if (mBluetoothAdapter.disable()) {
            Toast.makeText(getApplicationContext(), "블루투스를 먼저 활성화 시켜야합니다.",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth.",
                    Toast.LENGTH_SHORT).show();

        }
    }

    // Action 수행을 위한 블루투스 브로드캐스트리시버 객체 생성
    final BroadcastReceiver mSearchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                // 블루투스 기기 검색 시작 action
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    // Toast.makeText(getApplicationContext(), "블루투스 기기 검색 시작", Toast.LENGTH_SHORT).show();
                    mTvBluetoothStatus.setText("블루투스 기기 검색 중...");

                    // 검색 전, 화면 초기화하기!
                    if (mBtAryAdapter != null) {
                        mBtAryAdapter.clear();

                    }
                    break;

                // 블루투스 기기 검색 성공 action
                case BluetoothDevice.ACTION_FOUND:
                    // 검색한 주변의 블루투스 기기 객체 생성 1
                    BluetoothDevice searchDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 주변의 블루투스 기기가 null인 경우를 제외한다.
                    assert searchDevice != null;

                    if (mPairedDevice != null && mPairedDevice.size() > 0) {
                        if (searchDevice.getName() != null) {
                            // 주변의 블루투스 기기 이름이 중복 표시되는 오류를 방지한다.
                            if (mBtAryAdapter.getPosition(searchDevice.getName()) == -1) {
                                // ArrayList에 리스트뷰 주변의 블루투스 기기 주소를 저장한다.
                                ListPairedDevice.add(searchDevice.getAddress());
                                // ArrayAdapter에 리스트뷰 주변의 블루투스 기기 이름을 저장하고 화면에 뿌려준다.
                                mBtAryAdapter.add(searchDevice.getName());
//                              mBtAryAdapter.add(searchDevice.getName() + "\n" + searchDevice.getAddress());
                                // 리스트뷰에 저장된 주변의 블루투스 기기 데이터를 최신화한다.
                                mBtAryAdapter.notifyDataSetChanged();

                            }
                        }
                    }

                    // 이유를 모르겠다. null인 경우에 검색 버튼을 누르면 기기 검색이 되지 않는다.
                    // 연결 버튼을 먼저 누른 후에 기기 검색이 잘된다.
                    if (mPairedDevice == null) {
                        // 블루투스가 현재 기기 검색 프로세스에 있다면
                        if (mBluetoothAdapter.isDiscovering()) {
                            // 즉시, 주변 블루투스 기기 검색을 종료한다.
                            mBluetoothAdapter.cancelDiscovery();

                        }
                    }

                    // 리스트(List)로 선언된 블루투스디바이스 객체 변수에
                    // 검색한 주변의 블루투스 기기를 저장한다.
                    mExtraDevice.add(searchDevice);
                    break;

                // 블루투스 기기 검색 종료 action
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // Toast.makeText(getApplicationContext(), "블루투스 기기 검색 종료", Toast.LENGTH_SHORT).show();
                    mTvBluetoothStatus.setText("블루투스 기기 검색을 종료합니다.");
                    // 블루투스 검색하기 버튼 활성화!
                    mBtnSearch.setEnabled(true);
                    break;

                // 블루투스 기기 페어링 상태 변화 action
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    // 검색한 주변의 블루투스 기기 객체 생성 2
                    BluetoothDevice pairedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 주변의 블루투스 기기가 null인 경우를 제외한다.
                    assert pairedDevice != null;

                    // 즉, 페어링 요청 후 연결에 성공하면 다음에 페어링 기기를 불러올 때 데이터가 저장된다.
                    if (pairedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        // 주변의 블루투스 기기 이름이 중복 표시되는 오류를 방지한다.
                        if (mBtAryAdapter.getPosition(pairedDevice.getName()) == -1) {
                            // ArrayList에 리스트뷰 주변의 블루투스 기기 주소를 저장한다.
                            ListPairedDevice.add(pairedDevice.getAddress());
                            // ArrayAdapter에 리스트뷰 주변의 블루투스 기기 이름을 저장하고 화면에 뿌려준다.
                            mBtAryAdapter.add(pairedDevice.getName());
//                          mBtAryAdapter.add(pairedDevice.getName() + "\n" + pairedDevice.getAddress());
                            // 리스트뷰에 저장된 주변의 블루투스 기기 데이터를 최신화한다.
                            mBtAryAdapter.notifyDataSetChanged();

                        }
                    }
                    break;

            }
        }
    };

    // GPS ON/OFF 확인 : OFF => (위치 서비스 활성화 창으로 이동)
    private void checkLocationService() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);

        } else {
            Toast.makeText(getApplicationContext(), "위치 서비스가 이미 활성화 되었습니다.",
                    Toast.LENGTH_SHORT).show();

        }
    }

//    브로드캐스트리시버 해제를 위한 onDestroy 호출
//    등록 전 App 종료 시 일어나는 RuntimeException 오류 때문에 호출하지 않는 편이 낫다.
//    @Override
//    protected void onDestroy() {
//        this.unregisterReceiver(mSearchReceiver);
//        super.onDestroy();
//    }

    // 검색하기 버튼을 클릭함으로써 기존 디바이스에 존재하지 않는
    // 검색된 외부 블루투스 기기 목록을 나열하는 블루투스 이벤트
    private void listSearchDevice() {

        // Action 등록을 위한 블루투스 브로드캐스트리시버 타입 선언
        IntentFilter mSearchFilter = new IntentFilter();
        mSearchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mSearchFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mSearchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mSearchFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mSearchReceiver, mSearchFilter);

        // 블루투스 검색하기 버튼 비활성화!
        mBtnSearch.setEnabled(false);

        // 블루투스가 현재 기기 검색 프로세스에 있는지 확인 후
        if (!mBluetoothAdapter.isDiscovering()) {
            // 검색 프로세스에 없다면 주변 블루투스 기기 검색을 시작
            mBluetoothAdapter.startDiscovery();

        }
    }

    // 블루투스 활성화 및 비활성화에 필요하기 때문에 상수로 선언한 requestCode(에) 대하여
    // 액티비티 결과를 처리하기 위하여 호출하는 메서드로 resultCode 유형에 따라 달라진다.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == BT_REQUEST_ENABLE) {
            if (resultCode == RESULT_OK) {
                mTvBluetoothStatus.setText("블루투스 활성화");
                Toast.makeText(this, "블루투스 활성화 성공", Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "블루투스 활성화 취소", Toast.LENGTH_SHORT).show();

            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }


    // 블루투스 페어링 기기 리스트 중 하나를 클릭 시 나타나는 이벤트 처리
    private class myOnItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @SuppressLint("SetTextI18n")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            // 페어링 기기 클릭 후 "try..." 상태 메시지를 불러옵니다.
            mTvBluetoothStatus.setText("try...");

            // 블루투스가 현재 기기 검색 프로세스에 있다면
            if (mBluetoothAdapter.isDiscovering()) {
                // 그 즉시, 주변 블루투스 검색을 종료한다.
                mBluetoothAdapter.cancelDiscovery();

            }

            // 블루투스 기기 페어링 요청하기
            for (BluetoothDevice pairingDevice : mExtraDevice) {
                try {
                    Method method = pairingDevice.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(pairingDevice, (Object[]) null);

                } catch (Exception e) {
                    e.printStackTrace();
                    break;

                }
            }

            // 페어링된 내부 및 외부 블루투스 기기 이름 가져오기
            final String selectedDeviceName = mBtAryAdapter.getItem(position);

            // 페어링된 내부 및 외부 블루투스 기기 주소 가져오기
            final String selectedDeviceAddress = ListPairedDevice.get(position);

            // 연결에 필요한 값은 블루투스 기기의 주소입니다.
            // for 문으로 페어링 된 모든 블루투스 기기를 검색을 하면서
            // 매개 변수 값과 비교하여 같으면 그 기기의 주소 값을 가져옵니다.

            for (BluetoothDevice bluetoothDevice : mPairedDevice) {
                if (selectedDeviceAddress.equals(bluetoothDevice.getAddress())) {
                    mBluetoothDevice = bluetoothDevice;
                    break;

                }
            }

            // 먼저, UUID(시리얼 통신용 ID)를 호출하여 블루투스소켓 객체를 가져온 후 초기화합니다.
            // 그리고, connect() 메서드를 호출하여 블루투스소켓 연결을 시작합니다.
            // 소켓 연결에 성공하면 ["Connected to" + 블루투스 기기 이름] 상태 메시지를 불러옵니다.
            // 만약 소켓 연결에 실패하면 "Connection failed!" 상태 메시지를 불러옵니다.
            // 또한, 블루투스 연결 실패 상태에 맞는 토스트 메시지를 불러옵니다.

            if (mBluetoothDevice != null) {
                try {
                    mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID1);
                    mBluetoothSocket.connect();
                    mTvBluetoothStatus.setText("Connected to " + selectedDeviceName);
                    // 블루투스 페어링 기기 연결 성공 시 블루투스 기기의 이름을 토스트 메시지로 전달합니다.
                    Toast.makeText(getApplicationContext(),
                            mBtAryAdapter.getItem(position) + " connected!", Toast.LENGTH_LONG).show();

                } catch (IOException e) {
                    mTvBluetoothStatus.setText("Connection failed!");
                    // 블루투스 페어링 기기 연결 실패 시 연결 실패 상태를 토스트 메시지로 전달합니다.
                    Toast.makeText(getApplicationContext(),
                            "블루투스 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }

            // 다음으로, 블루투스소켓을 연결한 스레드 객체를 생성하고 스레드를 시작합니다.
            // 핸들러를 이용하여 (BT_CONNECTING_STATUS 값, 참조 값 등)이 지정된 메시지 객체를 획득합니다.

            if (mBluetoothSocket != null) {
                mThreadConnectedBt = new ConnectedThread(mBluetoothSocket);
                mThreadConnectedBt.start();
                mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();

            }
        }
    }

    // 블루투스 통신을 위한 스레드 클래스 생성
    private class ConnectedThread extends Thread {

        // 스레드 통신에 필요한 객체 선언
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // 스레드 초기화 과정 => 데이터 송수신을 위한 작업을 수행합니다.
        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중에 오류가 발생했습니다.",
                        Toast.LENGTH_LONG).show();

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        // 수신 데이터가 존재할 때 데이터를 읽어오는 작업을 수행합니다.
        @Override
        public void run() {
            Queue<byte[]> bufferQueue = new LinkedList<>();
            int bytes;

            while (true) {
                try {
                    int availableBytes = mmInStream.available();
                    if (availableBytes > 0) {
                        // 버퍼 속도에 따라 초기 데이터 손실이 있을 수 있으므로, 약 0.2초간 버퍼를 지연시킴.
                        SystemClock.sleep(200);
                        bufferQueue.offer(new byte[1024]);
                        bytes = mmInStream.read(bufferQueue.peek());
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ,
                                bytes, -1, bufferQueue.poll()).sendToTarget();

                    }

                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),
                            "데이터 수신 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                    break;

                }
            }
        }

        // 데이터 전송을 위한 스레드 작업을 수행합니다.
        public void write(String str) {
            byte[] bytes = str.getBytes();

            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.",
                        Toast.LENGTH_LONG).show();

            }
        }

        // 데이터 송수신 완료 후 블루투스소켓을 닫는 작업을 수행합니다.
        public void cancel() {
            try {
                mmSocket.close();

            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중에 오류가 발생했습니다.",
                        Toast.LENGTH_LONG).show();

            }
        }
    }
}
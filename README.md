아두이노 블루투스 통신 (Arduino_BT_Serial_Socket_Communication)
실제 인턴십 프로그램을 진행하면서 작성했던 보고서, 파일, 아두이노_코드 등을 아래에 첨부합니다.

[2021_02_19_Android_Arduino_Bluetooth_App_HSong.pptx](https://github.com/Haseung-Song/Arduino_BT/files/10055831/2021_02_19_Android_Arduino_Bluetooth_App_HSong.pptx)

[Arduino_LED_Control.pptx](https://github.com/Haseung-Song/Arduino_BT/files/10055833/Arduino_LED_Control.pptx)

[3~5주차 Arduino_BT 보고서.docx](https://github.com/Haseung-Song/Arduino_BT/files/10055834/3.5.Arduino_BT.docx)

[3~5주차 Arduino_Code (AT Mode Included) 보고서.docx](https://github.com/Haseung-Song/Arduino_BT/files/10055838/3.5.Arduino_Code.AT.Mode.Included.docx)


1. Arduino_Master_NANO 코드(LED 제어 기능 포함)
#include <mcp_can.h>
#include <mcp_can_dfs.h>

#include <SoftwareSerial.h>                        // 시리얼 통신 라이브러리 호출

String cmd;                                        // 문자를 읽기 위한 String형 변수 선언
int RX = 7;                                        // RX (수신 핀 설정)
int TX = 8;                                        // TX (송신 핀 설정)
int value1 = 0;
int FSRsensor1 = A0;
int value2 = 0;
int FSRsensor2 = A1;

SoftwareSerial myBTserial(RX, TX);                 // 시리얼 통신을 위한 객체 선언

void setup() {
  Serial.begin(115200);                             // 시리얼 모니터
  myBTserial.begin(115200);                         // 블루투스 모듈
  pinMode(LED_BUILTIN, OUTPUT);                    // 디지털 핀(= LED_BUTILTIN)을 출력으로 초기화

}

void loop() {
  value1 = analogRead(FSRsensor1);
  value2 = analogRead(FSRsensor2);
  if (value1 >= 300) {
    myBTserial.write("j");
    delay(10);

  } else if (value2 >= 300) {
    myBTserial.write("k");
    delay(10);

  }

  if (myBTserial.available()) {                    // 블루투스 모듈로부터 송신한 데이터가 있으면
    cmd = myBTserial.readString();                 // 시리얼 모니터가 수신한 데이터를 읽고 변수 cmd에 저장 (Scanf 형식)

    if (cmd == "ON") {                             // 문자 "ON"을 입력하면
      Serial.println("Command = " + cmd);          // "Command = 메시지 내용(수신 데이터)" 출력
      delay(100);                                  // 0.1초 지연
      digitalWrite(LED_BUILTIN, HIGH);             // LED 켜짐.

    }

    else if (cmd == "BLINK") {                     // 문자 "BLINK"를 입력하면
      Serial.println("Command = " + cmd);          // "Command = 메시지 내용(수신 데이터)" 출력

      while (1) {                                  // 무한 루프문으로 LED BLINK 반복 수행
        digitalWrite(LED_BUILTIN, HIGH);           // LED 켜짐.
        delay(1000);                               // 1.0초 지연

        // 무한 루프문을 빠져나갈 때, break가 걸리므로 (실제 LED가 꺼진 후) delay는 0.1초보다 길어진다.
        // 따라서, LED를 켤 때 1초, 끌 때 0.1초로 지연 시간을 맞추면 BLINK 주기가 거의 비슷해진다.

        digitalWrite(LED_BUILTIN, LOW);            // LED 꺼짐.
        delay(100);                                // 0.1초 지연

        cmd = myBTserial.readString();             // 무한 루프문을 빠져나가려면 반드시 한 번더 수신한 데이터를 읽어야 한다. (Scanf 형식)

        if (cmd == "OFF") {                        // 문자 "OFF"를 입력하면
          Serial.println("Command = " + cmd);      // "Command = 메시지 내용(수신 데이터)" 출력
          break;                                   // 무한 루프문 break;

        } else if (cmd == "ON") {                  // 문자 "ON"을 입력하면
          Serial.println("Command = " + cmd);      // "Command = 메시지 내용(수신 데이터)" 출력
          delay(100);                              // 0.1초 지연
          digitalWrite(LED_BUILTIN, HIGH);         // LED 켜짐.
          break;                                   // 무한 루프문 break;

        }
      }
    }

    else if (cmd == "OFF") {                       // 문자 "OFF"를 입력하면
      Serial.println("Command = " + cmd);          // "Command = 메시지 내용(수신 데이터)" 출력
      delay(100);                                  // 0.1초 지연
      digitalWrite(LED_BUILTIN, LOW);              // LED 꺼짐.

    } else {                                       // 이외의 문자를 입력하면
      delay(100);                                  // 0.1초 지연
      Serial.println(cmd);                         // cmd에 저장된 수신 데이터를 출력

    }
  }

  if (Serial.available()) {                        // 시리얼 모니터로부터 송신한 데이터가 있으면
    myBTserial.write(Serial.read());               // 블루투스측이 수신한 데이터를 읽음.

  }
}

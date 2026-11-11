package com.crt.serial;

import com.crt.ai.Verdict;

// 串口抽象：合理发 'O'，离谱发 'E'，并等待下位机 ACK
public interface SerialPortService {

    SerialResult send(Verdict verdict);

    boolean isOpen();
}

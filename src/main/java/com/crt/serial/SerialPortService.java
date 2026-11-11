package com.crt.serial;

import com.crt.ai.Verdict;

// 串口抽象：合理发 'O'，离谱发 'E'
public interface SerialPortService {

    boolean send(Verdict verdict);

    boolean isOpen();
}

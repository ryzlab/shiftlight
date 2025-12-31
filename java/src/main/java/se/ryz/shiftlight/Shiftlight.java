package se.ryz.shiftlight;

import com.fazecast.jSerialComm.SerialPort;

public class Shiftlight {
    public static void main(String[] args) {
        System.out.println("Hello, Shiftlight!");
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println("Port: " + port.getSystemPortName());
        }
    }
}


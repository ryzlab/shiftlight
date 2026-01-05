package se.ryz.shiftlight;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;

public class SerialPortComboBox extends JComboBox<String> {
    
    public SerialPortComboBox() {
        super();
        setEditable(false);
        refreshPorts();
    }

    @Override
    public void showPopup() {
        // Refresh ports before showing the popup
        refreshPorts();
        super.showPopup();
    }

    public void refreshPorts() {
        String selectedPort = (String) getSelectedItem();
        
        removeAllItems();
        
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            addItem(port.getSystemPortName());
        }
        
        // Restore selection if it still exists
        if (selectedPort != null) {
            for (int i = 0; i < getItemCount(); i++) {
                if (getItemAt(i).equals(selectedPort)) {
                    setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public String getSelectedPortName() {
        return (String) getSelectedItem();
    }
}


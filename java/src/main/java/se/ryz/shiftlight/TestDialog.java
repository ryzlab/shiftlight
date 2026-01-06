package se.ryz.shiftlight;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TestDialog extends JDialog {
    private JTextField textField;
    private JSlider slider;
    private boolean updatingFromTextField = false;
    private boolean updatingFromSlider = false;
    private SerialPortComboBox serialPortComboBox;
    private SerialPort serialPort = null;

    public TestDialog(JFrame parent, SerialPortComboBox serialPortComboBox) {
        super(parent, "Test", true);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        this.serialPortComboBox = serialPortComboBox;
        
        // Close port when dialog is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeSerialPort();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                closeSerialPort();
            }
        });
        
        initializeComponents();
        layoutComponents();
        
        pack();
        // Make dialog twice as wide
        Dimension size = getSize();
        setSize(size.width * 2, size.height);
        setLocationRelativeTo(parent);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            openSerialPort();
        } else {
            closeSerialPort();
        }
        super.setVisible(visible);
    }

    private void initializeComponents() {
        // Text field
        textField = new JTextField(10);
        textField.setText("0");
        textField.setHorizontalAlignment(JTextField.CENTER);
        
        // Add document listener to sync text field -> slider
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSliderFromTextField();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSliderFromTextField();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSliderFromTextField();
            }
        });
        
        // Add focus listener to validate on focus loss
        textField.addActionListener(e -> validateAndUpdateTextField());
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateAndUpdateTextField();
            }
        });

        // Slider
        slider = new JSlider(JSlider.HORIZONTAL, 0, 9999, 0);
        slider.setMajorTickSpacing(2000);
        slider.setMinorTickSpacing(500);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        
        // Add change listener to sync slider -> text field
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!updatingFromTextField) {
                    updateTextFieldFromSlider();
                }
            }
        });

        // OK button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }

    private void updateSliderFromTextField() {
        if (updatingFromSlider) {
            return;
        }
        
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        try {
            int value = Integer.parseInt(text);
            if (value >= 0 && value <= 9999) {
                updatingFromTextField = true;
                slider.setValue(value);
                updatingFromTextField = false;
                sendRpmValue(value);
            }
        } catch (NumberFormatException e) {
            // Invalid number, ignore
        }
    }

    private void updateTextFieldFromSlider() {
        if (updatingFromTextField) {
            return;
        }
        
        updatingFromSlider = true;
        int value = slider.getValue();
        textField.setText(String.valueOf(value));
        updatingFromSlider = false;
        sendRpmValue(value);
    }

    private void validateAndUpdateTextField() {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            textField.setText("0");
            slider.setValue(0);
            return;
        }
        
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                textField.setText("0");
                slider.setValue(0);
            } else if (value > 9999) {
                textField.setText("9999");
                slider.setValue(9999);
            } else {
                slider.setValue(value);
                sendRpmValue(value);
            }
        } catch (NumberFormatException e) {
            // Invalid number, reset to current slider value
            int currentValue = slider.getValue();
            textField.setText(String.valueOf(currentValue));
        }
    }
    
    private void openSerialPort() {
        String selectedPort = serialPortComboBox.getSelectedPortName();
        if (selectedPort == null || selectedPort.isEmpty()) {
            return;
        }
        
        serialPort = SerialPort.getCommPort(selectedPort);
        if (serialPort == null) {
            return;
        }
        
        serialPort.setBaudRate(9600);
        if (serialPort.openPort()) {
            try {
                Thread.sleep(100); // Give port time to initialize
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            serialPort = null;
        }
    }
    
    private void closeSerialPort() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            serialPort = null;
        }
    }
    
    private void sendRpmValue(int value) {
        if (serialPort == null || !serialPort.isOpen()) {
            return;
        }
        
        String message = "rpm=" + value + "\n";
        System.out.println(message.trim());
        
        try {
            serialPort.getOutputStream().write(message.getBytes());
            serialPort.getOutputStream().flush();
        } catch (Exception e) {
            System.err.println("Error sending RPM value: " + e.getMessage());
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Center panel with text field and slider
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Text field panel
        JPanel textFieldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        textFieldPanel.add(new JLabel("Value:"));
        textFieldPanel.add(textField);
        centerPanel.add(textFieldPanel);
        
        centerPanel.add(Box.createVerticalStrut(10));
        
        // Slider panel
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(slider, BorderLayout.CENTER);
        centerPanel.add(sliderPanel);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // OK button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}


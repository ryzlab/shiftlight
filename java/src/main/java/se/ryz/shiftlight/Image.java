package se.ryz.shiftlight;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
public class Image {
    private List<Integer> ledIndices;
    private int startRPM;
    private int endRPM;
    private int startRed;
    private int startGreen;
    private int startBlue;
    private int endRed;
    private int endGreen;
    private int endBlue;
    private String blinkMode;
    private Integer optionalValue; // Optional value (0-255) when blinkMode != 0

    public Image(String csvLine) {
        parseCsvLine(csvLine, null);
    }

    public Image(String csvLine, VariableParser variableParser) {
        parseCsvLine(csvLine, variableParser);
    }

    private void parseCsvLine(String csvLine, VariableParser variableParser) {
        String trimmed = csvLine.trim();
        
        // Find the bracket part (first value)
        int bracketStart = trimmed.indexOf('[');
        int bracketEnd = trimmed.indexOf(']');
        
        if (bracketStart != 0 || bracketEnd == -1) {
            throw new IllegalArgumentException("First value must be in brackets: [1,2,4-6,8]");
        }
        
        // Extract the bracket part (including brackets)
        String ledPart = trimmed.substring(bracketStart, bracketEnd + 1);
        
        // Extract the rest after the closing bracket and comma
        String rest = trimmed.substring(bracketEnd + 1).trim();
        if (!rest.startsWith(",")) {
            throw new IllegalArgumentException("Missing comma after bracket part");
        }
        rest = rest.substring(1); // Remove the comma
        
        // Split the remaining part by commas (these are the actual separators)
        String[] parts = rest.split(",");
        
        // Parse LED indices from the bracket part
        ledIndices = parseLedIndices(ledPart.substring(1, ledPart.length() - 1));
        
        // Parse the remaining values, supporting variable expressions
        // First 8 values are always required
        if (parts.length < 9) {
            throw new IllegalArgumentException("CSV line must have at least 10 values (9 commas outside brackets)");
        }
        
        startRPM = parseNumericValue(parts[0].trim(), variableParser);
        endRPM = parseNumericValue(parts[1].trim(), variableParser);
        startRed = parseNumericValue(parts[2].trim(), variableParser);
        startGreen = parseNumericValue(parts[3].trim(), variableParser);
        startBlue = parseNumericValue(parts[4].trim(), variableParser);
        endRed = parseNumericValue(parts[5].trim(), variableParser);
        endGreen = parseNumericValue(parts[6].trim(), variableParser);
        endBlue = parseNumericValue(parts[7].trim(), variableParser);
        
        // Validate RPM values (range 0-9999)
        if (startRPM < 0 || startRPM > 9999) {
            throw new IllegalArgumentException("Start RPM must be in range 0-9999, got: " + startRPM);
        }
        if (endRPM < 0 || endRPM > 9999) {
            throw new IllegalArgumentException("End RPM must be in range 0-9999, got: " + endRPM);
        }
        
        // Validate startRPM <= endRPM
        if (startRPM > endRPM) {
            throw new IllegalArgumentException("Start RPM (" + startRPM + ") must be less than or equal to End RPM (" + endRPM + ")");
        }
        
        // Validate RGB values (range 0-255)
        if (startRed < 0 || startRed > 255) {
            throw new IllegalArgumentException("Start Red must be in range 0-255, got: " + startRed);
        }
        if (startGreen < 0 || startGreen > 255) {
            throw new IllegalArgumentException("Start Green must be in range 0-255, got: " + startGreen);
        }
        if (startBlue < 0 || startBlue > 255) {
            throw new IllegalArgumentException("Start Blue must be in range 0-255, got: " + startBlue);
        }
        if (endRed < 0 || endRed > 255) {
            throw new IllegalArgumentException("End Red must be in range 0-255, got: " + endRed);
        }
        if (endGreen < 0 || endGreen > 255) {
            throw new IllegalArgumentException("End Green must be in range 0-255, got: " + endGreen);
        }
        if (endBlue < 0 || endBlue > 255) {
            throw new IllegalArgumentException("End Blue must be in range 0-255, got: " + endBlue);
        }
        
        // Validate blinkMode (9th value) is in range 0-2
        String blinkModeStr = parts[8].trim();
        int blinkModeValue;
        try {
            blinkModeValue = Integer.parseInt(blinkModeStr);
            if (blinkModeValue < 0 || blinkModeValue > 2) {
                throw new IllegalArgumentException("Blink mode must be in range 0-2, got: " + blinkModeValue);
            }
            blinkMode = blinkModeStr;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Blink mode must be a number in range 0-2, got: " + blinkModeStr);
        }
        
        // If blinkMode is not 0, expect an optional value (10th value)
        optionalValue = null;
        if (blinkModeValue != 0) {
            if (parts.length < 10) {
                throw new IllegalArgumentException("When blink mode is not 0, an additional value (0-255) is required");
            }
            int optional = parseNumericValue(parts[9].trim(), variableParser);
            if (optional < 0 || optional > 255) {
                throw new IllegalArgumentException("Optional value must be in range 0-255, got: " + optional);
            }
            optionalValue = optional;
            
            // Validate we don't have extra values
            if (parts.length > 10) {
                throw new IllegalArgumentException("CSV line has too many values. Expected 10 values when blink mode is not 0, got: " + (parts.length + 1));
            }
        } else {
            // When blinkMode is 0, we should have exactly 9 values
            if (parts.length > 9) {
                throw new IllegalArgumentException("CSV line has too many values. When blink mode is 0, expected 9 values, got: " + (parts.length + 1));
            }
        }
    }

    private List<Integer> parseLedIndices(String ledString) {
        List<Integer> indices = new ArrayList<>();
        String[] elements = ledString.split(",");
        
        for (String element : elements) {
            element = element.trim();
            if (element.contains("-")) {
                // Parse range (e.g., "4-6")
                String[] range = element.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("Invalid range format: " + element);
                }
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                
                if (start > end) {
                    throw new IllegalArgumentException("Range start must be <= end: " + element);
                }
                
                for (int i = start; i <= end; i++) {
                    if (i < 1 || i > 13) {
                        throw new IllegalArgumentException("LED index must be between 1 and 13: " + i);
                    }
                    indices.add(i);
                }
            } else {
                // Parse single number
                int index = Integer.parseInt(element);
                if (index < 1 || index > 13) {
                    throw new IllegalArgumentException("LED index must be between 1 and 13: " + index);
                }
                indices.add(index);
            }
        }
        
        return indices;
    }

    private int parseNumericValue(String value, VariableParser variableParser) {
        if (variableParser != null) {
            try {
                return variableParser.evaluateExpression(value);
            } catch (IllegalArgumentException e) {
                // If variable parsing fails, try as plain integer
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid numeric value or expression: " + value + ". " + e.getMessage());
                }
            }
        } else {
            // No variable parser, parse as plain integer
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric value: " + value);
            }
        }
    }

    /**
     * Generates a CSV line representation of this Image.
     * Consecutive LED indices of 3 or more are represented as ranges (e.g., 3,4,5 -> 3-5).
     * 
     * @return CSV line in the format: [1,2,4-6,8],startRPM,endRPM,startRed,startGreen,startBlue,endRed,endGreen,endBlue,blinkMode
     */
    public String toCsvLine() {
        StringBuilder sb = new StringBuilder();
        
        // Format LED indices with ranges for consecutive sequences of 3+
        sb.append("[");
        sb.append(formatLedIndices());
        sb.append("]");
        
        // Append the rest of the values
        sb.append(",").append(startRPM);
        sb.append(",").append(endRPM);
        sb.append(",").append(startRed);
        sb.append(",").append(startGreen);
        sb.append(",").append(startBlue);
        sb.append(",").append(endRed);
        sb.append(",").append(endGreen);
        sb.append(",").append(endBlue);
        sb.append(",").append(blinkMode);
        if (optionalValue == null) {
            sb.append(",0");
        } else {
            sb.append(",").append(optionalValue);
        }
        return sb.toString();
    }

    private String formatLedIndices() {
        if (ledIndices.isEmpty()) {
            return "";
        }
        
        // Create a sorted copy to find consecutive sequences
        List<Integer> sorted = new ArrayList<>(ledIndices);
        sorted.sort(Integer::compareTo);
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        
        while (i < sorted.size()) {
            int start = sorted.get(i);
            int end = start;
            int count = 1;
            
            // Find consecutive sequence
            while (i + count < sorted.size() && sorted.get(i + count) == start + count) {
                end = sorted.get(i + count);
                count++;
            }
            
            if (count >= 3) {
                // Format as range (e.g., 3-5)
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(start).append("-").append(end);
            } else {
                // Format as individual numbers
                for (int j = 0; j < count; j++) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(sorted.get(i + j));
                }
            }
            
            i += count;
        }
        
        return sb.toString();
    }
}


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

    public Image(String csvLine) {
        parseCsvLine(csvLine);
    }

    private void parseCsvLine(String csvLine) {
        String[] parts = csvLine.split(",");
        
        if (parts.length != 10) {
            throw new IllegalArgumentException("CSV line must have exactly 10 values (9 commas)");
        }

        // Parse LED indices from the first part (format: [1,2,4-6,8])
        String ledPart = parts[0].trim();
        if (!ledPart.startsWith("[") || !ledPart.endsWith("]")) {
            throw new IllegalArgumentException("First value must be in brackets: [1,2,4-6,8]");
        }
        
        ledIndices = parseLedIndices(ledPart.substring(1, ledPart.length() - 1));
        
        // Parse the remaining values
        startRPM = Integer.parseInt(parts[1].trim());
        endRPM = Integer.parseInt(parts[2].trim());
        startRed = Integer.parseInt(parts[3].trim());
        startGreen = Integer.parseInt(parts[4].trim());
        startBlue = Integer.parseInt(parts[5].trim());
        endRed = Integer.parseInt(parts[6].trim());
        endGreen = Integer.parseInt(parts[7].trim());
        endBlue = Integer.parseInt(parts[8].trim());
        blinkMode = parts[9].trim();
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


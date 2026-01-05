package se.ryz.shiftlight;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class VariableParser {
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern VARIABLE_LINE_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9_-]+)\\s*=\\s*(\\d+)\\s*$");
    
    private final Map<String, Integer> variables;

    public VariableParser() {
        this.variables = new HashMap<>();
    }

    /**
     * Parses variable definitions from a multi-line string.
     * Each line should be in format: variableName=variableValue
     * 
     * @param variablesText the text containing variable definitions
     */
    public void parseVariables(String variablesText) {
        variables.clear();
        if (variablesText == null || variablesText.trim().isEmpty()) {
            return;
        }

        String[] lines = variablesText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // Skip empty lines and comments
            }

            // Match pattern: variableName=variableValue
            java.util.regex.Matcher matcher = VARIABLE_LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                String name = matcher.group(1);
                int value = Integer.parseInt(matcher.group(2));
                
                // Validate variable name
                if (!VARIABLE_NAME_PATTERN.matcher(name).matches()) {
                    throw new IllegalArgumentException("Invalid variable name: " + name + ". Valid characters: a-z, A-Z, 0-9, _, -");
                }
                
                // Validate variable value (0-9999)
                if (value < 0 || value > 9999) {
                    throw new IllegalArgumentException("Variable value must be in range 0-9999, got: " + value);
                }
                
                variables.put(name, value);
            } else {
                throw new IllegalArgumentException("Invalid variable format: " + line + ". Expected: variableName=variableValue");
            }
        }
    }

    /**
     * Evaluates an expression that may contain variables and arithmetic operations.
     * Supports: variable names, integers, +, -, *, /, parentheses
     * 
     * @param expression the expression to evaluate
     * @return the evaluated integer value
     * @throws IllegalArgumentException if the expression is invalid
     */
    public int evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }
        
        expression = expression.trim();
        
        // Simple expression evaluator for: variable names, integers, +, -, *, /
        // Handle parentheses first, then multiplication/division, then addition/subtraction
        return evaluateExpressionRecursive(expression);
    }

    private int evaluateExpressionRecursive(String expr) {
        expr = expr.trim();
        
        // Remove outer parentheses if present
        while (expr.startsWith("(") && expr.endsWith(")")) {
            int depth = 0;
            boolean valid = true;
            for (int i = 0; i < expr.length(); i++) {
                if (expr.charAt(i) == '(') depth++;
                if (expr.charAt(i) == ')') depth--;
                if (depth == 0 && i < expr.length() - 1) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                expr = expr.substring(1, expr.length() - 1).trim();
            } else {
                break;
            }
        }
        
        // Check if it's a simple value (variable or integer)
        if (isSimpleValue(expr)) {
            return parseSimpleValue(expr);
        }
        
        // Handle operators in order of precedence: *, /, +, -
        // Start with addition/subtraction (lowest precedence)
        int addSubIndex = findLowestPrecedenceOperator(expr, "+-");
        if (addSubIndex > 0) {
            char op = expr.charAt(addSubIndex);
            int left = evaluateExpressionRecursive(expr.substring(0, addSubIndex));
            int right = evaluateExpressionRecursive(expr.substring(addSubIndex + 1));
            return op == '+' ? left + right : left - right;
        }
        
        // Handle multiplication/division
        int mulDivIndex = findLowestPrecedenceOperator(expr, "*/");
        if (mulDivIndex > 0) {
            char op = expr.charAt(mulDivIndex);
            int left = evaluateExpressionRecursive(expr.substring(0, mulDivIndex));
            int right = evaluateExpressionRecursive(expr.substring(mulDivIndex + 1));
            return op == '*' ? left * right : left / right;
        }
        
        // If we get here, it should be a simple value
        return parseSimpleValue(expr);
    }

    private boolean isSimpleValue(String expr) {
        expr = expr.trim();
        // Check if it's a variable name or an integer
        return VARIABLE_NAME_PATTERN.matcher(expr).matches() || 
               expr.matches("^-?\\d+$");
    }

    private int parseSimpleValue(String expr) {
        expr = expr.trim();
        
        // First, try to parse as integer (numbers take precedence)
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            // Not a number, continue to check for variables
        }
        
        // Check if it's a variable
        if (variables.containsKey(expr)) {
            return variables.get(expr);
        }
        
        // Check if it's a valid variable name (but not defined)
        if (VARIABLE_NAME_PATTERN.matcher(expr).matches()) {
            throw new IllegalArgumentException("Undefined variable: " + expr);
        }
        
        // If we get here, it's invalid
        throw new IllegalArgumentException("Invalid expression: " + expr);
    }

    private int findLowestPrecedenceOperator(String expr, String operators) {
        int depth = 0;
        int lastIndex = -1;
        
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && operators.indexOf(c) >= 0) {
                lastIndex = i;
            }
        }
        
        return lastIndex;
    }

    public Map<String, Integer> getVariables() {
        return new HashMap<>(variables);
    }
}


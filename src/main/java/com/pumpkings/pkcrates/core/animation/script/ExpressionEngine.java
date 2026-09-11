package com.pumpkings.pkcrates.core.animation.script;

import java.util.Map;
import java.util.Random;

/**
 * Small sandboxed arithmetic expression evaluator used by animation scripts.
 * It deliberately has no reflection, assignments, method calls, or access to Java.
 */
public final class ExpressionEngine {

    private ExpressionEngine() {}

    public static double evaluate(Object source, Map<String, Double> variables, Random random) {
        if (source instanceof Number number) return number.doubleValue();
        if (source == null) return 0.0;
        return new Parser(String.valueOf(source), variables, random).parse();
    }

    public static boolean condition(Object source, Map<String, Double> variables, Random random) {
        if (source == null) return true;
        if (source instanceof Boolean value) return value;
        String text = String.valueOf(source).trim();
        for (String operator : new String[]{"<=", ">=", "==", "!=", "<", ">"}) {
            int index = text.indexOf(operator);
            if (index > 0) {
                double left = evaluate(text.substring(0, index), variables, random);
                double right = evaluate(text.substring(index + operator.length()), variables, random);
                return switch (operator) {
                    case "<=" -> left <= right;
                    case ">=" -> left >= right;
                    case "==" -> Math.abs(left - right) < 1.0E-9;
                    case "!=" -> Math.abs(left - right) >= 1.0E-9;
                    case "<" -> left < right;
                    default -> left > right;
                };
            }
        }
        return evaluate(text, variables, random) != 0.0;
    }

    private static final class Parser {
        private final String input;
        private final Map<String, Double> variables;
        private final Random random;
        private int position;

        private Parser(String input, Map<String, Double> variables, Random random) {
            this.input = input;
            this.variables = variables;
            this.random = random;
        }

        private double parse() {
            double value = expression();
            whitespace();
            if (position != input.length()) {
                throw new IllegalArgumentException("Unexpected token at position " + position + " in '" + input + "'");
            }
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Expression is not finite: " + input);
            return value;
        }

        private double expression() {
            double value = term();
            while (true) {
                whitespace();
                if (take('+')) value += term();
                else if (take('-')) value -= term();
                else return value;
            }
        }

        private double term() {
            double value = power();
            while (true) {
                whitespace();
                if (take('*')) value *= power();
                else if (take('/')) {
                    double divisor = power();
                    if (Math.abs(divisor) < 1.0E-12) throw new IllegalArgumentException("Division by zero");
                    value /= divisor;
                } else if (take('%')) value %= power();
                else return value;
            }
        }

        private double power() {
            double value = unary();
            whitespace();
            return take('^') ? Math.pow(value, power()) : value;
        }

        private double unary() {
            whitespace();
            if (take('+')) return unary();
            if (take('-')) return -unary();
            return primary();
        }

        private double primary() {
            whitespace();
            if (take('(')) {
                double value = expression();
                require(')');
                return value;
            }
            if (position < input.length() && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
                return number();
            }
            String name = identifier();
            if (name.isEmpty()) throw new IllegalArgumentException("Expected value at position " + position);
            whitespace();
            if (!take('(')) {
                if (name.equalsIgnoreCase("pi")) return Math.PI;
                if (name.equalsIgnoreCase("e")) return Math.E;
                Double value = variables.get(name);
                if (value == null) throw new IllegalArgumentException("Unknown variable '" + name + "'");
                return value;
            }
            java.util.List<Double> args = new java.util.ArrayList<>();
            whitespace();
            if (!peek(')')) {
                do { args.add(expression()); whitespace(); } while (take(','));
            }
            require(')');
            return function(name, args);
        }

        private double function(String name, java.util.List<Double> a) {
            return switch (name.toLowerCase(java.util.Locale.ROOT)) {
                case "sin" -> Math.sin(arg(a, 0, name));
                case "cos" -> Math.cos(arg(a, 0, name));
                case "tan" -> Math.tan(arg(a, 0, name));
                case "abs" -> Math.abs(arg(a, 0, name));
                case "sqrt" -> Math.sqrt(arg(a, 0, name));
                case "floor" -> Math.floor(arg(a, 0, name));
                case "ceil" -> Math.ceil(arg(a, 0, name));
                case "round" -> Math.round(arg(a, 0, name));
                case "min" -> Math.min(arg(a, 0, name), arg(a, 1, name));
                case "max" -> Math.max(arg(a, 0, name), arg(a, 1, name));
                case "clamp" -> Math.max(arg(a, 1, name), Math.min(arg(a, 2, name), arg(a, 0, name)));
                case "lerp" -> arg(a, 0, name) + (arg(a, 1, name) - arg(a, 0, name)) * arg(a, 2, name);
                case "random" -> {
                    double min = a.isEmpty() ? 0.0 : a.get(0);
                    double max = a.size() < 2 ? 1.0 : a.get(1);
                    yield min + random.nextDouble() * (max - min);
                }
                default -> throw new IllegalArgumentException("Unknown function '" + name + "'");
            };
        }

        private static double arg(java.util.List<Double> args, int index, String function) {
            if (index >= args.size()) throw new IllegalArgumentException("Missing argument for " + function);
            return args.get(index);
        }

        private double number() {
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (!Character.isDigit(c) && c != '.' && c != 'e' && c != 'E' && c != '+' && c != '-') break;
                if ((c == '+' || c == '-') && position > start && input.charAt(position - 1) != 'e' && input.charAt(position - 1) != 'E') break;
                position++;
            }
            return Double.parseDouble(input.substring(start, position));
        }

        private String identifier() {
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') break;
                position++;
            }
            return input.substring(start, position);
        }

        private void whitespace() { while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++; }
        private boolean peek(char c) { whitespace(); return position < input.length() && input.charAt(position) == c; }
        private boolean take(char c) { if (position < input.length() && input.charAt(position) == c) { position++; return true; } return false; }
        private void require(char c) { whitespace(); if (!take(c)) throw new IllegalArgumentException("Expected '" + c + "' at position " + position); }
    }
}

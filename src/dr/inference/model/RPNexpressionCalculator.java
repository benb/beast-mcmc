package dr.inference.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Simple RPN expression evaluator.
 *
 * Limitations:
 *   - variables are statistics of 1 dimension.
 *   - Four basic operations (easy to extend, though)
 *
 * @author Joseph Heled
 *         Date: 10/05/2008
 */
public class RPNexpressionCalculator {
    /**
     * Interfave for variable access by name
     */
    public interface GetVariable {
        /**
         *
         * @param name
         * @return  variable value
         */
        double get(String name);
    }

   private enum OP { OP_ADD, OP_SUB, OP_MULT, OP_DIV, OP_CONST, OP_REF }

    private class Eelement {
        OP op;
        String name;
        private double value;

        Eelement(OP op) {
            this.op = op;
            name = null;
        }

         Eelement(String name) {
            this.op = OP.OP_REF;
            this.name = name;
        }

         Eelement(double val) {
             this.op = OP.OP_CONST;
             this.value = val;
         }
    }

    Eelement[] expression;

    public RPNexpressionCalculator(String expressionString) {
        String[] tokens = expressionString.trim().split("\\s+");

        List<Eelement> e = new ArrayList<Eelement>();
        expression = new Eelement[tokens.length];
        for(int k = 0; k < tokens.length; ++k) {
            String tok = tokens[k];
            Eelement element;
            if( tok.equals("+") ) {
                element = new Eelement(OP.OP_ADD);
            } else if( tok.equals("-") ) {
                element = new Eelement(OP.OP_SUB);
            } else if( tok.equals("*") ) {
                element = new Eelement(OP.OP_MULT);
            } else if( tok.equals("/") ) {
                element = new Eelement(OP.OP_DIV);
            } else {
                try {
                    double val =  Double.parseDouble(tok);
                    element = new Eelement(val);
                } catch(java.lang.NumberFormatException ex) {
                    element = new Eelement(tok);
                }
            }
            expression[k] = element;
        }
    }

    /**
     *
     * @param variables
     * @return evaluate expression given context (i.e. variables)
     */
    public double evaluate(GetVariable variables) {
        Stack<Double> stack = new Stack<Double>();

        for( Eelement elem : expression ) {
            switch( elem.op ) {
                case OP_ADD: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x+y);
                    break;
                }
                case OP_SUB: {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x-y);
                    break;
                }
                case OP_MULT : {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x*y);
                    break;
                }
                case OP_DIV : {
                    final Double y = stack.pop();
                    final Double x = stack.pop();
                    stack.push(x/y);
                    break;
                }
                case OP_CONST: {
                    stack.push(elem.value);
                    break;
                }
                case OP_REF: {
                    stack.push(variables.get(elem.name) );
                    break;
                }
            }
        }
        return stack.pop();
    }
}

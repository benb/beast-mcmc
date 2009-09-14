/*
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beauti.options;

import dr.app.beauti.enumTypes.OperatorType;
import dr.app.beauti.enumTypes.PriorScaleType;
import dr.app.beauti.enumTypes.PriorType;
import dr.evolution.util.TaxonList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class ModelOptions {

    protected Map<String, Parameter> parameters = new HashMap<String, Parameter>();
    protected Map<String, Operator> operators = new HashMap<String, Operator>();
   	protected Map<TaxonList, Parameter> statistics = new HashMap<TaxonList, Parameter>();

    public static final double demoTuning = 0.75;
    public static final double demoWeights = 3.0;

	protected static final double branchWeights = 30.0;
	protected static final double treeWeights = 15.0;
	protected static final double rateWeights = 3.0;
	
	private final List<ComponentOptions> components = new ArrayList<ComponentOptions>();  

    
	//+++++++++++++++++++ Create Operator ++++++++++++++++++++++++++++++++
    public void createOperator(String parameterName, OperatorType type, double tuning, double weight) {
        Parameter parameter = getParameter(parameterName);
        operators.put(parameterName, new Operator(parameterName, "", parameter, type, tuning, weight));
    }

    public void createOperator(String key, String name, String description, String parameterName, OperatorType type, double tuning, double weight) {
        Parameter parameter = getParameter(parameterName);
        operators.put(key, new Operator(name, description, parameter, type, tuning, weight));
    }

    public void createOperator(String key, String name, String description, Parameter parameter1, Parameter parameter2, OperatorType type, double tuning, double weight) {
//        Parameter parameter1 = getParameter(parameterName1);
//        Parameter parameter2 = getParameter(parameterName2);
        operators.put(key, new Operator(name, description, parameter1, parameter2, type, tuning, weight));
    }
    
    public void createTagOperator(String key, String name, String description, String parameterName, String tag, String idref,
    		OperatorType type, double tuning, double weight) {
    	Parameter parameter = getParameter(parameterName);
      operators.put(key, new Operator(name, description, parameter, tag, idref, type, tuning, weight));
  }

    public void createScaleOperator(String parameterName, double tuning, double weight) {
        Parameter parameter = getParameter(parameterName);
        operators.put(parameterName, new Operator(parameterName, "", parameter, OperatorType.SCALE, tuning, weight));
    }

    public void createScaleAllOperator(String parameterName, double weight) {
        Parameter parameter = getParameter(parameterName);
        operators.put(parameterName, new Operator(parameterName, "", parameter, OperatorType.SCALE_ALL, 0.75, weight));
    }
    
    public void createUpDownAllOperator(String paraName, String opName, String description, double tuning, double weight) {
        final Parameter parameter = new Parameter.Builder(paraName, description).build();
        operators.put(paraName, new Operator(opName, description, parameter, OperatorType.UP_DOWN_ALL_RATES_HEIGHTS, tuning, weight));
    }

    //+++++++++++++++++++ Create Parameter ++++++++++++++++++++++++++++++++
    public void createParameter(String name, String description) {
        new Parameter.Builder(name, description).build(parameters);
    }

    public void createParameterUniformPrior(String name, String description, PriorScaleType scaleType, double initial, double lower, double upper) {
        new Parameter.Builder(name, description).scaleType(scaleType).prior(PriorType.UNIFORM_PRIOR)
                  .initial(initial).lower(lower).upper(upper).build(parameters);
    }

    public void createParameterGammaPrior(String name, String description, PriorScaleType scaleType, double initial,
                                          double shape, double scale, boolean priorFixed) {
        new Parameter.Builder(name, description).scaleType(scaleType).prior(PriorType.GAMMA_PRIOR)
                  .initial(initial).shape(shape).scale(scale).priorFixed(priorFixed).build(parameters);
    }

    public void createParameterJeffreysPrior(String name, String description, PriorScaleType scaleType, double initial,
                                             double lower, double upper) {
        new Parameter.Builder(name, description).scaleType(scaleType).prior(PriorType.JEFFREYS_PRIOR)
                .initial(initial).lower(lower).upper(upper).build(parameters);
    }

  //+++++++++++++++++++ Create Statistic ++++++++++++++++++++++++++++++++
    protected void createDiscreteStatistic(String name, String description) {
        new Parameter.Builder(name, description).isDiscrete(true).isStatistic(true)
                 .prior(PriorType.POISSON_PRIOR).mean(Math.log(2)).build(parameters);
    }

    protected void createStatistic(String name, String description, double lower, double upper) {
        new Parameter.Builder(name, description).isStatistic(true).prior(PriorType.UNIFORM_PRIOR)
                  .lower(lower).upper(upper).build(parameters);
    }

    //+++++++++++++++++++ Methods ++++++++++++++++++++++++++++++++
    public Parameter getParameter(String name) {
        Parameter parameter = parameters.get(name);
        if (parameter == null) {
            for (String key : parameters.keySet()) {
                System.err.println(key);
            }
            throw new IllegalArgumentException("Parameter with name, " + name + ", is unknown");
        }
        return parameter;
    }

    public Parameter getStatistic(TaxonList taxonList) {
        Parameter parameter = statistics.get(taxonList);
        if (parameter == null) {
            for (TaxonList key : statistics.keySet()) {
                System.err.println("Taxon list: " + key.getId());
            }
            throw new IllegalArgumentException("Statistic for taxon list, " + taxonList.getId() + ", is unknown");
        }
        return parameter;
    }

    public Operator getOperator(String name) {
        Operator operator = operators.get(name);
        if (operator == null) throw new IllegalArgumentException("Operator with name, " + name + ", is unknown");
        return operator;
    }

//    abstract public String getPrefix();

    protected void addComponent(ComponentOptions component) {
        components.add(component);
        component.createParameters(this);
    }

    public ComponentOptions getComponentOptions(Class<?> theClass) {
        for (ComponentOptions component : components) {
            if (theClass.isAssignableFrom(component.getClass())) {
                return component;
            }
        }

        return null;
    }

    protected void selectComponentParameters(ModelOptions options, List<Parameter> params) {
        for (ComponentOptions component : components) {
            component.selectParameters(options, params);
        }
    }

    protected void selectComponentStatistics(ModelOptions options, List<Parameter> stats) {
        for (ComponentOptions component : components) {
            component.selectStatistics(options, stats);
        }
    }

    protected void selectComponentOperators(ModelOptions options, List<Operator> ops) {
        for (ComponentOptions component : components) {
            component.selectOperators(options, ops);
        }
    }

    public Map<String, Parameter> getParameters() {
		return parameters;
	}

	public Map<TaxonList, Parameter> getStatistics() {
		return statistics;
	}

	public Map<String, Operator> getOperators() {
		return operators;
	}

}

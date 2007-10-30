package dr.inference.model;

import dr.xml.*;

/**
 * @author Marc Suchard
 */
public class CompoundSymmetricMatrix extends MatrixParameter {

	public final static String MATRIX_PARAMETER = "compoundSymmetricMatrix";
	public static final String DIAGONAL = "diagonal";
	public static final String OFF_DIAGONAL = "offDiagonal";

	private Parameter diagonalParameter;
	private Parameter offDiagonalParameter;

	private int dim;

	public CompoundSymmetricMatrix(Parameter diagonals, Parameter offDiagonal) {
		super(MATRIX_PARAMETER);
		diagonalParameter = diagonals;
		offDiagonalParameter = offDiagonal;
		addParameter(diagonalParameter);
		addParameter(offDiagonal);
		dim = diagonalParameter.getDimension();
//		diagonalParameter = param;
	}

//	public DiagonalMatrix(String name, Parameter parameter) {
//		Parameter.Default(name, parameters);
//	}

	public double getParameterValue(int row, int col) {
		if (row != col)
			return offDiagonalParameter.getParameterValue(0);
		return diagonalParameter.getParameterValue(row);
	}

	public double[][] getParameterAsMatrix() {
		final int I = dim;
		double[][] parameterAsMatrix = new double[I][I];
		final double offDiagonal = offDiagonalParameter.getParameterValue(0);
		for (int i = 0; i < I; i++) {
			parameterAsMatrix[i][i] = diagonalParameter.getParameterValue(i);
			for (int j = i + 1; j < I; j++) {
				parameterAsMatrix[j][i] = parameterAsMatrix[i][j] = offDiagonal;
			}
		}
		return parameterAsMatrix;
	}

	public int getColumnDimension() {
		return diagonalParameter.getDimension();
	}

	public int getRowDimension() {
		return diagonalParameter.getDimension();
	}

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return MATRIX_PARAMETER;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			XMLObject cxo = (XMLObject) xo.getChild(DIAGONAL);
			Parameter diagonalParameter = (Parameter) cxo.getChild(Parameter.class);

			cxo = (XMLObject) xo.getChild(OFF_DIAGONAL);
			Parameter offDiagonalParameter = (Parameter) cxo.getChild(Parameter.class);

			return new CompoundSymmetricMatrix(diagonalParameter, offDiagonalParameter);
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "A diagonal matrix parameter constructed from its diagonals.";
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				new ElementRule(DIAGONAL,
						new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
				new ElementRule(OFF_DIAGONAL,
						new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
		};

		public Class getReturnType() {
			return CompoundSymmetricMatrix.class;
		}
	};


}

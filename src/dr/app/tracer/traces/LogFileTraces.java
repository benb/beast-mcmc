package dr.app.tracer.traces;

import java.io.*;
import java.util.*;

/**
 * A class that stores a set of traces from a single chain
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: LogFileTraces.java,v 1.4 2006/11/30 17:39:29 rambaut Exp $
 */

public class LogFileTraces implements TraceList {

	public LogFileTraces(String name, File file) {
		this.name = name;
		this.file = file;
	}

	/** @return the name of this traceset */
	public String getName() { return name; }

	public File getFile() {
	    return file;
	}

	/** @return the last state in the chain */
	public int getMaxState() {
		return lastState;
	}

    public boolean isIncomplete() {
        return false;
    }

    /** @return the number of states excluding the burnin */
    public int getStateCount() {
        return ((lastState - getBurnIn()) / stepSize) + 1;
    }

	/** @return the size of the step between states */
	public int getStepSize() {
		return stepSize;
	}

	public int getBurnIn() {
		return burnIn;
	}

	/** @return the number of traces in this traceset */
	public int getTraceCount() { return traces.size(); }

	/** @return the index of the trace with the given name */
	public int getTraceIndex(String name) {
		for (int i = 0; i < traces.size(); i++) {
			Trace trace = getTrace(i);
			if (name.equals(trace.getName())) {
				return i;
			}
		}
		return -1;
	}

	/** @return the name of the trace with the given index */
	public String getTraceName(int index) {
		return getTrace(index).getName();
	}

	/**
	 * @return the trace for a given index
	 */
	public Trace getTrace(int index) {
		return (Trace)traces.get(index);
	}

	public void setBurnIn(int burnIn) {
		this.burnIn = burnIn;
		traceStatistics = null;
	}

	public double getStateValue(int trace, int index) {
		return getTrace(trace).getValue(index + (burnIn / stepSize));
	}

	public void getValues(int index, double[] destination) {
		 getTrace(index).getValues((burnIn / stepSize), destination, 0);
	}

	public void getValues(int index, double[] destination, int offset) {
		 getTrace(index).getValues((burnIn / stepSize), destination, offset);
	}

	public TraceDistribution getDistributionStatistics(int index) {
		return getCorrelationStatistics(index);
	}

	public TraceCorrelation getCorrelationStatistics(int index) {
		if (traceStatistics == null) {
			return null;
		}

		return traceStatistics[index];
	}

	public void analyseTrace(int index) {
		double[] values = new double[getStateCount()];
		int offset = (getBurnIn() / stepSize);

		if (traceStatistics == null) {
			traceStatistics = new TraceCorrelation[getTraceCount()];
		}

		Trace trace = getTrace(index);
		trace.getValues(offset, values);
		traceStatistics[index] = new TraceCorrelation(values, stepSize);
	}

	/**
	 * Loads all the traces in a file
	 */
	public void loadTraces(Reader r) throws TraceException, java.io.IOException {

		TrimLineReader reader = new LogFileTraces.TrimLineReader(r);

		// Read through to first token
		StringTokenizer tokens = reader.tokenizeLine();
		while (!tokens.hasMoreTokens()) {
			tokens = reader.tokenizeLine();
		}

		// skip the first column which should be the state number
		String token = tokens.nextToken();
		while (token.startsWith("[")) { // Probably a MrBayes trace file starting with a comment

			tokens = reader.tokenizeLine();
			while (!tokens.hasMoreTokens()) {
				 tokens = reader.tokenizeLine();
			}
			// read state token and ignore
			token = tokens.nextToken();
		}

		// read label tokens
		while (tokens.hasMoreTokens()) {
			String label = tokens.nextToken();
			addTrace(label);
		}

		int statCount = getTraceCount();

        boolean firstState = true;

		tokens = reader.tokenizeLine();
		while (tokens != null && tokens.hasMoreTokens()) {

			String stateString = tokens.nextToken();
			int state = 0;
			try {
				state = Integer.parseInt(stateString);

                if (firstState) {
                    // MrBayes puts 1 as the first state, BEAST puts 0
                    // In order to get the same gap between subsequent samples,
                    // we force this to 0.
                    if (state == 1) state = 0;
                    firstState = false;
                }
				addState(state);

			} catch (NumberFormatException nfe) {
				throw new TraceException("State " + state + ":Expected real value in column " + reader.getLineNumber());
			}

			for (int i = 0; i < statCount; i++) {
				if (tokens.hasMoreTokens()) {

					try {
						addValue(i, Double.parseDouble(tokens.nextToken()));
					} catch (NumberFormatException nfe) {
						throw new TraceException("State " + state + ": Expected real value in column " + reader.getLineNumber());
					}
				} else {
					throw new TraceException("State " + state + ": Expected real value in column " + reader.getLineNumber());
				}

			}

			tokens = reader.tokenizeLine();
		}

		burnIn = (int)(0.1 * lastState);
	}


	//************************************************************************
	// private methods
	//************************************************************************

	// These methods are used by the load function, above

	/**
	 * Add a trace for a statistic of the given name
	 */
	private void addTrace(String name) {
		traces.add(new Trace(name));
	}

	/**
	 * Add a state number for these traces. This should be
	 * called before adding values for each trace. The spacing
	 * between stateNumbers should remain constant.
	 */
	private void addState(int stateNumber) throws TraceException {
		if (firstState < 0) {
			firstState = stateNumber;
		} else if (stepSize < 0) {
			stepSize = stateNumber - firstState;
		} else {
			int step = stateNumber - lastState;
			if (step != stepSize) {
				throw new TraceException("State step sizes are not constant");
			}
		}
		lastState = stateNumber;
	}

	/**
	 * add a value for the ith trace
	 */
	private void addValue(int i, double value) {
		getTrace(i).add(value);
	}

	private final File file;
	private String name;

	private List<Trace> traces = new ArrayList<Trace>();
	private TraceCorrelation[] traceStatistics = null;

	private int burnIn = -1;
	private int firstState = -1;
	private int lastState = -1;
	private int stepSize = -1;

    public static class TrimLineReader extends BufferedReader {

        public TrimLineReader(Reader reader) {
            super(reader);
        }

        public String readLine() throws java.io.IOException {
            lineNumber += 1;
            String line = super.readLine();
            if (line != null) return line.trim();
            return null;
        }

        public StringTokenizer tokenizeLine() throws java.io.IOException {
            String line = readLine();
            if (line == null) return null;
            return new StringTokenizer(line, "\t");
        }

        public int getLineNumber() { return lineNumber; }

        private int lineNumber = 0;
    };
}


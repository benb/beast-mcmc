package dr.inference.trace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexei Drummond
 */
public class ArrayTraceList extends AbstractTraceList {

    String name;
    Map<String, Integer> traceIndex = new HashMap<String, Integer>();
    int burnin = 0;
    List<Trace> traces;
    int stepSize;

    public ArrayTraceList(String name, List<Trace> traces, int burnin) {
        this.name = name;
        this.traces = traces;
        this.burnin = burnin;

        for (int i = 0; i < traces.size(); i++) {
            traceIndex.put(traces.get(i).getName(), i);
        }

        Trace stateTrace = traces.get(0);
        this.stepSize = (int) Math.round(stateTrace.getValue(1) - stateTrace.getValue(0));
    }

    public String getName() {
        return name;
    }

    public int getTraceCount() {
        return traces.size();
    }

    public int getTraceIndex(String name) {
        Integer index = traceIndex.get(name);
        if (index == null) return -1;
        return index;
    }

    public String getTraceName(int index) {
        return traces.get(index).getName();
    }

    /**
     * @return the burn-in for this trace list (the number of sampled states to discard)
     */
    public int getBurnIn() {
        return burnin;
    }

    /**
     * @return the number of states in the traces (after burnin removed)
     */
    public int getStateCount() {
        return traces.get(0).getCount();
    }

    public int getStepSize() {
        return stepSize;
    }

    public int getMaxState() {
        return getStateCount() * getStepSize();
    }

    public boolean isIncomplete() {
        return false;
    }

    /**
     * get the values of trace with the given index (without burnin)
     *
     * @param index       the index of trace
     * @param destination the array to copy values into
     */
    public void getValues(int index, double[] destination) {

        traces.get(index).getValues(0, destination, burnin);
    }

    public void getValues(int index, double[] destination, int offset) {
        traces.get(index).getValues(0, destination, offset);
    }

    Trace getTrace(int index) {
        return traces.get(index);
    }
}
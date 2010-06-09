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
        this.stepSize = (int) Math.round((Double) stateTrace.getValue(1) - (Double) stateTrace.getValue(0));
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

    /**
     * @return the number of states in the burnin
     */
    public int getBurninStateCount() {
        return (getBurnIn() / stepSize);
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
    public <T> void getValues(int index, T[] destination) {
        getTrace(index).getValues(0, destination, burnin);
    }

    public <T> void getValues(int index, T[] destination, int offset) {
        getTrace(index).getValues(0, destination, offset);
    }

    public <T> void getBurninValues(int index, T[] destination) {
        getTrace(index).getValues(0, (burnin / stepSize), destination, 0);
    }

    public Trace getTrace(int index) {
        return traces.get(index);
    }

    @Override
    public void createTraceFilter(Filter filter) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void getSelected(int index, boolean[] destination) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void getSelected(int index, boolean[] destination, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void getBurningSelected(int index, boolean[] destination) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
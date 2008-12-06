package dr.evomodel.arg;

import dr.inference.loggers.LogColumn;
import dr.inference.loggers.NumberColumn;
import dr.inference.model.Likelihood;
import dr.inference.model.Model;

public abstract class ARGPartitionLikelihood implements Likelihood{

	private String id;
	private ARGModel arg;
	
	public abstract double[] generatePartition();
	public abstract double getLogLikelihood(double[] partition);
	
	public double getLogLikelihood() {
    			
    	double logPrior = 0;
    	
    	for(int i = 0, n = getReassortmentNodeCount(); i < n; i++){
    		logPrior += getLogLikelihood(getPartition(i));
    	}
    	
    	return logPrior;
	}
	
	public double[] getPartition(int i){
		if(arg.getReassortmentNodeCount() == 0){
			return null;
		}
		
		return arg.getPartitioningParameters().getParameter(i).getParameterValues();
	}
	
	public ARGPartitionLikelihood(String id, ARGModel arg){
		setId(id);
		
		this.arg = arg;
	}
	
	public int getNumberOfPartitionsMinusOne(){
		return arg.getNumberOfPartitions() - 1;
	}
	
	public int getReassortmentNodeCount(){
		return arg.getReassortmentNodeCount();
	}
	
	
	public LogColumn[] getColumns() {
		return new LogColumn[]{
			new NumberColumn(getId()){
				
				public double getDoubleValue() {
					return getLogLikelihood();
				}
				
			}
		};
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
		
	}
	
	public Model getModel() {
		return null;
	}
	public void makeDirty() {
		
	}
	
}
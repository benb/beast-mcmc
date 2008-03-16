package dr.evomodel.operators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;



import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evomodel.tree.ARGModel;
import dr.evomodel.tree.ARGModel.Node;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.StringAttributeRule;
import dr.xml.XMLObject;
import dr.xml.XMLObjectParser;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

/**
 * This method moves the arg model around.  Use of both the 
 * reassortment and bifurcation modes, as well as the event operator,
 * satisfies irreducibility.
 * @author ebloomqu
 *
 */
public class ARGSwapOperator extends SimpleMCMCOperator{
	
	public static final String ARG_SWAP_OPERATOR = "argSwapOperator";
	public static final String SWAP_TYPE = "type";
	public static final String BIFURCATION_SWAP = "bifurcationSwap";
	public static final String REASSORTMENT_SWAP = "reassortmentSwap";
	public static final String DUAL_SWAP = "dualSwap";
	public static final String FULL_SWAP = "fullSwap";
	public static final String DEFAULT = BIFURCATION_SWAP;
	
	private ARGModel arg;
	private String mode;
	
	public ARGSwapOperator(ARGModel arg, String mode, int weight){
		this.arg = arg;
		this.mode = mode;
		
		setWeight(weight);
		
	}
		
	
	public double doOperation() throws OperatorFailedException {
		
		if((mode.equals(REASSORTMENT_SWAP) || mode.equals(DUAL_SWAP)) &&
				arg.getReassortmentNodeCount() == 0){
			return 0.0;
		}
		
		
		ArrayList<NodeRef> bifurcationNodes = new ArrayList<NodeRef>(arg.getNodeCount());
		ArrayList<NodeRef> reassortmentNodes = new ArrayList<NodeRef>(arg.getNodeCount());
					
		//Not sure if this part is totally needed.
		setupBifurcationNodes(bifurcationNodes);
		setupReassortmentNodes(reassortmentNodes);
		
		if(mode.equals(BIFURCATION_SWAP)){
			return bifurcationSwap(bifurcationNodes.get(MathUtils.nextInt(bifurcationNodes.size())));
		}else if(mode.equals(REASSORTMENT_SWAP)){
			return reassortmentSwap(reassortmentNodes.get(MathUtils.nextInt(reassortmentNodes.size())));
		}else if(mode.equals(DUAL_SWAP)){
			reassortmentSwap(reassortmentNodes.get(MathUtils.nextInt(reassortmentNodes.size())));
			return bifurcationSwap(bifurcationNodes.get(MathUtils.nextInt(bifurcationNodes.size())));
		}
				
		bifurcationNodes.addAll(reassortmentNodes);
		
		//TODO Change to heapsort method
		Collections.sort(bifurcationNodes,NodeSorter);
		
		for(NodeRef x : bifurcationNodes){
			if(arg.isBifurcation(x)) 
				bifurcationSwap(x);
			else 
				reassortmentSwap(x);
		}
				
		return 0;
	}
	
	private double bifurcationSwap(NodeRef x){
		Node startNode = (Node) x;
		
		
		
		Node keepChild = startNode.leftChild;
		Node moveChild = startNode.rightChild;
		
		if(MathUtils.nextBoolean()){
			keepChild = moveChild;
			moveChild = startNode.leftChild;
		}
				
		ArrayList<NodeRef> possibleNodes = new ArrayList<NodeRef>(arg.getNodeCount());
		
		findNodesAtHeight(possibleNodes,startNode.getHeight());
		
		assert !possibleNodes.contains(keepChild) && !possibleNodes.contains(moveChild);
		assert possibleNodes.size() > 0;
		
		
		
		Node swapChild = (Node) possibleNodes.get(MathUtils.nextInt(possibleNodes.size()));
		Node swapChildParent = null;
		
		
		
		arg.beginTreeEdit();
		
		if(swapChild.bifurcation){
			swapChildParent = swapChild.leftParent;
			
			arg.singleRemoveChild(startNode, moveChild);
			
			if(swapChildParent.bifurcation){
				arg.singleRemoveChild(swapChildParent, swapChild);
				arg.singleAddChild(swapChildParent, moveChild);
			}else{
				arg.doubleRemoveChild(swapChildParent, swapChild);
				arg.doubleAddChild(swapChildParent, moveChild);
			}
			
			arg.singleAddChild(startNode, swapChild);
			
		}else{
			boolean[] sideOk = {swapChild.leftParent.getHeight() > startNode.getHeight(),
								swapChild.rightParent.getHeight() > startNode.getHeight()};
			
			if(sideOk[0] && sideOk[1]){
				if(MathUtils.nextBoolean()){
					swapChildParent = swapChild.leftParent;
				}else{
					swapChildParent = swapChild.rightParent;
				}
			}else if(sideOk[0]){
				swapChildParent = swapChild.leftParent;
			}else{
				swapChildParent = swapChild.rightParent;
			}
			
			System.out.println("here");
			System.exit(-1);
			
		}
		
		arg.pushTreeChangedEvent(startNode);
		arg.pushTreeChangedEvent(moveChild);
		arg.pushTreeChangedEvent(swapChild);
		arg.pushTreeChangedEvent(swapChildParent);
		
		try{ 
			arg.endTreeEdit(); 
		}catch(MutableTree.InvalidTreeException ite){
			System.err.println(ite.getMessage());
			System.exit(-1);
		}
		
		return 0;
	}
	
	private double reassortmentSwap(NodeRef x){
		Node startNode = (Node) x;
		Node startChild = startNode.leftChild;
		
		ArrayList<ARGSwap> possibleSwaps = new ArrayList<ARGSwap>(arg.getNodeCount());

		findSwaps(possibleSwaps,startNode);
		
		assert !possibleSwaps.contains(new ARGSwap(startNode,true,false));
		assert possibleSwaps.size() > 0;
		
		ARGSwap swap = possibleSwaps.get(MathUtils.nextInt(possibleSwaps.size()));
		
		Node swapChild = swap.son;
		Node swapParent = swap.getParent();
		
		arg.beginTreeEdit();
		
		if(swap.bifurcation){
			
			System.out.println(arg.toARGSummary());
			System.out.println(swap);
			arg.doubleRemoveChild(startNode, startChild);
			arg.removeChild(swapParent,swapChild);

//			System.out.println(arg.toARGSummary());
			
			arg.doubleAddChild(startNode, swapChild);
			
			startChild.leftParent = swapParent;
			startChild.rightParent = swapParent;
			
			if(swapParent.bifurcation){
				arg.singleAddChild(swapParent, startChild);
			}else{
				swapParent.leftChild = startChild;
				swapParent.rightChild = startChild;
			}
			
			assert nodeCheck() : arg.toARGSummary();
		}else{
			if(swapChild.leftParent == swapChild.rightParent){
				arg.doubleRemoveChild(startNode, startChild);
				if(swap.left){
					swapParent.leftChild = null;
					swapChild.leftParent = null;
					
					swapChild.leftParent = startNode;
					startNode.leftChild = swapChild;
					startNode.rightChild = swapChild;
					
					arg.singleAddChild(swapParent, startChild);
				}else{
					swapParent.rightChild = null;
					swapChild.rightParent = null;
					
					swapChild.rightParent = startNode;
					startNode.leftChild = swapChild;
					startNode.rightChild = swapChild;
					
					arg.singleAddChild(swapParent, startChild);
				}
				
				assert nodeCheck() : arg.toARGSummary();
			}else{
				arg.doubleRemoveChild(startNode, startChild);
				arg.singleRemoveChild(swapParent,swapChild);
							
				if(swap.left){
					swapChild.leftParent = startNode;
					startNode.leftChild = swapChild;
					startNode.rightChild = swapChild;
					arg.singleAddChild(swapParent, startChild);
				}else{
					swapChild.rightParent = startNode;
					startNode.leftChild = swapChild;
					startNode.rightChild = swapChild;
					arg.singleAddChild(swapParent, startChild);
					
				}
				assert nodeCheck() : arg.toARGSummary();
				
			}
		}
		
		
		arg.pushTreeChangedEvent(startNode);
		arg.pushTreeChangedEvent(startChild);
		arg.pushTreeChangedEvent(swapChild);
		arg.pushTreeChangedEvent(swapParent);
		
		try{ 
			arg.endTreeEdit(); 
		}catch(MutableTree.InvalidTreeException ite){
			System.err.println(ite.getMessage());
			System.exit(-1);
		}
		
		
		return 0;
	}
	
	private void setupBifurcationNodes(ArrayList<NodeRef> list){
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			NodeRef x = arg.getNode(i);
			if(arg.isInternal(x) && arg.isBifurcation(x) && !arg.isRoot(x)){
				list.add(x);
			}
		}
	}
	
	private void setupReassortmentNodes(ArrayList<NodeRef> list){
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			NodeRef x = arg.getNode(i);
			if(arg.isReassortment(x)){
				list.add(x);
			}
		}
	}
	
	private void findSwaps(ArrayList<ARGSwap> x, Node node){
		double height = node.getHeight();
		
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			Node y = (Node) arg.getNode(i);
			if(y.getHeight() < height && y != node){
				if(y.bifurcation){
					if(y.leftParent.getHeight() > height){
						x.add(new ARGSwap(y,true,true));
					}
				}else{
					if(y.leftParent.getHeight() > height){
						x.add(new ARGSwap(y,false,true));
					}
					if(y.rightParent.getHeight() > height){
						x.add(new ARGSwap(y,false,false));
					}
				}
			}
		}
	}
	
	
	private void findNodesAtHeight(ArrayList<NodeRef> x, double height){
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			Node y = (Node) arg.getNode(i);
			if(y.getHeight() < height){
				if(y.bifurcation){
					if(y.leftParent.getHeight() > height){
						x.add(y);
					}
				}else{
					if(y.leftParent.getHeight() > height){
						x.add(y);
					}
					if(y.rightParent.getHeight() > height){
						x.add(y);
					}
				}
			}
		}
	}
	
	public String getOperatorName() {
		return ARG_SWAP_OPERATOR;
	}

	public String getPerformanceSuggestion() {
		return "";
	}
	
	private class ARGSwap{
		
		public Node son;
		public boolean bifurcation;
		public boolean left;
		
		public ARGSwap(Node son, boolean bif, boolean left){
			this.son = son;
			this.bifurcation = bif;
			this.left = left;
		}
		
		public Node getParent(){
			if(bifurcation){
				return son.leftParent;
			}
			
			if(left){
				return son.leftParent;
			}
			
			return son.rightParent;
			
		}
		
		public boolean equals(Object o){
			if(!(o instanceof ARGSwap))
				return false;
			
			ARGSwap s = (ARGSwap)o;
			
			if(s.son == this.son){
				return true;
			}
			return false;
		}
		
		public String toString(){
			return "(" + son.toString() + ", b= " + bifurcation + ", l= " + left + ")"; 
		}
		
	}
	
	private Comparator<NodeRef> NodeSorter = new Comparator<NodeRef>(){

		public int compare(NodeRef o1, NodeRef o2) {
			double[] heights = {arg.getNodeHeight(o1),arg.getNodeHeight(o2)};
			
			if(heights[0] < heights[1]){
				return -1;
			}else if(heights[0] > heights[1]){
				return 1;
			}
			
			return 0;
		}
	};
	
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser(){

		public String getParserDescription() {
			return "Swaps nodes on a tree";
		}

		public Class getReturnType() {
			return ARGSwapOperator.class;
		}

		private String[] validFormats = {BIFURCATION_SWAP, REASSORTMENT_SWAP,
				DUAL_SWAP, FULL_SWAP};
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
			AttributeRule.newIntegerRule(WEIGHT),	
			new StringAttributeRule(SWAP_TYPE,"The mode of the operator",
					validFormats, false),
			new ElementRule(ARGModel.class),
				
		};
		
		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			int weight = xo.getIntegerAttribute(WEIGHT);
			
			String mode = DEFAULT;
			if(xo.hasAttribute(SWAP_TYPE)){
				mode = xo.getStringAttribute(SWAP_TYPE);
			}
			
			ARGModel arg = (ARGModel) xo.getChild(ARGModel.class);
			return new ARGSwapOperator(arg,mode,weight);
		}

		public String getParserName() {
			return ARG_SWAP_OPERATOR;
		}
		
	};
	
	public boolean nodeCheck() {
		for (int i = 0, n = arg.getNodeCount(); i < n; i++) {
			Node x = (Node) arg.getNode(i);

			if (x.leftParent != x.rightParent &&
					x.leftChild != x.rightChild) {
				return false;
			}
			if (x.leftParent != null) {
				if (x.leftParent.leftChild.getNumber() != i &&
						x.leftParent.rightChild.getNumber() != i)
					return false;
			}
			if (x.rightParent != null) {
				if (x.rightParent.leftChild.getNumber() != i &&
						x.rightParent.rightChild.getNumber() != i)
					return false;
			}
			if (x.leftChild != null) {
				if (x.leftChild.leftParent.getNumber() != i &&
						x.leftChild.rightParent.getNumber() != i)
					return false;
			}
			if (x.rightChild != null) {
				if (x.rightChild.leftParent.getNumber() != i &&
						x.rightChild.rightParent.getNumber() != i)
					return false;
			}
		}

		return true;
	}

}

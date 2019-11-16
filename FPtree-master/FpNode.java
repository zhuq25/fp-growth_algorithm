import java.util.ArrayList;
import java.util.List;

public class FpNode 
{
	String ID;
	List<FpNode> children;
	FpNode parent;
	FpNode next;
	long count;
//----------------------------------------------------------
	public FpNode()  // Root Node
	{
		this.ID = null;
		this.count = -1;
		children = new ArrayList<FpNode>();
		next = null;
		parent = null;
	}
//----------------------------------------------------------
	public FpNode(String ID) //Non-root Node Structure
	{
		this.ID = ID;
		this.count = 1;
		children = new ArrayList<FpNode>();
		next = null;
		parent = null;
	}
//----------------------------------------------------------
	public FpNode(String ID, long count) // Generate non-root node
	{
		this.ID = ID;
		this.count = count;
		children = new ArrayList<FpNode>();
		next = null;
		parent = null;
	}
//----------------------------------------------------------
	public void addChild(FpNode child)   //Add a Child
	{ 
		children.add(child);
	}

	public void addCount(int count) 
	{
		this.count += count;
	}
//----------------------------------------------------------
	public void addCount()  //Count add 1
	{ 
		this.count += 1;
	}
//----------------------------------------------------------
	public void NextNode(FpNode next)   //Setup next node
	{
		this.next = next;
	}

	public void setParent(FpNode parent) 
	{
		this.parent = parent;
	}

//----------------------------------------------------------
	public FpNode pickChild(int index) // Pick pointed Child
	{
		return children.get(index);
	}

//----------------------------------------------------------
	public int ifChild(String ID) // Search Child ID and see if exist, and return result
	{
		for (int i = 0; i < children.size(); i++)
			if (children.get(i).ID.equals(ID))
				return i;
		return -1;
	}

	public String outSearch() 
	{
		return "id: " + ID + " Count: " + count + " Target amount "
				+ children.size();
	}
}

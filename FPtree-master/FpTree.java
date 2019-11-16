import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FpTree 
{
	private static final float sup_rate = 0.001f;
	private static long min_sup;

	public static void main(String[] args) 
	{
		List<String[]> table = fileReader.scanChart("onlineretail2.txt", " ", "utf-8");
		min_sup = (long) (sup_rate * table.size());
		long totalMilliSeconds1 = System.currentTimeMillis();
		System.out.println("Absolute support value: " + min_sup);
		System.out.println("Frequent items: ");
		Map<String, Integer> frequentLink = new LinkedHashMap<String, Integer>();// First level frequent item
		Map<String, FpNode> topHead = getTop(table, frequentLink);
		FpNode root = findFpTree(table, topHead, frequentLink);
		Map<Set<FpNode>, Long> frequent = growthFunc(root, topHead, null);
		long totalMilliSeconds2 = System.currentTimeMillis();
		long total = (totalMilliSeconds2 - totalMilliSeconds1);
		System.out.println("Time Cost: " + total + " milliseconds");
		for (Map.Entry<Set<FpNode>, Long> Freq : frequent.entrySet()) 
		{
			for (FpNode node : Freq.getKey())
				System.out.print(node.ID + " ");
			System.out.println("\t" + Freq.getValue());
		}
		
	}
//-------------------------------------------------------------------------------------------------

    //Based on FP growth, recursively finding frequent items
	private static Map<Set<FpNode>, Long> growthFunc(FpNode root, Map<String, FpNode> topHead, String idMark)
	{
		Map<Set<FpNode>, Long> qualifyFreq = new HashMap<Set<FpNode>, Long>();
		Set<String> keys = topHead.keySet();
		String[] keysArray = keys.toArray(new String[0]);
		String headID = keysArray[keysArray.length - 1];
		if (discretePath(topHead, headID)) // When there is only one path, all combinations on the 
		{                                      // path can be obtained to get the adjusted frequent set
			if (idMark == null)
			{	
				return qualifyFreq;
			}
			FpNode leaf = topHead.get(headID);
			List<FpNode> paths = new ArrayList<FpNode>();// Save path node from itself to the top
			paths.add(leaf);
			FpNode node = leaf;
			while (node.parent.ID != null) 
			{
				paths.add(node.parent);
				node = node.parent;
			}
			qualifyFreq = combiPattern(paths, idMark);
			FpNode tempNode = new FpNode(idMark, -1L);
			qualifyFreq = addLeafToFrequent(tempNode, qualifyFreq);

		} 
		else 
		{
			for (int i = keysArray.length - 1; i >= 0; i--) //Recursively seeking frequent sets of conditional trees
			{
				String key = keysArray[i];
				List<FpNode> leafs = new ArrayList<FpNode>();
				FpNode link = topHead.get(key);
				while (link != null) 
				{
					leafs.add(link);
					link = link.next;
				}
				Map<List<String>, Long> paths = new HashMap<List<String>, Long>();
				Long leafCount = 0L;
				FpNode noParentNode = null;
				for (FpNode leaf : leafs) 
				{
					List<String> path = new ArrayList<String>();
					FpNode node = leaf;
					while (node.parent.ID != null) 
					{
						path.add(node.parent.ID);
						node = node.parent;
					}
					leafCount += leaf.count;
					if (path.size() > 0)
					{
						paths.put(path, leaf.count);
					}
					else      // No parent node
					{
						noParentNode = leaf;
					}
				}
				if (noParentNode != null) 
				{
					Set<FpNode> oneItem = new HashSet<FpNode>();
					oneItem.add(noParentNode);
					if (idMark != null)
					{
						oneItem.add(new FpNode(idMark, -2));
					}
					qualifyFreq.put(oneItem, leafCount);
				}
				Holder holder = getConditionFpTree(paths);
				if (holder.topHead.size() != 0) 
				{
					Map<Set<FpNode>, Long> preFres = growthFunc(holder.root,
							holder.topHead, key);
					if (idMark != null) {
						FpNode tempNode = new FpNode(idMark, leafCount);
						preFres = addLeafToFrequent(tempNode, preFres);
					}
					qualifyFreq.putAll(preFres);
				}
			}
		}
		return qualifyFreq;

	}
//-------------------------------------------------------------------------------------------------
	// Add leaf nodes to frequent set
	private static Map<Set<FpNode>, Long> addLeafToFrequent(FpNode leaf, Map<Set<FpNode>, Long> qualifyFreq) 
	{
		if (qualifyFreq.size() == 0) 
		{
			Set<FpNode> set = new HashSet<FpNode>();
			set.add(leaf);
			qualifyFreq.put(set, leaf.count);
		} 
		else 
		{
			Set<Set<FpNode>> keys = new HashSet<Set<FpNode>>(qualifyFreq.keySet());
			for (Set<FpNode> set : keys) 
			{
				Long count = qualifyFreq.get(set);
				qualifyFreq.remove(set);
				set.add(leaf);
				qualifyFreq.put(set, count);
			}
		}
		return qualifyFreq;
	}
//-------------------------------------------------------------------------------------------------
	//Determine if an fp-tree is a single path
	private static boolean discretePath(Map<String, FpNode> topHead, String tableLink) 
	{
		if (topHead.size() == 1 && topHead.get(tableLink).next == null)
		{
			return true;
		}
		return false;
	}
//-------------------------------------------------------------------------------------------------
	//Generate condition tree
	private static Holder getConditionFpTree(Map<List<String>, Long> paths) 
	{
		List<String[]> table = new ArrayList<String[]>();
		for (Map.Entry<List<String>, Long> entry : paths.entrySet()) 
		{
			for (long i = 0; i < entry.getValue(); i++) 
			{
				table.add(entry.getKey().toArray(new String[0]));
			}
		}
		Map<String, Integer> frequentLink = new LinkedHashMap<String, Integer>(); // First level frequent set
		Map<String, FpNode> cHeader = getTop(table, frequentLink);
		FpNode cRoot = findFpTree(table, cHeader, frequentLink);
		return new Holder(cRoot, cHeader);
	}
//-------------------------------------------------------------------------------------------------
	//Find all combinations on a single path and frequent items generate from ID
	private static Map<Set<FpNode>, Long> combiPattern(List<FpNode> paths, String idMark)
	{
		Map<Set<FpNode>, Long> qualifyFreq = new HashMap<Set<FpNode>, Long>();
		int size = paths.size();
		for (int mask = 1; mask < (1 << size); mask++) // Find out all the combinations, count from 1 and ignore all empty set
		{
			Set<FpNode> set = new HashSet<FpNode>();
			
			for (int i = 0; i < paths.size(); i++) // Find out every possible choice
			{
				if ((mask & (1 << i)) > 0) 
				{
					set.add(paths.get(i));
				}
			}
			long minValue = Long.MAX_VALUE;
			for (FpNode node : set) 
			{
				if (node.count < minValue)
				{	
					minValue = node.count;
				}
			}
			qualifyFreq.put(set, minValue);
		}
		return qualifyFreq;
	}
//-------------------------------------------------------------------------------------------------
    // Print out the FpTree
	private static void printTree(FpNode root) 
	{
		System.out.println(root);
		FpNode node = root.pickChild(0);
		System.out.println(node);
		for (FpNode child : node.children)
			System.out.println(child);
		System.out.println("*****");
		node = root.pickChild(1);
		System.out.println(node);
		for (FpNode child : node.children)
			System.out.println(child);

	}
//-------------------------------------------------------------------------------------------------
	// Build FpTree structure 
	private static FpNode findFpTree(List<String[]> table, Map<String, FpNode> topHead, Map<String, Integer> frequentLink) 
	{
		FpNode root = new FpNode();
		int count = 0;
		for (String[] line : table) 
		{
			String[] orderLink = orderedLink(line, frequentLink);

			FpNode parent = root;
			for (String idMark : orderLink) 
			{
				int index = parent.ifChild(idMark);
				if (index != -1)    //Don't need build new node because already contain this ID
				{
					parent = parent.pickChild(index);
					parent.addCount();
				} 
				else 
				{
					FpNode node = new FpNode(idMark);
					parent.addChild(node);
					node.setParent(parent);
					FpNode nextNode = topHead.get(idMark);
					if (nextNode == null)    //If the node is empty, added to the node
					{
						topHead.put(idMark, node);
					} 
					else    // added pointer to the node
					{
						while (nextNode.next != null) 
						{
							nextNode = nextNode.next;
						}
						nextNode.next = node;
					}
					parent = node;   // All the child node will under this parent node
				}
			}
		}
		return root;
	}
//-------------------------------------------------------------------------------------------------
	// Sort the ID in descending order based on the value of the frequentLink
	private static String[] orderedLink(String[] line, Map<String, Integer> frequentLink) 
	{
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		for (String idMark : line)
		{
			if (frequentLink.containsKey(idMark)) // Filter out non-primary frequent items
			{
				countMap.put(idMark, frequentLink.get(idMark));
			}
		}
		List<Map.Entry<String, Integer>> mapList = new ArrayList<Map.Entry<String, Integer>>(countMap.entrySet());
		Collections.sort(mapList, new Comparator<Map.Entry<String, Integer>>() 
		{@Override public int compare(Entry<String, Integer> v1, Entry<String, Integer> v2) 
		{return v2.getValue() - v1.getValue();}});  //Descending order ID
		
		String[] orderLink = new String[countMap.size()];
		int i = 0;
		for (Map.Entry<String, Integer> entry : mapList) 
		{
			orderLink[i] = entry.getKey();
			i++;
		}
		return orderLink;
	}
//-------------------------------------------------------------------------------------------------
	// Generate table. The key is equal to ID value. Descend ordering based on frequent value.
	private static Map<String, FpNode> getTop(List<String[]> table, Map<String, Integer> frequentLink) 
	{
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		for (String[] line : table) 
		{
			for (String idMark : line) 
			{
				if (countMap.containsKey(idMark)) 
				{
					countMap.put(idMark, countMap.get(idMark) + 1);
				} 
				else 
				{
					countMap.put(idMark, 1);
				}
			}
		}
		for (Map.Entry<String, Integer> entry : countMap.entrySet()) 
		{
			if (entry.getValue() >= min_sup) // Filter out items that do not meet the support value
			{
			frequentLink.put(entry.getKey(), entry.getValue());
			}
		}
		List<Map.Entry<String, Integer>> mapList = new ArrayList<Map.Entry<String, Integer>>(frequentLink.entrySet());
		Collections.sort(mapList, new Comparator<Map.Entry<String, Integer>>() 
		{@Override public int compare(Entry<String, Integer> v1, Entry<String, Integer> v2) 
		{return v2.getValue() - v1.getValue();}});  //Descending order ID
		
		frequentLink.clear();  // Clear the table for keeping key values in order
		Map<String, FpNode> topHead = new LinkedHashMap<String, FpNode>();
		for (Map.Entry<String, Integer> entry : mapList) 
		{
			topHead.put(entry.getKey(), null);
			frequentLink.put(entry.getKey(), entry.getValue());
		}
		return topHead;
	}
}
//-------------------------------------------------------------------------------------------------
// Generate holder for conditional tree.
class Holder 
{
	public final FpNode root;
	public final Map<String, FpNode> topHead;

	public Holder(FpNode root, Map<String, FpNode> topHead) 
	{
		this.root = root;
		this.topHead = topHead;
	}
}
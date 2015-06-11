package snu.cse.kr;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class ItemSets {
	/** We store the itemsets in a list named "levels".
	 Position i in "levels" contains the list of itemsets of size i */
	private final List<List<ItemSet>> levels = new ArrayList<List<ItemSet>>(); 
	/** the total number of itemsets **/
	private int itemsetsCount = 0;
	/** a name that we give to these itemsets (e.g. "frequent itemsets") */
	private final String name;

	/**
	 * Constructor
	 * @param name the name of these itemsets
	 */
	public ItemSets(String name) {
		this.name = name;
		levels.add(new ArrayList<ItemSet>()); // We create an empty level 0 by
												// default.
	}

	/**
	 * Print all itemsets to System.out, ordered by their size.
	 * @param nbObject The number of transaction/sequence in the database where
	 * there itemsets were found.
	 */
	public void printItemsets(int totCount) {
		System.out.println(" ------- " + name + " -------");
		int patternCount = 0;
		int levelCount = 0;
		DecimalFormat format = new DecimalFormat();
		format.setMinimumFractionDigits(0); 
		format.setMaximumFractionDigits(5); 
		//return format.format(frequence);
		// for each level (a level is a set of itemsets having the same number of items)
		for (List<ItemSet> level : levels) {
			// print how many items are contained in this level
			System.out.println("  L" + levelCount + " ");
			// for each itemset
			for (ItemSet itemset : level) {
				// print the itemset
				System.out.print("  pattern " + patternCount + ":  ");
				System.out.print(itemset.getItemsString() +" : " + format.format((double)(itemset.getCount()/(double)totCount)));
				// print the support of this itemset
				//System.out.print(" support :  "
				//		+ itemset.getRelativeSupportAsString(nbObject));
				patternCount++;
				System.out.println("");
			}
			levelCount++;
		}
		System.out.println("PATTERN COUNT:"+ patternCount);
		System.out.println(" --------------------------------");
	}

	/** 
	 * Add an itemset to this structure
	 * @param itemset the itemset
	 * @param k the number of items contained in the itemset
	 */
	public void addItemset(ItemSet itemset, int k) {
		while (levels.size() <= k) {
			levels.add(new ArrayList<ItemSet>());
		}
		levels.get(k).add(itemset);
		itemsetsCount++;
	}

	/**
	 * Get all itemsets.
	 * @return A list of list of itemsets.
	 * Position i in this list is the list of itemsets of size i.
	 */
	public List<List<ItemSet>> getLevels() {
		return levels;
	}

	/**
	 * Get the total number of itemsets
	 * @return the number of itemsets.
	 */
	public int getItemsetsCount() {
		return itemsetsCount;
	}
}

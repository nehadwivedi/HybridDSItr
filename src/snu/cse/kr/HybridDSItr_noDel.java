package snu.cse.kr;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class stores all the transactions in a Hybrid data structure of Bitset
 * and Array of integers and mines this data structure in an iterative fashion
 * to find the rules. This class omits pruning of supersets of deleted items
 * unlike HybridDSItr.
 * 
 * @author neha
 * 
 */

public class HybridDSItr_noDel {

	private long startTimestamp; // start time of the latest execution
	private long endTime; // end time of the latest execution

	int[] colCardinality;

	int countLast = 0;

	int MAX = 10;

	int transactionCount = 0;

	// int support = 3180;
	int support = 0;
	int peak = 0;

	List<Integer> highFreqItems = new ArrayList<Integer>();
	HashMap<Integer, Integer> itemMap = null;
	HashMap<Integer, Integer> countMap = null;
	Map<Integer, Integer> mapSupport = new HashMap<Integer, Integer>();
	Map<Integer, Integer> itemSupport = new HashMap<Integer, Integer>();
	Map<Integer, HashSet<IntArray>> lastItems = new HashMap<Integer, HashSet<IntArray>>();

	int rulesInts = 0;
	long maxMemory = 0;

	// Wrapper for array of ints created to modify the hash function.
	class IntArray {
		int[] a;

		@Override
		public int hashCode() {
			int hashCode = 0;

			for (int a1 : a) {
				hashCode += a1 * a1;
			}

			return Arrays.hashCode(a) + hashCode;
		}

		public boolean equals(Object arg0) {

			if (this.hashCode() == arg0.hashCode()) {
				return true;
			}
			return false;
		}

	}

	List<BitSet> bitSets = null;
	BitSet[] colSets = null;
	ArrayList<int[]> remColSets = null;

	int branchSize = 0;
	int fpSize;

	ItemSets itemSets = null;
	public int rulesCount = 0;

	int dsChangePos = 0;

	long maxNodes = 0;
	long maxKeys = 0;
	long maxRuleKeys = 0;
	long maxKeysBranch = 0;

	public static void main(String[] args) throws NumberFormatException,
			IOException {

		String fileName = args[0];
		float supVal = Float.parseFloat(args[1]);
		HybridDSItr_noDel hybridDS = new HybridDSItr_noDel();
		hybridDS.miningAlgo(fileToPath(fileName), supVal);

		System.out.println("FILENAME: " + fileName + " SUPPORT: " + supVal);

		hybridDS.printStats();

	}

	/**
	 * Run mining algorithm
	 * 
	 * @param input
	 * @param supportPerc
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void miningAlgo(String input, double supportPerc)
			throws FileNotFoundException, IOException {
		startTimestamp = System.currentTimeMillis();

		/*
		 * Scan frequency of all items
		 */
		scanDatabaseToDetermineFrequencyOfSingleItems(input);

		buildDS(input, supportPerc);

		itemSets = new ItemSets(input);

		System.out.println("Count of high freq items: " + branchSize);
		for (int i = 0; i <= branchSize - 1; ++i) {
			mineDS(i);

		}

		endTime = System.currentTimeMillis();

	}

	/**
	 * This method scans the input database to calculate the support of single
	 * items
	 * 
	 * @param input
	 *            the path of the input file
	 * @param mapSupport
	 *            a map for storing the support of each item (key: item, value:
	 *            support)
	 * @throws IOException
	 *             exception if error while writing the file
	 */
	private void scanDatabaseToDetermineFrequencyOfSingleItems(String input)
			throws FileNotFoundException, IOException {
		// Create object for reading the input file
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line;
		// for each line (transaction) until the end of file
		while (((line = reader.readLine()) != null)) {
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the line into items
			String[] lineSplited = line.split(" ");
			// for each item
			for (String itemString : lineSplited) {
				// increase the support count of the item
				Integer item = Integer.parseInt(itemString);
				// increase the support count of the item
				Integer count = mapSupport.get(item);
				if (count == null) {
					mapSupport.put(item, 1);
				} else {
					mapSupport.put(item, ++count);
				}
			}
			// increase the transaction count
			transactionCount++;
		}
		// close the input file
		reader.close();
	}

	/**
	 * This function builds an array of bit vectors pertaining to all the
	 * different kind of transactions.
	 * 
	 * @param input
	 * @param minSupp
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void buildDS(String input, double minSupp)
			throws NumberFormatException, IOException {

		support = (int) Math.ceil(minSupp * transactionCount);
		System.out.println("Support: " + support);
		createHeaderList(support);

		this.branchSize = highFreqItems.size();

		BufferedReader reader = new BufferedReader(new FileReader(input));

		String line;

		BitSet tempSet = new BitSet(branchSize);

		// HashMap<BitSet, Integer> bitMap = new HashMap<BitSet, Integer>();
		ArrayList<BitSet> bitMap = new ArrayList<BitSet>();

		// for each line (transaction) until the end of the file
		List<Integer> currentTrans = new ArrayList<Integer>();
		colCardinality = new int[branchSize];
		while (((line = reader.readLine()) != null)) {
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#'
					|| line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			String[] lineSplited = line.split(" ");
			// for each item in the transaction
			currentTrans.clear();
			for (String itemString : lineSplited) {
				// only add items that have the minimum support
				Integer index = itemMap.get(Integer.parseInt(itemString));
				if (index != null) {
					currentTrans.add(index + 1);
				}
			}

			lineSplited = null;

			if (currentTrans.size() > 0) {
				Collections.sort(currentTrans);

				for (int tempItem : currentTrans) {
					tempSet.set(branchSize - tempItem + 1);
				}

				bitMap.add((BitSet) tempSet.clone());
				for (int tempItem : currentTrans) {
					colCardinality[branchSize - tempItem]++;
				}

				/*
				 * if (!bitMap.containsKey(tempSet)) { bitMap.put((BitSet)
				 * tempSet.clone(), 1); for (int tempItem : currentTrans) {
				 * colCardinality[branchSize - tempItem]++; }
				 * memoryLogger.checkMemory(); } else { Integer tempVal =
				 * bitMap.get(tempSet); tempVal = tempVal + 1;
				 * bitMap.put(tempSet, tempVal); }
				 */
				tempSet.clear();

			}

		}

		fpSize = bitMap.size();
		System.out.println("BRANCHSIZE:" + branchSize);
		dsChangePos = 0;
		for (int i = highFreqItems.size() - 1; i >= 0; --i) {
			// System.out.println(colCardinality[i]);
			if (colCardinality[i] * 32 < fpSize) {
				dsChangePos = i;
				break;
			}
		}

		if (dsChangePos > 0) {

			System.out.println("dsChangePos:" + dsChangePos);
			remColSets = new ArrayList<int[]>(dsChangePos);
			colSets = new BitSet[branchSize - dsChangePos];
		} else {
			dsChangePos = 0;
			colSets = new BitSet[branchSize];
		}

		System.out.println("dsChangePos:" + dsChangePos);

		System.out.println("Items greater than i: " + dsChangePos);

		colSets = new BitSet[branchSize - dsChangePos];
		remColSets = new ArrayList<int[]>(dsChangePos);

		for (int j = 0; j < branchSize - dsChangePos; ++j) {
			colSets[j] = new BitSet(fpSize);
		}
		for (int j = 0; j < dsChangePos; ++j) {
			int[] tempHashSet = new int[colCardinality[j]];
			remColSets.add(tempHashSet);
			maxKeysBranch += colCardinality[j];
		}
		
		

		// Convert row bit maps to column bit maps
		int[] counter = new int[dsChangePos];
		for (int i = 0; i < bitMap.size(); ++i) {
			// int bitPos = 1;
			BitSet b = bitMap.get(i);
			for (int j = 0; j < branchSize; ++j) {
				if (b.get(j + 1)) {
					if (j >= dsChangePos) {
						colSets[j - dsChangePos].set(i + 1);
					} else {
						remColSets.get(j)[counter[j]] = i + 1;
						counter[j]++;
					}
				}

			}
		}

		// System.out.println("Bitsets size::" + this.treeArr.length);

		System.out.println("FREQUENCIES");
		int memorySaveCount = 0;

		/*
		 * for (BitSet col : colSets) { System.out.println(col.cardinality());
		 * if (col.cardinality() * 32 < this.treeArr.size()) {
		 * memorySaveCount++; }
		 * 
		 * }
		 */

		System.out.println("Cols count::" + colSets.length);
		System.out.println("Memory save cols::" + memorySaveCount);
		bitSets = null;

		reader.close();

	}

	/**
	 * This function mines the array of bit vectors generated and finds the
	 * frequent patterns.
	 * 
	 * @param pos
	 * @param itemSets
	 * @param keySet
	 * @param delKeys
	 */
	public void mineDS(int pos) {

		ItemSet item = new ItemSet();
		int[] items = { countMap.get(pos) };
		item.setItems(items);
		item.setCount(itemSupport.get(pos));
		itemSets.addItemset(item, 1);
		++rulesCount;

		List<int[]> keySet = new ArrayList<int[]>();
		List<int[]> tempIntSets = new ArrayList<int[]>();
		List<BitSet> tempBitSets = new ArrayList<BitSet>();

		HashSet<IntArray> delKeys = new HashSet<IntArray>();

		if (pos < dsChangePos) {

			tempIntSets.add(remColSets.get(pos));

		} else {

			tempBitSets.add(colSets[pos - dsChangePos]);
		}

		keySet.add(items);

		minePerItem(pos, itemSets, keySet, tempIntSets, tempBitSets, delKeys);

	}

	public void minePerItem(int pos, ItemSets itemSets, List<int[]> keySet,
			List<int[]> tempIntSets, List<BitSet> tempBitSets,
			HashSet<IntArray> delKeys) {
		int nodesCount = 0;
		int keysCount = 0;

		// For each item with frequency greater than the current item

		for (int i = pos + 1; i < branchSize; ++i) {

			int currentListSize = keySet.size();
			int setSize = tempIntSets.size();
			for (int k = 0; k < currentListSize; k++) {

				int[] keyItems = keySet.get(k);

				int[] chkKey = new int[keyItems.length];
				System.arraycopy(keyItems, 0, chkKey, 0, keyItems.length - 1);
				chkKey[keyItems.length - 1] = countMap.get(i);
				int[] newKey = new int[keyItems.length + 1];
				System.arraycopy(keyItems, 0, newKey, 0, keyItems.length);
				newKey[keyItems.length] = countMap.get(i);

				IntArray arrNewKey = new IntArray();
				arrNewKey.a = newKey;

				IntArray arrChkKey = new IntArray();
				arrChkKey.a = chkKey;

				/*if (
						 delKeys.isEmpty() || (!delKeys.isEmpty() &&
						  !delKeys.contains(arrChkKey))
						 ) {*/

					if ((i == branchSize - 1)
							&& lastItems.containsKey(countMap.get(pos))
							&& lastItems.get(countMap.get(pos)).contains(
									arrNewKey)) {
						++rulesCount;
						countLast++;
						continue;
					} else {

						int countFreq = 0;

						if (i < dsChangePos && k < setSize) {

							int[] andVec = remColSets.get(i);
							int[] secondAndVec = tempIntSets.get(k);

							int rawResult[] = new int[secondAndVec.length];

							int c = 0;

							int l = 0, m = 0;

							if (andVec != null) {
								while (l < andVec.length
										&& m < secondAndVec.length) {

									if (secondAndVec[m] > andVec[l]) {
										++l;
									} else if (andVec[l] > secondAndVec[m]) {
										++m;
									} else {
										countFreq += 1; // treeArr[andVec[l] -
														// 1];
										rawResult[c] = andVec[l];
										c++;
										++l;
										++m;
									}
								}
							}

							if (countFreq >= support) {
								int[] result = new int[c];
								System.arraycopy(rawResult, 0, result, 0, c);
								tempIntSets.add(result);
							}

						} else if (i >= dsChangePos && k < setSize) {

							BitSet andVec = colSets[i - dsChangePos];
							int[] secondAndVec = tempIntSets.get(k);

							int rawResult[] = new int[secondAndVec.length];
							int c = 0;
							for (int num : secondAndVec) {
								if (andVec.get(num)) {
									rawResult[c] = num;
									countFreq += 1;// treeArr[num - 1];
									c++;

								}
							}

							if (countFreq >= support) {

								int[] result = new int[c];
								System.arraycopy(rawResult, 0, result, 0, c);
								if (i < branchSize - 1)
									tempIntSets.add(result);

							}

						} else if (i >= dsChangePos && k >= setSize) {

							BitSet andVec = tempBitSets.get(k);
							BitSet tempAndVec = new BitSet(fpSize);
							tempAndVec.or(colSets[i - dsChangePos]);
							tempAndVec.and(andVec);
							countFreq += tempAndVec.cardinality();

							if (countFreq >= support) {
								if (i < branchSize - 1)
									tempBitSets.add(tempAndVec);
							} else {
								tempAndVec = null;
							}

						}

						if (countFreq >= support) {

							keySet.add(newKey);
							ItemSet newItem = new ItemSet();
							newItem.setItems(newKey);
							newItem.setCount(countFreq);
							itemSets.addItemset(newItem, newKey.length);
							maxRuleKeys += newKey.length;
							++rulesCount;

							if (i == branchSize - 1 && newKey.length >= 3) {
								for (int l = 1; l < newKey.length - 1; ++l) {
									int size = newKey.length - l;
									IntArray iArr = new IntArray();
									int[] newK = new int[newKey.length - l];
									System.arraycopy(newKey, l, newK, 0, size);
									iArr.a = newK;
									if (lastItems.containsKey(newKey[l])) {
										lastItems.get(newKey[l]).add(iArr);
									} else {
										HashSet<IntArray> newSet = new HashSet<IntArray>();
										newSet.add(iArr);
										lastItems.put(newKey[l], newSet);
									}
								}
							}

						}

						/*else {

							delKeys.add(arrNewKey);
						}*/
					}

				/*} else {
					// delKeys.add(arrNewKey);
				}*/
			}
			// delKeys.clear();

		}

		nodesCount += tempBitSets.size();
		for (int[] k : keySet) {
			keysCount += k.length;
		}
		for (int[] k : tempIntSets) {
			keysCount += k.length;
		}

		for (int k1 : lastItems.keySet()) {
			keysCount += lastItems.get(k1).size();
		}

		// Calculating memory

		if ((maxNodes * transactionCount / 8 + maxKeys * 4) < (nodesCount
				* transactionCount / 8 + keysCount * 4)) {
			maxNodes = nodesCount;
			maxKeys = keysCount;
			peak = pos;
		}

		keySet.clear();

	}

	/**
	 * Creates list of high frequency items and utils maps which will help in
	 * fast mining
	 * 
	 * @param minSupport
	 */

	void createHeaderList(int minSupport) {
		// create an array to store the header list with
		// all the items stored in the map received as parameter
		Set<Integer> keySet = mapSupport.keySet();
		for (Integer key : keySet) {
			if (mapSupport.get(key) >= minSupport) {
				highFreqItems.add(key);
			}
		}

		// sort the header table by decreasing order of support
		Collections.sort(highFreqItems, new Comparator<Integer>() {
			public int compare(Integer id1, Integer id2) {
				// compare the support
				int compare = mapSupport.get(id2) - mapSupport.get(id1);
				// if the same frequency, we check the lexical ordering!
				if (compare == 0) {
					return (id2 - id1);
				}
				// otherwise we use the support
				return compare;
			}
		});
		int count = 0;
		System.out.println("ALL FREQUENCIES");

		// Helper maps
		countMap = new HashMap<Integer, Integer>();
		itemMap = new HashMap<Integer, Integer>();
		for (Integer item : highFreqItems) {
			itemMap.put(item, count);
			count++;
		}
		count = 0;
		for (Integer item : highFreqItems) {
			countMap.put(count, item);
			itemSupport.put(count, mapSupport.get(item));
			count++;
		}

	}

	public void printStats() {

		System.out.println("COUNT Last: " + countLast);
		System.out
				.println("=============  ArrayVectorDataMiningItr_noDel =============");

		System.out.println("Transactions count from database : "
				+ transactionCount);
		System.out.println("Frequent itemsets count : " + rulesCount);

		System.out.println("Total time  ~ " + (endTime - startTimestamp)
				+ " ms");

		System.out.println("here");
		if (peak < dsChangePos) {
			for (int i = 0; i < (dsChangePos - peak); ++i) {
				maxKeys += colCardinality[i];
			}
			maxNodes += (branchSize - dsChangePos);

		} else {
			maxNodes += (branchSize - peak);
		}
		System.out.println("Max nodes: " + (maxNodes) + "  \n");
		System.out.println("Max keys: " + (maxKeys) + "  \n");

		maxMemory = ((maxNodes) * (transactionCount / 8) + (maxKeys) * 4) / 1024;
		System.out.println("Max Memory: " + maxMemory);

		System.out
				.println("===================================================");
	}

	/**
	 * Util function to decode file URLS.
	 * 
	 * @param filename
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String fileToPath(String filename)
			throws UnsupportedEncodingException {
		URL url = HybridDSItr_noDel.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}

}

package snu.cse.kr;

public class ItemSet {
	int[] items;
	Integer count;
	
	public int[] getItems() {
		return items;
	}
	public void setItems(int[] items) {
		this.items = items;
	}
	
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}
	
	String getItemsString(){
		StringBuffer res = new StringBuffer("");
		for(int item: items ){
			res.append(item +" ");
		}
		return res.toString();
		
	}
	
	
}

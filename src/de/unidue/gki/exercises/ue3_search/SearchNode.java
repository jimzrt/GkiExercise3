package de.unidue.gki.exercises.ue3_search;

public class SearchNode {

	private SearchNode parent;
	private int depth;
	private String href;
	public boolean visited;


	/* könnte an dieser Stelle verbessert werden, nein spaß */
	
	public SearchNode(SearchNode parent, int depth, String href) {
		super();
		this.parent = parent;
		this.depth = depth;
		this.href = href;
		visited = false;
	}

	public SearchNode getParent() {
		return parent;
	}

	public void setParent(SearchNode parent) {
		this.parent = parent;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {

		this.href = href;
	}

	@Override
	public String toString() {
		String parentHref = "";
		if (parent != null) {
			parentHref = parent.href;
		}

		return depth + "\t" + href + "\t(" + parentHref + ")";
	}
}

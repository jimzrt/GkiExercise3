package de.unidue.gki.exercises.ue3_search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchLinks {

	// startUrl
	private final static String URL_START = "https://www.uni-due.de/en/";
	// goalUrl
	private final static String URL_GOAL = "http://pinterest.com";
	// max depth after which the programm stops searching
	private final int MAX_DEPTH = 5;
	// timeout for the Jsoup method
	private final int TIMEOUT_MILLIS = 600;
	// which tactic to be used? 1 = breadth first, 2 = depth first,
	// 3 = iterative deepening
	private final int TACTIC = 1;

	// these are used to store the start URL and the goal URL
	private String start = null;
	private String goal = null;

	// Hashset of all visited links
	private Set<String> visited = new HashSet<>();
	private int visitedWebsites = 0;
	private URL goalURL = null;

	public SearchLinks(final String startHref, final String goalHref) {
		this.start = startHref;
		this.goal = goalHref;
	}

	public static void main(String[] args) {
		/* here we run our search */
		new SearchLinks(URL_START, URL_GOAL).start();
	}

	public final void start() {

		try {
			goalURL = new URL(goal);
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			return;
		}
		visitedWebsites = 0;
		SearchNode node = null;

		switch (TACTIC) {
		case 1: // bfs

			 node = bfs(new SearchNode(null, 0, start), MAX_DEPTH);
			break;
		case 2: // dfs

			System.out.println("Limit = " + MAX_DEPTH);
			 node = dfs(new SearchNode(null, 0, start), MAX_DEPTH);



			break;
		case 3: // iterative deepening

			for (int i = 1; i <= MAX_DEPTH; i++) {

				System.out.println("Limit = " + i);
				node = dfs(new SearchNode(null, 0, start), i);
				visited.clear();

				System.out.println("");

			}
			break;

		}
        System.out.println("");
        System.out.println("Searched " + visitedWebsites + " unique sites.");
        if(node != null){
			System.out.println(goal + " found!");
			System.out.println("Depth: " + (node.getDepth()));
			System.out.println("");
			StringBuilder result = new StringBuilder(goal);
			while (node != null) {
				result.insert(0, node.getHref() + " --> ");
				node = node.getParent();
			}
			System.out.println(result.toString());
			System.out.println("");
		} else {
			System.out.println("No path found!");
		}

	}



	public SearchNode dfs(SearchNode node, int depth) {

		Stack<SearchNode> searchList = new Stack<>();
		node.visited = true;
		searchList.push(node);

		while (!searchList.isEmpty()) {

            System.out.println(searchList.size() + " elements in queue");
            SearchNode headNode = searchList.pop();

// Visit child first before grandchild
			System.out.println("Searching " + headNode.getHref() + "...");
			if(headNode.getDepth() < depth){
                List<SearchNode> nodeList = getChildNodes(headNode,headNode.getDepth() + 1);
                //int nodesLeft = headNode.getDepth() + 1 < depth ? (searchList.size()+ nodeList.size()) : searchList.size();
				System.out.println("Added " + nodeList.size() + " links (depth "+headNode.getDepth()+")");


			for (SearchNode n : nodeList) {
				if(checkFound(n.getHref()))
					return n;
				if (!n.visited && n.getDepth() < depth) {
					searchList.add(n);
					n.visited = true;

				}

			}
            }
		}
		return null;
	}

	public SearchNode bfs(SearchNode node, int depth) {

		Queue<SearchNode> searchList = new ArrayDeque<>();
		node.visited = true;
		searchList.add(node);

		while (!searchList.isEmpty()) {

            System.out.println(searchList.size() + " elements in queue");
            // removes head of queue
			SearchNode headNode = searchList.poll();

			// Visit child first before grandchild
			System.out.println("Searching " + headNode.getHref() + "...");
			if(headNode.getDepth() < depth){
                List<SearchNode> nodeList = getChildNodes(headNode,headNode.getDepth() + 1);
				System.out.println("Added " + nodeList.size() + " links (depth "+headNode.getDepth()+")");


			for (SearchNode n : nodeList) {
				if(checkFound(n.getHref()))
					return n;
				if (!n.visited && n.getDepth() < depth) {
					searchList.add(n);
					n.visited = true;

				}

			}
            }
		}
		return null;
	}

	private boolean checkFound(String href) {
        return href.contains(goalURL.getHost());
	}

	private List<SearchNode> getChildNodes(SearchNode node, int depth) {
		visitedWebsites++;
        List<SearchNode> childNodes = new ArrayList<>();

        Document doc;
		try {
			doc = Jsoup.connect(node.getHref()).timeout(TIMEOUT_MILLIS).get();
		} catch (IOException e) {
            System.out.println("Could not connect to " + node.getHref());
            return childNodes;
        }

        if (doc != null) {
			//doc.select("a[href*=intranet]").remove();
			for (Element link : doc.select("a")) {
                String href = link.absUrl("href").trim();
				if (href.startsWith("http://") || href.startsWith("https://")) {

					URL url = null;
					try {
						url = new URL(href);



					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

					Pattern p = Pattern.compile(".*?([^.]+\\.[^.]+)");
					Matcher m = p.matcher(url.getHost());
					if (m.matches() && !visited.contains(m.group(1))) {
						visited.add(m.group(1));
						childNodes.add(new SearchNode(node, depth, url.getProtocol() +"://"+ url.getHost()));


					}



				}
			}
		}
		return childNodes;
	}


}

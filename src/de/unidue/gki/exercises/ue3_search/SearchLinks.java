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
    private final int TACTIC = 3;

    // these are used to store the start URL and the goal URL
    private String start = null;
    private String goal = null;

    // HashSet of all visited links
    private Set<String> visited = new HashSet<>();
    private int visitedWebsites = 0;
    private URL goalURL = null;


    //Constructor to set start and goal
    private SearchLinks(final String startHref, final String goalHref) {
        this.start = startHref;
        this.goal = goalHref;
    }

    //main - instantiate new SearchLinks object and run our search
    public static void main(String[] args) {
        new SearchLinks(URL_START, URL_GOAL).start();
    }

    private void start() {
        //create URL object from goal string
        try {
            goalURL = new URL(goal);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            return;
        }

        //keep track of visited websites
        visitedWebsites = 0;


        //choose search tactic depending on
        SearchNode node = null;
        switch (TACTIC) {
            case 1: // bfs
                node = bfs(new SearchNode(null, 0, start), MAX_DEPTH);
                break;
            case 2: // dfs
                node = dfs(new SearchNode(null, 0, start), MAX_DEPTH);
                break;
            case 3: // iterative deepening
                //use dfs with ascending depth, starting at 1 up to max depth
                for (int i = 1; i <= MAX_DEPTH; i++) {
                    System.out.println("Max depth: " + i);
                    node = dfs(new SearchNode(null, 0, start), i);
                    visited.clear();
                    System.out.println("");
                }
                break;
        }

        System.out.println("");
        System.out.println("Searched " + visitedWebsites + " unique sites.");

        //if goal is found
        if(node != null){
            System.out.println(goal + " found!");
            System.out.println("Depth: " + (node.getDepth()));
            System.out.println("");

            //visualize path of nodes
            StringBuilder result = new StringBuilder(goal);
            while (node != null) {
                result.insert(0, node.getHref() + " --> ");
                node = node.getParent();
            }
            System.out.println(result.toString());
            System.out.println("");

        } else {
            //goal not found
            System.out.println("No path found!");
        }

    }


    //iterative depth first search implementation using a stack
    private SearchNode dfs(SearchNode node, int depth) {

        Stack<SearchNode> searchList = new Stack<>();
        node.visited = true;

        //push root node to stack
        searchList.push(node);

        while (!searchList.isEmpty()) {

            System.out.println(searchList.size() + " elements in queue");

            //remove head of stack
            SearchNode headNode = searchList.pop();

            System.out.println("Searching " + headNode.getHref() + "...");
            if (headNode.getDepth() < depth) {
                //get all child nodes i.e. get all urls found in headNode
                List<SearchNode> nodeList = getChildNodes(headNode,headNode.getDepth() + 1);
                System.out.println("Added " + nodeList.size() + " links (depth " + headNode.getDepth() + ")");

                //for all found child nodes
                for (SearchNode n : nodeList) {

                    //if goal url then return
                    if (checkFound(n.getHref()))
                        return n;

                    //if it has not been visited and depth is less than max depth then add to search
                    if (!n.visited && n.getDepth() < depth) {
                        searchList.add(n);
                        n.visited = true;

                    }

                }
            }
        }
        return null;
    }

    //breadth first search implementation using a queue
    private SearchNode bfs(SearchNode node, int depth) {

        Queue<SearchNode> searchList = new ArrayDeque<>();
        node.visited = true;

        //push root node to queue
        searchList.add(node);

        while (!searchList.isEmpty()) {

            System.out.println(searchList.size() + " elements in queue");

            // removes head of queue
            SearchNode headNode = searchList.poll();

            System.out.println("Searching " + headNode.getHref() + "...");
            if (headNode.getDepth() < depth) {
                //get all child nodes i.e. get all urls found in headNode
                List<SearchNode> nodeList = getChildNodes(headNode,headNode.getDepth() + 1);
                System.out.println("Added " + nodeList.size() + " links (depth " + headNode.getDepth() + ")");

                //for all found child nodes
                for (SearchNode n : nodeList) {

                    //if goal url then return
                    if (checkFound(n.getHref()))
                        return n;

                    //if it has not been visited and depth is less than max depth then add to search
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


        //parse node URL via JSoup
        Document doc;
        try {
            doc = Jsoup.connect(node.getHref()).timeout(TIMEOUT_MILLIS).get();
        } catch (IOException e) {
            System.out.println("Could not connect to " + node.getHref());
            return childNodes;
        }

        //find all links in node
        if (doc != null) {
            for (Element link : doc.select("a")) {
                String href = link.absUrl("href").trim();
                if (href.startsWith("http://") || href.startsWith("https://")) {

                    //create URL object from link
                    URL url;
                    try {
                        url = new URL(href);
                    } catch (MalformedURLException e) {
                        System.out.println("error at parsing url " + href);
                        break;
                    }

                    //use regex to filter root domain like facebook.com in de-de.facebook.com
                    Pattern p = Pattern.compile(".*?([^.]+\\.[^.]+)");
                    Matcher m = p.matcher(url.getHost());

                    //if url is valid and has not been visited
                    if (m.matches() && !visited.contains(m.group(1))) {
                        //mark as visited
                        visited.add(m.group(1));

                        //add to child nodes list
                        childNodes.add(new SearchNode(node, depth, url.getProtocol() + "://" + url.getHost()));

                    }


                }
            }
        }
        return childNodes;
    }


}

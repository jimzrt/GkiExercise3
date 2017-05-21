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

    //-----------
    // global values
    // startUrl
    private final static String URL_START = "https://www.uni-due.de/en/";
    // goalUrl
    private final static String URL_GOAL = "http://pinterest.com";
    // timeout for connecting to websites
    private final static int TIMEOUT_MILLIS = 600;

    //All possible tactics as type
    public enum Tactic {
        BFS,
        DFS,
        ITERATIVE_DEEPENING
    }

    //All possible modes as type
    //FULL_PATH = follow every path on host at most once
    //          -> e.g. https://www.uni-due.de/studium, https://www.uni-due.de/forschung, https://www.uni-due.de/international
    //HOST_ONLY = visit each host at most once
    //          -> if https://www.uni-due.de/studium has been visited, don't follow any link that leads to https://www.uni-due.de/.....
    public enum Mode {
        FULL_PATH,
        HOST_ONLY
    }

    //-----------

    // which tactic to be used?
    private final Tactic tactic;
    //which mode to be used?
    private final Mode mode;
    //how deep do we go?
    private final int maxDepth;
    // these are used to store the start URL and the goal URL
    private String start = null;
    private String goal = null;
    private URL goalURL = null;
    // HashSet of all visited links
    private Set<String> visited = new HashSet<>();
    private int visitedWebsites = 0;

    //Constructor to set start and goal
    private SearchLinks(final String startHref, final String goalHref, int maxDepth, Tactic tactic, Mode mode) {
        this.start = startHref;
        this.goal = goalHref;
        this.maxDepth = maxDepth;
        this.tactic = tactic;
        this.mode = mode;
    }

    //main - instantiate new SearchLinks object via user input and run our search
    public static void main(String[] args) {
        System.out.println("******************************");
        System.out.println("Find shortest path between " + URL_START + " and " + URL_GOAL);
        System.out.println("******************************");

        //get tactic
        String tacticInput;
        Scanner sc = new Scanner(System.in);
        while(true) {
            System.out.print("Choose tactic (1=breadth first search, 2=depth first search, 3=iterative deepening): ");
            tacticInput = sc.nextLine().trim();
            if(!tacticInput.matches("[1-3]")){
                System.out.println("Wrong input! Only 1,2,3 allowed.\n");
            } else {
                break;
            }
        }
        Tactic tactic = null;
        switch (Integer.parseInt(tacticInput)) {
            case 1:
                tactic = Tactic.BFS;
                break;
            case 2:
                tactic = Tactic.DFS;
                break;
            case 3:
                tactic = Tactic.ITERATIVE_DEEPENING;
                break;
        }

        //get depth
        String depthInput;
        while(true) {
            System.out.print("Choose max depth: ");
            depthInput = sc.nextLine().trim();
            if (!depthInput.matches("[1-9]+[0-9]*")) {
                System.out.println("Wrong input! Only numbers allowed.\n");
            } else {
                break;
            }
        }

        //get mode
        String modeInput;
        while (true) {
            System.out.println("Choose mode (1=full path, 2=host only): ");
            System.out.println("full path = follow every path on host (at most once)");
            System.out.println("host only = visit each host at most once [faster]");
            modeInput = sc.nextLine().trim();
            if (!modeInput.matches("[1-2]")) {
                System.out.println("Wrong input! Only 1,2 allowed.\n");
            } else {
                break;
            }
        }
        Mode mode = null;
        switch (Integer.parseInt(modeInput)) {
            case 1:
                mode = Mode.FULL_PATH;
                break;
            case 2:
                mode = Mode.HOST_ONLY;
                break;

        }

        new SearchLinks(URL_START, URL_GOAL, Integer.parseInt(depthInput), tactic, mode).start();
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

        //starting node
        SearchNode rootNode = new SearchNode(null, 0, start);
        //final node
        SearchNode targetNode = null;

        //choose search tactic depending on input
        System.out.print("Finding path with max depth of " + maxDepth + " using ");
        switch (tactic) {
            case BFS: // bfs
                System.out.println("breadth first search...");
                targetNode = bfs(rootNode, maxDepth);
                break;
            case DFS: // dfs
                System.out.println("depth first search...");
                targetNode = dfs(rootNode, maxDepth);
                break;
            case ITERATIVE_DEEPENING: // iterative deepening
                //use dfs with ascending depth, starting at 1 up to max depth
                System.out.println("iterative deepening...");
                for (int i = 1; i <= maxDepth; i++) {
                    System.out.println("Max depth: " + i);
                    targetNode = dfs(rootNode, i);
                    visited.clear();
                    System.out.println("");
                    if (checkFound(targetNode))
                        break;
                }
                break;
        }

        System.out.println("");
        System.out.println("Searched " + visitedWebsites + " unique sites.");

        //if goal is found
        if (checkFound(targetNode)) {
            System.out.println(goal + " found!");
            System.out.println("Depth: " + (targetNode.getDepth()));
            System.out.println("");

            //visualize path of nodes
            StringBuilder result = new StringBuilder(goal);
            while (targetNode != null) {
                result.insert(0, targetNode.getHref() + " --> ");
                targetNode = targetNode.getParent();
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
                for (SearchNode childNode : nodeList) {

                    //if goal url then return
                    if (checkFound(childNode))
                        return childNode;

                    //if it has not been visited and depth is less than max depth then add to search
                    if (!childNode.visited && childNode.getDepth() < depth) {
                        searchList.add(childNode);
                        childNode.visited = true;

                    }

                }
            }
        }
        return node;
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
                for (SearchNode childNode : nodeList) {

                    //if goal url then return
                    if (checkFound(childNode))
                        return childNode;

                    //if it has not been visited and depth is less than max depth then add to search
                    if (!childNode.visited && childNode.getDepth() < depth) {
                        searchList.add(childNode);
                        childNode.visited = true;

                    }

                }
            }
        }
        return node;
    }

    private boolean checkFound(SearchNode node) {
        return node.getHref().contains(goalURL.getHost());
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
                    Matcher m = null;
                    if (mode == Mode.FULL_PATH) {
                        m = p.matcher(url.getHost() + url.getPath());
                    } else if (mode == Mode.HOST_ONLY) {
                        m = p.matcher(url.getHost());
                    }


                    //if url is valid and has not been visited
                    if (m.matches() && !visited.contains(m.group(1))) {
                        //mark as visited
                        visited.add(m.group(1));

                        //add to child nodes list
                        if (mode == Mode.FULL_PATH) {
                            childNodes.add(new SearchNode(node, depth, url.getProtocol() + "://" + url.getHost() + url.getPath()));
                        } else if (mode == Mode.HOST_ONLY) {
                            childNodes.add(new SearchNode(node, depth, url.getProtocol() + "://" + url.getHost()));
                        }


                    }


                }
            }
        }
        return childNodes;
    }


}

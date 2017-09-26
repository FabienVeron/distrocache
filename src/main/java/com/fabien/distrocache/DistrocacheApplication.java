package com.fabien.distrocache;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

//@SpringBootApplication
public class DistrocacheApplication {

    public static void main(String[] args) {
//		SpringApplication.run(DistrocacheApplication.class, args);

        Node lisa = new Node("lisa");
        Node bart = new Node("bart");
        System.out.println("lisa = " + lisa);
        System.out.println("bart = " + bart);
        lisa.registerOtherNode(bart);
        bart.registerOtherNode(lisa);

        bart.setCacheAndReplicate("secret", 42);
        System.out.println("secret = " + lisa.getCache("secret"));

        double delta = 0.10;
        double closeCallPrice = 1.3;
        double closePrice = 15;

        Thread rtPrice = new Thread(() -> {
            while (true) {
                try {
                    long waitingTime = ThreadLocalRandom.current().nextLong(300,1000);
                    Thread.sleep(waitingTime);
                    double price = ThreadLocalRandom.current().nextDouble(15,18);

                    double currentPrice = closeCallPrice + delta * (closePrice - price);
                    System.out.println("currentPrice = " + currentPrice);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        rtPrice.start();

        IntStream.range(0,50).forEach( (n) -> {
            new Thread(() -> {
                lisa.setCacheAndReplicate("secret", 42);
                System.out.println(bart.toString() + bart.getCache("secret"));
            }).start();
            new Thread(() -> {
                bart.setCacheAndReplicate("secret", 78);
                System.out.println(lisa.toString() + lisa.getCache("secret"));
            }).start();
        });
    }
}

class Node {

    String name;

    boolean enabled;

    List<Node> otherNodes;

    HashMap<String, Integer> map;

    public Node(String name) {
        enabled = true;
        map = new HashMap<>();
        otherNodes = new ArrayList<>();

        this.name = name;
    }

    public void registerOtherNode(Node otherNode) {
        otherNodes.add(otherNode);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void networkDelay() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setCacheAndReplicate(String key, int value) {
        setCache(key,value);

        for (Node otherNode : otherNodes) {
            networkDelay();
            otherNode.setCache(key,value);
        }
    }

    public void setCache(String key, int value) {
        if (!enabled)
            return;

        map.put(key, value);
    }

    public int getCache(String key) {
        if (!enabled)
            throw new IllegalStateException();

        return map.get(key);
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

class NodeService {
    List<HashMap<String, Integer>> maps;

    @PostConstruct
    public void initCache() {
        maps = new ArrayList<>();
        IntStream.range(0, 2).forEach(i -> maps.add(new HashMap<>()));
    }

    public void setCache(int node, String key, int value) {
        for (Map<String, Integer> map : maps) {
            map.put(key, value);
        }
    }

    public int getCache(int node, String key) {
        return maps.get(node).get(key);
    }
}

@RestController
@RequestMapping("api/node/")
class NodeController {

    @RequestMapping
    public String health() {
        return "OK";
    }

    @RequestMapping("/set/{key}")
    public void setCache(@PathVariable String key, @RequestBody String value) {

    }
}

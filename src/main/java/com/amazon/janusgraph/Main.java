package com.amazon.janusgraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        JanusGraph graph = JanusGraphFactory.open("dynamodb.properties");
        GraphTraversalSource g = graph.traversal();
        JanusGraphManagement mgmt = graph.openManagement();

        final Vertex user1 = g.addV("name", "pavel").next();

        final Vertex user2 = g.addV("name", "artem").next();

        user1.addEdge("friend", user2);
        user2.addEdge("friend", user1);

        graph.tx().commit();

        final List<Vertex> vertexes = g.V().toList();
        final List<Edge> edges = g.E().toList();

        System.out.println(vertexes.toString());
        System.out.println(edges.toString());
        System.exit(0);
    }
}

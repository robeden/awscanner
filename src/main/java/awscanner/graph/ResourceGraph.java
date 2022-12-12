package awscanner.graph;

import awscanner.RegionInfo;
import awscanner.util.ResourceInfo;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;


public class ResourceGraph {
    private final Graph<GraphResource, DefaultEdge> graph = new DefaultDirectedGraph<>( DefaultEdge.class );
    private final Map<String,GraphResource> vertex_map = new HashMap<>();


    public void appendRegionInfo( RegionInfo region ) {
        // Pass 1: verticies
        loadResourceVertices( region.ebs_volumes().values(), ResourceType.EBS_VOLUME, region );
        loadResourceVertices( region.rds_instances().values(), ResourceType.DB_INSTANCE, region );
        loadResourceVertices( region.images().values(), ResourceType.EC2_IMAGE, region );
        loadResourceVertices( region.instances().values(), ResourceType.EC2_INSTANCE, region );
        loadResourceVertices( region.snapshots().values(), ResourceType.EC2_SNAPSHOT, region );

        // Pass 2: edges
        //noinspection unchecked
        Stream.<Stream<? extends ResourceInfo>>of(
            region.ebs_volumes().values().stream(),
            region.rds_instances().values().stream(),
            region.images().values().stream(),
            region.instances().values().stream(),
            region.snapshots().values().stream()
        )
            .flatMap( o -> ( Stream<ResourceInfo> ) o )
            .forEach( resource -> {
                loadConnections( resource, resource.usesIds() );
            } );
    }

    public void export( File file ) throws IOException {
        // DOT Language docs: https://www.graphviz.org/doc/info/lang.html
        DOTExporter<GraphResource,DefaultEdge> exporter =
            new DOTExporter<>( gr -> gr.id().replace( '-', '_' ) );
        exporter.setVertexAttributeProvider(
            ( v ) -> Map.of( "label", DefaultAttribute.createAttribute( v.id() ) ) );
        exporter.exportGraph( graph, file );
    }


    private <R extends ResourceInfo> void loadResourceVertices(Collection<R> resources,
        ResourceType type, RegionInfo region ) {

        resources.forEach( r -> {
            var resource = new GraphResource( r.id(), type, region.region().id(), region.ownerId() );
            graph.addVertex( resource );
            vertex_map.put( r.id(), resource );
        } );
    }

    private void loadConnections(ResourceInfo from, Stream<String> to) {
        GraphResource from_resource = vertex_map.get(from.id());
        to.forEach( to_id -> {
            GraphResource to_resource = vertex_map.get(to_id);
            if( to_resource != null ) {
                graph.addEdge( from_resource, to_resource );
            }
        } );
    }
}

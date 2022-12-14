package awscanner.graph;

import awscanner.RegionInfo;
import awscanner.util.ResourceInfo;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.ExportException;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


public class ResourceGraph {
    private final Graph<ResourceInfo, DefaultEdge> graph = new DefaultDirectedGraph<>( DefaultEdge.class );
    private final Map<String,ResourceInfo> vertex_map = new HashMap<>();


    public void appendRegionInfo( RegionInfo region ) {
        // Pass 1: vertices
        loadResourceVertices( region.ebs_volumes().values() );
        loadResourceVertices( region.rds_instances().values() );
        loadResourceVertices( region.images().values() );
        loadResourceVertices( region.instances().values() );
        loadResourceVertices( region.snapshots().values() );

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
            .forEach( resource -> loadConnections( resource, resource.usesIds() ) );
    }

    public void export( File file ) throws ExportException {
        // DOT Language docs: https://www.graphviz.org/doc/info/lang.html
        DOTExporter<ResourceInfo,DefaultEdge> exporter =
            new DOTExporter<>( gr -> gr.id().replace( '-', '_' ) );
        exporter.setVertexAttributeProvider(
            ( v ) -> Map.of( "label", DefaultAttribute.createAttribute( v.id() ) ) );
        exporter.exportGraph( graph, file );
    }


    private <R extends ResourceInfo> void loadResourceVertices(Collection<R> resources ) {
        resources.forEach( r -> {
            graph.addVertex( r );
            vertex_map.put( r.id(), r );
        } );
    }

    private void loadConnections(ResourceInfo from, Stream<String> to) {
        ResourceInfo from_resource = vertex_map.get(from.id());
        to.forEach( to_id -> {
            ResourceInfo to_resource = vertex_map.get(to_id);
            if( to_resource != null ) {
                graph.addEdge( from_resource, to_resource );
            }
        } );
    }
}

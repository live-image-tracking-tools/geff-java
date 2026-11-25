package org.litt.geff;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Mesh shape specification for GEFF.
 * <p>
 * Metadata specifying which node properties store the vertices, triangles and
 * optional normals of a triangular mesh, corresponding to the {@code geff/mesh}
 * attribute of the GEFF specification.
 * <p>
 * For instance:
 *
 * <pre>
 * "mesh": {
 *     "vertices": "mesh_vertices",
 *     "triangles": "mesh_triangles",
 *     "vertex_normals": "mesh_vertex_normals",
 *     "triangle_normals": "mesh_triangle_normals",
 *     "vertex_axes": [ "x", "y", "z" ]
 * }
 * </pre>
 *
 * {@code vertices} and {@code triangles} are required; {@code vertex_normals},
 * {@code triangle_normals} and {@code vertex_axes} are optional.
 *
 * @see <a href=
 *      "https://github.com/live-image-tracking-tools/geff/pull/449">mesh
 *      support proposal for GEFF specification</a>
 * @author Jean-Yves Tinevez
 */
public class MeshMetadata
{

	@SerializedName( "vertices" )
	private String vertices;

	@SerializedName( "triangles" )
	private String triangles;

	@SerializedName( "vertex_normals" )
	private String vertexNormals; // Optional - can be null

	@SerializedName( "triangle_normals" )
	private String triangleNormals; // Optional - can be null

	/**
	 * Optional list of axis names specifying the order of the spatial
	 * dimensions (columns) used in the {@code vertices} and
	 * {@code vertex_normals} arrays, when that order differs from the
	 * default. Names must match {@code name} values in the
	 * {@code GeffMetadata.axes} list. If {@code null}, the default is the
	 * order of the spatial-type axes as they appear in
	 * {@code GeffMetadata.axes}.
	 */
	@SerializedName( "vertex_axes" )
	private List< String > vertexAxes; // Optional - can be null

	/**
	 * Default constructor
	 */
	public MeshMetadata()
	{}

	/**
	 * Constructor with the required fields
	 */
	public MeshMetadata( final String vertices, final String triangles )
	{
		this.vertices = vertices;
		this.triangles = triangles;
	}

	/**
	 * Constructor with all fields
	 */
	public MeshMetadata( final String vertices, final String triangles, final String vertexNormals, final String triangleNormals )
	{
		this.vertices = vertices;
		this.triangles = triangles;
		this.vertexNormals = vertexNormals;
		this.triangleNormals = triangleNormals;
	}

	// Getters and Setters
	public String getVertices()
	{
		return vertices;
	}

	public void setVertices( final String vertices )
	{
		this.vertices = vertices;
	}

	public String getTriangles()
	{
		return triangles;
	}

	public void setTriangles( final String triangles )
	{
		this.triangles = triangles;
	}

	public String getVertexNormals()
	{
		return vertexNormals;
	}

	public void setVertexNormals( final String vertexNormals )
	{
		this.vertexNormals = vertexNormals;
	}

	public String getTriangleNormals()
	{
		return triangleNormals;
	}

	public void setTriangleNormals( final String triangleNormals )
	{
		this.triangleNormals = triangleNormals;
	}

	/**
	 * Returns the optional list of axis names for the columns of the
	 * {@code vertices} and {@code vertex_normals} arrays, or {@code null} to
	 * use the order of the spatial-type axes as they appear in
	 * {@code GeffMetadata.axes}.
	 *
	 * @return the axis names, or {@code null}.
	 */
	public List< String > getVertexAxes()
	{
		return vertexAxes;
	}

	/**
	 * Sets the optional list of axis names for the columns of the
	 * {@code vertices} and {@code vertex_normals} arrays.
	 *
	 * @param vertexAxes
	 *            the axis names, or {@code null} to use the default.
	 */
	public void setVertexAxes( final List< String > vertexAxes )
	{
		this.vertexAxes = vertexAxes;
	}

	@Override
	public String toString()
	{
		return String.format( "MeshMetadata{vertices=%s, triangles=%s, vertexNormals=%s, triangleNormals=%s, vertexAxes=%s}",
				vertices, triangles, vertexNormals, triangleNormals, vertexAxes );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof MeshMetadata ) )
			return false;
		final MeshMetadata that = ( MeshMetadata ) o;
		return Objects.equals( vertices, that.vertices ) && Objects.equals( triangles, that.triangles )
				&& Objects.equals( vertexNormals, that.vertexNormals ) && Objects.equals( triangleNormals, that.triangleNormals );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( vertices, triangles, vertexNormals, triangleNormals );
	}
}

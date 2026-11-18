package org.litt.geff;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * A single entry of the {@code geff/related_objects} metadata, describing data
 * associated with the graph. The types {@code labels}, {@code image} and
 * {@code geff} have a defined meaning; other types are also allowed.
 * <p>
 * Paths are relative to the geff attributes file, and it is strongly
 * recommended all related objects are stored as siblings of the geff group
 * within the top-level zarr group.
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
 * @author Jean-Yves Tinevez
 */
public class RelatedObject
{

	/**
	 * The type of the related object: {@code labels} for label images,
	 * {@code image} for raw images, {@code geff} for GEFFception (a nested
	 * geff file), or any other application-defined type.
	 */
	@SerializedName( "type" )
	private String type;

	/**
	 * Path of the related object within the zarr group, relative to the geff
	 * attributes file.
	 */
	@SerializedName( "path" )
	private String path;

	/**
	 * The node property that links the related object to the nodes of the
	 * graph. Before geff-spec v1.2.1, this field was called {@code
	 * label_prop}; both names are accepted when reading, and {@code node_prop}
	 * is written.
	 */
	@SerializedName( value = "node_prop", alternate = { "label_prop" } )
	private String nodeProp;

	/**
	 * Default constructor
	 */
	public RelatedObject()
	{}

	/**
	 * Constructor with the required fields.
	 *
	 * @param type
	 *            the type of the related object, e.g. {@code "image"}
	 * @param path
	 *            the path of the related object, relative to the geff
	 *            attributes file
	 */
	public RelatedObject( final String type, final String path )
	{
		this.type = type;
		this.path = path;
	}

	/**
	 * Constructor with all fields.
	 *
	 * @param type
	 *            the type of the related object, e.g. {@code "labels"}
	 * @param path
	 *            the path of the related object, relative to the geff
	 *            attributes file
	 * @param nodeProp
	 *            the node property that links the related object to the nodes
	 *            of the graph
	 */
	public RelatedObject( final String type, final String path, final String nodeProp )
	{
		this.type = type;
		this.path = path;
		this.nodeProp = nodeProp;
	}

	public String getType()
	{
		return type;
	}

	public void setType( final String type )
	{
		this.type = type;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath( final String path )
	{
		this.path = path;
	}

	/**
	 * Returns the node property that links the related object to the nodes of
	 * the graph, or {@code null} if there is none.
	 *
	 * @return the node property name, or {@code null}.
	 */
	public String getNodeProp()
	{
		return nodeProp;
	}

	public void setNodeProp( final String nodeProp )
	{
		this.nodeProp = nodeProp;
	}

	@Override
	public String toString()
	{
		return String.format( "RelatedObject{type=%s, path=%s, nodeProp=%s}", type, path, nodeProp );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof RelatedObject ) )
			return false;
		final RelatedObject that = ( RelatedObject ) o;
		return Objects.equals( type, that.type ) && Objects.equals( path, that.path )
				&& Objects.equals( nodeProp, that.nodeProp );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( type, path, nodeProp );
	}
}

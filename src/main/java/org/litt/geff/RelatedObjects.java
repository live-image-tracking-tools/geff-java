package org.litt.geff;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The collection of {@link RelatedObject} of a geff file, corresponding to the
 * {@code geff/related_objects} metadata. Since it extends {@link ArrayList},
 * it is serialized by Gson as a JSON array of {@link RelatedObject} objects,
 * as required by the GEFF specification.
 * <p>
 * Example:
 *
 * <pre>
 * final RelatedObjects relatedObjects = new RelatedObjects()
 *         .image( "../raw/" )
 *         .labels( "../segmentation/", "seg_id" );
 * metadata.setRelatedObjects( relatedObjects );
 * </pre>
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
 * @author Jean-Yves Tinevez
 */
public final class RelatedObjects extends ArrayList< RelatedObject >
{

	private static final long serialVersionUID = 1L;

	/**
	 * Add a related object of type 'labels' with the specified path and node
	 * property.
	 *
	 * @param path
	 *            Path of the labels within the zarr group, relative to the
	 *            geff zarr-attributes file. It is strongly recommended all
	 *            related objects are stored as siblings of the geff group
	 *            within the top-level zarr group.
	 * @param nodeProp
	 *            Node property that links the labels to the nodes of the
	 *            graph, e.g. the property storing the label value of each
	 *            node.
	 * @return this RelatedObjects instance for method chaining.
	 */
	public RelatedObjects labels( final String path, final String nodeProp )
	{
		add( new RelatedObject( "labels", path, nodeProp ) );
		return this;
	}

	/**
	 * Add a related object of type 'image' with the specified path.
	 *
	 * @param path
	 *            Path of the image within the zarr group, relative to the
	 *            geff zarr-attributes file. It is strongly recommended all
	 *            related objects are stored as siblings of the geff group
	 *            within the top-level zarr group.
	 * @return this RelatedObjects instance for method chaining.
	 */
	public RelatedObjects image( final String path )
	{
		add( new RelatedObject( "image", path ) );
		return this;
	}

	/**
	 * Add a related object of type 'geff' with the specified path, for
	 * GEFFception (a geff file nested within another one).
	 *
	 * @param path
	 *            Path of the nested geff file within the zarr group, relative
	 *            to the geff zarr-attributes file.
	 * @return this RelatedObjects instance for method chaining.
	 */
	public RelatedObjects geff( final String path )
	{
		add( new RelatedObject( "geff", path ) );
		return this;
	}

	/**
	 * Returns all the related objects of this collection.
	 *
	 * @return an unmodifiable view of the related objects.
	 */
	public List< RelatedObject > getObjects()
	{
		return List.copyOf( this );
	}

	/**
	 * Returns all the related objects of type 'image'.
	 *
	 * @return the image related objects.
	 */
	public List< RelatedObject > getImages()
	{
		return stream().filter( obj -> "image".equals( obj.getType() ) ).collect( Collectors.toList() );
	}

	/**
	 * Returns all the related objects of type 'labels'.
	 *
	 * @return the labels related objects.
	 */
	public List< RelatedObject > getLabels()
	{
		return stream().filter( obj -> "labels".equals( obj.getType() ) ).collect( Collectors.toList() );
	}

	/**
	 * Returns all the related objects of type 'geff', for GEFFception.
	 *
	 * @return the geff related objects.
	 */
	public List< RelatedObject > getGeffObjects()
	{
		return stream().filter( obj -> "geff".equals( obj.getType() ) ).collect( Collectors.toList() );
	}

	/**
	 * Returns the paths of all the related objects of type 'image'.
	 *
	 * @return the paths of the image related objects.
	 */
	public List< String > getImagePaths()
	{
		return getImages().stream().map( RelatedObject::getPath ).collect( Collectors.toList() );
	}
}

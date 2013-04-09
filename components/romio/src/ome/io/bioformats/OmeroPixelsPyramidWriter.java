/*
 *   $Id$
 *
 *   Copyright 2011 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.io.bioformats;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.in.TiffReader;
import loci.formats.out.TiffWriter;
import loci.formats.tiff.IFD;
import loci.formats.tiff.TiffCompression;

import ome.xml.model.OME;
import ome.xml.model.Pixels;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.primitives.PositiveInteger;

/**
 * File format writer for OMERO pixels pyramid files.
 * 
 * @author Chris Allan, callan at blackcat dot ca
 * @since Beta4.3
 */
public class OmeroPixelsPyramidWriter extends TiffWriter {

    /** Logger for this class. */
    private final static Log log =
        LogFactory.getLog(OmeroPixelsPyramidWriter.class);

    /** Current TIFF image comment for OMERO pixels pyramid TIFFs. */
    public static final String IMAGE_DESCRIPTION = "OmeroPixelsPyramid v1.0.0";

    /** TIFF tag we're using to store the Bio-Formats series. */
    public static final int IFD_TAG_SERIES = 65000;

    /** TIFF tag we're using to store the Bio-Formats plane number. */
    public static final int IFD_TAG_PLANE_NUMBER = 65001;

    /** Last IFD we used during a tile write operation. */
    private IFD lastIFD;

    /** Last z-section offset we used during a tile write operation. */
    private int lastZ = -1;

    /** Last channel offset we used during a tile write operation. */
    private int lastC = -1;

    /** Last timepoint offset  we used during a tile write operation. */
    private int lastT = -1;

    private IMetadata metadata;


    /* (non-Javadoc)
     * @see loci.formats.out.TiffWriter#close()
     */
    @Override
    public void close() throws IOException
    {
        log.debug("close(" + currentId + ")");
        try
        {
            if (currentId != null)
            {
                postProcess();
            }
        } catch (FormatException e)
        {
            String m = "Error during process processing!";
            log.error(m, e);
            throw new IOException(m);
        } finally
        {
            super.close();
        }
        metadata = null;
    }

    @Override
    public void saveBytes(int planeNumber, byte[] buffer,
      int x, int y, int w, int h)
      throws FormatException, IOException
    {
            String dimOrder = metadata.getPixelsDimensionOrder(0).getValue();
            int sizeZ = metadata.getPixelsSizeZ(0).getValue();
            int sizeC = metadata.getPixelsSizeC(0).getValue();
            int sizeT = metadata.getPixelsSizeT(0).getValue();
            int[] coordinates = FormatTools.getZCTCoords(dimOrder,
              sizeZ, sizeC, sizeT, sizeZ * sizeC * sizeT, planeNumber);
            int z = coordinates[0];
            int c = coordinates[1];
            int t = coordinates[2];
            IFD ifd = getIFD(z, c, t, w, h);
            super.saveBytes(planeNumber, buffer, ifd, x, y, w, h);
    }

    /* (non-Javadoc)
     * @see loci.formats.FormatWriter#setId(java.lang.String)
     */
    @Override
    public void setId(String id) throws FormatException, IOException
    {
        log.debug("setId(" + id + ")");
        super.setId(id);
    }

    /**
     * Performs re-compression post processing on the pixel pyramid.
     * @throws IOException
     * @throws FormatException
     */
    protected void postProcess() throws IOException, FormatException
    {
        TiffReader reader = new TiffReader();
        try
        {
            reader.setId(currentId);
            // First we want to re-compress resolution level 0 (the source series,
            // with resolution levels exposed, are in reverse order).
            //recompressSeries(reader, 1);
            // Second we want to re-compress resolution level 1 (the source series,
            // with resolution levels exposed, are in reverse order).
            //recompressSeries(reader, 2);
        } finally {
            reader.close();
        }
    }

    /**
     * Re-compresses a source series, that is JPEG 2000 compressed, via its
     * resolution level.
     * @param source Reader created of ourselves.
     * @param series Target series for the re-compressed data which is the
     * inverse of the source resolution level.
     * @throws FormatException
     * @throws IOException
     */
    protected void recompressSeries(TiffReader source, int series)
        throws FormatException, IOException
    {
        int sourceSeries = source.getSeriesCount() - series;
        source.setSeries(sourceSeries);
        int imageCount = source.getImageCount();
        setSeries(series);
        for (int i = 0; i < imageCount; i++)
        {
            byte[] plane = source.openBytes(i);
            IFD ifd = new IFD();
            // Ensure that we're compressing all rows of the image in a single
            // JPEG 2000 block.
            ifd.put(IFD.ROWS_PER_STRIP, new long[] { source.getSizeY() });
            // Set the TIFF image description so that we are able to
            // differentiate ourselves from basic TIFFs.
            ifd.put(IFD.IMAGE_DESCRIPTION, IMAGE_DESCRIPTION);
            // First re-usable TIFF IFD (series)
            ifd.put(IFD_TAG_SERIES, sourceSeries - 1);
            // Second re-usable TIFF IFD (plane number)
            ifd.put(IFD_TAG_PLANE_NUMBER, i);
            saveBytes(i, plane, ifd);
        }
    }

    /**
     * Retrieves the IFD that should be used for a given planar offset.
     * @param z Z-section offset requested.
     * @param c Channel offset requested.
     * @param t Timepoint offset requested.
     * @param w Tile width requested.
     * @param h Tile height requested.
     * @return A new or already allocated IFD for use when writing tiles.
     */
    private synchronized IFD getIFD(int z, int c, int t, int w, int h)
    {
        if (lastT != t || lastC != c || lastZ != z)
        {
            lastIFD = new IFD();
            lastIFD.put(IFD.IMAGE_DESCRIPTION, IMAGE_DESCRIPTION);
            lastIFD.put(IFD.TILE_WIDTH, w);
            lastIFD.put(IFD.TILE_LENGTH, h);
            if (log.isDebugEnabled())
            {
                log.debug(String.format(
                        "Creating new IFD z:%d c:%d t:%d w:%d: h:%d -- %s",
                        z, c, t, w, h, lastIFD));
            }
        }
        lastT = t;
        lastC = c;
        lastZ = z;
        return lastIFD;
    }

    /**
     * Creates a new series for the destination metadata store.
     * @param metadata Metadata store and retrieve implementation.
     * @param pixels Source pixels set.
     * @param series Destination series.
     * @param sizeX Destination X width. Not necessarily
     * <code>Pixels.SizeX</code>.
     * @param sizeY Destination Y height. Not necessarily
     * <code>Pixels.SizeY</code>.
     * @throws EnumerationException
     */
    private void createSeries(int series, int sizeX, int sizeY)
        throws EnumerationException
    {
        boolean bigEndian = metadata.getPixelsBinDataBigEndian(0, 0);

        metadata.setImageID("Image:" + series, series);
        metadata.setPixelsID("Pixels: " + series, series);
        metadata.setPixelsBinDataBigEndian(bigEndian, series, 0);
        metadata.setPixelsDimensionOrder(DimensionOrder.XYZCT, series);
        metadata.setPixelsType(metadata.getPixelsType(0), series);
        metadata.setPixelsSizeX(new PositiveInteger(sizeX), series);
        metadata.setPixelsSizeY(new PositiveInteger(sizeY), series);
        int totalPlanes = metadata.getPixelsSizeZ(0).getValue() *
          metadata.getPixelsSizeC(0).getValue() *
          metadata.getPixelsSizeT(0).getValue();
        metadata.setPixelsSizeZ(new PositiveInteger(1), series);
        metadata.setPixelsSizeC(new PositiveInteger(1), series);
        metadata.setPixelsSizeT(new PositiveInteger(totalPlanes), series);
        metadata.setChannelID("Channel:" + series, series, 0);
        metadata.setChannelSamplesPerPixel(new PositiveInteger(1), series, 0);
        if (log.isDebugEnabled())
        {
            log.debug(String.format("Added series %d %dx%dx%d",
                    series, sizeX, sizeY, totalPlanes));
        }
    }

    /**
     * During tile writing, adds additional all series.
     * @param tileWidth Tile width of full resolution tiles.
     * @param tileLength Tile length of full resolution tiles.
     * @throws EnumerationException
     */
    public void setupTileMetadata(int image, int tileWidth, int tileLength)
        throws EnumerationException
    {
        OME root = (OME) ((MetadataStore) metadataRetrieve).getRoot();
        for (int i=image + 1; i<root.sizeOfImageList(); ) {
          root.removeImage(root.getImage(i));
        }
        for (int i=0; i<image; i++) {
          root.removeImage(root.getImage(0));
        }
        Pixels pix = root.getImage(0).getPixels();
        for (int i=0; i<pix.sizeOfChannelList(); ) {
          pix.removeChannel(pix.getChannel(i));
        }

        metadata = MetadataTools.createOMEXMLMetadata();
        metadata.setRoot(root);

        int series = 0;
        for (int level : new int[] { 0, 5, 4 })
        {
            long imageWidth = metadata.getPixelsSizeX(0).getValue();
            long imageLength = metadata.getPixelsSizeY(0).getValue();
            long factor = (long) Math.pow(2, level);
            long newTileWidth = Math.round((double) tileWidth / factor);
            newTileWidth = newTileWidth < 1? 1 : newTileWidth;
            long newTileLength = Math.round((double) tileLength / factor);
            newTileLength = newTileLength < 1? 1: newTileLength;
            long evenTilesPerRow = imageWidth / tileWidth;
            long evenTilesPerColumn = imageLength / tileLength;
            double remainingWidth =
                    ((double) (imageWidth - (evenTilesPerRow * tileWidth))) /
                    factor;
            remainingWidth = remainingWidth < 1? Math.ceil(remainingWidth) :
                Math.round(remainingWidth);
            double remainingLength =
              ((double) imageLength - (evenTilesPerColumn * tileLength)) /
              factor;
            remainingLength = remainingLength < 1? Math.ceil(remainingLength) :
                Math.round(remainingLength);
            int newImageWidth = (int) ((evenTilesPerRow * newTileWidth) +
                remainingWidth);
            int newImageLength = (int) ((evenTilesPerColumn * newTileLength) +
                remainingLength);

            createSeries(series, newImageWidth, newImageLength);
            series++;
        }
        setMetadataRetrieve(metadata);
    }

}

/*
 * blitzgateway.service.ImageService 
 *
  *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package blitzgateway.service;


//Java imports
import java.nio.ByteBuffer;
import java.util.List;

//Third-party libraries

//Application-internal dependencies
import omero.api.RawPixelsStore;
import omero.model.Channel;
import omero.model.Image;
import omero.model.Pixels;
import omero.model.StatsInfo;
import omero.model.StatsInfoHolder;
import omero.model.StatsInfoPrx;

import org.openmicroscopy.shoola.env.data.DSAccessException;
import org.openmicroscopy.shoola.env.data.DSOutOfServiceException;
import org.openmicroscopy.shoola.env.rnd.data.DataSink;
import org.openmicroscopy.shoola.env.rnd.data.Plane2D;

import blitzgateway.service.gateway.IPixelsGateway;
import blitzgateway.service.gateway.IQueryGateway;
import blitzgateway.service.gateway.IUpdateGateway;
import blitzgateway.service.gateway.RawPixelsStoreGateway;
import blitzgateway.util.PixelTypes;

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
class ImageServiceImpl
	implements ImageService
{	
	
	IQueryGateway 			iQuery;
	RawPixelsStoreGateway	pixelsStore;
	IPixelsGateway			iPixels;
	IUpdateGateway			iUpdate;
	
	/**
	 * Create the ImageService passing the gateway.
	 * @param gateway
	 */
	ImageServiceImpl(RawPixelsStoreGateway 	pixelsStore, IPixelsGateway iPixels, 
					IQueryGateway iQuery, IUpdateGateway iUpdate) 
	{
		this.iUpdate = iUpdate;
		this.iQuery = iQuery;
		this.iPixels = iPixels;
		this.pixelsStore = pixelsStore;
	}

	/* (non-Javadoc)
	 * @see blitzgateway.service.ImageService#getRawPlane(long, int, int, int)
	 */
	public double[][] getPlane(long pixelsId, int z, int c, int t) 
		throws DSOutOfServiceException, DSAccessException
	{
		pixelsStore.setPixelsId(pixelsId);
		Pixels pixels = getPixels(pixelsId);
		return convertRawPlane(pixels, z, c, t);
	}
	
	
	/* (non-Javadoc)
	 * @see blitzgateway.service.ImageService#getRawPlane(long, int, int, int)
	 */
	public byte[] getRawPlane(long pixelsId, int z, int c, int t) 
			throws DSOutOfServiceException, DSAccessException
	{
		byte[] plane;
		pixelsStore.setPixelsId(pixelsId);
		plane = pixelsStore.getPlane(z, c, t);
		return plane;
	}
	
	/**
	 * Convert the rawPlane to doubles depending on the endianess and type of
	 * values in the rawplane.
	 * @param pixels The pixels data of the plane.
	 * @param z The z section.
	 * @param c The channel.
	 * @param t The time point.
	 * @return See above.
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	private double[][] convertRawPlane(Pixels pixels, int c, int t, int z) 
		throws DSOutOfServiceException, DSAccessException 
	{
		DataSink sink = DataSink.makeNew(pixels, this);
		Plane2D plane = sink.getPlane(z, t, c);
		return plane.getPixelsArray();
	}

	
	/* (non-Javadoc)
	 * @see blitzgateway.service.ImageService#getPixels(long)
	 */
	public Pixels getPixels(long pixelsId) 
		throws DSOutOfServiceException, DSAccessException
	{
		String queryStr = new String("select p from Pixels as p left outer " +
			"join fetch p.pixelsType as pt "+
			"left outer join fetch p.channels as c "+
			"left outer join fetch p.pixelsDimensions " + 
			"left outer join fetch c.statsInfo " + 
			"left outer join fetch c.colorComponent " + 
			"where p.id = " + pixelsId);
		return (Pixels)iQuery.findByQuery(queryStr);
		
	}

	/* (non-Javadoc)
	 * @see blitzgateway.service.ImageService#getImage(long)
	 */
	public Image getImage(long imageID) 
		throws DSOutOfServiceException, DSAccessException
	{
		String queryStr = new String("select i from Image as i where i.id = " 
			+ imageID);
		return (Image)iQuery.findByQuery(queryStr);
	}

	/* (non-Javadoc)
	 * @see blitzgateway.service.ImageService#copyPixels(long, int, int, int, int, java.lang.String)
	 */
	public Long copyPixels(long pixelsID, int x, int y, int t, int z, List<Integer> channelList,
			String methodology) throws DSOutOfServiceException, DSAccessException
	{
		Long newID = iPixels.copyAndResizePixels
						(pixelsID, x, y, t, z, channelList, methodology);
		return newID;
	}
	
	/**
	 * Upload the plane to the server, on pixels id with channel and the 
	 * time, + z section. the data is the client 2d data values. This will
	 * be converted to the raw server bytes.
	 * @param pixelsId pixels id to upload to .  
	 * @param c channel.
	 * @param t time point.
	 * @param z z section. 
	 * @param data plane data. 
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	public void uploadPlane(long pixelsId, int c, int t, int z, 
			double [][] data) throws DSOutOfServiceException, DSAccessException
	{
		Pixels pixels = getPixels(pixelsId);
		byte[] convertedData = convertClientToServer(pixels, data);
		pixelsStore.setPixelsId(pixelsId);
		pixelsStore.setPlane(convertedData, z,c,t);
	}

	/**
	 * convert the client data pixels to server byte array, also sets the data
	 * pixel size to the size of the pixels in the pixels Id param.
	 * @param pixels the pixels in the server.
	 * @param data the data on the client. 
	 * @return the bytes for server.
	 */
	public byte[] convertClientToServer(Pixels pixels, double [][] data)
	{
		String pixelsType  = pixels.pixelsType.value.val;
		int pixelsSize = getPixelsSize(pixelsType); 
		int sizex = pixels.sizeX.val;
		int sizey = pixels.sizeY.val;
		byte[] rawbytes =  new byte[sizex*sizey*pixelsSize];
		for ( int x = 0 ; x < sizex ; x++)
			for ( int y = 0 ; y < sizey ; y++)
			{
				int offset = calcOffset(pixelsSize, sizex, x, y);
				byte[] newBytes = convertValue(pixelsType, data[x][y]);
				for( int offsetLength = 0 ; offsetLength < newBytes.length ; offsetLength++)
					rawbytes[offset+offsetLength] = newBytes[offsetLength];  
			}
		return rawbytes;
	}
	
	/** 
	 * Determines the offset value.
	 * @param pixelSize pixels size in bytes. 
	 * @param x	The x-coordinate.
	 * @param y	The y-coordinate.
	 * @return See above.
	 */
	private int calcOffset(int pixelSize, int sizex, int x, int y)
	{
		return pixelSize*(y*sizex+x);
	}
	
	/**
	 * Get the pixel size of the pixels.
	 * @param pixelsType see above.
	 * @return the size in bytes. 
	 */
	private int getPixelsSize(String pixelsType)
	{	
		return PixelTypes.pixelMap.get(pixelsType);
	}

	/**
	 * Map the byte data to a byte value 
	 * @param v see above.
	 * @return see above.
	 */
	private byte[] mapToByteArray(byte v) 
	{
		ByteBuffer bb = ByteBuffer.allocate(1);
		bb.put(v);
		return bb.array();
	}

	/**
	 * Map the byte data to a byte value 
	 * @param v see above.
	 * @return see above.
	 */
	private byte[] mapToByteArray(short v) 
	{
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.putShort(v);
		return bb.array();
	}

	/**
	 * Map the byte data to a byte value 
	 * @param v see above.
	 * @return see above.
	 */
	private byte[] mapToByteArray(int v) 
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(v);
		return bb.array();
	}

	/**
	 * Map the byte data to a byte value 
	 * @param v see above.
	 * @return see above.
	 */
	private byte[] mapToByteArray(float v) 
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putFloat(v);
		return bb.array();
	}
	
	/**
	 * Map the byte data to a byte value 
	 * @param v see above.
	 * @return see above.
	 */
	private byte[] mapToByteArray(double v) 
	{
		ByteBuffer bb = ByteBuffer.allocate(8);
		bb.putDouble(v);
		return bb.array();
	}

	/**
	 * Convert the value of the pixel in the client to the server byte value.
	 * @param pixelsType PixelType. 
	 * @param val the value to convert.
	 * @return the converted value.
	 */
	private byte[] convertValue(String pixelsType, Double val)
	{
		if(pixelsType.equals(PixelTypes.INT_8) || pixelsType.equals(PixelTypes.UINT_8))
			return mapToByteArray(val.byteValue());
		else if(pixelsType.equals(PixelTypes.INT_16) || pixelsType.equals(PixelTypes.UINT_16))
			return mapToByteArray(val.shortValue());
		else if(pixelsType.equals(PixelTypes.INT_32) || pixelsType.equals(PixelTypes.UINT_32))
			return mapToByteArray(val.intValue());
		else if(pixelsType.equals(PixelTypes.FLOAT))
			return mapToByteArray(val.floatValue());
		else
			return mapToByteArray(val.doubleValue());
	}
	
	/**
	 * Test method to make sure the client converts it's data to the server data
	 * correctly. 
	 * @param pixelsId pixels id to upload to .  
	 * @param c channel.
	 * @param t time point.
	 * @param z z section.
	 * @return the converted data. 
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException */
	public double[][] testVal(long pixelsId, int z, int c, int t) 
		throws DSOutOfServiceException, DSAccessException
	{
	
		Pixels pixels = getPixels(pixelsId);
		double[][] serverPlane = getPlane(pixelsId, z, c, t);
		byte[] clientBytes = convertClientToServer(pixels, serverPlane);
		return DataSink.mapServerToClient(clientBytes, pixels.sizeX.val, 
			pixels.sizeY.val, pixels.pixelsType.value.val);
	}
	
	/**
	 * update the pixels object, and return the new pixels.
	 * @param object the pixels object.
	 * @return see above.
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException 
	 */
	public Pixels updatePixels(Pixels object) 
	throws DSOutOfServiceException, DSAccessException
	{
	
		return (Pixels)iUpdate.saveAndReturnObject(object);
	}

}



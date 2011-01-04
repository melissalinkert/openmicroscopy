/*
 * org.openmicroscopy.shoola.env.data.model.ImportObject
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.env.data.model;


//Java imports
import java.io.File;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.env.data.util.StatusLabel;

/**
 * Helper class hosting information about the file to import.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class ImportObject
{

	/** The file to import. */
	private File 		file;
	
	/** The object displaying the import status. */
	private StatusLabel status;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param file 		The file to import.
	 * @param status 	The object displaying the import status.
	 */
	public ImportObject(File file, StatusLabel status)
	{
		if (file == null)
			throw new IllegalArgumentException("No file to import.");
		this.file = file;
		this.status = status;
	}
	
	/**
	 * Returns the file to import.
	 * 
	 * @return See above.
	 */
	public File getFile() { return file; }
	
	/**
	 * Returns the component indicating the status of the import.
	 * 
	 * @return See above.
	 */
	public StatusLabel getStatus() { return status; }
	
}
